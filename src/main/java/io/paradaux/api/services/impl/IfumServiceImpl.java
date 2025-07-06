package io.paradaux.api.services.impl;

import io.paradaux.api.mappers.IfumMapper;
import io.paradaux.api.services.IfumService;
import org.springframework.stereotype.Service;

@Service
public class IfumServiceImpl implements IfumService {

    private final IfumMapper ifumMapper;

    public IfumServiceImpl(IfumMapper ifumMapper) {
        this.ifumMapper = ifumMapper;
    }

    @Override
    public void insertVisit() {
        ifumMapper.insertVisit();
    }

    @Override
    public Integer getVisitCount() {
        return ifumMapper.getVisitCount();
    }
}
