package io.paradaux.api.controllers;

import io.paradaux.api.models.VisitEntry;
import io.paradaux.api.models.annotations.ProtectedRoute;
import io.paradaux.api.services.VisitsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics/visits")
public class VisitsController {

    private final VisitsService visitsService;

    public VisitsController(VisitsService visitsService) {
        this.visitsService = visitsService;
    }

    @ProtectedRoute
    @PostMapping("/visit")
    public void insertVisit(@RequestBody VisitEntry visit) {
        visitsService.insertVisit(visit);
    }

    @GetMapping("/project/{project}")
    public Integer getVisitCountByProject(@PathVariable String project) {
        return visitsService.getVisitCount(project);
    }
}
