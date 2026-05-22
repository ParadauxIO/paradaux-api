package io.paradaux.api.jobs;

import io.paradaux.api.services.DiscordService;
import io.paradaux.api.services.GeoIPInformationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class MaxMindSyncJob {

    private final GeoIPInformationService geoIPInformationService;
    private final DiscordService discordService;

    @Scheduled(cron = "0 0 3 * * WED")
    public void refreshMaxMindDbScheduled() {
        runSync();
    }

    @Async
    public void runSync() {
        log.info("Starting MaxMind GeoIP data synchronization...");
        discordService.sendMessage("Starting MaxMind GeoIP data synchronization...", "", new HashMap<>());

        try {
            geoIPInformationService.importAllData();
        } catch (Exception e) {
            // Catch Exception (not just IOException) — mybatis surfaces DB failures as
            // unchecked exceptions, and those must still alert + abort, not be lost.
            log.error("Failed to import MaxMind GeoIP data", e);
            discordService.sendMessage("MaxMind GeoIP data synchronization failure",
                    String.valueOf(e.getMessage()), new HashMap<>());
            return;
        }

        discordService.sendMessage("MaxMind GeoIP data synchronization completed successfully", "", new HashMap<>());
        log.info("MaxMind GeoIP data synchronization completed successfully.");
    }
}
