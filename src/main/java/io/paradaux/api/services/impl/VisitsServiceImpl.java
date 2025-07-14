package io.paradaux.api.services.impl;

import io.paradaux.api.mappers.VisitsMapper;
import io.paradaux.api.models.VisitCountEntry;
import io.paradaux.api.models.VisitEntry;
import io.paradaux.api.services.VisitsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Slf4j
@Service
public class VisitsServiceImpl implements VisitsService {

    private static final TreeSet<String> IGNORED_USER_AGENTS = new TreeSet<>(Arrays.asList(
            "bot", "crawler", "spider", "scanner", "curl", "wget",
            "python", "java", "php", "unknown", "discord", "(compatible;",
            "zgrab", "scrapy", "censys", "okhttp", "axios", "go-http-client",
            "google", "bing", "yahoo", "WhatCMS"
    ));

    private final VisitsMapper visitsMapper;

    public VisitsServiceImpl(VisitsMapper visitsMapper) {
        this.visitsMapper = visitsMapper;
    }

    @CacheEvict(value = "visitCounts", key = "#visit.project")
    @Override
    public void insertVisit(VisitEntry visit) {
        String userAgent = Optional.ofNullable(visit.getUserAgent())
                .map(String::toLowerCase)
                .orElse("unknown");

        // If the user agent matches any of the ignored partial agents, skip the insert
        if (IGNORED_USER_AGENTS.stream().anyMatch(userAgent::contains)) {
            log.warn("Blocked bot visit for project: {} with user agent: {}", visit.getProject(), userAgent);
            return;
        }

        log.info("Attempting to inserting seemingly valid visit for: {}", visit.getProject());
        visitsMapper.insertVisit(visit);
    }

    @Cacheable(value = "visitCounts", key = "#project")
    @Override
    public Integer getVisitCount(String project) {
        return visitsMapper.getVisitCount(project);
    }

    @Override
    public List<VisitCountEntry> getVisitCountsForProjects() {
        return visitsMapper.getVisitCountsForProjects();
    }

    @CacheEvict(value = "visitCounts", allEntries = true)
    @Override
    public void refreshVisitCache() {
        visitsMapper.refreshVisitCache();
    }
}
