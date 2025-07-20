package io.paradaux.api.models.geoip;

import lombok.Data;

@Data
public class IPBlock {
    private String network;
    private Integer geonameId;
    private Integer registeredCountryGeonameId;
    private Integer representedCountryGeonameId;
    private Boolean isAnonymousProxy;
    private Boolean isSatelliteProvider;
    private String postalCode;
    private Double latitude;
    private Double longitude;
    private Integer accuracyRadius;
    private Boolean isAnycast;
    private Integer autonomousSystemNumber;
}
