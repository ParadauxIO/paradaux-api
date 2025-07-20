package io.paradaux.api.controllers;

import io.paradaux.api.jobs.MaxMindSyncJob;
import io.paradaux.api.models.annotations.ProtectedRoute;
import io.paradaux.api.services.GeoIPInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static io.paradaux.api.utils.FileUtils.extractZip;

@RestController
@RequestMapping("/api/geoip")
@Validated
@Slf4j
@RequiredArgsConstructor
public class GeoIPController {

    private final GeoIPInformationService geoIPInformationService;
    private final MaxMindSyncJob maxMindSyncJob;

    @GetMapping("/lookup/{ipAddress}")
    public Map<String, Object> lookup(@PathVariable String ipAddress) {
        return geoIPInformationService.lookupIP(ipAddress);
    }

    @PostMapping("/sync")
    @ProtectedRoute
    public ResponseEntity<String> syncGeoIPData() {
        maxMindSyncJob.runSync();
        return ResponseEntity.accepted().body("MaxMind sync started");
    }
}
