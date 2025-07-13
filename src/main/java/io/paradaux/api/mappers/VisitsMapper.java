package io.paradaux.api.mappers;

import io.paradaux.api.models.VisitEntry;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VisitsMapper {
    void insertVisit(VisitEntry visit);
    Integer getVisitCount(String project);
    void refreshVisitCache();
}
