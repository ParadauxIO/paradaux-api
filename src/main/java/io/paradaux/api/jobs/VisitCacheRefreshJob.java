package io.paradaux.api.jobs;

import io.paradaux.api.mappers.VisitsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class VisitCacheRefreshJob {

    private final VisitsMapper visitsMapper;

    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 mins
    public void refreshMaterializedView() {
        log.info("Starting visit cache refresh...");
        visitsMapper.refreshVisitCache();
        log.info("Visit cache refreshed successfully.");

    }
}
