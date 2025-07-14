package io.paradaux.api.models;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class VisitCountEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String project;
    private int totalCount;
}
