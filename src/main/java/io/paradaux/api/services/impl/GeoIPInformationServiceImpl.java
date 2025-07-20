package io.paradaux.api.services.impl;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import io.paradaux.api.mappers.GeoIPMapper;
import io.paradaux.api.models.geoip.ASN;
import io.paradaux.api.models.geoip.IPBlock;
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
            importLocations(cityLocationsFile);
        } catch (Exception e) {
            log.error("Failed to import locations", e);
            return; // or handle fail-fast
        }

        CompletableFuture<Void> asnsFuture = CompletableFuture.runAsync(() -> {
            try {
                importASNs(asnBlocksIPv4File, asnBlocksIPv6File);
            } catch (Exception e) {
                log.error("Failed to import ASN", e);
            }
        });

        try {
            importIPBlocks(cityBlocksIPv4File, cityBlocksIPv6File);
        } catch (Exception e) {
            log.error("Failed to import IP blocks", e);
        }

        asnsFuture.join();
        log.info("All GeoIP data imported");
    }

    private void importLocations(String csvPath) throws IOException, CsvValidationException {
        List<IPLocation> locations = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
            reader.readNext(); // skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                IPLocation loc = new IPLocation();
                loc.setGeonameId(parseIntOrNull(line[0]));
                loc.setLocaleCode(line[1]);
                loc.setContinentCode(line[2]);
                loc.setContinentName(line[3]);
                loc.setCountryIsoCode(line[4]);
                loc.setCountryName(line[5]);
                loc.setSubdivision1IsoCode(line[6]);
                loc.setSubdivision1Name(line[7]);
                loc.setSubdivision2IsoCode(line[8]);
                loc.setSubdivision2Name(line[9]);
                loc.setCityName(line[10]);
                loc.setMetroCode(line[11]);
                loc.setTimeZone(line[12]);
                loc.setIsInEuropeanUnion("1".equals(line[13]));
                locations.add(loc);
            }
        }
        batchInsert(locations, geoIPMapper::insertLocations);
    }

    private void importASNs(String... csvPaths) throws IOException, CsvValidationException {
        Map<Integer, ASN> uniqueAsns = new HashMap<>();

        for (String csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
                reader.readNext(); // skip header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    Integer asnNumber = parseIntOrNull(line[1]);
                    if (asnNumber == null || uniqueAsns.containsKey(asnNumber)) continue;

                    ASN asn = new ASN();
                    asn.setAutonomousSystemNumber(asnNumber);
                    asn.setAutonomousSystemOrganization(line[2]);
                    uniqueAsns.put(asnNumber, asn);
                }
            }
        }

        batchInsert(new ArrayList<>(uniqueAsns.values()), geoIPMapper::insertASNs);
    }

    private void importIPBlocks(String... csvPaths) throws IOException, CsvValidationException {
        List<IPBlock> blocks = new ArrayList<>();

        for (String csvPath : csvPaths) {
            try (CSVReader reader = new CSVReader(new FileReader(csvPath))) {
                reader.readNext(); // skip header
                String[] line;
                while ((line = reader.readNext()) != null) {
                    IPBlock block = new IPBlock();
                    block.setNetwork(line[0]);
                    block.setGeonameId(parseIntOrNull(line[1]));
                    block.setRegisteredCountryGeonameId(parseIntOrNull(line[2]));
                    block.setRepresentedCountryGeonameId(parseIntOrNull(line[3]));
                    block.setIsAnonymousProxy("1".equals(line[4]));
                    block.setIsSatelliteProvider("1".equals(line[5]));
                    block.setPostalCode(line[6]);
                    block.setLatitude(parseDoubleOrNull(line[7]));
                    block.setLongitude(parseDoubleOrNull(line[8]));
                    block.setAccuracyRadius(parseIntOrNull(line[9]));
                    block.setIsAnycast("1".equals(line[10]));
                    blocks.add(block);
                }
            }
        }

        batchInsert(blocks, geoIPMapper::insertIPBlocks);
    }

    private <T> void batchInsert(List<T> list, Consumer<List<T>> insertFunction) {
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, list.size());
            insertFunction.accept(list.subList(i, end));
        }
    }

    private Integer parseIntOrNull(String s) {
        try { return s == null || s.isBlank() ? null : Integer.parseInt(s); }
        catch (NumberFormatException e) { return null; }
    }

    private Double parseDoubleOrNull(String s) {
        try { return s == null || s.isBlank() ? null : Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }
}