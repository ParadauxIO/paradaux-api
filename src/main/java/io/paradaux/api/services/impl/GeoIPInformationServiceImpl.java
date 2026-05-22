package io.paradaux.api.services.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.paradaux.api.mappers.GeoIPMapper;
import io.paradaux.api.models.geoip.ASN;
import io.paradaux.api.models.geoip.ASNBlock;
import io.paradaux.api.models.geoip.CityBlock;
import io.paradaux.api.models.geoip.IPLocation;
import io.paradaux.api.services.GeoIPInformationService;
import io.paradaux.api.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoIPInformationServiceImpl implements GeoIPInformationService {

    /**
     * Rows per multi-row INSERT. Kept conservative so (BATCH_SIZE * widest table's
     * column count) stays well under Postgres' 65535 bind-parameter limit
     * (location is the widest at 14 cols -> 14k params). Rows are flushed in
     * batches AS the CSV is streamed, so heap stays flat regardless of file size.
     */
    private static final int BATCH_SIZE = 1000;

    private final GeoIPMapper geoIPMapper;

    @Value("${maxmind.license-key}")
    private String maxMindLicenseKey;

    @Value("${maxmind.user-id}")
    private String maxMindUserId;

    private static final String MAXMIND_DOWNLOAD_URL = "https://download.maxmind.com/geoip/databases/GeoLite2-%s-CSV/download?suffix=zip";

    // ── Lookups ────────────────────────────────────────────────────────────────

    @Override
    public Map<String, Object> lookupIP(String ipAddress) {
        Map<String, Object> result = new HashMap<>();

        CityBlock cityBlock = getCityBlock(ipAddress);
        result.put("ip", ipAddress);
        result.put("details", getIPDetails(ipAddress));
        result.put("asn", getASNDetails(ipAddress));
        result.put("city", cityBlock);
        result.put("location", cityBlock != null ? geoIPMapper.getLocationById(cityBlock.getGeonameId()) : null);
        result.put("attribution", "This database incorporates GeoNames [https://www.geonames.org] geographical data, which is made available under the Creative Commons Attribution 4.0 License. To view a copy of this license, visit https://creativecommons.org/licenses/by/4.0");
        return result;
    }

    @Override
    public Map<String, Object> getIPDetails(String ipAddress) {
        return geoIPMapper.getIPInfo(ipAddress);
    }

    @Override
    public CityBlock getCityBlock(String ipAddress) {
        return geoIPMapper.getCityBlockByIP(ipAddress);
    }

    @Override
    public Map<String, Object> getASNDetails(String ipAddress) {
        return geoIPMapper.getASNByIP(ipAddress);
    }

    @Override
    public IPLocation getLocationByGeoNameId(Integer geonameId) {
        return geoIPMapper.getLocationById(geonameId);
    }

    // ── Import ──────────────────────────────────────────────────────────────────

    public void importAllData() throws IOException {
        Path dataDir = downloadAllData();
        try {
            importAllData(dataDir);
        } finally {
            // The MaxMind zips/CSVs are hundreds of MB; never leak them into the pod's
            // ephemeral storage between weekly runs. Don't let cleanup mask a real failure.
            try {
                FileSystemUtils.deleteRecursively(dataDir);
            } catch (IOException e) {
                log.warn("Failed to clean up temp GeoIP dir {}", dataDir, e);
            }
        }
    }

    /**
     * Full refresh of the geoip schema. Loads in FK order (location before
     * city_block, autonomous_system before asn_block) and streams each CSV so the
     * 5.7M-row city-blocks file no longer materialises in heap. Any failure
     * propagates (the caller alerts + the run is retried) rather than being
     * swallowed into a misleading "success".
     */
    public void importAllData(Path dataDir) throws IOException {
        log.info("Starting GeoIP import from {}", dataDir);
        geoIPMapper.truncateAll();

        try {
            // 1) Locations first — city_block.geoname_id FKs to it. Remember which
            //    geoname_ids actually exist so we can null out dangling refs below.
            Set<Integer> validGeonames = new HashSet<>();
            int locations = streamInsert(14, geoIPMapper::insertLocations, line -> {
                IPLocation loc = new IPLocation();
                loc.setGeonameId(parseIntOrNull(line[0]));
                loc.setLocaleCode(parseStringOrNull(line[1]));
                loc.setContinentCode(parseStringOrNull(line[2]));
                loc.setContinentName(parseStringOrNull(line[3]));
                loc.setCountryIsoCode(parseStringOrNull(line[4]));
                loc.setCountryName(parseStringOrNull(line[5]));
                loc.setSubdivision1IsoCode(parseStringOrNull(line[6]));
                loc.setSubdivision1Name(parseStringOrNull(line[7]));
                loc.setSubdivision2IsoCode(parseStringOrNull(line[8]));
                loc.setSubdivision2Name(parseStringOrNull(line[9]));
                loc.setCityName(parseStringOrNull(line[10]));
                loc.setMetroCode(parseStringOrNull(line[11]));
                loc.setTimeZone(parseStringOrNull(line[12]));
                loc.setIsInEuropeanUnion("1".equals(line[13]));
                if (loc.getGeonameId() != null) validGeonames.add(loc.getGeonameId());
                return loc;
            }, dataDir.resolve("city/GeoLite2-City-Locations-en.csv"));

            // 2) Unique ASNs (small — ~tens of thousands) before asn_block (FK).
            int asns = importAsns(
                    dataDir.resolve("asn/GeoLite2-ASN-Blocks-IPv4.csv"),
                    dataDir.resolve("asn/GeoLite2-ASN-Blocks-IPv6.csv"));

            // 3) ASN blocks (streamed).
            int asnBlocks = streamInsert(3, geoIPMapper::insertASNBlocks, line -> {
                String network = parseStringOrNull(line[0]);
                Integer asnNumber = parseIntOrNull(line[1]);
                if (network == null || asnNumber == null) return null;
                ASNBlock block = new ASNBlock();
                block.setNetwork(network);
                block.setAutonomousSystemNumber(asnNumber);
                return block;
            }, dataDir.resolve("asn/GeoLite2-ASN-Blocks-IPv4.csv"),
               dataDir.resolve("asn/GeoLite2-ASN-Blocks-IPv6.csv"));

            // 4) City blocks (the big one, ~5.7M) — streamed. Drop a geoname_id that
            //    isn't in the locations file so a stray ref can't fail a whole batch.
            int cityBlocks = streamInsert(11, geoIPMapper::insertCityBlocks, line -> {
                String network = parseStringOrNull(line[0]);
                if (network == null) return null; // network is the PK
                CityBlock block = new CityBlock();
                block.setNetwork(network);
                Integer geonameId = parseIntOrNull(line[1]);
                block.setGeonameId(geonameId != null && validGeonames.contains(geonameId) ? geonameId : null);
                block.setRegisteredCountryGeonameId(parseIntOrNull(line[2]));
                block.setRepresentedCountryGeonameId(parseIntOrNull(line[3]));
                block.setIsAnonymousProxy("1".equals(line[4]));
                block.setIsSatelliteProvider("1".equals(line[5]));
                block.setPostalCode(parseStringOrNull(line[6]));
                block.setLatitude(parseDoubleOrNull(line[7]));
                block.setLongitude(parseDoubleOrNull(line[8]));
                block.setAccuracyRadius(parseIntOrNull(line[9]));
                block.setIsAnycast("1".equals(line[10]));
                return block;
            }, dataDir.resolve("city/GeoLite2-City-Blocks-IPv4.csv"),
               dataDir.resolve("city/GeoLite2-City-Blocks-IPv6.csv"));

            log.info("GeoIP import complete: {} locations, {} ASNs, {} ASN blocks, {} city blocks",
                    locations, asns, asnBlocks, cityBlocks);
        } catch (CsvValidationException e) {
            throw new IOException("Failed parsing a GeoIP CSV", e);
        }
    }

    /** Collects the unique ASNs across the given files into a small map and inserts them. */
    private int importAsns(Path... csvPaths) throws IOException, CsvValidationException {
        Map<Integer, ASN> uniqueAsns = new HashMap<>();
        for (Path csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile()))) {
                reader.readNext(); // header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < 3) continue;
                    Integer asnNumber = parseIntOrNull(line[1]);
                    if (asnNumber == null || uniqueAsns.containsKey(asnNumber)) continue;
                    ASN asn = new ASN();
                    asn.setAutonomousSystemNumber(asnNumber);
                    asn.setAutonomousSystemOrganization(parseStringOrNull(line[2]));
                    uniqueAsns.put(asnNumber, asn);
                }
            }
        }
        insertInBatches(new ArrayList<>(uniqueAsns.values()), geoIPMapper::insertASNs);
        return uniqueAsns.size();
    }

    /**
     * Streams one or more CSVs, mapping each row and flushing to the DB every
     * BATCH_SIZE rows so heap usage stays flat. Rows the mapper returns null for
     * (or that are too short) are skipped. Insert failures propagate.
     */
    private <T> int streamInsert(int minColumns, Consumer<List<T>> insert,
                                 Function<String[], T> mapper, Path... csvPaths)
            throws IOException, CsvValidationException {
        int inserted = 0;
        int skipped = 0;
        List<T> batch = new ArrayList<>(BATCH_SIZE);
        for (Path csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile()))) {
                reader.readNext(); // header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < minColumns) { skipped++; continue; }
                    T row = mapper.apply(line);
                    if (row == null) { skipped++; continue; }
                    batch.add(row);
                    if (batch.size() >= BATCH_SIZE) {
                        insert.accept(batch);
                        inserted += batch.size();
                        batch.clear();
                    }
                }
            }
        }
        if (!batch.isEmpty()) {
            insert.accept(batch);
            inserted += batch.size();
        }
        if (skipped > 0) log.warn("Skipped {} malformed/incomplete rows across {} file(s)", skipped, csvPaths.length);
        return inserted;
    }

    /** Inserts an in-memory list in BATCH_SIZE chunks (used only for the small ASN set). */
    private <T> void insertInBatches(List<T> list, Consumer<List<T>> insert) {
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            insert.accept(list.subList(i, Math.min(i + BATCH_SIZE, list.size())));
        }
    }

    private Path downloadAllData() throws IOException {
        Path dataDir = Files.createTempDirectory("geoip-");
        Path cityDir = dataDir.resolve("city");
        Path asnDir = dataDir.resolve("asn");

        FileUtils.downloadAndExtractZip(String.format(MAXMIND_DOWNLOAD_URL, "City"), cityDir, maxMindUserId, maxMindLicenseKey);
        FileUtils.downloadAndExtractZip(String.format(MAXMIND_DOWNLOAD_URL, "ASN"), asnDir, maxMindUserId, maxMindLicenseKey);
        return dataDir;
    }

    // ── Parsing helpers ─────────────────────────────────────────────────────────

    private Integer parseIntOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String parseStringOrNull(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        return s.trim();
    }
}
