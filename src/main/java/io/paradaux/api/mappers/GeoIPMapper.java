package io.paradaux.api.mappers;

import io.paradaux.api.models.geoip.ASN;
import io.paradaux.api.models.geoip.IPBlock;
import io.paradaux.api.models.geoip.IPLocation;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface GeoIPMapper {
    void insertLocations(List<IPLocation> locations);
    void insertASNs(List<ASN> asns);
    void insertIPBlocks(List<IPBlock> blocks);
}
