package io.paradaux.api.services;

public interface IfumService {
    /**
     * Inserts a new IfumVisit record into the database.
     */
    void insertVisit();

    /**
     * Retrieves the total count of IfumVisit records.
     *
     * @return the count of IfumVisit records
     */
    Integer getVisitCount();
}
