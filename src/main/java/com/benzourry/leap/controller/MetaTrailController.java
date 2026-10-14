package com.benzourry.leap.controller;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.MetaTrailRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
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


//    @PreAuthorize("@authz.isDesigner()")
//    @PostMapping("/{id}/revert")
//    @Transactional
//    public Map<String, Object> revertTrail(@PathVariable String id) throws Exception {
//        MetaTrail trail = metaTrailRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Trail not found"));
//
//        if (trail.getData() == null || trail.getData().isNull()) {
//            throw new IllegalArgumentException("No snapshot data available to revert.");
//        }
//
//        Class<?> entityClass = getEntityClass(trail.getModule());
//        if (entityClass == null) {
//            throw new UnsupportedOperationException("Revert not supported for module: " + trail.getModule());
//        }
//
//        // 1. Fetch current LIVE database state
//        Object managedEntity = entityManager.find(entityClass, trail.getModuleId());
//        if (managedEntity == null) {
//            throw new IllegalArgumentException("Target entity no longer exists. Cannot revert.");
//        }
//
//        // 2. Convert current DB state to a JSON tree for comparison
//        com.fasterxml.jackson.databind.JsonNode dbState = mapper.valueToTree(managedEntity);
//
//        // 3. Deep copy the snapshot we want to revert to
//        com.fasterxml.jackson.databind.node.ObjectNode snapshot =
//                (com.fasterxml.jackson.databind.node.ObjectNode) trail.getData().deepCopy();
//
//        // 4. Intelligently compare and strip IDs only for deleted items
//        syncIdsWithDatabaseState(snapshot, dbState);
//
//        // 5. Apply the safe, sanitized snapshot
//        mapper.readerForUpdating(managedEntity).readValue(snapshot);
//        entityManager.merge(managedEntity);
//
//        return Map.of("success", true, "message", "Entity successfully reverted to state from " + trail.getTimestamp());
//    }
//
//    /**
//     * Recursively compares the snapshot to the database state.
//     * Existing items keep their IDs. Deleted items get their IDs stripped so Hibernate can INSERT them.
//     */
//    private void syncIdsWithDatabaseState(com.fasterxml.jackson.databind.JsonNode snap, com.fasterxml.jackson.databind.JsonNode db) {
//        if (snap != null && snap.isObject()) {
//            com.fasterxml.jackson.databind.node.ObjectNode snapObj = (com.fasterxml.jackson.databind.node.ObjectNode) snap;
//
//            java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = snapObj.fields();
//            while (fields.hasNext()) {
//                java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
//                String key = field.getKey();
//                com.fasterxml.jackson.databind.JsonNode snapChild = field.getValue();
//                com.fasterxml.jackson.databind.JsonNode dbChild = (db != null && db.isObject()) ? db.get(key) : null;
//
//                if (snapChild.isArray()) {
//                    com.fasterxml.jackson.databind.node.ArrayNode snapArray = (com.fasterxml.jackson.databind.node.ArrayNode) snapChild;
//                    com.fasterxml.jackson.databind.node.ArrayNode dbArray = (dbChild != null && dbChild.isArray()) ? (com.fasterxml.jackson.databind.node.ArrayNode) dbChild : null;
//
//                    for (com.fasterxml.jackson.databind.JsonNode snapItem : snapArray) {
//                        if (snapItem.isObject()) {
//                            if (snapItem.has("id")) {
//                                Long snapId = snapItem.get("id").asLong();
//                                com.fasterxml.jackson.databind.JsonNode matchingDbItem = findById(dbArray, snapId);
//
//                                if (matchingDbItem == null) {
//                                    // Entity was deleted from DB. Strip ID to allow clean INSERT.
//                                    ((com.fasterxml.jackson.databind.node.ObjectNode) snapItem).remove("id");
//                                    stripAllIds(snapItem); // Recursively clear nested children too
//                                } else {
//                                    // Entity exists. Recurse to check its children.
//                                    syncIdsWithDatabaseState(snapItem, matchingDbItem);
//                                }
//                            } else {
//                                syncIdsWithDatabaseState(snapItem, null);
//                            }
//                        }
//                    }
//                } else if (snapChild.isObject()) {
//                    if (snapChild.has("id")) {
//                        Long snapId = snapChild.get("id").asLong();
//                        if (dbChild == null || !dbChild.has("id") || dbChild.get("id").asLong() != snapId) {
//                            ((com.fasterxml.jackson.databind.node.ObjectNode) snapChild).remove("id");
//                            stripAllIds(snapChild);
//                        } else {
//                            syncIdsWithDatabaseState(snapChild, dbChild);
//                        }
//                    } else {
//                        // It's a Map collection (like Form.items), continue traversing
//                        syncIdsWithDatabaseState(snapChild, dbChild);
//                    }
//                }
//            }
//        }
//    }
//
//    private com.fasterxml.jackson.databind.JsonNode findById(com.fasterxml.jackson.databind.node.ArrayNode array, Long id) {
//        if (array == null) return null;
//        for (com.fasterxml.jackson.databind.JsonNode node : array) {
//            if (node.isObject() && node.has("id") && node.get("id").asLong() == id) {
//                return node;
//            }
//        }
//        return null;
//    }
//
//    private void stripAllIds(com.fasterxml.jackson.databind.JsonNode node) {
//        if (node.isObject()) {
//            ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("id");
//            node.elements().forEachRemaining(this::stripAllIds);
//        } else if (node.isArray()) {
//            node.elements().forEachRemaining(this::stripAllIds);
//        }
//    }


    @PreAuthorize("@authz.isDesigner()")
    @PostMapping("/{id}/revert")
    @Transactional
    public Map<String, Object> revertTrail(@PathVariable String id) throws Exception {
        MetaTrail trail = metaTrailRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trail not found"));

        if (trail.getData() == null || trail.getData().isNull()) {
            throw new IllegalArgumentException("No snapshot data available to revert.");
        }

        Class<?> entityClass = getEntityClass(trail.getModule());
        if (entityClass == null) {
            throw new UnsupportedOperationException("Revert not supported for module: " + trail.getModule());
        }

        Object managedEntity = entityManager.find(entityClass, trail.getModuleId());
        if (managedEntity == null) {
            throw new IllegalArgumentException("Target entity no longer exists.");
        }

        // 1. Get Live DB state and make a deep copy of the Snapshot
        com.fasterxml.jackson.databind.JsonNode dbState = mapper.valueToTree(managedEntity);
        com.fasterxml.jackson.databind.node.ObjectNode snapshot =
                (com.fasterxml.jackson.databind.node.ObjectNode) trail.getData().deepCopy();

        applyRelationships(snapshot, managedEntity);

        // 2. Scrub deleted IDs from the JSON before Jackson reads it
        sanitizeSnapshot(snapshot, dbState);

        // 3. Apply the sanitized snapshot safely
        mapper.readerForUpdating(managedEntity).readValue(snapshot);

        // 6. Swap Jackson's transient stubs with real Hibernate Proxies
//        resolveStubs(managedEntity);

        entityManager.merge(managedEntity);

        return Map.of("success", true, "message", "Entity successfully reverted to state from " + trail.getTimestamp());
    }

    /**
     * Compact, 15-line recursive method to strip IDs of deleted items only.
     */
    private void sanitizeSnapshot(com.fasterxml.jackson.databind.JsonNode snap, com.fasterxml.jackson.databind.JsonNode db) {
        if (snap.isObject()) {
            com.fasterxml.jackson.databind.node.ObjectNode obj = (com.fasterxml.jackson.databind.node.ObjectNode) snap;
            if (obj.has("id") && (db == null || !db.has("id") || db.get("id").asLong() != obj.get("id").asLong())) {
                obj.remove("id");
                db = null; // Parent is resurrected, force all children to be new inserts
            }
            final com.fasterxml.jackson.databind.JsonNode finalDb = db;
            obj.fields().forEachRemaining(e -> sanitizeSnapshot(e.getValue(), finalDb != null ? finalDb.get(e.getKey()) : null));
        } else if (snap.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : snap) {
                com.fasterxml.jackson.databind.JsonNode dbMatch = null;
                if (item.has("id") && db != null && db.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode dbItem : db) {
                        if (dbItem.has("id") && dbItem.get("id").asLong() == item.get("id").asLong()) { dbMatch = dbItem; break; }
                    }
                }
                sanitizeSnapshot(item, dbMatch);
            }
        }
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
            case "emailtemplate": return EmailTemplate.class;
            default: return null;
        }
    }

    /**
     * Replaces transient entities created by Jackson's stub mapping
     * with managed Hibernate proxies to prevent TransientObjectExceptions.
     */
