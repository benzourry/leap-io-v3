package com.benzourry.leap.controller;

import com.benzourry.leap.model.MetaTrail;
import com.benzourry.leap.repository.MetaTrailRepository;
import com.benzourry.leap.service.MetaTrailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/metatrail")
public class MetaTrailController {

    private final MetaTrailRepository metaTrailRepository;
    private final MetaTrailService metaTrailService;

    public MetaTrailController(MetaTrailRepository metaTrailRepository,
                               MetaTrailService metaTrailService) {
        this.metaTrailRepository = metaTrailRepository;
        this.metaTrailService = metaTrailService;
    }

    // Endpoint 1: Filter by explicit Scope string
    @GetMapping("/{scope}")
    @PreAuthorize("@authz.isDesigner()")
    public Page<MetaTrail> getTrailsByScope(
            @PathVariable String scope,
            @RequestParam(required = false) String searchText,
            @RequestParam(required = false) Long dateFrom,
            @RequestParam(required = false) Long dateTo,
            Pageable pageable) {

        Date fromDate = dateFrom != null ? new Date(dateFrom) : null;
        Date toDate = dateTo != null ? new Date(dateTo) : null;

        return metaTrailRepository.findByScopeWithFilters(scope, searchText, fromDate, toDate, pageable);
    }

    // Endpoint 2: Filter globally by App ID, Module, and Module ID
    @GetMapping("/app/{appId}")
    @PreAuthorize("@authz.isDesigner()")
    public Page<MetaTrail> getTrailsByApp(
            @PathVariable Long appId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) Long dateFrom,
            @RequestParam(required = false) Long dateTo,
            Pageable pageable) {

        Date fromDate = dateFrom != null ? new Date(dateFrom) : null;
        Date toDate = dateTo != null ? new Date(dateTo) : null;

        return metaTrailRepository.findByAppIdWithFilters(appId, module, moduleId, fromDate, toDate, pageable);
    }

    /**
     * Endpoint to manually trigger the baseline snapshot generation.
     * Can optionally be filtered by appId and/or scope.
     * GET /metatrail/init?appId=1&scope=form_123
     */
    @PreAuthorize("@authz.isManager()")
    @GetMapping("/init")
    public ResponseEntity<?> initializeMetaTrails(
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) String scope) {
        try {
            metaTrailService.initialize(appId, scope);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MetaTrail initialization has started in the background. Check server logs for completion status."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Endpoint to clear metatrails by appId and/or scope.
     * DELETE /metatrail/clear?appId=1&scope=form_123
     */
    @PreAuthorize("@authz.isManager()")
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearMetaTrails(
            @RequestParam(required = false) Long appId,
            @RequestParam(required = false) String scope) {
        try {
            metaTrailService.clear(appId, scope);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "MetaTrail records successfully cleared."
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PreAuthorize("@authz.isDesigner()")
    @PostMapping("/{id}/revert")
    public Map<String, Object> revertTrail(@PathVariable String id) {
        try {
            metaTrailService.revertTrail(id);
            return Map.of("success", true, "message", "Entity successfully reverted.");
        } catch (Exception e) {
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}