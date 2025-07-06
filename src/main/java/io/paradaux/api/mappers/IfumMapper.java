package io.paradaux.api.mappers;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IfumMapper {

    void insertVisit();
    Integer getVisitCount();
}
