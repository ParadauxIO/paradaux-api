package io.paradaux.api.services;

import io.paradaux.api.models.geoip.CityBlock;
import io.paradaux.api.models.geoip.IPLocation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public interface GeoIPInformationService {

    void importAllData() throws IOException;
    void importAllData(Path dataDir) throws IOException;
    Map<String, Object> getIPDetails(String ipAddress);
    CityBlock getCityBlock(String ipAddress);
    Map<String, Object> getASNDetails(String ipAddress);
    IPLocation getLocationByGeoNameId(Integer geonameId);
    Map<String, Object> lookupIP(String ipAddress);
}
