package io.paradaux.api.models.geoip;

import lombok.Data;

@Data
public class IPLocation {
    private Integer geonameId;
    private String localeCode;
    private String continentCode;
    private String continentName;
    private String countryIsoCode;
    private String countryName;
    private String subdivision1IsoCode;
    private String subdivision1Name;
    private String subdivision2IsoCode;
    private String subdivision2Name;
    private String cityName;
    private String metroCode;
    private String timeZone;
    private Boolean isInEuropeanUnion;
}
