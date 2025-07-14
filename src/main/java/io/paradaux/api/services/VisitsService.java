package io.paradaux.api.services;

import io.paradaux.api.models.VisitCountEntry;
import io.paradaux.api.models.VisitEntry;

import java.util.List;

public interface VisitsService {
    /**
     * Inserts a new IfumVisit record into the database.
     */
    void insertVisit(VisitEntry visit);

    /**
     * Retrieves the total count of IfumVisit records.
     *
     * @return the count of IfumVisit records
     */
    Integer getVisitCount(String project);

    /**
     * Retrieves a list of visit counts grouped by project.
     *
     * @return a list of VisitCountEntry objects containing project names and their visit counts
     */
    List<VisitCountEntry> getVisitCountsForProjects();

    /**
     * Refreshes the visit view which groups visit counts by project.
     */
    void refreshVisitCache();
}
