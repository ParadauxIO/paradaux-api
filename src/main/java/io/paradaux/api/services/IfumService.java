package io.paradaux.api.services;

import io.paradaux.api.models.IfumVisit;

public interface IfumService {
    /**
     * Inserts a new IfumVisit record into the database.
     */
    void insertVisit(IfumVisit visit);

    /**
     * Retrieves the total count of IfumVisit records.
     *
     * @return the count of IfumVisit records
     */
    Integer getVisitCount();
}
