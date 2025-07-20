package io.paradaux.api.controllers;

import com.opencsv.exceptions.CsvValidationException;
import io.paradaux.api.models.annotations.ProtectedRoute;
import io.paradaux.api.services.GeoIPInformationService;
import io.paradaux.api.services.impl.GeoIPInformationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/geoip")
@Validated
@Slf4j
@RequiredArgsConstructor
public class GeoIPController {

    private final GeoIPInformationService geoIPInformationService;

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
