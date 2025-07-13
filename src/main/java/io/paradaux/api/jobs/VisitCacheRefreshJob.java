package io.paradaux.api.jobs;

import io.paradaux.api.mappers.VisitsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class VisitCacheRefreshJob {

    private final VisitsMapper visitsMapper;

    public VisitCacheRefreshJob(VisitsMapper visitsMapper) {
        this.visitsMapper = visitsMapper;
    }

    @Scheduled(fixedRate = 5 * 60 * 1000) // 5 mins
    public void refreshMaterializedView() {
        visitsMapper.refreshVisitCache();
        log.info("Visit cache refreshed successfully.");
    }
}
