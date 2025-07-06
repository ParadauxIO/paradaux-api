package io.paradaux.api.controllers;

import io.paradaux.api.models.IfumVisit;
import io.paradaux.api.models.annotations.ProtectedRoute;
import io.paradaux.api.services.IfumService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ifum")
public class IfumController {

    private final IfumService ifumService;

    public IfumController(IfumService ifumService) {
        this.ifumService = ifumService;
    }

    @ProtectedRoute
    @PostMapping("/visit")
    public void insertVisit(@RequestBody IfumVisit visit) {
        ifumService.insertVisit(visit);
    }

    @GetMapping("/visits")
    public Integer getVisitCount() {
        return ifumService.getVisitCount();
    }
}
