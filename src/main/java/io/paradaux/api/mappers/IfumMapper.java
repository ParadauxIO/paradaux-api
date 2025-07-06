package io.paradaux.api.mappers;

import io.paradaux.api.models.IfumVisit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IfumMapper {
    void insertVisit(IfumVisit visit);
    Integer getVisitCount();
}