//    private void resolveStubs(Object entity) {
//        if (entity instanceof com.benzourry.leap.model.Screen screen) {
//            if (screen.getForm() != null && screen.getForm().getId() != null) {
//                screen.setForm(entityManager.getReference(com.benzourry.leap.model.Form.class, screen.getForm().getId()));
//            }
//            if (screen.getDataset() != null && screen.getDataset().getId() != null) {
//                screen.setDataset(entityManager.getReference(com.benzourry.leap.model.Dataset.class, screen.getDataset().getId()));
//            }
//            if (screen.getBucket() != null && screen.getBucket().getId() != null) {
//                screen.setBucket(entityManager.getReference(com.benzourry.leap.model.Bucket.class, screen.getBucket().getId()));
//            }
//            if (screen.getCogna() != null && screen.getCogna().getId() != null) {
//                screen.setCogna(entityManager.getReference(com.benzourry.leap.model.Cogna.class, screen.getCogna().getId()));
//            }
//        } else if (entity instanceof com.benzourry.leap.model.Dataset dataset) {
//            if (dataset.getForm() != null && dataset.getForm().getId() != null) {
//                dataset.setForm(entityManager.getReference(com.benzourry.leap.model.Form.class, dataset.getForm().getId()));
//            }
//        }
//    }



    private void applyRelationships(com.fasterxml.jackson.databind.node.ObjectNode snapshot, Object entity) {
        if (entity instanceof com.benzourry.leap.model.Screen screen) {
            if (snapshot.has("form")) {
                screen.setForm(getProxy(com.benzourry.leap.model.Form.class, snapshot.get("form")));
                snapshot.remove("form"); // Delete from JSON so Jackson ignores it!
            }
            if (snapshot.has("dataset")) {
                screen.setDataset(getProxy(com.benzourry.leap.model.Dataset.class, snapshot.get("dataset")));
                snapshot.remove("dataset");
            }
            if (snapshot.has("bucket")) {
                screen.setBucket(getProxy(com.benzourry.leap.model.Bucket.class, snapshot.get("bucket")));
                snapshot.remove("bucket");
            }
            if (snapshot.has("cogna")) {
                screen.setCogna(getProxy(com.benzourry.leap.model.Cogna.class, snapshot.get("cogna")));
                snapshot.remove("cogna");
            }
            if (snapshot.has("app")) {
                screen.setApp(getProxy(com.benzourry.leap.model.App.class, snapshot.get("app")));
                snapshot.remove("app");
            }
        } else if (entity instanceof com.benzourry.leap.model.Dataset dataset) {
            if (snapshot.has("form")) {
                dataset.setForm(getProxy(com.benzourry.leap.model.Form.class, snapshot.get("form")));
                snapshot.remove("form");
            }
            if (snapshot.has("app")) {
                dataset.setApp(getProxy(com.benzourry.leap.model.App.class, snapshot.get("app")));
                snapshot.remove("app");
            }
        } else if (entity instanceof com.benzourry.leap.model.Form form) {
            if (snapshot.has("app")) {
                form.setApp(getProxy(com.benzourry.leap.model.App.class, snapshot.get("app")));
                snapshot.remove("app");
            }
            if (snapshot.has("prev")) {
                form.setPrev(getProxy(com.benzourry.leap.model.Form.class, snapshot.get("prev")));
                snapshot.remove("prev");
            }
        } else if (entity instanceof com.benzourry.leap.model.Lambda lambda) {
            if (snapshot.has("app")) {
                lambda.setApp(getProxy(com.benzourry.leap.model.App.class, snapshot.get("app")));
                snapshot.remove("app");
            }
        } else if (entity instanceof com.benzourry.leap.model.EmailTemplate mailer) {
            if (snapshot.has("app")) {
                mailer.setApp(getProxy(com.benzourry.leap.model.App.class, snapshot.get("app")));
                snapshot.remove("app");
            }
        }
    }

    private <T> T getProxy(Class<T> clazz, com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject() && node.has("id")) {
            return entityManager.getReference(clazz, node.get("id").asLong());
        } else if (node.isNumber()) {
            return entityManager.getReference(clazz, node.asLong());
        }
        return null;
    }
}