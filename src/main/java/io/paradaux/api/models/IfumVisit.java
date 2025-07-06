package io.paradaux.api.models;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class IfumVisit {
    private int id;
    private Timestamp createdAt;
}
