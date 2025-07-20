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

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoIPInformationServiceImpl implements GeoIPInformationService {

    private static final int BATCH_SIZE = 1000;

    private final GeoIPMapper geoIPMapper;

    @Value("${maxmind.license-key}")
    private String maxMindLicenseKey;

    @Value("${maxmind.user-id}")
    private String maxMindUserId;

    private static final String MAXMIND_DOWNLOAD_URL = "https://download.maxmind.com/geoip/databases/GeoLite2-%s-CSV/download?suffix=zip";

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

    public void importAllData() throws IOException {
        // Download zips from MaxMind
        Path dataDir = downloadAllData();

        // Process and import data from MaxMind
        importAllData(dataDir);
    }

    public void importAllData(Path dataDir) {
        geoIPMapper.truncateAll();

        try {
            // Import locations first - use Path instead of String
            importLocations(dataDir.resolve("city/GeoLite2-City-Locations-en.csv"));

            // Import ASNs and ASN blocks in parallel with city blocks
            CompletableFuture<Void> asnFuture = CompletableFuture.runAsync(() -> {
                try {
                    importASNsAndBlocks(
                            dataDir.resolve("asn/GeoLite2-ASN-Blocks-IPv4.csv"),
                            dataDir.resolve("asn/GeoLite2-ASN-Blocks-IPv6.csv")
                    );
                } catch (Exception e) {
                    log.error("Failed to import ASN data", e);
                }
            });

            CompletableFuture<Void> cityFuture = CompletableFuture.runAsync(() -> {
                try {
                    importCityBlocks(
                            dataDir.resolve("city/GeoLite2-City-Blocks-IPv4.csv"),
                            dataDir.resolve("city/GeoLite2-City-Blocks-IPv6.csv")
                    );
                } catch (Exception e) {
                    log.error("Failed to import city blocks", e);
                }
            });

            // Wait for both to complete
            CompletableFuture.allOf(asnFuture, cityFuture).join();
            log.info("All GeoIP data imported successfully from: {}", dataDir);

        } catch (Exception e) {
            log.error("Failed to import GeoIP data from: {}", dataDir, e);
        }
    }

    private Path downloadAllData() throws IOException {
        Path dataDir = Files.createTempDirectory("geoip-");
        Path cityDir = dataDir.resolve("city");
        Path asnDir = dataDir.resolve("asn");

        String url = String.format(MAXMIND_DOWNLOAD_URL, "City");
        FileUtils.downloadAndExtractZip(url, cityDir, maxMindUserId, maxMindLicenseKey);
        url = String.format(MAXMIND_DOWNLOAD_URL, "ASN");
        FileUtils.downloadAndExtractZip(url, asnDir,  maxMindUserId, maxMindLicenseKey);
        return dataDir;
    }

    // Update method signatures to accept Path instead of String
    private void importLocations(Path csvPath) throws IOException, CsvValidationException {
        List<IPLocation> locations = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile()))) {
            reader.readNext(); // skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 14) {
                    log.warn("Skipping location row with insufficient columns: {}", line.length);
                    continue;
                }

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
                locations.add(loc);
            }
        }
        log.debug("Imported {} locations", locations.size());
        batchInsert(locations, geoIPMapper::insertLocations);
    }

    private void importASNsAndBlocks(Path... csvPaths) throws IOException, CsvValidationException {
        Map<Integer, ASN> uniqueAsns = new HashMap<>();
        List<ASNBlock> asnBlocks = new ArrayList<>();

        for (Path csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile()))) {
                reader.readNext(); // skip header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < 3) {
                        log.warn("Skipping ASN row with insufficient columns: {}", line.length);
                        continue;
                    }

                    String network = parseStringOrNull(line[0]);
                    Integer asnNumber = parseIntOrNull(line[1]);
                    String asnOrg = parseStringOrNull(line[2]);

                    if (network == null || asnNumber == null) {
                        continue;
                    }

                    // Store unique ASN
                    if (!uniqueAsns.containsKey(asnNumber)) {
                        ASN asn = new ASN();
                        asn.setAutonomousSystemNumber(asnNumber);
                        asn.setAutonomousSystemOrganization(asnOrg);
                        uniqueAsns.put(asnNumber, asn);
                    }

                    // Store ASN block
                    ASNBlock asnBlock = new ASNBlock();
                    asnBlock.setNetwork(network);
                    asnBlock.setAutonomousSystemNumber(asnNumber);
                    asnBlocks.add(asnBlock);
                }
            }
        }

        log.debug("Imported {} unique ASNs and {} ASN blocks", uniqueAsns.size(), asnBlocks.size());

        // Insert ASNs first (due to foreign key constraint)
        batchInsert(new ArrayList<>(uniqueAsns.values()), geoIPMapper::insertASNs);

        // Then insert ASN blocks
        batchInsert(asnBlocks, geoIPMapper::insertASNBlocks);
    }

    private void importCityBlocks(Path... csvPaths) throws IOException, CsvValidationException {
        List<CityBlock> cityBlocks = new ArrayList<>();

        for (Path csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath.toFile()))) {
                reader.readNext(); // skip header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    if (line.length < 11) {
                        log.warn("Skipping city block row with insufficient columns: {}", line.length);
                        continue;
                    }

                    CityBlock block = new CityBlock();
                    block.setNetwork(parseStringOrNull(line[0]));
                    block.setGeonameId(parseIntOrNull(line[1]));
                    block.setRegisteredCountryGeonameId(parseIntOrNull(line[2]));
                    block.setRepresentedCountryGeonameId(parseIntOrNull(line[3]));
                    block.setIsAnonymousProxy("1".equals(line[4]));
                    block.setIsSatelliteProvider("1".equals(line[5]));
                    block.setPostalCode(parseStringOrNull(line[6]));
                    block.setLatitude(parseDoubleOrNull(line[7]));
                    block.setLongitude(parseDoubleOrNull(line[8]));
                    block.setAccuracyRadius(parseIntOrNull(line[9]));
                    block.setIsAnycast("1".equals(line[10]));

                    cityBlocks.add(block);
                }
            }
        }

        log.debug("Imported {} city blocks", cityBlocks.size());
        batchInsert(cityBlocks, geoIPMapper::insertCityBlocks);
    }

    private <T> void batchInsert(List<T> list, Consumer<List<T>> insertFunction) {
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, list.size());
            List<T> batch = list.subList(i, end);
            try {
                insertFunction.accept(batch);
                if (i % (BATCH_SIZE * 10) == 0) {
                    log.debug("Processed {} records", i + batch.size());
                }
            } catch (Exception e) {
                log.error("Failed to insert batch starting at index {}", i, e);
            }
        }
    }

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