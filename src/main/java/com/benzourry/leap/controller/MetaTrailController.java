package com.benzourry.leap.controller;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.MetaTrailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/metatrail")
public class MetaTrailController {

    private final MetaTrailRepository metaTrailRepository;
    private final EntityManager entityManager;
    private final ObjectMapper mapper;

    public MetaTrailController(MetaTrailRepository metaTrailRepository,
                               EntityManager entityManager,
                               ObjectMapper mapper) {
        this.metaTrailRepository = metaTrailRepository;
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    // Endpoint 1: Filter by explicit Scope string
    @GetMapping("/{scope}")
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


    @PostMapping("/{id}/revert")
    @Transactional
    public Map<String, Object> revertTrail(@PathVariable String id) throws Exception {
        MetaTrail trail = metaTrailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trail not found"));

        if (trail.getData() == null || trail.getData().isNull()) {
            throw new IllegalArgumentException("No snapshot data available to revert.");
        }

        // 1. Resolve which entity class this module maps to
        Class<?> entityClass = getEntityClass(trail.getModule());
        if (entityClass == null) {
            throw new UnsupportedOperationException("Revert not supported for module: " + trail.getModule());
        }

        // 2. Fetch the LIVE, MANAGED entity from the database
        Object managedEntity = entityManager.find(entityClass, trail.getModuleId());
        if (managedEntity == null) {
            throw new IllegalArgumentException("Target entity no longer exists. Cannot revert.");
        }

        // 3. OVERLAY the JSON snapshot directly onto the managed entity
        // This safely restores the data without detaching the object or breaking Hibernate proxies!
        mapper.readerForUpdating(managedEntity).readValue(trail.getData());

        // 4. Save changes
        entityManager.merge(managedEntity);

        return Map.of(
                "success", true,
                "message", "Entity successfully reverted to state from " + trail.getTimestamp()
        );
    }

    // Helper method to map the module string to your actual Entity classes
    private Class<?> getEntityClass(String module) {
        switch (module.toLowerCase()) {
            case "form": return Form.class;
            case "form_item": return Item.class;
            case "form_section": return Section.class;
            case "dataset": return Dataset.class;
            case "screen": return Screen.class;
            case "lambda": return Lambda.class;
            default: return null;
        }
    }
}