package io.paradaux.api.mappers;

import io.paradaux.api.models.geoip.ASN;
import io.paradaux.api.models.geoip.ASNBlock;
import io.paradaux.api.models.geoip.CityBlock;
import io.paradaux.api.models.geoip.IPLocation;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface GeoIPMapper {
    void insertLocations(List<IPLocation> locations);
    void insertASNs(List<ASN> asns);
    void insertCityBlocks(List<CityBlock> cityBlocks);
    void insertASNBlocks(List<ASNBlock> asnBlocks);

    Map<String, Object> getIPInfo(String ipAddress);
    CityBlock getCityBlockByIP(String ipAddress);
    Map<String, Object> getASNByIP(String ipAddress);
    IPLocation getLocationById(Integer geonameId);
}