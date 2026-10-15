package com.benzourry.leap.service;

import com.benzourry.leap.model.MetaTrail;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.audit.Trailable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class MetaTrailInitService {

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
    private final ApplicationContext context; // <-- Added ApplicationContext

    public MetaTrailInitService(FormRepository formRepo, ScreenRepository screenRepo,
                                DatasetRepository datasetRepo, EmailTemplateRepository emailTemplateRepo,
                                LambdaRepository lambdaRepo, CognaRepository cognaRepo,
                                ItemRepository itemRepo, SectionRepository sectionRepo,
                                MetaTrailRepository metaTrailRepo, ObjectMapper objectMapper,
                                ApplicationContext context) {
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
    }

    // ⚠️ Notice: @Transactional is REMOVED from here so the global transaction doesn't fail
    @Async("asyncExec")
    public void initializeAll() {
        System.out.println("⏳ Starting MetaTrail baseline initialization via API...");

        // Get a Spring Proxy of this service to enforce the REQUIRES_NEW transactions
        MetaTrailInitService proxy = context.getBean(MetaTrailInitService.class);

        // Fetch all, but process them one by one in isolated transactions
        formRepo.findAll().forEach(e -> safeProcess(proxy, "form", e.getId()));
        itemRepo.findAll().forEach(e -> safeProcess(proxy, "form_item", e.getId()));
        sectionRepo.findAll().forEach(e -> safeProcess(proxy, "form_section", e.getId()));
        screenRepo.findAll().forEach(e -> safeProcess(proxy, "screen", e.getId()));
        datasetRepo.findAll().forEach(e -> safeProcess(proxy, "dataset", e.getId()));
        emailTemplateRepo.findAll().forEach(e -> safeProcess(proxy, "emailTemplate", e.getId()));
        lambdaRepo.findAll().forEach(e -> safeProcess(proxy, "lambda", e.getId()));
        cognaRepo.findAll().forEach(e -> safeProcess(proxy, "cogna", e.getId()));

        System.out.println("✅ MetaTrail baseline initialization complete!");
    }

    // Safely wraps the execution so a failure doesn't crash the loop
    private void safeProcess(MetaTrailInitService proxy, String module, Long id) {
        try {
            proxy.processEntity(module, id);
        } catch (Exception ex) {
            // Print the actual error message so you aren't flying blind!
            System.err.println("⚠️ Skipped " + module + " ID " + id + " - Reason: " + ex.getMessage());
        }
    }

    // Starts a BRAND NEW transaction for every single record
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processEntity(String module, Long id) {
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

        // Jackson serialization happens here. If it throws EntityNotFoundException,
        // ONLY this specific saveTrail transaction will roll back!
        ObjectNode data = (ObjectNode) objectMapper.convertValue(trailable, JsonNode.class);
        stripParentReferences(data, trailable.getTrailModule());
        stripComputedFields(data);
        trail.setData(data);

        metaTrailRepo.save(trail);
    }

    // =========================================================================
    // JSON SCRUBBING HELPERS
    // =========================================================================

    private void stripParentReferences(ObjectNode rootNode, String module) {
        if (rootNode == null || module == null) return;

        // An entity never changes which App it belongs to, so this is 100% safe to drop
        rootNode.remove("app");

        // For relationships that CAN change, we stub them to just { "id": X }
        switch (module.toLowerCase()) {
            case "form":
                stubReference(rootNode, "prev"); // <-- ADD THIS to stub the previous form!
                break;
            case "dataset":
            case "form_item":
            case "form_section":
                stubReference(rootNode, "form"); // <-- Ensure item/section are here too!
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