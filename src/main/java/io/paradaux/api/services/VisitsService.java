package io.paradaux.api.services;

import io.paradaux.api.models.VisitEntry;

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
     * Refreshes the visit view which groups visit counts by project.
     */
    void refreshVisitCache();
}
