package io.paradaux.api.controllers;

import io.paradaux.api.models.annotations.ProtectedRoute;
import io.paradaux.api.services.GeoIPInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/geoip")
@Validated
@Slf4j
@RequiredArgsConstructor
public class GeoIPController {

    private final GeoIPInformationService geoIPInformationService;

    @GetMapping("/lookup/{ipAddress}")
    public Map<String, Object> lookup(@PathVariable String ipAddress) {
        return geoIPInformationService.lookupIP(ipAddress);
    }

    @PostMapping("/sync")
    @ProtectedRoute
    public void syncGeoIPData() {
        try {
            log.info("Starting GeoIP data synchronization...");
            geoIPInformationService.importAllData();
        } catch (IOException e) {
            log.error("Failed to import GeoIP data", e);
        }
    }
}
