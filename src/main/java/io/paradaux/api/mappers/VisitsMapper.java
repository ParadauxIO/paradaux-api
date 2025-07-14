package io.paradaux.api.mappers;

import io.paradaux.api.models.VisitCountEntry;
import io.paradaux.api.models.VisitEntry;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface VisitsMapper {
    void insertVisit(VisitEntry visit);
    Integer getVisitCount(String project);
    List<VisitCountEntry> getVisitCountsForProjects();
    void refreshVisitCache();
}
