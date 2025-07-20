package io.paradaux.api.models.geoip;

import lombok.Data;

@Data
public class ASNBlock {
    private String network;
    private Integer autonomousSystemNumber;
}
