package io.paradaux.api.models.geoip;

import lombok.Data;

@Data
public class ASN {
    private Integer autonomousSystemNumber;
    private String autonomousSystemOrganization;
}
