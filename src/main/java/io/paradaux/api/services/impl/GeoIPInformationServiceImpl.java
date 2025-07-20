package io.paradaux.api.services.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.paradaux.api.mappers.GeoIPMapper;
import io.paradaux.api.models.geoip.ASN;
import io.paradaux.api.models.geoip.ASNBlock;
import io.paradaux.api.models.geoip.CityBlock;
import io.paradaux.api.models.geoip.IPLocation;
import io.paradaux.api.services.GeoIPInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.IOException;
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

    String cityLocationsFile = "C:\\Workspace\\data-wrangling\\geoip\\GeoLite2-City-CSV_20250718\\GeoLite2-City-Locations-en.csv";
    String asnBlocksIPv4File = "C:\\Workspace\\data-wrangling\\geoip\\GeoLite2-ASN-CSV_20250720\\GeoLite2-ASN-Blocks-IPv4.csv";
    String asnBlocksIPv6File = "C:\\Workspace\\data-wrangling\\geoip\\GeoLite2-ASN-CSV_20250720\\GeoLite2-ASN-Blocks-IPv6.csv";
    String cityBlocksIPv4File = "C:\\Workspace\\data-wrangling\\geoip\\GeoLite2-City-CSV_20250718\\GeoLite2-City-Blocks-IPv4.csv";
    String cityBlocksIPv6File = "C:\\Workspace\\data-wrangling\\geoip\\GeoLite2-City-CSV_20250718\\GeoLite2-City-Blocks-IPv6.csv";

    public void importAllData() {
        try {
            // Import locations first
            importLocations(cityLocationsFile);

            // Import ASNs and ASN blocks in parallel with city blocks
            CompletableFuture<Void> asnFuture = CompletableFuture.runAsync(() -> {
                try {
                    importASNsAndBlocks(asnBlocksIPv4File, asnBlocksIPv6File);
                } catch (Exception e) {
                    log.error("Failed to import ASN data", e);
                }
            });

            CompletableFuture<Void> cityFuture = CompletableFuture.runAsync(() -> {
                try {
                    importCityBlocks(cityBlocksIPv4File, cityBlocksIPv6File);
                } catch (Exception e) {
                    log.error("Failed to import city blocks", e);
                }
            });

            // Wait for both to complete
            CompletableFuture.allOf(asnFuture, cityFuture).join();

            log.info("All GeoIP data imported successfully");
        } catch (Exception e) {
            log.error("Failed to import locations", e);
        }
    }

    private void importLocations(String csvPath) throws IOException, CsvValidationException {
        List<IPLocation> locations = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
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
        log.info("Imported {} locations", locations.size());
        batchInsert(locations, geoIPMapper::insertLocations);
    }

    private void importASNsAndBlocks(String... csvPaths) throws IOException, CsvValidationException {
        Map<Integer, ASN> uniqueAsns = new HashMap<>();
        List<ASNBlock> asnBlocks = new ArrayList<>();

        for (String csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
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

        log.info("Imported {} unique ASNs and {} ASN blocks", uniqueAsns.size(), asnBlocks.size());

        // Insert ASNs first (due to foreign key constraint)
        batchInsert(new ArrayList<>(uniqueAsns.values()), geoIPMapper::insertASNs);

        // Then insert ASN blocks
        batchInsert(asnBlocks, geoIPMapper::insertASNBlocks);
    }

    private void importCityBlocks(String... csvPaths) throws IOException, CsvValidationException {
        List<CityBlock> cityBlocks = new ArrayList<>();

        for (String csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
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

        log.info("Imported {} city blocks", cityBlocks.size());
        batchInsert(cityBlocks, geoIPMapper::insertCityBlocks);
    }

    private <T> void batchInsert(List<T> list, Consumer<List<T>> insertFunction) {
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, list.size());
            List<T> batch = list.subList(i, end);
            try {
                insertFunction.accept(batch);
                if (i % (BATCH_SIZE * 10) == 0) {
                    log.info("Processed {} records", i + batch.size());
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