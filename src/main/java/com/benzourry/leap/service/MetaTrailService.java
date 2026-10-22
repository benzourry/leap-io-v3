package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class MetaTrailService {

    private final FormRepository formRepo;
    private final ScreenRepository screenRepo;
    private final DatasetRepository datasetRepo;
    private final EmailTemplateRepository emailTemplateRepo;
    private final LambdaRepository lambdaRepo;
    private final CognaRepository cognaRepo;
    private final ItemRepository itemRepo;
    private final SectionRepository sectionRepo;
    private final MetaTrailRepository metaTrailRepo;
    private final ObjectMapper objectMapper;
    private final ApplicationContext context;
    private final EntityManager entityManager;

    public MetaTrailService(FormRepository formRepo, ScreenRepository screenRepo,
                            DatasetRepository datasetRepo, EmailTemplateRepository emailTemplateRepo,
                            LambdaRepository lambdaRepo, CognaRepository cognaRepo,
                            ItemRepository itemRepo, SectionRepository sectionRepo,
                            MetaTrailRepository metaTrailRepo, ObjectMapper objectMapper,
                            ApplicationContext context, EntityManager entityManager) {
        this.formRepo = formRepo;
        this.screenRepo = screenRepo;
        this.datasetRepo = datasetRepo;
        this.emailTemplateRepo = emailTemplateRepo;
        this.lambdaRepo = lambdaRepo;
        this.cognaRepo = cognaRepo;
        this.itemRepo = itemRepo;
        this.sectionRepo = sectionRepo;
        this.metaTrailRepo = metaTrailRepo;
        this.objectMapper = objectMapper;
        this.context = context;
        this.entityManager = entityManager;
    }

    // =========================================================================
    // INITIALIZATION LOGIC
    // =========================================================================

    @Async("asyncExec")
    public void initialize(Long targetAppId, String targetScope) {
        System.out.println("⏳ Starting MetaTrail baseline initialization via API...");

        MetaTrailService proxy = context.getBean(MetaTrailService.class);

        // Safely check if scope is missing OR just an empty string
        boolean noScope = (targetScope == null || targetScope.trim().isEmpty());
        String scope = noScope ? "" : targetScope.trim();

        // Matching logic for Form and its sub-types
        boolean matchForm = noScope || scope.equalsIgnoreCase("form") || scope.matches("^form_\\d+$");
        boolean matchFormItem = noScope || scope.equalsIgnoreCase("form_item")
                || scope.matches("^form_.*_item(_\\d+)?$");
        boolean matchFormSection = noScope || scope.equalsIgnoreCase("form_section")
                || scope.matches("^form_.*_section(_\\d+)?$");

        if (matchForm) {
            formRepo.findAllIds().forEach(id -> safeProcess(proxy, "form", id, targetAppId, targetScope));
        }
        if (matchFormItem) {
            itemRepo.findAllIds().forEach(id -> safeProcess(proxy, "form_item", id, targetAppId, targetScope));
        }
        if (matchFormSection) {
            sectionRepo.findAllIds().forEach(id -> safeProcess(proxy, "form_section", id, targetAppId, targetScope));
        }

        if (noScope || targetScope.startsWith("screen")) {
            screenRepo.findAllIds().forEach(id -> safeProcess(proxy, "screen", id, targetAppId, targetScope));
        }

        if (noScope || targetScope.startsWith("dataset")) {
            datasetRepo.findAllIds().forEach(id -> safeProcess(proxy, "dataset", id, targetAppId, targetScope));
        }

        if (noScope || targetScope.startsWith("emailTemplate")) {
            emailTemplateRepo.findAllIds().forEach(id -> safeProcess(proxy, "emailTemplate", id, targetAppId, targetScope));
        }

        if (noScope || targetScope.startsWith("lambda")) {
            lambdaRepo.findAllIds().forEach(id -> safeProcess(proxy, "lambda", id, targetAppId, targetScope));
        }

        if (noScope || targetScope.startsWith("cogna")) {
            cognaRepo.findAllIds().forEach(id -> safeProcess(proxy, "cogna", id, targetAppId, targetScope));
        }

        System.out.println("✅ MetaTrail baseline initialization complete!");
    }

    private void safeProcess(MetaTrailService proxy, String module, Long id, Long targetAppId, String targetScope) {
        try {
            proxy.processEntity(module, id, targetAppId, targetScope);
        } catch (Exception ex) {
            System.err.println("⚠️ Skipped " + module + " ID " + id + " - Reason: " + ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEntity(String module, Long id, Long targetAppId, String targetScope) {
        Trailable entity = null;

        // Fetch a fresh copy of the entity inside the new transaction
        switch (module) {
            case "form": entity = formRepo.findById(id).orElse(null); break;
            case "screen": entity = screenRepo.findById(id).orElse(null); break;
            case "dataset": entity = datasetRepo.findById(id).orElse(null); break;
            case "emailTemplate": entity = emailTemplateRepo.findById(id).orElse(null); break;
            case "lambda": entity = lambdaRepo.findById(id).orElse(null); break;
            case "cogna": entity = cognaRepo.findById(id).orElse(null); break;
            case "form_item": entity = itemRepo.findById(id).orElse(null); break;
            case "form_section": entity = sectionRepo.findById(id).orElse(null); break;
        }

        if (entity != null) {
            // Filter by requested App ID safely
            if (targetAppId != null && !targetAppId.equals(entity.getTrailAppId())) return;

            // Filter by Scope safely (Exact match, strict child match, or wildcard match)
            if (targetScope != null && !targetScope.trim().isEmpty()) {
                String currentScope = entity.getTrailScope();

                if (currentScope == null) return;

                // Convert wildcard '*' to regex '.*' for matching
                String regexPattern = "^" + targetScope.replace("*", ".*") + "(_.*)?$";

                if (!currentScope.equals(targetScope) &&
                        !currentScope.startsWith(targetScope + "_") &&
                        !currentScope.matches(regexPattern)) {
                    return;
                }
            }
            saveTrail(entity);
        }
    }

    private void saveTrail(Trailable trailable) {
        MetaTrail trail = new MetaTrail();
        trail.setModule(trailable.getTrailModule());
        trail.setAction("INIT");
        trail.setModuleId(trailable.getTrailId());
        trail.setScope(trailable.getTrailScope());
        trail.setAppId(trailable.getTrailAppId());
        trail.setTimestamp(new Date());
        trail.setEmail("system@init");

        ObjectNode data = (ObjectNode) objectMapper.convertValue(trailable, JsonNode.class);
        stripParentReferences(data, trailable.getTrailModule());
        stripComputedFields(data);
        trail.setData(data);

        metaTrailRepo.save(trail);
    }

    // =========================================================================
    // CLEAR LOGIC
    // =========================================================================

    @Transactional
    public void clear(Long appId, String scope) {
        if (appId == null && (scope == null || scope.trim().isEmpty())) {
            throw new IllegalArgumentException("You must provide either an appId or a scope to clear trails.");
        }

        StringBuilder jpql = new StringBuilder("DELETE FROM MetaTrail m WHERE 1=1");

        if (appId != null) {
            jpql.append(" AND m.appId = :appId");
        }
        if (scope != null && !scope.trim().isEmpty()) {
            jpql.append(" AND (m.scope = :scope OR m.scope LIKE CONCAT(:scope, '/_%') ESCAPE '/')");
        }

        Query query = entityManager.createQuery(jpql.toString());

        if (appId != null) {
            query.setParameter("appId", appId);
        }
        if (scope != null && !scope.trim().isEmpty()) {
            query.setParameter("scope", scope);
        }

        int deletedCount = query.executeUpdate();
        System.out.println("🗑️ Cleared " + deletedCount + " MetaTrail records.");
    }

    // =========================================================================
    // REVERT LOGIC
    // =========================================================================

    @Transactional
    public void revertTrail(String id) throws Exception {
        MetaTrail trail = metaTrailRepo.findById(id)
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

        JsonNode dbState = objectMapper.valueToTree(managedEntity);
        ObjectNode snapshot = (ObjectNode) trail.getData().deepCopy();

        applyRelationships(snapshot, managedEntity);
        sanitizeSnapshot(snapshot, dbState);
        objectMapper.readerForUpdating(managedEntity).readValue(snapshot);

        entityManager.merge(managedEntity);
    }

    private void sanitizeSnapshot(JsonNode snap, JsonNode db) {
        if (snap.isObject()) {
            ObjectNode obj = (ObjectNode) snap;
            if (obj.has("id") && (db == null || !db.has("id") || db.get("id").asLong() != obj.get("id").asLong())) {
                obj.remove("id");
                db = null; // Parent is resurrected, force all children to be new inserts
            }
            final JsonNode finalDb = db;
            obj.fields().forEachRemaining(e -> sanitizeSnapshot(e.getValue(), finalDb != null ? finalDb.get(e.getKey()) : null));
        } else if (snap.isArray()) {
            for (JsonNode item : snap) {
                JsonNode dbMatch = null;
                if (item.has("id") && db != null && db.isArray()) {
                    for (JsonNode dbItem : db) {
                        if (dbItem.has("id") && dbItem.get("id").asLong() == item.get("id").asLong()) { dbMatch = dbItem; break; }
                    }
                }
                sanitizeSnapshot(item, dbMatch);
            }
        }
    }

    private void applyRelationships(ObjectNode snapshot, Object entity) throws Exception {
        Class<?> clazz = entity.getClass();
        while (clazz != null && clazz != Object.class) {
            for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                        field.isAnnotationPresent(jakarta.persistence.OneToOne.class)) {
                    String fieldName = field.getName();
                    if (snapshot.has(fieldName)) {
                        Object proxy = getProxy(field.getType(), snapshot.get(fieldName));
                        field.setAccessible(true);
                        field.set(entity, proxy);
                        snapshot.remove(fieldName);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
    }

    private <T> T getProxy(Class<T> clazz, JsonNode node) {
        if (node == null || node.isNull()) return null;
        if (node.isObject() && node.has("id")) {
            return entityManager.getReference(clazz, node.get("id").asLong());
        } else if (node.isNumber()) {
            return entityManager.getReference(clazz, node.asLong());
        }
        return null;
    }

    private Class<?> getEntityClass(String module) {
        switch (module.toLowerCase()) {
            case "form": return Form.class;
            case "form_item": return Item.class;
            case "form_section": return Section.class;
            case "dataset": return Dataset.class;
            case "screen": return Screen.class;
            case "lambda": return Lambda.class;
            case "emailtemplate": return EmailTemplate.class;
            case "cogna": return Cogna.class;
            default: return null;
        }
    }

    // =========================================================================
    // JSON SCRUBBING HELPERS
    // =========================================================================

    private void stripParentReferences(ObjectNode rootNode, String module) {
        if (rootNode == null || module == null) return;

        rootNode.remove("app");

        switch (module.toLowerCase()) {
            case "form":
                stubReference(rootNode, "prev");
                break;
            case "dataset":
            case "form_item":
            case "form_section":
                stubReference(rootNode, "form");
                break;
            case "screen":
                stubReference(rootNode, "form");
                stubReference(rootNode, "dataset");
                stubReference(rootNode, "bucket");
                stubReference(rootNode, "cogna");
                break;
        }
    }

    private void stubReference(ObjectNode rootNode, String fieldName) {
        if (rootNode.hasNonNull(fieldName) && rootNode.get(fieldName).isObject()) {
            JsonNode ref = rootNode.get(fieldName);
            if (ref.has("id")) {
                long refId = ref.get("id").asLong();
                rootNode.putObject(fieldName).put("id", refId);
            }
        }
    }

    private static final Set<String> COMPUTED_FIELDS = Set.of(
            "_f", "_onSave", "_onSubmit", "_onView",
            "_pre", "_post", "_placeholder", "_data"
    );

    private void stripComputedFields(JsonNode node) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            List<String> keysToRemove = new ArrayList<>();
            obj.fields().forEachRemaining(entry -> {
                String key = entry.getKey();
                JsonNode value = entry.getValue();

                if (COMPUTED_FIELDS.contains(key) && value.isTextual()) {
                    keysToRemove.add(key);
                }
            });

            obj.remove(keysToRemove);
            obj.elements().forEachRemaining(this::stripComputedFields);
        } else if (node.isArray()) {
            node.elements().forEachRemaining(this::stripComputedFields);
        }
    }
}