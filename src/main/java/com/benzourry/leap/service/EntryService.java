package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.JsonSchemaValidationException;
import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.filter.EntryFilter;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.benzourry.leap.utility.Helper.*;

@Service
public class EntryService {
    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);
    final EntryRepository entryRepository;
    final EntryTrailRepository entryTrailRepository;
    final CustomEntryRepository customEntryRepository;
    final EntryAttachmentRepository entryAttachmentRepository;
    final BucketService bucketService;
    final EntryApprovalRepository entryApprovalRepository;
    final EntryApprovalTrailRepository entryApprovalTrailRepository;
    final DynamicSQLRepository dynamicSQLRepository;
    final TierRepository tierRepository;
    final FormService formService;
    final FormRepository formRepository;
    final DashboardService dashboardService;
    final DatasetRepository datasetRepository;

    final ChartRepository chartRepository;
    final ScreenRepository screenRepository;
    final MailService mailService;
    final PushService pushService;
    final EmailTemplateRepository emailTemplateRepository;
    final UserRepository userRepository;
    final AppUserRepository appUserRepository;
    final ItemRepository itemRepository;
    final SectionItemRepository sectionItemRepository;
    final NotificationService notificationService;
    final EndpointService endpointService;
    final ChartQuery chartQuery;
    final AppService appService;
    final KryptaService kryptaService;
    final ApiKeyRepository apiKeyRepository;
    @PersistenceContext
    private EntityManager entityManager;
    private KeyValueRepository keyValueRepository;
    private final ObjectMapper MAPPER;
    private final EntryService self;
    private final HttpClient HTTP_CLIENT;

    private final EntryBatchRepository entryBatchRepository;

//    private Executor executor;

    public EntryService(EntryRepository entryRepository,
                        EntryTrailRepository entryTrailRepository,
                        CustomEntryRepository customEntryRepository,
                        EntryAttachmentRepository entryAttachmentRepository,
                        BucketService bucketService,
                        EntryApprovalRepository entryApprovalRepository,
                        EntryApprovalTrailRepository entryApprovalTrailRepository,
                        DynamicSQLRepository dynamicSQLRepository,
                        FormService formService,
                        FormRepository formRepository,
                        MailService mailService,
                        PushService pushService,
                        TierRepository tierRepository,
                        DashboardService dashboardService,
                        DatasetRepository datasetRepository,
                        ChartRepository chartRepository,
                        ScreenRepository screenRepository,
                        UserRepository userRepository,
                        AppUserRepository appUserRepository,
                        EmailTemplateRepository emailTemplateRepository,
                        ItemRepository itemRepository, SectionItemRepository sectionItemRepository, NotificationService notificationService,
                        EndpointService endpointService,
                        ApiKeyRepository apiKeyRepository,
                        KeyValueRepository keyValueRepository,
                        ChartQuery chartQuery,
                        AppService appService,
                        KryptaService kryptaService,
                        PlatformTransactionManager transactionManager,
                        ObjectMapper MAPPER,
                        EntryBatchRepository entryBatchRepository,
                        HttpClient HTTP_CLIENT,
//                        @Qualifier("asyncExec") Executor executor,
                        @Lazy EntryService self) {
        this.entryRepository = entryRepository;
        this.entryTrailRepository = entryTrailRepository;
        this.customEntryRepository = customEntryRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.bucketService = bucketService;
        this.entryApprovalRepository = entryApprovalRepository;
        this.entryApprovalTrailRepository = entryApprovalTrailRepository;
        this.dynamicSQLRepository = dynamicSQLRepository;
        this.formService = formService;
        this.formRepository = formRepository;
        this.mailService = mailService;
        this.pushService = pushService;
        this.dashboardService = dashboardService;
        this.tierRepository = tierRepository;
        this.datasetRepository = datasetRepository;
        this.chartRepository = chartRepository;
        this.screenRepository = screenRepository;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.itemRepository = itemRepository;
        this.sectionItemRepository = sectionItemRepository;
        this.notificationService = notificationService;
        this.endpointService = endpointService;
        this.apiKeyRepository = apiKeyRepository;
        this.chartQuery = chartQuery;
        this.appService = appService;
        this.kryptaService = kryptaService;
        this.keyValueRepository = keyValueRepository;
//        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.MAPPER = MAPPER;
        this.entryBatchRepository = entryBatchRepository;
        this.HTTP_CLIENT = HTTP_CLIENT;
//        this.executor = executor;
        this.self = self;


        String dayjs = null;
        try {
            dayjs = new String(new ClassPathResource("dayjs.min.js")
                    .getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.dayjsSource = Source.newBuilder("js", dayjs, "dayjs.js").buildLiteral();

    }

    @Transactional
    public Entry assignApprover(Long entryId, Long atId, String email) {
        Entry entry = entryRepository.findById(entryId).orElseThrow();

        Tier gat = tierRepository.findById(atId).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", atId));
        Map<Long, String> approver = entry.getApprover();

        approver.put(atId, email);
        entry.setApprover(approver);
        entryRepository.save(entry); //should already have $id

        List<Long> emailTemplates = gat.getAssignMailer();

        for (Long t : emailTemplates) {
            triggerMailer(t, entry, gat, email);
        }

        return entry;
    }

    /**
     * FOR LAMBDA USAGE
     **/
    @Transactional(readOnly = true)
    public Entry byId(Long id, Lambda lambda) throws Exception {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return entry;
        } else {
            throw new Exception("Lambda trying to view external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    @Transactional(readOnly = true)
    public Page<EntryDto> dataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        String cond = String.valueOf(Optional.ofNullable(filters.get("@cond")).orElse("AND"));
        String searchText = String.valueOf(Optional.ofNullable(filters.get("searchText")).orElse(""));
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return findListByDataset(datasetId, searchText, email, filters, cond, null, filters.get("ids") != null ? (List<Long>) filters.get("ids") : null, PageRequest.of(0, Integer.MAX_VALUE), null);
        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    @Transactional(readOnly = true)
    public List<ObjectNode> flatDataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        String cond = String.valueOf(Optional.ofNullable(filters.get("@cond")).orElse("AND"));
        String searchText = String.valueOf(Optional.ofNullable(filters.get("searchText")).orElse(""));

        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            Map<String, Set<String>> fieldsMap = getFieldsMap(dataset);

            // --- NEW: Safely parse status and prevStatus from Dataset ---
            Map<String, String> statusMap = null;
            if (dataset.getStatusFilter() != null && !dataset.getStatusFilter().isNull() && !dataset.getStatusFilter().isEmpty()) {
                statusMap = MAPPER.convertValue(dataset.getStatusFilter(), Map.class);
            }

            Map<String, String> prevStatusMap = null;
            if (dataset.getPrevStatusFilter() != null && !dataset.getPrevStatusFilter().isNull() && !dataset.getPrevStatusFilter().isEmpty()) {
                prevStatusMap = MAPPER.convertValue(dataset.getPrevStatusFilter(), Map.class);
            }
            // ------------------------------------------------------------

            Page<EntryDto> entryList = customEntryRepository.findDataPaged(EntryFilter.builder()
                    .filters(filters)
                    .searchText(searchText)
                    .status(statusMap)         // <-- PASS IT TO THE BUILDER
                    .prevStatus(prevStatusMap) // <-- PASS IT TO THE BUILDER
                    .cond(cond)
                    .form(dataset.getForm())
                    .formId(dataset.getForm().getId())
                    .action(false)
                    .build().filter(), fieldsMap, PageRequest.of(0, Integer.MAX_VALUE));

            return entryList.map(e -> {
                ObjectNode o = e.getData().deepCopy();
                o.set("$prev", e.getPrev());
                return o;
            }).getContent();

        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }
    /**
     * FOR LAMBDA
     **/
    @Transactional(readOnly = true)
    public Long count(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        String cond = Optional.ofNullable(filters.get("@cond")).orElse("AND") + "";
        String searchText = String.valueOf(Optional.ofNullable(filters.get("searchText")).orElse(""));
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return countByDataset(datasetId, searchText, email, filters, cond, null);
        } else {
            throw new Exception("Lambda trying to count external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    @Transactional(readOnly = true)
    public Stream<Entry> streamDataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        String cond = Optional.ofNullable(filters.get("@cond")).orElse("AND") + "";
        String searchText = String.valueOf(Optional.ofNullable(filters.get("searchText")).orElse(""));
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return findListByDatasetStream(datasetId, searchText, email, filters, cond, null, filters.get("ids") != null ? (List<Long>) filters.get("ids") : null, null);
        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    @Transactional
    public Entry updateOwner(Long id, String email, Lambda lambda) throws Exception {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            entry.setEmail(email);
            entryRepository.save(entry); //already have $id
            try {
                self.trail(entry.getId(), entry.getData(), EntryTrail.UPDATED, entry.getForm().getId(), getPrincipalEmail(), "Change data owner to " + email,
                        entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
            } catch (Exception e) {
            }

            return entry;
        } else {
            throw new Exception("Lambda trying to update external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Entry save(Entry entry, Lambda lambda) throws Exception {
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return save(entry.getForm().getId(), entry, null, entry.getEmail(), true);
        } else {
            throw new Exception("Lambda trying to update external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Entry save(Map entry, Long formId, Long prevId, Lambda lambda) throws Exception {

        Entry e = MAPPER.convertValue(entry, Entry.class);
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        if (Objects.equals(form.getApp().getId(), lambda.getApp().getId())) {
            return save(form.getId(), e, prevId, e.getEmail(), true);
        } else {
            throw new Exception("Lambda trying to update external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Entry submit(Long id, String email, Lambda lambda) throws Exception {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return submit(id, email);
        } else {
            throw new Exception("Lambda trying to submit external entry");
        }
    }



    /**
     * FOR LAMBDA
     **/
    public void delete(Long id, String email, Lambda lambda) throws Exception {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            deleteEntry(id, email);
        } else {
            throw new Exception("Lambda trying to delete external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Object chart(Long id, Map filters, String email, Lambda lambda) {
        return getChartMapDataNative(id, filters, email, null);
    }

    /**
     * FOR LAMBDA
     **/
    public Entry update(Long entryId, JsonNode obj, String root, Lambda lambda) throws Exception {
        return updateField(entryId, obj, root, lambda.getApp().getId());
    }

    /**
     * FOR LAMBDA
     **/
    public Entry update(Long entryId, Map obj, Lambda lambda) throws Exception {
        return updateField(entryId, MAPPER.convertValue(obj, JsonNode.class), null, lambda.getApp().getId());
    }

    /**
     * FOR LAMBDA
     **/
    public Entry approval(Long entryId, Map approval, String email, Lambda lambda) throws Exception {
        boolean silent = false;
        if (approval.get("silent") != null) {
            silent = (boolean) approval.get("silent");
            approval.remove("silent");
        }

        EntryApproval ea = MAPPER.convertValue(approval, EntryApproval.class);
        //// CHECK TOK KLAK
        //Long id, EntryApproval gaa, String email
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return actionApp(entryId, ea, silent, email);
        } else {
            TenantLogger.error(entry.getForm().getAppId(), "form", entry.getFormId(), "Lambda with app id " + lambda.getApp().getId() + " trying to approve external entry");
            throw new Exception("Lambda trying to approve external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    @Transactional
    public Entry relinkPrev(Long entryId, Long prevEntryId, Lambda lambda) throws Exception {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));
        Entry prevEntry = entryRepository.findById(prevEntryId).orElseThrow(() -> new ResourceNotFoundException("Prev entry", "id", prevEntryId));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            entry.setPrevEntry(prevEntry);
            return entryRepository.save(entry); //already have $id
        } else {
            throw new Exception("Lambda trying to approve external entry");
        }
    }

//    @Transactional // TRANSACTIONAL NOT REQUIRED
    public Entry save(Long formId, Entry entry, Long prevId, String email, boolean trail) throws Exception {

        final boolean isNewEntry = entry.getId() == null;

        // RESOLVE FORM
        Form form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        JsonNode formX = form.getX(); // always use config x from original form

        if (formX != null && formX.has("extended")) {
            Long extendedId = formX.path("extended").asLong();
            form = formRepository.findById(extendedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", extendedId));
        }

        // Detach to prevent unintended updates
        // After this, form was updated in memory to be used to set counter postPersist
        entityManager.detach(form); // Is this really needed?

        // CHECK FOR PREVIOUS ENTRY. Only require prevId if entry is new and form have prev form.
        if (form.getPrev() != null && prevId == null && isNewEntry) {
            TenantLogger.error(form.getAppId(),"form",formId,"Previous entry Id is required for form with previous form");
            throw new IllegalArgumentException("Previous entry Id is required for form with previous form");
        }

        // SERVER-SIDE VALIDATION
        // load validation setting from KV config (CACHED)
        Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation"); // CACHED
        boolean serverValidation = validateOpt.map("true"::equals).orElse(false);
        boolean skipValidate = formX != null && formX.path("skipValidate").asBoolean(false);

        /** NEW!!!!!!!!!! Check before deploy! Server-side data validation ***/
        if (form.isValidateSave() && serverValidation && !skipValidate) {
            String jsonSchema = formService.getJsonSchema(form); // CACHED!!
            Helper.ValidationResult result = Helper.validateJson(jsonSchema, entry.getData());
            if (!result.valid()) {
                logger.error("Invalid JSON: " + result.errorMessagesAsString());
                TenantLogger.error(form.getAppId(),"form",formId,"JSON validation failed: " + result.errorMessagesAsString());
                throw new JsonSchemaValidationException(result.errors());
            }
        }

        JsonNode snap = entry.getData();
        if (!isNewEntry) { // entry is not new
            var entryId = entry.getId();
            Entry entryFromDb = entryRepository.findById(entry.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));
            snap = entryFromDb.getData();
            entry.setPrevEntry(entryFromDb.getPrevEntry()); // ensure no prevEntry reassign

            if (entryFromDb.getForm() != null) {
                form = entryFromDb.getForm(); // ensure not reassign form if already set
                entityManager.detach(form);
            }
        } else {
            if (prevId != null) {
                // if prevId=null dont do any assignment/re-assignment of prevData.
                // only do prev assignment when entry is new and prevId not null
                entryRepository.findById(prevId).ifPresent(entry::setPrevEntry);
            }
        }

        if (entry.getEmail() == null) {
            entry.setEmail(email);
        }

        // TOTALLY NEW ENTRY
        if (isNewEntry) {
            entry.setCurrentStatus(Entry.STATUS_DRAFTED);
            int latestCounter = formService.incrementAndGetCounter(form.getId());
            // update form counter in memory -> This is needed because entry prePersist/postPersist rely on
            // assigned form to get the latest counter
            form.setCounter(latestCounter);
        }

        entry.setForm(form); // set form, either from formId, or existing entry form when update.
        ///// ##### Map data and compileTpl ##### /////
        entry = updateApprover(entry, email); // require form to be set

        final Entry savedEntry = self.justSave(entry);

        if (formX!=null && formX.path("autoSync").asBoolean(false) && entry.getId() != null) {
            // resync only when entry was saved and form is set to autosync
            self.resyncEntryData_ModelPicker(form.getId(), entry.getData());
        }

        // SHOULD BE ASYNC POSTPROCESSING
        if (trail) { // already ASYNC
            self.trail(entry.getId(), snap, isNewEntry ? EntryTrail.CREATED : EntryTrail.SAVED, form.getId(), email, "Saved by " + email,
                    entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        }

        if (isNewEntry) {
            form.getAddMailer().forEach(m -> triggerMailer(m, savedEntry, null, email));
        } else {
            form.getUpdateMailer().forEach(m -> triggerMailer(m, savedEntry, null, email));
        }

        try { // already async
            self.trailApproval(savedEntry.getId(), null, null, "saved", "Saved by " + email, getPrincipalEmail());
        } catch (Exception e) {}

        String action = isNewEntry ? "save" : "update";
        self.recordKryptaOn(formX, form.getKrypta(), action, savedEntry);

        // Performance boost: We only need the second save to capture $id/$code/$counter
        // generated by @PostPersist for brand NEW entries. Updates don't need a double-save.
        if (isNewEntry) {
            return self.justSave(savedEntry);
        }

        return savedEntry; // 2nd save to save $id, $code, $counter set at @PostPersist
    }

    @Transactional
    public void recordKryptaOn(JsonNode formX, JsonNode kryptaNode, String on, Entry savedEntry) {

        if (formX != null && formX.hasNonNull("wallet")
                && formX.path("wallet").asBoolean(false)
                && kryptaNode != null && kryptaNode.hasNonNull(on)
        ) {

            JsonNode parametersNode = kryptaNode.path(on);
            Long walletId = parametersNode.path("wallet").asLong();
            String walletFn = parametersNode.path("fn").asText();
            String walletTextTpl = parametersNode.path("tpl").asText();

            savedEntry.getTxHash().put(on, "pending");

            // ✅ Schedule after commit to avoid missing Entry
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                try {
                    self.recordKryptaContract(savedEntry.getId(), on, walletId, walletFn, walletTextTpl);
                } catch (Exception e) {
                    if (savedEntry.getForm() != null) {
                        TenantLogger.error(savedEntry.getForm().getAppId(), "form", savedEntry.getFormId(), "Problem recording to KRYPTA after commit: " + e.getMessage());
                    }
                    logger.error("Problem recording to KRYPTA after commit: " + e.getMessage());
                }
                }
            });
        }
    }



    // ## To handle issue with nested transactional that would lock outer transaction
    @Transactional
    public Entry justSave(Entry entry) {
        return entryRepository.save(entry);
    }

    // This is to actually process the value and perform actual blockchain transaction
//    @Transactional
    @Async("asyncExec")
    public void recordKryptaContract(Long entryId, String event, Long walletId, String functionName, String tpl) throws Exception {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

        Map entryMap = MAPPER.convertValue(entry, Map.class);
        Map entryDataMap = MAPPER.convertValue(entry.getData(), Map.class);
        Map prevDataMap = MAPPER.convertValue(entry.getPrev(), Map.class);

//        Map<String, Object> dataMap = new HashMap<>();
//        dataMap.put("data", entryDataMap);
//        dataMap.put("prev", prevDataMap);
//        dataMap.put("_", entryMap);
//        dataMap.put("now", Instant.now().toEpochMilli());
//
//        String compiled = Helper.compileTpl(tpl, dataMap);
//
//        String txHash = (String) kryptaService.call(walletId, functionName, MAPPER.readValue(compiled, Map.class));

        Map<String, Object> dataMapNew = new HashMap<>();
        dataMapNew.put("$", entryDataMap);
        dataMapNew.put("$prev$", prevDataMap);
        dataMapNew.put("$_", entryMap);
        dataMapNew.put("$prev$_", entry.getPrevEntry());
        dataMapNew.put("$now$", Instant.now().toEpochMilli());

        String result = execJs("krypta-"+ entry.getFormId()+'-'+tpl, tpl, dataMapNew);

        String txHashNew = (String) kryptaService.call(walletId, functionName, MAPPER.readValue(result, Map.class));

        if (txHashNew != null) {
            entryRepository.updateTxHash(entryId, event, txHashNew); //!This works
            logger.info("Recorded to KRYPTA: " + txHashNew + ", on event: " + event + ", for entry id: " + entryId);
        }
    }

    public Entry updateApprover(Entry entry, String email) {

        // Safely initialize the approver map if it's null
        Map<Long, String> approver = entry.getApprover() != null ? entry.getApprover() : new HashMap<>();

        Entry entryHolder = new Entry();
        BeanUtils.copyProperties(entry, entryHolder, "form", "prevEntry");

        // 1. Hoist heavy Jackson conversions OUTSIDE the loop (Compute exactly once)
        Map<String, Object> entryMap = MAPPER.convertValue(entryHolder, Map.class);
        Map<String, Object> entryDataMap = MAPPER.convertValue(entry.getData(), Map.class);
        Map<String, Object> prevDataMap = MAPPER.convertValue(entry.getPrev(), Map.class);

        // Pre-compute the approval Maps outside the loop
        Map<String, Object> approvalMap = null;
        Map<String, Object> apprDataMap = null;

        if (entry.getApproval() != null) {
            approvalMap = MAPPER.convertValue(entry.getApproval(), Map.class);
            Map<Long, JsonNode> apprData = new HashMap<>();

            entry.getApproval().forEach((ap, val) -> apprData.put(ap, val.getData()));
            apprDataMap = MAPPER.convertValue(apprData, Map.class);
        }

        // 2. Lazy-loader flags to prevent redundant database queries
        Map<String, Object> userMap = null;
        boolean userFetched = false;

        Long appId = entry.getForm() != null && entry.getForm().getApp() != null ? entry.getForm().getApp().getId() : null;

        for (Tier at : entry.getForm().getTiers()) {
            if (at == null) continue; // Safety check

            String a = "";
            String type = at.getType();

            if ("DYNAMIC".equals(type)) {
                try {
                    a = formService.getOrgMapApprover(at, email, entry);
                } catch (JsonProcessingException e) {
                    // 3. Replaced printStackTrace with proper logging
                    TenantLogger.error(appId, "form", entry.getFormId(), "Failed to get DYNAMIC approver for entry "+entry.getId()+": "+e.getMessage());
                    logger.error("Failed to get DYNAMIC approver for entry {}: {}", entry.getId(), e.getMessage());
                }
            } else if ("FIXED".equals(type)) {
                Map<String, Object> dataMap = new HashMap<>();

                String atApprover = at.getApprover() != null ? at.getApprover() : "";

                // Lazy load user ONLY ONCE if multiple tiers require it
                if (atApprover.contains("$user$")) {
                    if (!userFetched) {
                        User user = userRepository.findFirstByEmailAndAppId(entry.getEmail(), appId)
                                .orElseGet(() -> {
                                    User newUser = new User();
                                    newUser.setEmail(entry.getEmail());
                                    return newUser;
                                });
                        userMap = MAPPER.convertValue(user, Map.class);
                        userFetched = true;
                    }
                    dataMap.put("user", userMap);
                }

                dataMap.put("data", entryDataMap);
                dataMap.put("prev", prevDataMap);
                dataMap.put("_", entryMap);

                // Inject the pre-computed approval maps
                if (approvalMap != null) {
                    dataMap.put("approval_", approvalMap);
                    dataMap.put("approval", apprDataMap);
                }

                dataMap.put("now", Instant.now().toEpochMilli());

                String compiled = compileTpl(atApprover, dataMap);

                if (compiled != null && !compiled.isBlank()) {
                    a = Arrays.stream(compiled.split(","))
                            .map(String::trim) // Clean whitespace to prevent slipping past the empty check
                            .filter(Predicate.not(String::isEmpty))
                            .collect(Collectors.joining(","));
                }

            } else if ("GROUP".equals(type) && at.getApproverGroup() != null) {

                List<String> emails = appUserRepository.findEmailsByGroupId(at.getApproverGroup());

                if (emails != null) {
                    a = emails.stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(Predicate.not(String::isEmpty))
                            .collect(Collectors.joining(","));
                }
            }

            approver.put(at.getId(), a);
        }

        entry.setApprover(approver);
        return entry;
    }

    @Transactional
    public void triggerMailer(Long mailer, Entry entry, Tier gat, String initBy) {
        if (mailer==null) return;

        try {
            EmailTemplate template = emailTemplateRepository.findByIdAndEnabled(mailer, Constant.ENABLED);//.findByCodeAndEnabled(mailer, Constant.ENABLED);

            if (template == null) return;

            Map<String, Object> contentMap = new HashMap<>();
            contentMap.put("_", MAPPER.convertValue(entry, Map.class));
            Map<String, Object> result = MAPPER.convertValue(entry.getData(), Map.class);
            Map<String, Object> prev = MAPPER.convertValue(entry.getPrev(), Map.class);

            App app = entry.getForm().getApp();
            String url = "https://";
            if (entry.getForm().getApp().getAppDomain() != null) {
                url += app.getAppDomain() + "/#";
            } else {
                String dev = app.isLive() ? "" : "--dev";
                url += app.getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN + "/#";
            }

            contentMap.put("uiUri", url);
            contentMap.put("viewUri", url + "/form/" + entry.getForm().getId() + "/view?entryId=" + entry.getId());
            contentMap.put("editUri", url + "/form/" + entry.getForm().getId() + "/edit?entryId=" + entry.getId());

//            if (result != null) {
//                contentMap.put("code", result.get("$code"));
//                contentMap.put("id", result.get("$id"));
//                contentMap.put("counter", result.get("$counter"));
//            }
//
//            if (prev != null) {
//                contentMap.put("prev_code", prev.get("$code"));
//                contentMap.put("prev_id", prev.get("$id"));
//                contentMap.put("prev_counter", prev.get("$counter"));
//            }

            contentMap.put("data", result);
            contentMap.put("prev", prev);


//            System.out.println(contentMap);

//            Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId());
//            if (u.isPresent()) {
//                Map userMap = MAPPER.convertValue(u.get(), Map.class);
//                contentMap.put("user", userMap);
//            }

            userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId())
                    .ifPresentOrElse(
                            user -> contentMap.put("user", MAPPER.convertValue(user, Map.class)),
                            () -> {
                                String safeEmail = entry.getEmail() != null ? entry.getEmail() : "anonymous";
                                contentMap.put("user", Map.of("email", safeEmail, "name", safeEmail));
                            }
                    );

            if (gat != null) {
                contentMap.put("tier", gat);
            }

            if (entry.getApproval() != null && gat != null) {
                EntryApproval approval_ = entry.getApproval().get(gat.getId());
                if (approval_ != null) {
                    Map<String, Object> approval = MAPPER.convertValue(approval_.getData(), Map.class);
                    contentMap.put("approval_", approval_);
                    contentMap.put("approval", approval);
                }
            }


            List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
            if (template.isToUser()) {
                recipients.add(entry.getEmail());
            }
            if (template.isToAdmin()) {
                if (entry.getForm().getAdmin() != null) {
                    List<String> adminEmails = appUserRepository.findEmailsByGroupId(entry.getForm().getAdmin().getId());
                    if (!adminEmails.isEmpty()) {
                        recipients.addAll(adminEmails);
                    }
                }

            }
            if (gat != null && template.isToApprover()) {
                if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
                    recipients.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
                }
            }
            if (!Objects.isNull(template.getToExtra())) {
                String extra = Helper.compileTpl(template.getToExtra(), contentMap);
                if (!extra.isEmpty()) {
                    recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                            .filter(str -> !str.isBlank())
                            .toList());
                }
            }

            List<String> recipientsCc = new ArrayList<>();
            if (template.isCcUser()) {
                recipientsCc.add(entry.getEmail());
            }
            if (template.isCcAdmin()) {
                if (entry.getForm().getAdmin() != null) {
                    List<String> adminEmails = appUserRepository.findEmailsByGroupId(entry.getForm().getAdmin().getId());
                    if (!adminEmails.isEmpty()) {
                        recipientsCc.addAll(adminEmails);
                    }
                }

            }
            if (gat != null && template.isCcApprover()) {
                if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
                    recipientsCc.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
                }
            }
            if (!Objects.isNull(template.getCcExtra())) {
                String ccextra = Helper.compileTpl(template.getCcExtra(), contentMap);
                if (!ccextra.isEmpty()) {
                    recipientsCc.addAll(Arrays.stream(ccextra.replaceAll(" ", "").split(","))
                            .filter(str -> !str.isBlank())
                            .toList());
                }
            }

            String[] rec = recipients.toArray(new String[0]);
            String[] recCc = recipientsCc.toArray(new String[0]);

            if (template.isPushable()) {
                pushService.sendMailPush(entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, template, contentMap, entry.getForm().getApp());
            }

            mailService.sendMail(entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, template, contentMap, entry.getForm().getApp(), initBy, entry.getId());
        } catch (Exception e) {
            if (entry.getForm() != null) {
                TenantLogger.error(entry.getForm().getAppId(), "form", entry.getFormId(), "Error trigger mailer: " + e.getMessage());
            }
            logger.error("Error trigger mailer: " + e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public Entry findById(Long id, boolean anonymous, HttpServletRequest req) {

        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        Form form = entry.getForm(); // form get from entry

        boolean isPublic = form.isPublicEp();

        if (anonymous && !isPublic) {
            // access to private dataset from public endpoint is not allowed
            TenantLogger.error(form.getAppId(),"form",form.getId(),"Access to private form entry from public endpoint is not allowed");
            throw new OAuth2AuthenticationProcessingException("Private Form Entry: Access to private form entry from public endpoint is not allowed");
        } else {
            String apiKeyStr = Helper.getApiKey(req);
            if (apiKeyStr != null) {
                ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
                if (apiKey != null && apiKey.getAppId() != null && !form.getApp().getId().equals(apiKey.getAppId())) {
                    TenantLogger.error(form.getAppId(),"form",form.getId(),"Invalid API Key: API Key used is not designated for the app of the dataset");
                    throw new OAuth2AuthenticationProcessingException("Invalid API Key: API Key used is not designated for the app of the dataset");
                }
            }
        }

        return entry;
    }

//    public boolean checkAccess(List<Long> accessList, String email, Long appId) throws Exception {
//        if (accessList != null && accessList.size() > 0) {
//
//            List<Long> userAuthoritiesList = appUserRepository
//                    .findIdsByAppIdAndEmailAndStatus(appId, email, "approved");
//            accessList.retainAll(userAuthoritiesList);
//
//            if (accessList.size() == 0) {
//                throw new Exception("User doesn't have access to the dataset");
//            }
//            return true;
//        }
//        return true;
//    }


    public boolean checkAccess(List<Long> accessList, String email, Long appId) throws AccessDeniedException {
        // 1. Fail fast: If no specific access is required, grant access immediately
        if (accessList == null || accessList.isEmpty()) {
            return true;
        }

        // 2. Fetch user authorities (consider a Set for better lookup performance)
        Set<Long> userAuthorities = new HashSet<>(appUserRepository
                .findIdsByAppIdAndEmailAndStatus(appId, email, "approved"));

        // 3. Check for intersection without mutating the original input list
        boolean hasAccess = accessList.stream().anyMatch(userAuthorities::contains);

        if (!hasAccess) {
            // 4. Use a specific exception type rather than the generic 'Exception'
            throw new AccessDeniedException("User doesn't have access to the dataset");
        }

        return true;
    }


    @Transactional
    public Map<String, Object> blastEmailByDataset(Long datasetId, String searchText, String email, Map filters, String cond, EmailTemplate emailTemplate, List<Long> ids, HttpServletRequest req, String initBy, UserPrincipal userPrincipal) throws Exception {

        Map<String, Object> data = new HashMap<>();
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        App app = dataset.getApp();

        if (!dataset.isCanBlast()) {
            TenantLogger.error(dataset.getAppId(),"dataset",dataset.getId(),"Unauthorized email blast request. Dataset doesn't allow email blast.");
            throw new Exception("Unauthorized email blast request. Dataset doesn't allow email blast.");
        }

        // check only if userPrincipal not null. If null means it is scheduled. If scheduled skip check.
        if (userPrincipal != null) {
            try {
                checkAccess(dataset.getAccessList(), userPrincipal.getEmail(), dataset.getAppId());
            } catch (AccessDeniedException e) {
                // Log the failure to your TenantLogger before the method terminates
                TenantLogger.error(dataset.getAppId(),"dataset", dataset.getId(),"Access denied for user " + userPrincipal.getEmail() + ". " + e.getMessage());

                // Re-throw to stop the email blast
                throw e;
            }
        }

        AtomicInteger index = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        try (Stream<Entry> entryStream = findListByDatasetStream(datasetId, searchText, email, filters, cond, null, ids, req)) {
            entryStream.forEach(entry -> {
                total.getAndIncrement();

                // Resolve all lazy properties
                Form form = entry.getForm();
                List<Tier> tiers = form.getTiers();
                UserGroup admin = form.getAdmin();
                Map<Long, EntryApproval> approval = entry.getApproval();
                Map<Long, String> approver = entry.getApprover();


                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("_", entry);
                Map<String, Object> result = MAPPER.convertValue(entry.getData(), Map.class);
                Map<String, Object> prev = MAPPER.convertValue(entry.getPrev(), Map.class);

                this.entityManager.detach(entry);

                String url = "https://" + app.getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
                contentMap.put("uiUri", url);
                contentMap.put("viewUri", url + "/form/" + form.getId() + "/view?entryId=" + entry.getId());
                contentMap.put("editUri", url + "/form/" + form.getId() + "/edit?entryId=" + entry.getId());

                List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
                List<String> recipientsCc = new ArrayList<>();


//                if (result != null) {
//                    contentMap.put("code", result.get("$code"));
//                    contentMap.put("id", result.get("$id"));
//                    contentMap.put("counter", result.get("$counter"));
//                }
//
//                if (prev != null) {
//                    contentMap.put("prev_code", prev.get("$code"));
//                    contentMap.put("prev_id", prev.get("$id"));
//                    contentMap.put("prev_counter", prev.get("$counter"));
//                }

                contentMap.put("data", result);
                contentMap.put("prev", prev);

                assert result != null;
                if (dataset.isCanBlast() && dataset.getBlastTo() != null) {
                    String blastToStr = result.get(dataset.getBlastTo()) + "";
                    recipients.addAll(Arrays.stream(blastToStr.replaceAll(" ", "").split(","))
                            .filter(str -> !str.isBlank())
                            .toList());
                }

                try {
//                    Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), app.getId());
//                    if (u.isPresent()) {
//                        Map userMap = MAPPER.convertValue(u.get(), Map.class);
//                        contentMap.put("user", userMap);
//                    }

                    userRepository.findFirstByEmailAndAppId(entry.getEmail(), app.getId())
                            .ifPresentOrElse(
                                    user -> contentMap.put("user", MAPPER.convertValue(user, Map.class)),
                                    () -> {
                                        String safeEmail = entry.getEmail() != null ? entry.getEmail() : "anonymous";
                                        contentMap.put("user", Map.of("email", safeEmail, "name", safeEmail));
                                    }
                            );

                    Tier gat = null;
                    if (tiers.size() > 0 && entry.getCurrentTier() != null) {
                        gat = tiers.get(Math.max(0,entry.getCurrentTier()));
                        if (gat != null) {
                            contentMap.put("tier", gat);
                        }
                    }

                    if (approval != null && gat != null) {
                        EntryApproval eapproval_ = approval.get(gat.getId());
                        if (eapproval_ != null) {
                            Map<String, Object> eapproval = MAPPER.convertValue(eapproval_.getData(), Map.class);
                            contentMap.put("approval_", eapproval_);
                            contentMap.put("approval", eapproval);
                        }
                    }

                    if (emailTemplate.isToUser()) {
                        recipients.add(entry.getEmail());
                    }
                    if (emailTemplate.isToAdmin()) {
                        if (admin != null) {
                            List<String> adminEmails = appUserRepository.findEmailsByGroupId(admin.getId());
                            if (!adminEmails.isEmpty()) {
                                recipients.addAll(adminEmails);
                            }
                        }
                    }
                    if (gat != null && emailTemplate.isToApprover()) {
                        if (!approver.isEmpty() && approver.get(gat.getId()) != null) {
                            recipients.addAll(Arrays.asList(approver.get(gat.getId()).replaceAll(" ", "").split(",")));
                        }
                    }
                    if (!Objects.isNull(emailTemplate.getToExtra())) {
                        String extra = Helper.compileTpl(emailTemplate.getToExtra(), contentMap);
                        if (!extra.isEmpty()) {
                            recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                                    .filter(str -> !str.isBlank())
                                    .toList());
                        }
                    }

                    if (emailTemplate.isCcUser()) {
                        recipientsCc.add(entry.getEmail());
                    }
                    if (emailTemplate.isCcAdmin()) {
                        if (admin != null) {
                            List<String> adminEmails = appUserRepository.findEmailsByGroupId(admin.getId());
                            if (!adminEmails.isEmpty()) {
                                recipientsCc.addAll(adminEmails);
                            }
                        }

                    }
                    if (gat != null && emailTemplate.isCcApprover()) {
                        if (!approver.isEmpty() && approver.get(gat.getId()) != null) {
                            recipientsCc.addAll(Arrays.asList(approver.get(gat.getId()).replaceAll(" ", "").split(",")));
                        }
                    }
                    if (!Objects.isNull(emailTemplate.getCcExtra())) {
                        String extra = Helper.compileTpl(emailTemplate.getCcExtra(), contentMap);
                        if (!extra.isEmpty()) {
                            recipientsCc.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                                    .filter(str -> !str.isBlank())
                                    .toList());
                        }
                    }
                } catch (Exception e) {
                    logger.error("ERROR BLAST@@@######::" + e.getMessage());
                }

                String[] rec = recipients.toArray(new String[0]);
                String[] recCc = recipientsCc.toArray(new String[0]);

                if (emailTemplate.isPushable()) {
                    pushService.sendMailPush(app.getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, contentMap, app);
                }

                mailService.sendMail(app.getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, contentMap, app, initBy, entry.getId());
                index.getAndIncrement();
            });
        }

        data.put("totalCount", total.get());
        data.put("totalSent", index.get());
        data.put("success", total.get() == index.get());
        data.put("partial", total.get() > index.get());

        return data;

    }


    @Transactional(readOnly = true)
    public Entry findFirstByParam(Long formId, Map filters, HttpServletRequest req, boolean anonymous) throws Exception {
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        boolean isPublic = form.isPublicEp();


        // if form is extended form, then use original form
        if (form.getX().get("extended") != null) {
            Long extendedId = form.getX().get("extended").asLong();
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        if (anonymous && !isPublic) {
            // access to private dataset from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Form Entry: Access to private form entry from public endpoint is not allowed");
        } else {

            Map filtersReq = new HashMap();
            if (req != null) {
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    if (entry.getKey().contains("$")) {
                        filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                    }
                }
            }

            String cond = "AND";
            if (filters != null && filters.containsKey("@cond")) {
                cond = filters.get("@cond").toString();
            }

            final Map newFilter = new HashMap();
            if (filters != null) {
                newFilter.putAll(filters);
            }

            if (filtersReq.size() > 0) {
                newFilter.putAll(filtersReq);
            }

            JsonNode qFilter = null;
            try {
                qFilter = MAPPER.readTree(form.getX().at("/qFilter").asText());
            } catch (Exception e) { }

            // Perlu pake findAll (instead of findPaged) sbb perlu return type Entry
            Page<Entry> entry = entryRepository.findAll(EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .filters(newFilter)
                    .qBuilder(qFilter)
                    .action(false)
                    .cond(cond)
                    .build().filter(), PageRequest.of(0, 1));

            return entry.getContent().stream()
                    .findFirst().orElseThrow(() -> new ResourceNotFoundException("Entry", "filters", filters));
        }
    }

    public Page<EntryDto> findByFormId(Long formId, Pageable pageable) {
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        return customEntryRepository.findPaged(form, EntryFilter.builder()
                .formId(form.getId())
                .form(form)
                .action(false)
                .build().filter(), null, false, pageable);

//        return entryRepository.findByFormId(formId, form.isLive(), pageable);
    }

    @Transactional
    public Entry retractApp(Long id, String email) {

        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        self.trail(entry.getId(), entry.getData(), EntryTrail.RETRACTED, entry.getForm().getId(), email, "Retracted by " + email,
                entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());


        entry.setCurrentStatus(Entry.STATUS_DRAFTED);
        entry.setCurrentTier(null);
        entry.setCurrentTierId(null);
        entry.setFinalTierId(null);
        entry.getApproval().clear();

        entry.getForm().getRetractMailer().forEach(t -> triggerMailer(t, entry, null, email));

        self.trailApproval(id, null, null, Entry.STATUS_DRAFTED, "RETRACTED by User " + Optional.ofNullable(email).orElse(""), getPrincipalEmail());

        self.recordKryptaOn(entry.getForm().getX(), entry.getForm().getKrypta(), "retract", entry);

        return entryRepository.save(entry);

    }

    /*
     * $update$(id,{'name':'asdaa'},'prev')
     * */
//    @Transactional // !!!NOT TRANSACTIONAL SHOULD BE REQUIRED HERE
    public Entry updateField(Long entryId, JsonNode obj, String root, Long appId) throws Exception {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

        String principal = getPrincipalEmail();
        JsonNode snap = entry.getData();

        if (entry.getForm().getApp().getId().equals(appId) || appId == null) {


            Form form = entry.getForm();
            Long formId = form.getId();
            JsonNode formX = form.getX(); // always use config x from original form

            if (formX != null && formX.has("extended")) {
                Long extendedId = formX.path("extended").asLong();
                form = formRepository.findById(extendedId)
                        .orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", extendedId));
            }

            // SERVER-SIDE VALIDATION
            // load validation setting from KV config (CACHED)
            Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation"); // CACHED
            boolean serverValidation = validateOpt.map("true"::equals).orElse(false);
            boolean skipValidate = formX != null && formX.path("skipValidate").asBoolean(false);

            /** NEW!!!!!!!!!! Check before deploy! Server-side data validation ***/
            if (form.isValidateSave() && serverValidation && !skipValidate) {
                String jsonSchema = formService.getJsonSchema(form); // CACHED!!
                Helper.ValidationResult result = Helper.validateJson(jsonSchema, obj);
                if (!result.valid()) {
                    logger.error("Invalid JSON: " + result.errorMessagesAsString());
                    TenantLogger.error(form.getAppId(),"form",formId,"JSON validation failed: " + result.errorMessagesAsString());
                    throw new JsonSchemaValidationException(result.errors());
                }
            }


            // dari app yg sama atau appId == null
            JsonNode node1;
            boolean isPrev = "prev".equals(root);
            if (isPrev) {
                node1 = entry.getPrev();
            } else {
                node1 = entry.getData();
            }
            Map<String, Object> map2 = MAPPER.convertValue(obj, Map.class);

            if (isPrev) {
            } else {
                entry.setData(deepMerge(node1, obj));
            }

//            Long previousEntryId = Optional.ofNullable(entry.getPrevEntry())
//                    .map(prev -> prev.getId())
//                    .orElse(null);

            updateApprover(entry, entry.getEmail());
//            save(entry.getForm().getId(), entry, previousEntryId, entry.getEmail(), false);
            self.justSave(entry);


            self.trail(entryId, snap, EntryTrail.UPDATED, entry.getForm().getId(), principal, "Field(s) updated: " + map2.keySet() + " by " + principal,
                    entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());

        } else {
            TenantLogger.error(appId,"entry",entryId,"Unallowed attempt to update entry of different app");
            throw new Exception("Unallowed attempt to update entry of different app");
            // bukan app yg sama
        }


        return entry;
    }

    @Transactional(readOnly = true)
    public Page<EntryAttachment> findFilesById(long id, Pageable pageable) {
        return this.entryAttachmentRepository.findByEntryId(id, pageable);
    }

    @Transactional
    public Map<String, Object> undelete(long entryId, long trailId, String email) {
        int row = this.entryRepository.undeleteEntry(entryId);
        int r_approval = 0;
        if (row > 0) {
            r_approval = this.entryApprovalRepository.undeleteEntry(entryId);
        }
        EntryTrail entryTrail = entryTrailRepository.findById(trailId).orElseThrow(() -> new ResourceNotFoundException("EntryTrail", "id", trailId));
        entryTrail.setAction(EntryTrail.XREMOVED);
        entryTrailRepository.save(entryTrail);
        try {
            self.trail(entryId, null, EntryTrail.RESTORED, entryTrail.getFormId(), email, "Entry restored by " + email, null, null, null, null);
        } catch (Exception e) {
        }

        return Map.of("entry", row, "approval", r_approval);

    }

    @Transactional
    public Map<String, Object> undo(long entryId, long trailId, String email) {

        Optional<Entry> entryOpt = entryRepository.findById(entryId);
        Optional<EntryTrail> trailOpt = entryTrailRepository.findById(trailId);

        if (entryOpt.isPresent() && trailOpt.isPresent()) {
            Entry entry = entryOpt.get();
            EntryTrail trail = trailOpt.get();

            self.trail(entryId, entry.getData(), EntryTrail.REVERTED, entry.getForm().getId(), email, "Entry data reverted by " + email,
                    entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());

            EntryTrail et = entryTrailRepository.findById(trailId).orElseThrow(() -> new ResourceNotFoundException("EntryTrail", "id", trailId));
            et.setAction(EntryTrail.XUPDATED);
            entryTrailRepository.save(et);

            entry.setData(trail.getSnap());
            entry.setCurrentTier(trail.getSnapTier());
            entry.setCurrentTierId(trail.getSnapTierId());
            entry.setCurrentStatus(trail.getSnapStatus());
            entry.setCurrentEdit(trail.getSnapEdit());

            entryRepository.save(entry);

        }

        return Map.of("entry", 1, "approval", 0);

    }

    @Transactional
    public Entry actionApp(Long entryId, EntryApproval gaa, boolean silent, String email) {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

        try {
            String cp = getPrincipalEmail();
            boolean diffcp = !email.equals(cp);
            String remark = "Action taken by " + email;
            if (diffcp) {
                remark = "Action taken on behalf of " + email + " by " + cp;
            }
            self.trail(entry.getId(), entry.getData(), EntryTrail.APPROVAL, entry.getForm().getId(), cp, remark, entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        } catch (Exception e) { }

        entry = updateApprover(entry, email);

        Tier gat = tierRepository.findById(gaa.getTier().getId()).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", gaa.getTier().getId()));

        userRepository.findFirstByEmailAndAppId(email, entry.getForm().getApp().getId())
                        .ifPresent(gaa::setApprover);


        gaa.setEntry(entry);
        gaa.setTier(gat);
        gaa.setTierId(gat.getId());
        gaa.setEmail(email);
        gaa.setTimestamp(new Date());
        entry.setCurrentTierId(gat.getId());

        int currentTier = Math.toIntExact(gat.getSortOrder());

        List<MailerHolder> mailersToTrigger = new ArrayList<>();

        List<Tier> formTiers = entry.getForm().getTiers();

        Map<String, TierAction> gatActions = gat.getActions();

        if (gatActions != null) {
            if (gatActions.get(gaa.getStatus()) != null || gat.isAlwaysApprove()) {
                TierAction ta = gatActions.get(gaa.getStatus());

                if (ta != null) {
                    if ("nextTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < formTiers.size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = formTiers.get(currentTier);
                            if (ngat.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                ngat.getSubmitMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, ngat)));
                            }
                        }

                    } else if ("prevTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) - 1;

                    } else if ("goTier".equals(ta.getAction())) {
                        Entry finalEntry = entry;
                        Tier t1 = tierRepository.findById(ta.getNextTier()).orElseThrow(() -> {
                            TenantLogger.error(finalEntry.getForm().getAppId(),"form", finalEntry.getFormId(),"Tier not found for goTier action");
                            return new ResourceNotFoundException("Tier", "id", ta.getNextTier());
                        });
                        if (t1.getSubmitMailer() != null && !ta.isUserEdit()) {
                            // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                            t1.getSubmitMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, t1)));
                        }
                        currentTier = Math.toIntExact(t1.getSortOrder());
                    } else if ("curTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder());
                    }

                    entry.setCurrentStatus(ta.getCode());
                    entry.setCurrentEdit(ta.isUserEdit());

                    ta.getMailer().forEach(i -> mailersToTrigger.add(new MailerHolder(i, gat)));
                } else {
                    if (gat.isAlwaysApprove()) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < formTiers.size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = formTiers.get(currentTier);
                            if (ngat.getSubmitMailer() != null) { //&& !ta.isUserEdit()
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                ngat.getSubmitMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, ngat)));
                            }
                        }
                        entry.setCurrentStatus(Entry.STATUS_ALWAYS_APPROVE);
                        entry.setCurrentEdit(false);
                    }
                }
            }
        }

        if (currentTier == formTiers.size()) {
            // to mark this is final tier (the status could be approved, rejected, returned, resubmitted)
            entry.setFinalTierId(gat.getId());
        }

        entry.setCurrentTier(currentTier);

        entry.getApproval().put(gat.getId(), gaa);

        entry = entryRepository.save(entry);

        updateApprover(entry, entry.getEmail());

        EntryApprovalTrail eat = new EntryApprovalTrail(gaa);

        self.trailApproval(eat);

        if (!silent) {
            for (MailerHolder m : mailersToTrigger) {
                triggerMailer(m.mailerId, entry, m.tier, email);
            }
        }

        return entry;
    }

    /**
     * Shouldn't be used. Not used in any features
     *
     * @param ids
     * @param gas
     * @param email
     * @return
     */
    public Map<String, Object> actionApps(List<Long> ids, EntryApproval gas, String email) {
        Map<String, Object> data = new HashMap<>();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<String> failedMessage = new ArrayList<>();
        for (Long id : ids) {
            try {
                Entry e = actionApp(id, gas, false, email);
                success.getAndIncrement();
            } catch (Exception e) {
                failed.getAndIncrement();
                failedMessage.add(e.getMessage());
            }
        }
        if (ids.size() == success.get()) {
            data.put("success", "true");
        } else if (success.get() < ids.size() && success.get() > 0) {
            data.put("success", "partial");
        } else if (ids.size() > 0 && success.get() == 0) {
            data.put("success", "false");
        }
        data.put("successCount", success.get());
        data.put("errorCount", failed.get());
        data.put("logs", failedMessage);
        return data;
    }

    @Transactional
    public Entry saveApproval(Long entryId, EntryApproval gaa, String email) {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

        try {
            String cp = getPrincipalEmail();
            boolean diffcp = !email.equals(cp);
            String remark = "Action taken by " + email;
            if (diffcp) {
                remark = "Action taken on behalf of " + email + " by " + cp;
            }
            self.trail(entry.getId(), entry.getData(), EntryTrail.APPROVAL, entry.getForm().getId(), email, remark, entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        } catch (Exception e) {
        }

//        entry = updateApprover(entry, email); // knak comment???
        Entry finalEntry = entry;
        Tier gat = tierRepository.findById(gaa.getTier().getId()).orElseThrow(() -> {
            TenantLogger.error(finalEntry.getForm().getAppId(),"form", finalEntry.getFormId(),"Tier not found for saveApproval action");
            return new ResourceNotFoundException("Tier", "id", gaa.getTier().getId());
        });

        Optional<User> user = userRepository.findFirstByEmailAndAppId(email, entry.getForm().getApp().getId());

        user.ifPresent(gaa::setApprover);
        // Previously prevStatus is if prevStatus == 'nextTier' then move it back if the new update is prevTier or curTier.
        // But it no longer relevant since we are using sortOrder as index for tier movement
        // Planned for removal, and use actionApp to cater both action and save-approval.
        String prevStatus = entry.getApproval().get(gat.getId()).getStatus(); //why keep prevStatus?? // ONLY HERE

        gaa.setEntry(entry);
        gaa.setTier(gat); //disetara dgn actionApp
        gaa.setTierId(gat.getId()); //disetara dgn actionApp
        gaa.setEmail(email); //disetara dgn actionApp

        gaa.setTimestamp(new Date()); //disetara dgn actionApp
        entry.setCurrentTierId(gat.getId()); //disetara dgn actionApp (NO!, since saveApproval mungkin boleh dipolah kt tier lain selain dr currentTierId, tok mungkin bermasalah)
        entryApprovalRepository.save(gaa); // ONLY HERE

        entry.getApproval().put(gat.getId(), gaa); // ONLY HERE
        entry = entryRepository.save(entry); // ONLY HERE

        int currentTier = Math.toIntExact(gat.getSortOrder());

        List<MailerHolder> mailersToTrigger = new ArrayList<>();

        Map<String, TierAction> gatActions = gat.getActions();

        List<Tier> formTiers = entry.getForm().getTiers();

        if (gatActions != null) {
            if (gatActions.get(gaa.getStatus()) != null || gat.isAlwaysApprove()) {
                TierAction ta = gatActions.get(gaa.getStatus());

                if (ta != null) {
                    if ("nextTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < formTiers.size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = formTiers.get(currentTier);
                            if (ngat.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                ngat.getSubmitMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, ngat)));
                            }
                        }

                    } else {
                        if (gatActions.get(prevStatus) != null && "nextTier".equals(gatActions.get(prevStatus).getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder());
                        }

                        if ("prevTier".equals(ta.getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder()) - 1;

                        } else if ("goTier".equals(ta.getAction())) {
                            Entry finalEntry1 = entry;
                            Tier t1 = tierRepository.findById(ta.getNextTier()).orElseThrow(() -> {
                                TenantLogger.error(finalEntry1.getForm().getAppId(),"form", finalEntry1.getFormId(),"Tier not found for goTier action");
                                return new ResourceNotFoundException("Tier", "id", ta.getNextTier());
                            });
                            if (t1.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                t1.getSubmitMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, t1)));
                            }
                            currentTier = Math.toIntExact(t1.getSortOrder());
                        } else if ("curTier".equals(ta.getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder());
                        }
                    }

                    entry.setCurrentStatus(ta.getCode());
                    entry.setCurrentEdit(ta.isUserEdit());

                    ta.getMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, gat)));

                } else {
                    if (gat.isAlwaysApprove()) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < formTiers.size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = formTiers.get(currentTier);
                            if (ngat.getSubmitMailer() != null) {// && !ta.isUserEdit()
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                ngat.getSubmitMailer().forEach(mailerId -> mailersToTrigger.add(new MailerHolder(mailerId, ngat)));
                            }
                        }
                        entry.setCurrentStatus(Entry.STATUS_ALWAYS_APPROVE);
                        entry.setCurrentEdit(false);
                    }
                }
            }
        }

        if (currentTier == formTiers.size()) {
            // to mark this is final tier (the status could be approved, rejected, returned, resubmitted)
            entry.setFinalTierId(gat.getId());
        }

        entry.setCurrentTier(currentTier);

        entry = entryRepository.save(entry);

        updateApprover(entry, entry.getEmail());

        EntryApprovalTrail eat = new EntryApprovalTrail(gaa);
        eat.setRemark("Approval updated "); // ONLY HERE

        self.trailApproval(eat);

        if (entry.getForm().getUpdateApprovalMailer() != null) { // ONLY HERE
            mailersToTrigger.add(new MailerHolder(entry.getForm().getUpdateApprovalMailer(), gat));
        }

        for (MailerHolder m : mailersToTrigger) {
            triggerMailer(m.mailerId, entry, m.tier, email);
        }
        return entry;
    }

    @Transactional
    public void deleteEntry(Long id, String email) {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        JsonNode snap = entry.getData();
        bucketService.deleteFileByEntryId(id);
        self.deleteEntryChildren(id);

        try {
            self.trail(id, snap, EntryTrail.REMOVED, entry.getForm().getId(), email, "Entry removed by " + email, entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        } catch (Exception e) {}

        self.recordKryptaOn(entry.getForm().getX(), entry.getForm().getKrypta(), "delete", entry);

        entryRepository.deleteById(id);
    }

    @Transactional
    public void deleteEntryChildren(Long parentId) {
        // 1. Get the first level of children
        List<Long> currentLevel = entryRepository.findChildIds(List.of(parentId));
        List<Long> allChildrenIds = new ArrayList<>();

        // 2. Loop to find all descendants
        while (!currentLevel.isEmpty()) {
            allChildrenIds.addAll(currentLevel);
            currentLevel = entryRepository.findChildIds(currentLevel);
        }

        // 3. Bulk update only the children found
        if (!allChildrenIds.isEmpty()) {
            entryRepository.bulkSoftDelete(allChildrenIds);
            bucketService.deleteFileByEntryIds(allChildrenIds);
        }
    }

    @Transactional
    public void deleteEntries(List<Long> ids, String email) {
        for (Long id : ids) {
            deleteEntry(id, email);
        }
    }

    @Transactional
    public Entry reset(Long id) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow();

        entry.setCurrentTier(0);

        entry.setCurrentStatus(Entry.STATUS_SUBMITTED);

        return entryRepository.save(entry);
    }

    @Transactional
    public Entry removeApproval(Long tierId, Long entryId, String email) {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

        if (entry.getApproval() != null && entry.getApproval().get(tierId) != null) {

            if (!Optional.ofNullable(entry.getApprover().get(tierId)).orElse("")
                    .contains(email)) {
                TenantLogger.error(entry.getForm().getAppId(),"form", entry.getFormId(),"Unallowed attempt to remove approval data by user [" + email + "]");
                throw new RuntimeException("User [" + email + "] not allowed to remove approval data.");
            }

            try {
                self.trailApproval(tierId, null, null, EntryApprovalTrail.DELETE, "Approval removed by " + entry.getEmail(), getPrincipalEmail());
            } catch (Exception e) {
            }

            entry.getApproval().remove(tierId);
            entryApprovalRepository.deleteById(tierId);
        }

        return entryRepository.save(entry);
    }

//    @Transactional
    public Entry submit(Long id, String email) {
        Date dateNow = new Date();
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        Form form = entry.getForm();

        /* NEW!!! Perlu check n test bena2 sebelum deploy! Data validation on server-side */

        Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation");

        boolean serverValidation = validateOpt.map("true"::equals).orElse(false);
        boolean skipValidate = form.getX() != null
                && form.getX().at("/skipValidate").asBoolean(false);

        if (serverValidation && !skipValidate) {
            String jsonSchema = formService.getJsonSchema(form);
            Helper.ValidationResult result = Helper.validateJson(jsonSchema, entry.getData());
            if (!result.valid()) {
                TenantLogger.error(form.getAppId(),"form", form.getId(),"Invalid JSON data on entry submission: " + result.errorMessagesAsString());
                logger.error("INVALID JSON: " + result.errorMessagesAsString());
                throw new JsonSchemaValidationException(result.errors());
            }
        }

        entry.setSubmissionDate(dateNow);
        entry.setResubmissionDate(dateNow); // Why set resubmission here?

        entry.setCurrentTier(0);
        entry.setCurrentTierId(null); // need to set currentTier to null for it to be queriable as submitted

        List<Long> mailer = null;

        Tier gat = null;

        if (!form.getTiers().isEmpty()) {
            gat = form.getTiers().get(0);
            mailer = gat.getSubmitMailer();
            entry = updateApprover(entry, entry.getEmail());
        }
        entry.setCurrentStatus(Entry.STATUS_SUBMITTED);
        entry.setCurrentEdit(false);

        final Entry savedEntry = self.justSave(entry);

        self.trailApproval(id, null, null, Entry.STATUS_SUBMITTED, "SUBMITTED by User " + entry.getEmail(), getPrincipalEmail());

        self.recordKryptaOn(form.getX(), form.getKrypta(), "submit", savedEntry);

        self.justSave(savedEntry);

        if (mailer != null) {
            for (Long i : mailer) {
                triggerMailer(i, savedEntry, gat, email);
            }
        }
        return savedEntry;
    }

    @Transactional
    public Entry resubmit(Long id, String email) {
        Date now = new Date();
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        Form form = entry.getForm();

        Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation");

        boolean serverValidation = validateOpt.map("true"::equals).orElse(false);
        boolean skipValidate = form.getX() != null
                && form.getX().at("/skipValidate").asBoolean(false);

        if (serverValidation && !skipValidate) {
            String jsonSchema = formService.getJsonSchema(form);
            Helper.ValidationResult result = Helper.validateJson(jsonSchema, entry.getData());
            if (!result.valid()) {
                TenantLogger.error(form.getAppId(),"form", form.getId(),"Invalid JSON data on entry resubmission: " + result.errorMessagesAsString());
                logger.error("INVALID JSON: " + result.errorMessagesAsString());
                throw new JsonSchemaValidationException(result.errors());
            }
        }

        List<Tier> tiers = form.getTiers();

        // No tiers: handle with submit
        if (tiers.isEmpty()) {
//            throw new RuntimeException("Form ["+form.getId()+"] has no tier specified");
            return submit(id, email);
        }

        Integer currentTier = entry.getCurrentTier();

        // No current tier: treat as initial submit
        // Or currentStatus == drafted
        if (currentTier == null
                || Entry.STATUS_DRAFTED.equals(entry.getCurrentStatus())) {
            return submit(id, email);
        }

        // Resubmission logic
        Tier tier = tiers.get(Math.max(0,currentTier==null?0:currentTier));
        entry.setResubmissionDate(now);
        entry.setCurrentStatus(Entry.STATUS_RESUBMITTED);
        entry.setCurrentEdit(false);
        entry = updateApprover(entry, entry.getEmail());
        entry = entryRepository.save(entry);

        self.trailApproval(id, null, tier, Entry.STATUS_RESUBMITTED, "RESUBMITTED by User " + entry.getEmail(), getPrincipalEmail());

        self.recordKryptaOn(form.getX(), form.getKrypta(), "resubmit", entry);

        List<Long> mailerList = tier.getResubmitMailer();
        if (mailerList != null) {
            for (Long mailerId : mailerList) {
                triggerMailer(mailerId, entry, tier, email);
            }
        }
        return entry;
    }

    @Transactional
    public long countEntry(Dataset dataset, String email) {
        return countByDataset(dataset.getId(), null, email, null, null, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getStart(Long appId, String email) {
        Map<String, Long> data = new HashMap<>();

        List<NaviGroup> group = appService.findNaviByAppIdAndEmail(appId, email);
        List<Long> dsInNavi = new ArrayList<>();
        List<Long> sInNavi = new ArrayList<>();
        for (NaviGroup g : group) {
            for (NaviItem i : g.getItems()) {
                if ("dataset".equals(i.getType())) {
                    dsInNavi.add(i.getScreenId());
                }else if ("screen".equals(i.getType())) {
                    sInNavi.add(i.getScreenId());
                }
            }
        }

        List<Dataset> datasetList = datasetRepository.findByIds(dsInNavi);
        List<Screen> screenList = screenRepository.findByIds(sInNavi);


        for (Dataset ds : datasetList) {
            data.put(ds.getId() + "", countEntry(ds, email));
        }

        for (Screen s : screenList) {
            if ("list".equals(s.getType()) && s.getDataset() != null) {
                data.put("screen_" + s.getId(), countEntry(s.getDataset(), email));
            }
        }

        return data;
    }

    /**
     * Handle large dataset, without loading all data from database and hogging memory
     * memory hog, large-dataset
     *
     * @param formId
     * @return
     */
    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> updateApproverAllTier(Long formId) {
        Map<String, Object> data = new HashMap<>();

//        XPERLU, SBB KT UI UTK update Approver disabled
        Form form = formService.findFormById(formId);
//        // if form is extended form, then use original form
//        Long extendedId = form.getX().get("extended").asLong();
//        if (extendedId != null) {
//            formId = extendedId;
//            form = formService.findFormById(formId);
//        }

        try (Stream<Entry> entryStream = entryRepository.findByFormId(formId, form.isLive())) {
            entryStream.forEach(e -> {
                entryRepository.saveAndFlush(updateApprover(e, e.getEmail()));
                this.entityManager.detach(e);
            });
        }
        return CompletableFuture.completedFuture(data);
    }


    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> updateApproverBulk(Long formId, Long tierId, boolean updateApproved) {

        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        // if form is extended form, then use original form
        // xperlu sbb update approver disabled dalam UI
//        Long extendedId = form.getX().get("extended").asLong();
//        if (extendedId != null) {
//            formId = extendedId;
//            form = formService.findFormById(formId);
//        }


        Map<String, Object> data = new HashMap<>();

        final List<String> errors = new ArrayList<>();

        final AtomicInteger entryTotal = new AtomicInteger();
        final AtomicInteger successTotal = new AtomicInteger();
        final AtomicInteger errorsTotal = new AtomicInteger();
        final AtomicInteger notEmptyTotal = new AtomicInteger();

        try (Stream<Entry> entryStream = entryRepository.findByFormId(formId, form.isLive())) {
            entryStream.forEach(e -> {

                entryTotal.getAndIncrement();

                try {
                    if (e.getApproval().get(tierId) == null || updateApproved) {

                        entryRepository.saveAndFlush(updateApprover(e, e.getEmail()));
                        successTotal.getAndIncrement();

                        if (e.getApproval().get(tierId) != null) {
                            notEmptyTotal.getAndIncrement();
                        }
                    } else {
                        notEmptyTotal.getAndIncrement();
                    }
                } catch (Exception ex) {

                    logger.error("BULK UPDATE APPROVER:" + ex.getMessage() + ":" + e.getId());
                    errors.add(e.getId() + ": " + ex.getMessage());
                    errorsTotal.getAndIncrement();
                }

                this.entityManager.detach(e);
            });
        }

        data.put("total", entryTotal.get());
        data.put("successCount", successTotal.get());
        data.put("errorCount", errorsTotal.get());
        data.put("errorLog", errors);
        data.put("notEmptyCount", notEmptyTotal.get());
        data.put("success", true);

        if (errorsTotal.get() > 0) {
            TenantLogger.error(form.getAppId(),"form", form.getId(),"Bulk update approver completed with errors: " + errorsTotal.get() + " out of " + entryTotal.get() + " entries. Error details: " + errors);
        }

        return CompletableFuture.completedFuture(data);
    }

    private Engine sharedGraalEngine;

    @PostConstruct
    public void initializeEngines() {
        // Create shared GraalVM engine once
        sharedGraalEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
    }


    // reuse existing sharedGraalEngine initialized at startup
//    private final Map<String, Source> compiledScriptCache = new ConcurrentHashMap<>();
    private final Source dayjsSource;
    private static HostAccess access = HostAccess.newBuilder(HostAccess.ALL)
            .targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class))).build();

    @Async("asyncExec")
    @Transactional(readOnly = true)
    public CompletableFuture<Map<String, Object>> execValOld(Long formId, String field, String section, boolean force) {
        Map<String, Object> data = new HashMap<>();

        Form loadform = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        // get the script (may be from extended form)
        String script = loadform.getItems().get(field).getF();

        if (loadform.getX().get("extended") != null) {
            Long extendedId = loadform.getX().get("extended").asLong();
            loadform = formRepository.findById(extendedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        final Form form = loadform;
        final App app = form.getApp();

        final List<String> errors = new ArrayList<>();
        final List<String> success = new ArrayList<>();
        final List<String> notEmpty = new ArrayList<>();
        final AtomicInteger total = new AtomicInteger();

        try {
            // 1) Build (and cache) Source for the function wrapper
            // key by formId + field so different fields or forms produce different compiled sources
            String cacheKey = form.getId() + "#" + field;
            String fn =
                    "function fef(userJson) { " +
                            "  var $ = JSON.parse(dataModel); " +
                            "  var $prev$ = JSON.parse(prevModel); " +
                            "  var $_ = JSON.parse(entryModel); " +
                            "  var $user$ = userJson ? JSON.parse(userJson) : null; " +
                            "  return (" + script + "); " +
                            "}";
            Source fnSource = Source.newBuilder("js", fn, "fef-" + cacheKey + ".js").build();

            long start = System.currentTimeMillis();
            Map<String, Map> userMap = new HashMap<>();

            // create single context for this execVal call (isolated)
            try (Context ctx = Context.newBuilder("js")
                    .engine(sharedGraalEngine)
                    // strong sandboxing - no host access / no Java interop
                    .allowHostAccess(access)
                    .build()) {

                // 2) evaluate dayjs into this context (if you have dayjsSource loaded earlier)
                if (dayjsSource != null) {
                    ctx.eval(dayjsSource);
                }

                // 3) evaluate the compiled function Source (defines `fef` in the global bindings)
                ctx.eval(fnSource);

                // helper objects
                Value bindings = ctx.getBindings("js");
                Value jsonObj = bindings.getMember("JSON"); // JSON.stringify / parse

                // stream entries and evaluate per-entry
                try (Stream<Entry> entryStream = entryRepository.findByFormId(form.getId(), form.isLive())) {
                    entryStream.forEach(entry -> {

                        logger.info("Processing entry " + entry.getId());

                        JsonNode entryData = entry.getData();
                        JsonNode entryPrev = entry.getPrev();

                        this.entityManager.detach(entry);

                        total.incrementAndGet();

                        if (!(force || entryData.get(field) == null || entryData.get(field).isNull())) {
                            notEmpty.add(entry.getId() + ": Field not empty");
                            return;
                        }

                        // user handling
                        Map user = null;
                        boolean userOk = true;
                        String userJson = null;
                        if (script.contains("$user$")) {
                            if (entry.getEmail() != null) {
                                if (userMap.containsKey(entry.getEmail())) {
                                    user = userMap.get(entry.getEmail());
                                    userOk = true;
                                } else {
                                    Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), app.getId());
                                    if (u.isPresent()) {
                                        user = MAPPER.convertValue(u.get(), Map.class);
                                        userMap.put(entry.getEmail(), user);
                                        userOk = true;
                                    } else {
                                        userOk = false;
                                        errors.add("Entry " + entry.getId() + ": Contain $user$ but user not exist");
                                    }
                                }
                            } else {
                                userOk = false;
                                errors.add("Entry " + entry.getId() + ": Contain $user$ but entry has no email");
                            }
                            if (userOk && user != null) {
                                try {
                                    userJson = MAPPER.writeValueAsString(user);
                                } catch (JsonProcessingException e) {
                                    userJson = null;
                                }
                            }
                        }

                        if (!userOk) {
                            // skip if user required but not found
                            return;
                        }

                        try {
                            if (section != null && !section.isBlank()) {
                                // child section - iterate elements
                                ObjectNode o = (ObjectNode) entryData;
                                Iterator<JsonNode> elements = o.get(section).elements();
                                while (elements.hasNext()) {
                                    ObjectNode child = (ObjectNode) elements.next();

                                    // set bindings as JSON strings (strings only -> no host object crossing)
                                    try {
                                        bindings.putMember("dataModel", MAPPER.writeValueAsString(child));
                                        bindings.putMember("prevModel", MAPPER.writeValueAsString(entryPrev));
                                        bindings.putMember("entryModel", MAPPER.writeValueAsString(entry));
                                    } catch (JsonProcessingException ex) {
                                        errors.add(entry.getId() + ": JSON error - " + ex.getMessage());
                                        continue;
                                    }

                                    // execute fef(userJson)
                                    Value fef = bindings.getMember("fef");
                                    Value resultVal = fef.execute(userJson); // userJson may be null

                                    // stringify the result in JS and parse back in Java
                                    Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);
                                    String jsonStr = jsonStrVal.isNull() ? null : jsonStrVal.asString();

                                    if (jsonStr == null || "null".equals(jsonStr)) {
                                        child.set(field, NullNode.getInstance());
                                    } else {
                                        JsonNode rnode = MAPPER.readTree(jsonStr);
                                        child.set(field, rnode);
                                    }
                                }
                            } else {
                                // top-level field
                                try {
                                    bindings.putMember("dataModel", MAPPER.writeValueAsString(entryData));
                                    bindings.putMember("prevModel", MAPPER.writeValueAsString(entryPrev));
                                    bindings.putMember("entryModel", MAPPER.writeValueAsString(entry));
                                    String dev = app.isLive() ? "" : "--dev";
                                    bindings.putMember("$baseUrl$", "https://" + app.getAppPath() + dev +  "." + Constant.UI_BASE_DOMAIN + "/#" );
                                } catch (JsonProcessingException ex) {
                                    errors.add(entry.getId() + ": JSON error - " + ex.getMessage());
                                    return;
                                }

                                Value fef = bindings.getMember("fef");
                                Value resultVal = fef.execute(userJson);

                                Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);
                                String jsonStr = jsonStrVal.isNull() ? null : jsonStrVal.asString();

                                ObjectNode o = (ObjectNode) entryData;
                                if (jsonStr == null || "null".equals(jsonStr)) {
                                    o.set(field, NullNode.getInstance());
                                } else {
                                    JsonNode rnode = MAPPER.readTree(jsonStr);
                                    o.set(field, rnode);
                                }
                            }

                            // update DB via your repository method (same as before)
                            entryRepository.updateDataField(entry.getId(), entryData.toString());
                            success.add(entry.getId() + ": Success");

                            logger.info("Processed entry " + entry.getId() + " successfully.");

                        } catch (Exception ex) {
                            errors.add(entry.getId() + ": " + ex.getMessage());
                        }
                    });
                }
                long finish = System.currentTimeMillis();
                logger.info("completed in (stream + update):" + (finish - start));
            }
        } catch (Exception e) {
            TenantLogger.error(app.getId(),"form", form.getId(),"Error in execVal: " + e.getMessage());
            e.printStackTrace();
        }

        if (errors.size() > 0) {
            TenantLogger.error(app.getId(),"form", form.getId(),"Errors in execVal: " + String.join(", ", errors));
            logger.error("Errors in execVal: " + String.join(", ", errors));
        }

        data.put("total", total.get());
        data.put("successCount", success.size());
        data.put("errorCount", errors.size());
        data.put("errorLog", errors);
        data.put("notEmptyCount", notEmpty.size());
        data.put("notEmptyLog", notEmpty);
        data.put("success", true);

        return CompletableFuture.completedFuture(data);
    }


    @Async("asyncExec")
    @Transactional(readOnly = true)
    public CompletableFuture<Map<String, Object>> execValNormal(Long formId, String field, String section, boolean force) {
        Map<String, Object> data = new HashMap<>();

        Form loadform = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        // get the script (may be from extended form)
        String script = loadform.getItems().get(field).getF();

        // Safely check for extended metadata to prevent NPE
        if (loadform.getX() != null && loadform.getX().hasNonNull("extended")) {
            Long extendedId = loadform.getX().get("extended").asLong();
            loadform = formRepository.findById(extendedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        final Form form = loadform;
        final App app = form.getApp();

        // Use thread-safe collections just in case the stream is parallelized in the future
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());
        final List<String> success = Collections.synchronizedList(new ArrayList<>());
        final List<String> notEmpty = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger total = new AtomicInteger();

        try {
            // 1) Build (and cache) Source for the function wrapper
            String cacheKey = form.getId() + "#" + field;
            String fn =
                    "function fef(userJson) { " +
                            "  var $ = JSON.parse(dataModel); " +
                            "  var $prev$ = JSON.parse(prevModel); " +
                            "  var $_ = JSON.parse(entryModel); " +
                            "  var $user$ = userJson ? JSON.parse(userJson) : null; " +
                            "  return (" + script + "); " +
                            "}";
            Source fnSource = Source.newBuilder("js", fn, "fef-" + cacheKey + ".js").build();

            long start = System.currentTimeMillis();
            Map<String, Map> userMap = new ConcurrentHashMap<>();

            // create single context for this execVal call (isolated)
            try (Context ctx = Context.newBuilder("js")
                    .engine(sharedGraalEngine)
                    // strong sandboxing - no host access / no Java interop
                    .allowHostAccess(access)
                    .build()) {

                // 2) evaluate dayjs into this context
                if (dayjsSource != null) {
                    ctx.eval(dayjsSource);
                }

                // 3) evaluate the compiled function Source
                ctx.eval(fnSource);

                // helper objects
                Value bindings = ctx.getBindings("js");
                Value jsonObj = bindings.getMember("JSON");

                // HOISTED: Bind static App variables ONCE before the loop
                String dev = app.isLive() ? "" : "--dev";
                bindings.putMember("$baseUrl$", "https://" + app.getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN + "/#");

                List<EntryBatchRepository.EntryUpdateDto> pendingUpdates = new ArrayList<>();

                // stream entries and evaluate per-entry
                try (Stream<Entry> entryStream = entryRepository.findByFormId(form.getId(), form.isLive())) {
                    entryStream.forEach(entry -> {

                        logger.info("Processing entry " + entry.getId());

                        JsonNode entryData = entry.getData();
                        JsonNode entryPrev = entry.getPrev();

                        this.entityManager.detach(entry);
                        total.incrementAndGet();

                        // Safely handle missing entryData
                        if (entryData == null) {
                            errors.add(entry.getId() + ": entryData is null");
                            return;
                        }

                        if (!(force || entryData.get(field) == null || entryData.get(field).isNull())) {
                            notEmpty.add(entry.getId() + ": Field not empty");
                            return;
                        }

                        // Efficiently handle user fetching
                        Map user = null;
                        boolean userOk = true;
                        String userJson = null;

                        if (script.contains("$user$")) {
                            if (entry.getEmail() != null) {
                                user = userMap.computeIfAbsent(entry.getEmail(), email ->
                                        userRepository.findFirstByEmailAndAppId(email, app.getId())
                                                .map(u -> MAPPER.convertValue(u, Map.class))
                                                .orElse(null)
                                );

                                userOk = (user != null);
                                if (!userOk) {
                                    errors.add("Entry " + entry.getId() + ": Contain $user$ but user not exist");
                                }
                            } else {
                                userOk = false;
                                errors.add("Entry " + entry.getId() + ": Contain $user$ but entry has no email");
                            }

                            if (userOk) {
                                try {
                                    userJson = MAPPER.writeValueAsString(user);
                                } catch (JsonProcessingException e) {
                                    userJson = null;
                                }
                            }
                        }

                        if (!userOk) {
                            return; // skip if user required but not found
                        }

                        try {
                            // OPTIMIZATION: Serialize entry & prev models ONCE per entry, not per section child
                            String entryPrevStr = MAPPER.writeValueAsString(entryPrev);
                            String entryStr = MAPPER.writeValueAsString(entry);

                            bindings.putMember("prevModel", entryPrevStr);
                            bindings.putMember("entryModel", entryStr);

                            Value fef = bindings.getMember("fef");

                            if (section != null && !section.isBlank()) {
                                // child section - iterate elements
                                ObjectNode o = (ObjectNode) entryData;
                                if (o.hasNonNull(section) && o.get(section).isArray()) {
                                    Iterator<JsonNode> elements = o.get(section).elements();

                                    while (elements.hasNext()) {
                                        ObjectNode child = (ObjectNode) elements.next();

                                        try {
                                            bindings.putMember("dataModel", MAPPER.writeValueAsString(child));
                                        } catch (JsonProcessingException ex) {
                                            errors.add(entry.getId() + ": JSON error - " + ex.getMessage());
                                            continue;
                                        }

                                        Value resultVal = fef.execute(userJson);
                                        Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);
                                        String jsonStr = jsonStrVal.isNull() ? null : jsonStrVal.asString();


                                        if (jsonStr == null || "null".equals(jsonStr)) {
                                            child.set(field, NullNode.getInstance());
                                        } else {
                                            child.set(field, MAPPER.readTree(jsonStr));
                                        }
                                    }
                                }
                            } else {
                                // top-level field
                                try {
                                    bindings.putMember("dataModel", MAPPER.writeValueAsString(entryData));
                                } catch (JsonProcessingException ex) {
                                    errors.add(entry.getId() + ": JSON error - " + ex.getMessage());
                                    return;
                                }

                                Value resultVal = fef.execute(userJson);
                                Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);
                                String jsonStr = jsonStrVal.isNull() ? null : jsonStrVal.asString();

                                ObjectNode o = (ObjectNode) entryData;
                                if (jsonStr == null || "null".equals(jsonStr)) {
                                    o.set(field, NullNode.getInstance());
                                } else {
                                    o.set(field, MAPPER.readTree(jsonStr));
                                }
                            }

                            logger.info("Processed entry " + entry.getId() + " successfully, updating database.");

                            // update DB via your repository method
//                            entryRepository.updateDataField(entry.getId(), entryData.toString());
                            pendingUpdates.add(new EntryBatchRepository.EntryUpdateDto(entry.getId(), "", entryData.toString()));
                            success.add(entry.getId() + ": Success");

                        } catch (Exception ex) {
                            errors.add(entry.getId() + ": " + ex.getMessage());
                        }
                    });
                }

                entryBatchRepository.batchUpdateDataFields(pendingUpdates);

                long finish = System.currentTimeMillis();
                logger.info("completed in (stream + update):" + (finish - start));
            }
        } catch (Exception e) {
            TenantLogger.error(app.getId(), "form", form.getId(), "Error in execVal: " + e.getMessage());
            logger.error("Error in execVal for form {}: {}", form.getId(), e.getMessage(), e);
        }

        if (!errors.isEmpty()) {
            TenantLogger.error(app.getId(), "form", form.getId(), "Errors in execVal: " + String.join(", ", errors));
            logger.error("Errors in execVal: " + String.join(", ", errors));
        }

        data.put("total", total.get());
        data.put("successCount", success.size());
        data.put("errorCount", errors.size());
        data.put("errorLog", errors);
        data.put("notEmptyCount", notEmpty.size());
        data.put("notEmptyLog", notEmpty);
        data.put("success", true);

        return CompletableFuture.completedFuture(data);
    }

    @Async("asyncExec")
    @Transactional(readOnly = true)
    public CompletableFuture<Map<String, Object>> execVal(Long formId, String field, String section, boolean force) {
        Map<String, Object> data = new HashMap<>();

        Form loadform = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        // get the script (may be from extended form)
        String script = loadform.getItems().get(field).getF();

        // Safely check for extended metadata to prevent NPE
        if (loadform.getX() != null && loadform.getX().hasNonNull("extended")) {
            Long extendedId = loadform.getX().get("extended").asLong();
            loadform = formRepository.findById(extendedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        final Form form = loadform;
        final App app = form.getApp();

        // Use thread-safe collections just in case the stream is parallelized in the future
        final List<String> errors = Collections.synchronizedList(new ArrayList<>());
        final List<String> success = Collections.synchronizedList(new ArrayList<>());
        final List<String> notEmpty = Collections.synchronizedList(new ArrayList<>());
        final AtomicInteger total = new AtomicInteger();

        try {
            // 1) Build (and cache) Source for the function wrapper
            String cacheKey = form.getId() + "#" + field;
            String fn =
                    "function fef(userJson) { " +
                    "  var $ = JSON.parse(dataModel); " +
                    "  var $prev$ = JSON.parse(prevModel); " +
                    "  var $_ = JSON.parse(entryModel); " +
                    "  var $user$ = userJson ? JSON.parse(userJson) : null; " +
                    "  return (" + script + "); " +
                    "}";
            Source fnSource = Source.newBuilder("js", fn, "fef-" + cacheKey + ".js").build();

            long start = System.currentTimeMillis();
            Map<String, Map> userMap = new ConcurrentHashMap<>();

            // create single context for this execVal call (isolated)
            try (Context ctx = Context.newBuilder("js")
                    .engine(sharedGraalEngine)
                    // strong sandboxing - no host access / no Java interop
                    .allowHostAccess(access)
                    .build()) {

                // 2) evaluate dayjs into this context
                if (dayjsSource != null) {
                    ctx.eval(dayjsSource);
                }

                // 3) evaluate the compiled function Source
                ctx.eval(fnSource);

                // helper objects
                Value bindings = ctx.getBindings("js");
                Value jsonObj = bindings.getMember("JSON");

                // HOISTED: Bind static App variables ONCE before the loop
                String dev = app.isLive() ? "" : "--dev";
                bindings.putMember("$baseUrl$", "https://" + app.getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN + "/#");

                List<EntryBatchRepository.EntryUpdateDto> pendingUpdates = new ArrayList<>();
                // 1. Define a safe chunk size to prevent OOM and DB Packet drops
                final int BATCH_SIZE = 4000;

                // stream entries and evaluate per-entry
                try (Stream<Entry> entryStream = entryRepository.findByFormId(form.getId(), form.isLive())) {
                    entryStream.forEach(entry -> {

                        JsonNode entryData = entry.getData();
                        JsonNode entryPrev = entry.getPrev();

                        this.entityManager.detach(entry);
                        total.incrementAndGet();

                        // Safely handle missing entryData
                        if (entryData == null) {
                            errors.add(entry.getId() + ": entryData is null");
                            return;
                        }

                        if (!(force || entryData.get(field) == null || entryData.get(field).isNull())) {
                            notEmpty.add(entry.getId() + ": Field not empty");
                            return;
                        }

                        // Efficiently handle user fetching
                        Map user = null;
                        boolean userOk = true;
                        String userJson = null;

                        if (script.contains("$user$")) {
                            if (entry.getEmail() != null) {
                                user = userMap.computeIfAbsent(entry.getEmail(), email ->
                                        userRepository.findFirstByEmailAndAppId(email, app.getId())
                                                .map(u -> MAPPER.convertValue(u, Map.class))
                                                .orElse(null)
                                );

                                userOk = (user != null);
                                if (!userOk) {
                                    errors.add("Entry " + entry.getId() + ": Contain $user$ but user not exist");
                                }
                            } else {
                                userOk = false;
                                errors.add("Entry " + entry.getId() + ": Contain $user$ but entry has no email");
                            }

                            if (userOk) {
                                try {
                                    userJson = MAPPER.writeValueAsString(user);
                                } catch (JsonProcessingException e) {
                                    userJson = null;
                                }
                            }
                        }

                        if (!userOk) {
                            return; // skip if user required but not found
                        }

                        try {
                            // OPTIMIZATION: Serialize entry & prev models ONCE per entry
                            String entryPrevStr = MAPPER.writeValueAsString(entryPrev);
                            String entryStr = MAPPER.writeValueAsString(entry);

                            bindings.putMember("prevModel", entryPrevStr);
                            bindings.putMember("entryModel", entryStr);

                            Value fef = bindings.getMember("fef");

                            if (section != null && !section.isBlank()) {
                                // child section - iterate elements
                                ObjectNode o = (ObjectNode) entryData;
                                if (o.hasNonNull(section)) {
                                    JsonNode sectionNode = o.path(section);

                                    if (sectionNode != null && !sectionNode.isNull() && !sectionNode.isMissingNode() && sectionNode.isArray()) {

                                        for (int i = 0; i < sectionNode.size(); i++) {
                                            JsonNode child = sectionNode.get(i);
                                            String updatePath = "$." + section + "[" + i + "]." + field;

                                            try {
                                                bindings.putMember("dataModel", MAPPER.writeValueAsString(child));
                                            } catch (JsonProcessingException ex) {
                                                errors.add(entry.getId() + ": JSON error - " + ex.getMessage());
                                                continue;
                                            }

                                            Value resultVal = fef.execute(userJson);
                                            Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);
                                            String jsonStr = jsonStrVal.isNull() ? null : jsonStrVal.asString();

                                            if (jsonStr == null || "null".equals(jsonStr)) {
                                                pendingUpdates.add(new EntryBatchRepository.EntryUpdateDto(entry.getId(), updatePath, null));
                                            } else {
                                                pendingUpdates.add(new EntryBatchRepository.EntryUpdateDto(entry.getId(), updatePath, jsonStr));
                                            }
                                        }
                                    }
                                }
                            } else {
                                final String updatePath = "$." + field;

                                // top-level field
                                try {
                                    bindings.putMember("dataModel", MAPPER.writeValueAsString(entryData));
                                } catch (JsonProcessingException ex) {
                                    errors.add(entry.getId() + ": JSON error - " + ex.getMessage());
                                    return; // early return
                                }

                                Value resultVal = fef.execute(userJson);
                                Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);
                                String jsonStr = jsonStrVal.isNull() ? null : jsonStrVal.asString();

                                if (jsonStr == null || "null".equals(jsonStr)) {
                                    pendingUpdates.add(new EntryBatchRepository.EntryUpdateDto(entry.getId(), updatePath, null));
                                } else {
                                    pendingUpdates.add(new EntryBatchRepository.EntryUpdateDto(entry.getId(), updatePath, jsonStr));
                                }
                            }

                            success.add(entry.getId() + ": Success");

                            // 2. CHUNK EXECUTION: Fire batch to DB every 500 updates to keep memory usage low and network stable
                            if (pendingUpdates.size() >= BATCH_SIZE) {
                                try {
                                    entryBatchRepository.batchUpdateDataFields(pendingUpdates);
                                    logger.info("Successfully flushed batch of {} updates to DB", pendingUpdates.size());
                                    pendingUpdates.clear();
                                } catch (Exception e) {
                                    throw new RuntimeException("Batch chunk update failed: " + e.getMessage(), e);
                                }
                            }

                        } catch (Exception ex) {
                            errors.add(entry.getId() + ": " + ex.getMessage());
                        }
                    });
                }

                // 3. REMAINDER EXECUTION: Catch any remaining updates that didn't hit the BATCH_SIZE threshold
                if (!pendingUpdates.isEmpty()) {
                    try {
                        entryBatchRepository.batchUpdateDataFields(pendingUpdates);
                        logger.info("Successfully flushed final batch of {} updates to DB", pendingUpdates.size());
                        pendingUpdates.clear();
                    } catch (Exception e) {
                        TenantLogger.error(app.getId(), "form", form.getId(), "Final batch database update failed: " + e.getMessage());
                        logger.error("Failed to execute final batch update for form: {}", form.getId(), e);
                        errors.add("Final batch database update failed: " + e.getMessage());
                    }
                }

                long finish = System.currentTimeMillis();
                logger.info("completed in (stream + update):" + (finish - start));
            }
        } catch (Exception e) {
            TenantLogger.error(app.getId(), "form", form.getId(), "Error in execVal: " + e.getMessage());
            logger.error("Error in execVal for form {}: {}", form.getId(), e.getMessage(), e);
        }

        if (!errors.isEmpty()) {
            TenantLogger.error(app.getId(), "form", form.getId(), "Errors in execVal: " + String.join(", ", errors));
            logger.error("Errors in execVal: " + String.join(", ", errors));
        }

        data.put("total", total.get());
        data.put("successCount", success.size());
        data.put("errorCount", errors.size());
        data.put("errorLog", errors);
        data.put("notEmptyCount", notEmpty.size());
        data.put("notEmptyLog", notEmpty);
        data.put("success", true);

        return CompletableFuture.completedFuture(data);
    }


    @Transactional(readOnly = true)
    public Page<EntryDto> findListByDatasetCheck(Long datasetId, String searchText, String email, Map<String,
            Object> filters, String cond, List<String> sorts, List<Long> ids, boolean anonymous,
                                              Pageable pageable, HttpServletRequest req) {
        Dataset d = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new RuntimeException("Dataset does not exist, ID=" + datasetId));


        boolean isPublic = d.isPublicEp();

        if (anonymous && !isPublic) {
            TenantLogger.error(d.getAppId(),"dataset", datasetId,"Unallowed attempt to access private dataset from public endpoint");
            throw new OAuth2AuthenticationProcessingException("Private Dataset: Access to private dataset from public endpoint is not allowed");
        }

        String apiKeyStr = Helper.getApiKey(req);
        if (apiKeyStr != null) {
            ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
            if (apiKey != null && apiKey.getAppId() != null && !d.getApp().getId().equals(apiKey.getAppId())) {
                TenantLogger.error(d.getAppId(),"dataset", datasetId,"Unallowed attempt to access dataset with API Key not designated for the app of the dataset");
                throw new OAuth2AuthenticationProcessingException("Invalid API Key: API Key used is not designated for the app of the dataset");
            }
        }

        return findListByDataset(datasetId, searchText, email, filters, cond, sorts, ids, pageable, req);
    }

    public Stream<Entry> streamListByDatasetCheck(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, boolean anonymous, Pageable pageable, HttpServletRequest req) {
        Dataset d = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new RuntimeException("Dataset does not exist, ID=" + datasetId));

        boolean isPublic = d.isPublicEp();

        if (anonymous && !isPublic) {
            TenantLogger.error(d.getAppId(),"dataset", datasetId,"Unallowed attempt to access private dataset from public endpoint");
            throw new OAuth2AuthenticationProcessingException("Private Dataset: Access to private dataset from public endpoint is not allowed");
        } else {

            String apiKeyStr = Helper.getApiKey(req);
            if (apiKeyStr != null) {
                ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
                if (apiKey != null && apiKey.getAppId() != null && !d.getApp().getId().equals(apiKey.getAppId())) {
                    TenantLogger.error(d.getAppId(),"dataset", datasetId,"Unallowed attempt to access dataset with API Key not designated for the app of the dataset");
                    throw new OAuth2AuthenticationProcessingException("Invalid API Key: API Key used is not designated for the app of the dataset");
                }
            }

            return findListByDatasetStream(datasetId, searchText, email, filters, cond, sorts, ids, req);
        }
    }

    public Specification<Entry> buildSpecification(Long datasetId, String searchText, String email, Map<String, Object> filters, String cond, List<String> sorts, List<Long> ids, HttpServletRequest req) {

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        // Parse qFilter specific to Dataset
        JsonNode qFilter = null;
        if (dataset.getX() != null) {
            try {
                String qFilterText = dataset.getX().at("/qFilter").asText();
                if (qFilterText != null && !qFilterText.isEmpty()) {
                    qFilter = MAPPER.readTree(qFilterText);
                }
            } catch (JsonProcessingException e) {
                TenantLogger.error(dataset.getAppId(), "dataset", datasetId, "Failed to parse qFilter for dataset: " + e.getMessage());
                logger.warn("Failed to parse qFilter for dataset {}: {}", datasetId, e.getMessage());
            } catch (Exception e) {
                TenantLogger.error(dataset.getAppId(), "dataset", datasetId, "Unexpected error parsing qFilter for dataset:" + e.getMessage());
                logger.error("Unexpected error parsing qFilter for dataset {}: {}", datasetId, e.getMessage());
            }
        }

        return buildSharedSpecification(
                searchText, email, filters, cond, sorts, ids, req,
                dataset.getApp().getId(),
                dataset.getPresetFilters(),
                dataset.getStatusFilter(),
                dataset.getPrevStatusFilter(),
                dataset.getForm(),
                dataset.getDefSortField(),
                dataset.getDefSortDir(),
                qFilter,
                dataset.getType()
        );
    }

    public Specification<Entry> buildSpecificationFromChart(Long chartId, String searchText, String email, Map<String, Object> filters, String cond, List<String> sorts, List<Long> ids, HttpServletRequest req) {

        Chart chart = chartRepository.findById(chartId)
                .orElseThrow(() -> new ResourceNotFoundException("Chart", "id", chartId));

        return buildSharedSpecification(
                searchText, email, filters, cond, sorts, ids, req,
                chart.getDashboard().getApp().getId(),
                chart.getPresetFilters(),
                chart.getStatusFilter(),
                chart.getPrevStatusFilter(),
                chart.getForm(),
                null, // Chart lacks defSortField
                null, // Chart lacks defSortDir
                null, // Chart uses null qBuilder
                "all" // Forces builder.action(false).build().filter() like the original chart code
        );
    }

    /**
     * Shared logic for building specifications from Datasets and Charts
     */
    private Specification<Entry> buildSharedSpecification(
            String searchText, String email, Map<String, Object> filters, String cond,
            List<String> sorts, List<Long> ids, HttpServletRequest req,
            Long appId, Object rawPresetFilters, Object rawStatusFilters, Object rawPrevStatusFilters, Form baseForm,
            String defSortField, String defSortDir, JsonNode qFilter, String accessType) {

        if (searchText != null && searchText.isEmpty()) {
            searchText = null;
        }

        if (email != null) {
            email = email.trim();
        }

        Map<String, Object> dataMap = new HashMap<>();

        if (email != null) {
            String finalEmail = email;
            userRepository.findFirstByEmailAndAppId(email, appId).ifPresentOrElse(
                    user -> dataMap.put("user", MAPPER.convertValue(user, Map.class)),
                    () -> dataMap.put("user", Map.of("email", finalEmail, "name", finalEmail))
            );
        } else {
            dataMap.put("user", Map.of("email", "anonymous", "name", "anonymous"));
        }

        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());
        dataMap.put("conf", Map.of());

        Map<String, Object> filtersReq = new HashMap<>();
        if (req != null) {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getKey().contains("$") && !isNullOrEmpty(req.getParameter(entry.getKey()))) {
                    filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }
        }

        Map<String, Object> pF = rawPresetFilters != null ? MAPPER.convertValue(rawPresetFilters, Map.class) : new HashMap<>();
        if (pF.containsKey("@cond")) {
            cond = pF.get("@cond") + "";
            pF.remove("@cond");
        }

        Map<String, String> presetFilters = pF.entrySet().stream()
                .filter(x -> x.getKey().startsWith("$"))
                .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

        presetFilters.replaceAll((k, v) -> Helper.compileTpl(v, dataMap));

        final Map<String, Object> newFilter = new HashMap<>();
        if (filters != null) newFilter.putAll(filters);
        if (!presetFilters.isEmpty()) newFilter.putAll(presetFilters);
        if (!filtersReq.isEmpty()) newFilter.putAll(filtersReq);

        Map<String, String> statusFilter = rawStatusFilters != null ? MAPPER.convertValue(rawStatusFilters, Map.class) : new HashMap<>();
        Map<String, String> prevStatusFilter = rawPrevStatusFilters != null ? MAPPER.convertValue(rawPrevStatusFilters, Map.class) : new HashMap<>();

        List<String> sortFin = new ArrayList<>();
        Optional.ofNullable(sorts).ifPresent(sortFin::addAll);

        if (defSortField != null) {
            sortFin.add(defSortField + "~" + (defSortDir != null ? defSortDir : "asc"));
        }

        // Resolve form (handle extended forms)
        Form form = baseForm;
        if (form.getX() != null && form.getX().get("extended") != null) {
            Long extendedId = form.getX().get("extended").asLong();
            form = formRepository.findById(extendedId)
                    .orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", extendedId));
        }

        EntryFilter.EntryFilterBuilder builder = EntryFilter.builder()
                .formId(form.getId())
                .form(form)
                .searchText(searchText)
                .status(statusFilter)
                .prevStatus(prevStatusFilter)
                .sort(sortFin)
                .ids(ids)
                .qBuilder(qFilter)
                .dataMap(dataMap)
                .filters(newFilter)
                .cond(cond);

        // Default to "all" if accessType is null
        return switch (accessType == null ? "all" : accessType) {
            case "all" -> builder.action(false).build().filter();
            case "admin" -> builder.admin(email).action(false).build().filter();
            case "user" -> builder.email(email).action(false).build().filter();
            case "action" -> builder.approver(email).action(true).build().filter();
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public Page<EntryDto> findListByDataset(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, Pageable pageable, HttpServletRequest req) {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "Id", datasetId));

        boolean hasItems = dataset.getItems() != null && !dataset.getItems().isEmpty();

        boolean skipMask = dataset.getX() != null
                && dataset.getX().at("/skipMask").asBoolean(false);

        boolean fieldMaskEnabled = keyValueRepository.getValue("platform", "dataset-field-mask")
                .map("true"::equals)
                .orElse(false);

        boolean itemIncludeApproval = dataset.getItems().stream()
                .map(DatasetItem::getPrefix)
                .filter(Objects::nonNull)
                .anyMatch(p -> p.startsWith("$$"));

        boolean includeApproval = dataset.isShowStatus() || itemIncludeApproval;

        if (!hasItems || !fieldMaskEnabled || skipMask) {
            return customEntryRepository.findPaged(dataset.getForm(), buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req), null, includeApproval, pageable);
        }

        Map<String, Set<String>> fieldsMap = getFieldsMap(dataset);

        return customEntryRepository.findPaged(dataset.getForm(), buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req), fieldsMap, includeApproval, pageable);

    }

    @Transactional(readOnly = true)
    public Page<EntryDto> findListByChart(Long chartId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, Pageable pageable, HttpServletRequest req) {

        Chart chart = chartRepository.findById(chartId)
                .orElseThrow(() -> new ResourceNotFoundException("Chart", "Id", chartId));

        Map<String, Set<String>> fieldsMapFromChart = new HashMap<>();

        // Safely extract drillFields objects
        if (chart.getX() != null) {
            JsonNode drillFields = chart.getX().path("drillFields");

            if (drillFields.isArray()) {
                for (JsonNode node : drillFields) {
                    String root = node.path("root").asText("");
                    String code = node.path("code").asText("");

                    // Only process if code exists
                    if (!code.isEmpty()) {
                        // Map "prev" to "$prev$", and default "data" (or missing root) to "$"
                        String prefix = "prev".equals(root) ? "$prev$" : "$";

                        fieldsMapFromChart.computeIfAbsent(prefix, k -> new HashSet<>()).add(code);
                    }
                }
            }
        }

        return customEntryRepository.findPaged(
                chart.getForm(),
                buildSpecificationFromChart(chartId, searchText, email, filters, cond, sorts, ids, req),
                fieldsMapFromChart, // Passes the extracted mask
                false,              // includeApproval is false
                pageable
        );
    }


    private Map<String, Set<String>> getFieldsMap(Dataset dataset) {

        Form form = dataset.getForm();
        Form prevForm = form.getPrev();

        Set<String> textToExtract = new HashSet<>(Set.of(
                "$.$id",
                "$.$code",
                "$.$counter",
                "$prev$.$id",
                "$prev$.$code",
                "$prev$.$counter"
        ));

        // === Extract dataset items ===
        for (DatasetItem di : dataset.getItems()) {

            // Always include prefix.code
            textToExtract.add(di.getPrefix() + "." + di.getCode());

            // Include pre-computed expression
            Helper.addIfNonNull(textToExtract, di.getPre());

            // Resolve placeholders from Form Items (dependencies)
            if ("$".equals(di.getPrefix())) {

                Item item = form.getItems().get(di.getCode());
                if (item != null) {
                    Helper.addIfNonNull(textToExtract, item.getPlaceholder(), item.getF());
                }

            } else if ("$prev$".equals(di.getPrefix()) && prevForm != null) {

                Item item = prevForm.getItems().get(di.getCode());
                if (item != null) {
                    Helper.addIfNonNull(textToExtract, item.getPlaceholder(), item.getF());
                }
            }
        }

        // === Handle grouping field ===
        Helper.addIfNonNull(
                textToExtract,
                dataset.getX() == null ? null :
                        dataset.getX()
                                .at("/defGroupField")
                                .asText()
                                .replace("prev.", "$prev$.")
                                .replace("data.", "$.")
        );

        // === Extract action fields ===
        for (DatasetAction a : dataset.getActions()) {
            addIfNonNull(textToExtract, a.getPre(), a.getF(), a.getParams(), a.getUrl());
        }

        // === Convert extracted tokens into field map ===
        return extractVariables(
                Set.of("$", "$prev$", "$_", "$$"),
                String.join(",", textToExtract)
        );
    }

    @Transactional(readOnly = true)
    public Page<Long> findIdListByDataset(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, Pageable pageable, HttpServletRequest req) {
        return customEntryRepository.findAllIds(buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req), pageable);

    }

    public Stream<Entry> findListByDatasetStream(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, HttpServletRequest req) {
        return customEntryRepository.streamAll(buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req));
    }

    public Long countByDataset(Long datasetId, String searchText, String email, Map filters, String cond, HttpServletRequest req) {
        return entryRepository.count(buildSpecification(datasetId, searchText, email, filters, cond, null, null, req));
    }

    @Transactional(readOnly = true)
    public Map getDashboardDataNativeNew(Long dashboardId, Map<String, Object> filters, String email, HttpServletRequest req) {

        Dashboard dashboard = dashboardService.getDashboard(dashboardId);
        Map<Object, Object> data = new HashMap<>();
        for (Chart c : dashboard.getCharts()) {
            data.put(c.getId(), getChartDataNative(c.getId(), filters, email, req));
        }
        return data;
    }

    @Transactional(readOnly = true)
    public Map getDashboardMapDataNativeNew(Long dashboardId, Map<String, Object> filters, String email, HttpServletRequest req) {

        Dashboard dashboard = dashboardService.getDashboard(dashboardId);
        Map<Object, Object> data = new HashMap<>();
        for (Chart c : dashboard.getCharts()) {
            data.put(c.getId(), getChartMapDataNative(c.getId(), filters, email, req));
        }

        return data;
    }


    // #1 TO QUERY STATS
    @Transactional(readOnly = true)
    public Map<String, Object> getChartDataNative(Long chartId, Map<String, Object> filters, String email, HttpServletRequest req) {

        Chart c = dashboardService.getChart(chartId);

        boolean flipAxis = c.getX() != null && c.getX().at("/swap").asBoolean(false);

        Map<String, Object> data = new HashMap<>();

        Map<String, Object> dataMap = new HashMap<>();

        // need user
        // 1. Safely get the user or create a fallback
        User user = userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId())
                .orElseGet(() -> {
                    User fallback = new User();
                    String safeEmail = email != null ? email : "anonymous";
                    fallback.setEmail(safeEmail);
                    fallback.setName(safeEmail);
                    return fallback;
                });

        // 2. Put it in the map (it is guaranteed to not be null here)
        dataMap.put("user", MAPPER.convertValue(user, Map.class));

        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());
        dataMap.put("conf", Map.of());


        if ("db".equals(c.getSourceType())) {
            Map<String, String> filtersReq = new HashMap();
            if (req != null) {
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    if (entry.getKey().contains("$")) {
                        filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                    }
                }
            }
            Map<String, Object> pF = MAPPER.convertValue(c.getPresetFilters(), Map.class);
            Map<String, Object> presetFilters = Optional.ofNullable(pF).orElse(new HashMap<>())
                    .entrySet().stream()
                    .filter(x -> x.getKey().startsWith("$"))
                    .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

            presetFilters.replaceAll((k, v) -> Helper.compileTpl(v.toString(), dataMap));

            Map<String, Object> filtersNew = new HashMap<>();

            Optional.ofNullable(presetFilters).ifPresent(filtersNew::putAll);
            Optional.ofNullable(filtersReq).ifPresent(filtersNew::putAll);
            Optional.ofNullable(filters).ifPresent(filtersNew::putAll);

            Form form = c.getForm();
            // if form is extended form, then use original form
            if (form.getX().get("extended") != null) {
                Long extendedId = form.getX().get("extended").asLong();
                form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", extendedId));
            }

            // Handle multiple field codes and values with Cartesian product logic
            if (c.getFieldCode() != null && c.getFieldValue() != null) {
                String[] fieldCodes = c.getFieldCode().split(",");
                String[] fieldValues = c.getFieldValue().split(","); // Split multiple value fields


                boolean topN = false;
                String topNField = "";
                String topNFieldDir = "ASC";
                Integer topNLimit = 10;

                JsonNode xNode = c.getX();

                // 1. Check if the 'x' node exists at all
                if (xNode != null && !xNode.isNull()) {
                    // 2. Safely extract topNOrderBy (String)
                    if (xNode.hasNonNull("topN")) {
                        topN = xNode.get("topN").asBoolean(false);
                    }
                    if (xNode.hasNonNull("topNField")) {
                        topNField = xNode.get("topNField").asText();
                    }
                    if (xNode.hasNonNull("topNFieldDir")) {
                        topNFieldDir = xNode.get("topNFieldDir").asText("ASC");
                    }
                    if (xNode.hasNonNull("topNLimit")) {
                        topNLimit = xNode.get("topNLimit").asInt(10);
                    }
                }


                // Trigger Cartesian loop if EITHER categories or values have multiple entries
                if (fieldCodes.length > 1 || fieldValues.length > 1) {
                    Map<String, String> fieldLabels = new HashMap<>();

                    // 1. Cache labels for all Category Codes
                    for (String fieldCode : fieldCodes) {
                        String[] split = fieldCode.split("[#.]", 2);
                        if (split.length > 1) {
                            String[] actualCode = split[1].split("\\.");
                            Map<String, Item> items = "$prev$".equals(split[0]) ? form.getPrev().getItems() : c.getForm().getItems();
                            if (items != null && items.containsKey(actualCode[0])) {
                                fieldLabels.put(fieldCode, items.get(actualCode[0]).getLabel());
                            }
                        }
                    }

                    // 2. Cache labels for all Value Fields
                    for (String fieldValue : fieldValues) {
                        String[] split = fieldValue.split("[#.]", 2);
                        if (split.length > 1) {
                            String[] actualCode = split[1].split("\\.");
                            Map<String, Item> items = "$prev$".equals(split[0]) ? form.getPrev().getItems() : c.getForm().getItems();
                            if (items != null && items.containsKey(actualCode[0])) {
                                fieldLabels.put(fieldValue, items.get(actualCode[0]).getLabel());
                            }
                        }
                    }

                    // [NEW] 3. Prepare master containers for the single DB trip
                    List<String> unionQueries = new ArrayList<>();
                    Map<String, Object> masterSqlParams = new HashMap<>();
                    int paramCounter = 0;

                    // [NEW] 4. Nested loop: Build the SQL String for each combination
                    for (String fieldCode : fieldCodes) {
                        for (String fieldValue : fieldValues) {

                            String codeLabel = fieldLabels.get(fieldCode);
                            String valueLabel = fieldLabels.get(fieldValue);

                            codeLabel = (codeLabel != null) ? codeLabel.trim() : fieldCode;
                            valueLabel = (valueLabel != null) ? valueLabel.trim() : fieldValue;

                            String combinedLabel;
                            if (fieldCodes.length > 1 && fieldValues.length > 1) {
                                combinedLabel = codeLabel + " - " + valueLabel;
                            } else if (fieldCodes.length > 1) {
                                combinedLabel = codeLabel;
                            } else {
                                combinedLabel = valueLabel;
                            }

                            // Generate a unique parameter name for this label combination
                            String labelParam = "lbl_" + paramCounter++;
                            masterSqlParams.put(labelParam, combinedLabel);

                            // Build the SQL block and add it to the list
                            String sqlBlock = _buildChartizeDbDataSql(
                                    c.getAgg(), fieldCode, fieldValue, c.isSeries(),
                                    c.getFieldSeries(), form, user, c.getStatusFilter(), c.getPrevStatusFilter(),
                                    filtersNew, masterSqlParams, labelParam, flipAxis,topN,
                                    topNField + "," + topNFieldDir, topNLimit
                            );

                            unionQueries.add(sqlBlock);
                        }
                    }

                    // [NEW] 5. Join them all together with UNION ALL and execute EXACTLY ONCE
                    String finalMassiveSql = String.join(" UNION ALL ", unionQueries) + " ORDER BY name ASC";
                    List<Object[]> allList = dynamicSQLRepository.runQuery(finalMassiveSql, masterSqlParams, true);

                    return __transformResultset(true, c.isShowAgg(), allList);

                } else {
                    return _chartizeDbData(c.getAgg(), c.getFieldCode(), c.getFieldValue(), c.isSeries(),
                            c.getFieldSeries(), c.isShowAgg(), form, user, c.getStatusFilter(), c.getPrevStatusFilter(), filtersNew ,topN,
                            topNField + "," + topNFieldDir, topNLimit);
                }
            }


        } else if ("rest".equals(c.getSourceType())) {

            HttpResponse<String> response;

            try {
                // 1. Initialize the builder
                var requestBuilder = HttpRequest.newBuilder()
                        .uri(new URI(c.getEndpoint()))
                        .timeout(Duration.ofSeconds(15)) // <-- ADD THIS!
                        .GET();

                // 2. Safely extract and attach the Authorization header if it exists
                if (c.getEndpoint().contains(Constant.IO_BASE_DOMAIN) && req != null && req.getHeader("Authorization") != null) {
                    requestBuilder.header("Authorization", req.getHeader("Authorization"));
                }

                // 3. Build the final request
                var httpGet = requestBuilder.build();
                response = HTTP_CLIENT.send(httpGet, HttpResponse.BodyHandlers.ofString());
            }  catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Request interrupted -> ", e);
                }
                logger.error("Entry::getChartDataNative ->" + e.getMessage());
                throw new RuntimeException("Failed to load chart data from ["+c.getEndpoint()+"]", e);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }


            try {
                JsonNode node = MAPPER.readTree(response.body());
                data.put("data", node);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
        return data;
    }

    // #2 TO CHARTIZE DB DATA
    public Map<String, Object> _chartizeDbData(String __agg,
                                               String __codeField,
                                               String __valueField,
                                               boolean __isSeries,
                                               String __seriesField,
                                               boolean __showAgg,
                                               Form __form, User __user,
                                               JsonNode __status,JsonNode __prevStatus,
                                               Map<String, Object> __filters,
                                               boolean topN, String topNField, Integer topNLimit) {
        Map<String, Object> data = new HashMap<>();

        // Create a dictionary to hold the reverse mapping
        Map<String, String> dict = new HashMap<>();

        try {
            List<Object[]> result = _queryChartizeDbData(__agg, __codeField, __valueField, __isSeries, __seriesField, __showAgg, __form, __user, __status, __prevStatus, __filters, topN,
                    topNField, topNLimit);
            data = __transformResultset(__isSeries, __showAgg, result);

            String lang = "en";
            if (__form != null && __form.getApp() != null && __form.getApp().getX() != null && !__form.getApp().getX().isNull()) {
                lang = __form.getApp().getX().at("/lang").asText("en").toLowerCase();
            }



            // ======= VIRTUAL FIELD TRANSLATION =======
            if ("$.$statusText".equals(__codeField) || "$.$statusText".equals(__seriesField)) {

                // 1. Handle Flat Charts (Pie, Donut, standard Bar)
                if (!__isSeries && data.containsKey("data")) {
                    List<Map<String, Object>> chartData = (List<Map<String, Object>>) data.get("data");
                    if ("$.$statusText".equals(__codeField)) {
                        for (Map<String, Object> point : chartData) {
                            String rawConcat = (String) point.get("name");
                            String translated = __translateVirtualConcat(rawConcat, __form, lang);
                            dict.put(translated, rawConcat); // Store the mapping
                            point.put("name", translated);                        }
                    }
                }

                // 2. Handle Series Charts (Matrix Format: List<List<Object>>)
                if (__isSeries && data.containsKey("data")) {
                    List<List<Object>> dataset = (List<List<Object>>) data.get("data");

                    if (!dataset.isEmpty()) {
                        // Translate Categories (Header Row: dataset.get(0))
                        if ("$.$statusText".equals(__codeField)) {
                            List<Object> header = dataset.get(0);
                            for (int i = 1; i < header.size(); i++) { // Skip index 0 ("Series")
                                String rawConcat = header.get(i).toString();
                                String translated = __translateVirtualConcat(rawConcat, __form, lang);
                                dict.put(translated, rawConcat); // Store the mapping
                                header.set(i, translated);                            }
                        }

                        // Translate Series Legends (First item of all subsequent rows)
                        if ("$.$statusText".equals(__seriesField)) {
                            for (int i = 1; i < dataset.size(); i++) { // Skip row 0 (Header)
                                List<Object> row = dataset.get(i);
                                String rawConcat = row.get(0).toString();
                                String translated = __translateVirtualConcat(rawConcat, __form, lang);
                                dict.put(translated, rawConcat); // Store the mapping
                                row.set(0, translated);                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        // Attach the dictionary to the payload for the frontend to use
        data.put("_dict", dict);

        return data;
    }

    private String __translateVirtualConcat(String rawConcat, Form form, String lang) {
        if (rawConcat != null && rawConcat.contains("_##_")) {
            String[] parts = rawConcat.split("_##_");
            try {
                Long tierId = Long.parseLong(parts[0]);
                String rawStatus = parts.length > 1 ? parts[1] : "";
                return __computeStatusText(form, tierId, rawStatus, lang);
            } catch (Exception e) {
                // Ignored, returns raw string below
            }
        }
        return rawConcat;
    }

    // Holds the compiled SQL properties for a single chart field
    private static class ChartFieldMeta {
        public String selectSql = "";
        public String join = "";
        public String tierCond = "";
        public String multiJoin = "";
        public String multiPrevJoin = "";
    }

    // DRY Helper to parse fields into SQL statements
    private ChartFieldMeta __buildFieldMetadata(String rawField, boolean isValueField, String distinctAgg,
                                                Map<String, String> jsonRootMap, String approvalAlias, String multiAlias) {
        ChartFieldMeta meta = new ChartFieldMeta();
        if (rawField == null || rawField.isEmpty()) return meta;

        String[] parts = rawField.split("[#.]", 2);
        String prefix = parts[0];
        String field = parts.length > 1 ? parts[1] : "";
        String jsonRoot = jsonRootMap.getOrDefault(prefix, "e.data");

        String baseSql;

        // A. Handle Approval Joins (Using STRICT legacy aliases to prevent filter crashes)
        if ("$$".equals(prefix) || "approval".equals(prefix)) {
            String[] approvalParts = field.split("\\.", 2);
            String tier = approvalParts[0];
            String actualField = approvalParts.length > 1 ? approvalParts[1] : "";

            meta.join = " left join entry_approval " + approvalAlias + " on e.id = " + approvalAlias + ".entry ";
            meta.tierCond = " and " + approvalAlias + ".tier = " + tier;
            jsonRoot = approvalAlias + ".data";
            field = actualField;
        }

        // B. Handle Multi/Wildcard Logic (*.) json_table joins
        if (rawField.contains("*.")) {
            String[] multiParts = rawField.split(java.util.regex.Pattern.quote("*."));
            String rootPath = multiParts[0].replace("$prev$", "$").replace("$$", "$") + "[*]";
            String colPath = "$." + multiParts[1];
            String colNameSafe = multiParts[1].replaceAll("[.]+", "_");

            String joinSql = " left join json_table(" + jsonRoot + ", '" + rootPath + "' columns(" +
                    colNameSafe + " varchar(2000) path '" + colPath + "')) as " + multiAlias + " on 1=1 ";

            if (multiParts[0].contains("$prev$")) {
                meta.multiPrevJoin = joinSql;
            } else {
                meta.multiJoin = joinSql;
            }
            baseSql = "coalesce(" + multiAlias + "." + colNameSafe + ", 'n/a')";

        } else if ("$_".equals(prefix)) {
            // C. Base SQL for native columns (Always coalesce)
            baseSql = "coalesce(" + jsonRoot + "." + field + ", 'n/a')";

        } else {
            // D. Base SQL for standard JSON extraction
            if (isValueField) {
                // Value fields DO NOT get coalesced to 'n/a' to protect SUM/AVG math operations
                baseSql = "json_value(" + jsonRoot + ", '$." + field + "')";
            } else {
                baseSql = "coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a')";
            }
        }

        // E. Wrap aggregation and distinct conditions for values
        if (isValueField) {
            meta.selectSql = "(" + distinctAgg + " " + baseSql + ")";
        } else {
            meta.selectSql = baseSql;
        }

        return meta;
    }

    // =========================================================================
    // MAIN CHART QUERY METHOD
    // =========================================================================

    // #3 TO QUERY STATS
    private String _buildChartizeDbDataSql(String __agg, String __codeField, String __valueField,
                                           boolean __isSeries, String __seriesField,
                                           Form __form, User __user, JsonNode __status, JsonNode __prevStatus,
                                           Map<String, Object> __filters,
                                           Map<String, Object> sqlParams, // Shared Parameter Map
                                           String labelParamName,         // Custom Cartesian Label
                                           boolean flipAxis, boolean topN, String topNOrderBy, Integer topNLimit) {

        Map<String, Object> tplDataMap = new HashMap<>();
        if (__user != null) {
            Map userMap = MAPPER.convertValue(__user, Map.class);
            tplDataMap.put("user", userMap);
        } else {
            tplDataMap.put("user", Map.of("email", "anonymous", "name", "anonymous"));
        }

        tplDataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        tplDataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        tplDataMap.put("todayEnd", calendarEnd.getTimeInMillis());

        // The legacy "eac" entries remain here to support fallback logic,
        // but the filter loop overrides them dynamically below.
        Map<String, String> jsonRootMap = Map.of(
                "$_", "e",
                "$", "e.data",
                "$prev$", "e2.data",
                "$$", "eac.data",
                "$$_", "eac",
                "data", "e.data",
                "prev", "e2.data",
                "approval", "eac.data");

        // SAFELY parse __status
        Map<String, String> statusFilter = null;
        if (__status != null && !__status.isNull() && !__status.isEmpty()) {
            statusFilter = MAPPER.convertValue(__status, Map.class);
        }

        List<String> cond = new ArrayList<>();
        String statusCond = "";

        if (!Helper.isNullOrEmpty(statusFilter)) {
            boolean hasValidData = statusFilter.values().stream()
                    .anyMatch(val -> val != null && !val.isEmpty());

            if (hasValidData) {
                for (Map.Entry<String, String> ent : statusFilter.entrySet()) {
                    String key = ent.getKey();
                    String val = ent.getValue();
                    if (val != null && !val.isEmpty()) {
                        List<String> statusList = Arrays.asList(val.split(","));
                        String paramName = "status_" + key.replace("-", "m");
                        sqlParams.put(paramName, statusList);

                        if ("-1".equals(key)) {
                            cond.add("(e.current_tier_id is null and e.current_status in (:" + paramName + "))");
                        } else {
                            cond.add("(e.current_tier_id = " + key + " and e.current_status in (:" + paramName + "))");
                        }
                    }
                }
                statusCond = " AND (" + String.join(" or ", cond) + ")";
            }
        }

        // NEW: Parse and apply __prevStatus using the 'e2' table alias
        Map<String, String> prevStatusFilter = null;
        if (__prevStatus != null && !__prevStatus.isNull() && !__prevStatus.isEmpty()) {
            prevStatusFilter = MAPPER.convertValue(__prevStatus, Map.class);
        }

        List<String> prevCond = new ArrayList<>();
        String prevStatusCond = "";

        if (!Helper.isNullOrEmpty(prevStatusFilter)) {
            boolean hasValidPrevData = prevStatusFilter.values().stream()
                    .anyMatch(val -> val != null && !val.isEmpty());

            if (hasValidPrevData) {
                for (Map.Entry<String, String> ent : prevStatusFilter.entrySet()) {
                    String key = ent.getKey();
                    String val = ent.getValue();
                    if (val != null && !val.isEmpty()) {
                        List<String> statusList = Arrays.asList(val.split(","));
                        // Make sure paramName is unique to avoid colliding with standard status parameters
                        String paramName = "prev_status_" + key.replace("-", "m");
                        sqlParams.put(paramName, statusList);

                        if ("-1".equals(key)) {
                            prevCond.add("(e2.current_tier_id is null and e2.current_status in (:" + paramName + "))");
                        } else {
                            prevCond.add("(e2.current_tier_id = " + key + " and e2.current_status in (:" + paramName + "))");
                        }
                    }
                }
                prevStatusCond = " AND (" + String.join(" or ", prevCond) + ")";
            }
        }

        List<String> pred = new ArrayList<>();
        String filterCond = "";
        String filterJoinSql = ""; // [NEW] Catch dynamic joins triggered by filters

        if (!Helper.isNullOrEmpty(__filters)) {
            __filters.replaceAll((k, v) -> Helper.compileTpl(v.toString(), tplDataMap));
            for (String f : __filters.keySet()) {
                Object rawVal = __filters.get(f);
                if (rawVal == null) continue;

                String[] splitted1 = f.split("\\.");
                String rootCol = splitted1[0];
                String predRoot = jsonRootMap.get(rootCol);

                Long tierId = null;
                String fieldFull = "";
                String fieldCode = "";
                Form form = null;

                // ==============================================================
                // [NEW] MULTI-TIER APPROVAL FIX: Dynamic Aliases (eac_100, etc.)
                // ==============================================================
                if ("$$".equals(rootCol) || "$$_".equals(rootCol)) {
                    String[] splitted = f.split("\\.", 3);
                    tierId = Long.parseLong(splitted[1]);
                    String uniqueAlias = "eac_" + tierId;

                    // 1. Override the static 'eac' with our tier-specific alias
                    predRoot = "$$".equals(rootCol) ? uniqueAlias + ".data" : uniqueAlias;

                    // 2. Safely inject the unique join ONCE per tier
                    if (!filterJoinSql.contains(uniqueAlias + ".tier = " + tierId)) {
                        filterJoinSql += " left join entry_approval " + uniqueAlias +
                                " on e.id = " + uniqueAlias + ".entry and " + uniqueAlias + ".tier = " + tierId + " ";
                    }

                    // 3. Extract standard field references
                    fieldFull = splitted[2];
                    fieldCode = fieldFull.split("\\.")[0].split("~")[0];
                    if ("$$".equals(rootCol)) form = __form;

                } else if ("$".equals(rootCol) || "$prev$".equals(rootCol)) {
                    String[] splitted = f.split("\\.", 2);
                    fieldFull = splitted[1];
                    fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
                    form = "$".equals(rootCol) ? __form : __form != null ? __form.getPrev() : null;
                } else if ("$_".equals(rootCol)) {
                    fieldCode = f.split("\\.")[1];
                }
                // ==============================================================

                if (Arrays.asList("$$", "$", "$prev$").contains(rootCol)) {
                    String jsonFieldPath = fieldFull;
                    String operator = "eq";

                    if (fieldFull.contains("~")) {
                        String[] splitField = fieldFull.split("~", 2);
                        jsonFieldPath = splitField[0];
                        operator = splitField[1].toLowerCase();
                    }

                    String itemType = null;
                    String formSubType = null;

                    if (form != null && form.getItems() != null && form.getItems().get(fieldCode) != null) {
                        itemType = form.getItems().get(fieldCode).getType();
                        formSubType = form.getItems().get(fieldCode).getSubType();
                    }

                    __applyChartNativeSqlPredicate(pred, operator, rawVal, predRoot, jsonFieldPath, itemType, formSubType, rootCol, sqlParams);

                } else if ("$$_".equals(rootCol)) {
                    if ("timestamp".equals(fieldCode)) {
                        if (rawVal != null && !rawVal.toString().isEmpty()) {
                            String pName = "p" + sqlParams.size();
                            sqlParams.put(pName, rawVal);

                            if (fieldFull.contains("~")) {
                                String[] splitField = fieldFull.split("~");
                                if ("from".equals(splitField[1])) {
                                    pred.add(" (" + predRoot + ".timestamp >= :" + pName + ") ");
                                }
                                if ("to".equals(splitField[1])) {
                                    pred.add(" (" + predRoot + ".timestamp <= :" + pName + ") ");
                                }
                            } else {
                                pred.add(" (" + predRoot + ".timestamp = :" + pName + ") ");
                            }
                        }
                    } else if ("status".equals(fieldCode)) {
                        String pName = "p" + sqlParams.size();
                        sqlParams.put(pName, (rawVal + "").trim());
                        pred.add(" (" + predRoot + ".status = :" + pName + ") ");
                    } else if ("remark".equals(fieldCode)) {
                        String pName = "p" + sqlParams.size();
                        sqlParams.put(pName, "%" + rawVal + "%");
                        pred.add(" ( upper(" + predRoot + ".remark) like upper(:" + pName + ") ) ");
                    }
                } else if ("$_".equals(rootCol)) {
                    String pName = "p" + sqlParams.size();
                    sqlParams.put(pName, rawVal);

                    if ("email".equals(fieldCode)) {
                        sqlParams.put(pName, (rawVal + "").trim());
                        pred.add(" ( upper(" + predRoot + ".email) = upper(:" + pName + ") ) ");
                    } else if ("currentTier".equals(fieldCode)) {
                        pred.add(" (" + predRoot + ".current_tier = :" + pName + ") ");
                    } else if ("currentStatus".equals(fieldCode)) {
                        pred.add(" ( upper(" + predRoot + ".current_status) = upper(:" + pName + ") ) ");
                    }
                }
            }
            if (pred.size() > 0) {
                filterCond = " AND (" + String.join(" and ", pred) + ")";
            }
        }

        String distinctAgg = "count".equals(__agg) ? "distinct" : "";

        // Axis generation uses isolated aliases ('ec', 'eav', 'es') to prevent multi-tier filter collisions
        ChartFieldMeta codeMeta = __buildFieldMetadata(__codeField, false, "", jsonRootMap, "ec", "code_field_multi");
        ChartFieldMeta valueMeta = __buildFieldMetadata(__valueField, true, distinctAgg, jsonRootMap, "eav", "value_field_multi");
        ChartFieldMeta seriesMeta = __isSeries ? __buildFieldMetadata(__seriesField, false, "", jsonRootMap, "es", "series_field_multi") : new ChartFieldMeta();

        String codeSql = codeMeta.selectSql;
        if ("$.$statusText".equals(__codeField)) {
            // Force 'drafted' and 'submitted' into a single bucket so MariaDB sums them together
            codeSql = "IF(LOWER(e.current_status) IN ('drafted', 'submitted'), " +
                    "CONCAT('0_##_', LOWER(e.current_status)), " +
                    "CONCAT(COALESCE(e.current_tier_id, 0), '_##_', e.current_status))";
        }
        String valueSql = valueMeta.selectSql;
        String seriesSql = seriesMeta.selectSql;
        if ("$.$statusText".equals(__seriesField)) {
            seriesSql = "IF(LOWER(e.current_status) IN ('drafted', 'submitted'), " +
                    "CONCAT('0_##_', LOWER(e.current_status)), " +
                    "CONCAT(COALESCE(e.current_tier_id, 0), '_##_', e.current_status))";
        }

        if (__isSeries) {
            codeSql = "concat(" + codeSql + ",'_:_'," + seriesSql + ")";
        }

        // INJECT THE CARTESIAN LABEL DIRECTLY INTO SQL
        String finalNameCol = codeSql;
        if (labelParamName != null) {
            if (flipAxis) {
                finalNameCol = "concat(:" + labelParamName + ", '_:_', " + codeSql + ")";
            } else {
                finalNameCol = "concat(" + codeSql + ", '_:_', :" + labelParamName + ")";
            }
        }

        String prevJoin = " left join entry e2 on e.prev_entry = e2.id ";

        String liveCond = "";
        if (__form.isLive()) {
            liveCond = " AND e.live = " + __form.isLive();
        } else {
            if (__form.getApp() != null && __form.getApp().getX() != null && !__form.getApp().getX().at("/devInData").asBoolean(false)) {
                liveCond = " AND e.live = " + __form.isLive();
            }
        }

        // 1. Determine if Top-N is necessary (Skip it for COUNT)
        boolean isCount = __agg.trim().equalsIgnoreCase("count");

        if (topN && !isCount) {
            // --- TOP-N QUERY (For SUM, AVG, etc. - No DISTINCT regex needed!) ---
            String[] orderParams = topNOrderBy.split(",");
            String rawField = orderParams[0]; // e.g., "$.timestamp" or "null"
            String dir = orderParams.length > 1 ? orderParams[1] : "ASC";

            String finalTopNField = "e.id"; // Safe database fallback

            // Safely extract the root and the field path
            if (rawField != null && !rawField.trim().isEmpty() && !"null".equals(rawField)) {
                String[] fieldParts = rawField.split("\\.", 2); // splits "$ . timestamp"
                String prefix = fieldParts[0]; // "$" or "$_"
                String actualField = fieldParts.length > 1 ? fieldParts[1] : ""; // "timestamp"

                String rootTable = jsonRootMap.getOrDefault(prefix, "e.data");

                if ("$_".equals(prefix)) {
                    finalTopNField = rootTable + "." + actualField;
                } else if (!actualField.isEmpty()) {
                    finalTopNField = "json_value(" + rootTable + ",'$." + actualField + "')";
                }
            }

            // 1. Determine if the outer query needs Window Function syntax for Median
            boolean isTopNMedian = "median".equalsIgnoreCase(__agg.trim());
            String topNSelect = isTopNMedian ? "SELECT DISTINCT name, " : "SELECT name, ";
            String topNAggExtra = isTopNMedian ? " OVER (PARTITION BY name)" : "";
            String topNGroupBy = isTopNMedian ? "" : " GROUP BY name";

            // Explicitly CAST the raw_value to DECIMAL, converting empty strings to NULL first
            String outerAgg = isTopNMedian ? "MEDIAN(CAST(NULLIF(raw_value, '') AS DECIMAL(38,4)))" : __agg + "(raw_value)";

            // 2. Build the query
            return topNSelect + outerAgg + topNAggExtra + " as value FROM (" +
                    "SELECT " + finalNameCol + " as name, " +
                    valueSql + " as raw_value, " +
                    "ROW_NUMBER() OVER (PARTITION BY " + codeSql + " ORDER BY " + finalTopNField + " " + dir + ") as rn " +
                    "FROM entry e " +
                    codeMeta.join + codeMeta.multiJoin +
                    valueMeta.join + valueMeta.multiJoin +
                    seriesMeta.join + seriesMeta.multiJoin +
                    prevJoin + filterJoinSql + // INJECT FILTER JOINS HERE
                    codeMeta.multiPrevJoin + valueMeta.multiPrevJoin + seriesMeta.multiPrevJoin +
                    " WHERE e.form=" + __form.getId() +
                    codeMeta.tierCond + valueMeta.tierCond + seriesMeta.tierCond +
                    statusCond + prevStatusCond + filterCond + " AND e.deleted = false " + liveCond +
                    ") as ranked_data " +
                    "WHERE rn <= " + topNLimit + " " +
                    topNGroupBy;


        } else if ("median".equalsIgnoreCase(__agg.trim())) {
            // --- MEDIAN QUERY (MariaDB Window Function + DISTINCT) ---
            return "select distinct " + finalNameCol + " as name, " +
                    "MEDIAN(CAST(NULLIF(" + valueSql + ", '') AS DECIMAL(38,4))) OVER (PARTITION BY " + codeSql + ") as value " +
                    " from entry e " +
                    codeMeta.join + codeMeta.multiJoin +
                    valueMeta.join + valueMeta.multiJoin +
                    seriesMeta.join + seriesMeta.multiJoin +
                    prevJoin + filterJoinSql + // INJECT FILTER JOINS HERE
                    codeMeta.multiPrevJoin + valueMeta.multiPrevJoin + seriesMeta.multiPrevJoin +
                    " where e.form=" + __form.getId() +
                    codeMeta.tierCond + valueMeta.tierCond + seriesMeta.tierCond +
                    statusCond + prevStatusCond + filterCond + " and e.deleted = false " + liveCond;

        } else {
            // --- ORIGINAL FLAT QUERY (Handles COUNT and DISTINCT perfectly) ---
            return "select " + finalNameCol + " as name, " +
                    __agg + valueSql + " as value " +
                    " from entry e " +
                    codeMeta.join + codeMeta.multiJoin +
                    valueMeta.join + valueMeta.multiJoin +
                    seriesMeta.join + seriesMeta.multiJoin +
                    prevJoin + filterJoinSql + // INJECT FILTER JOINS HERE
                    codeMeta.multiPrevJoin + valueMeta.multiPrevJoin + seriesMeta.multiPrevJoin +
                    " where e.form=" + __form.getId() +
                    codeMeta.tierCond + valueMeta.tierCond + seriesMeta.tierCond +
                    statusCond+ prevStatusCond + filterCond + " and e.deleted = false " + liveCond +
                    " group by " + codeSql;
        }

    }


    // #4 Execute the query and transform the resultset into the expected format
    @Transactional(readOnly = true)
    public List<Object[]> _queryChartizeDbData(String __agg, String __codeField, String __valueField,
                                               boolean __isSeries, String __seriesField, boolean __showAgg,
                                               Form __form, User __user, JsonNode __status, JsonNode __prevStatus, Map<String, Object> __filters,
                                               boolean topN, String topNField, Integer topNLimit) {

        Map<String, Object> sqlParams = new HashMap<>();

        // Call our new builder for a single query (no label injection)
        String sql = _buildChartizeDbDataSql(__agg, __codeField, __valueField, __isSeries, __seriesField,
                __form, __user, __status, __prevStatus, __filters, sqlParams, null, false, topN,
                topNField, topNLimit)
                + " ORDER BY name ASC"; // <-- APPEND IT HERE

        return dynamicSQLRepository.runQuery(sql, sqlParams, true);
    }


    // #3.1 THE HELPER METHOD (Fully Refactored & Enterprise Safe)
    private void __applyChartNativeSqlPredicate(List<String> pred, String operator, Object rawVal,
                                                String predRoot, String jsonFieldPath,
                                                String itemType, String formSubType, String rootCol,
                                                Map<String, Object> sqlParams) {
        if (rawVal == null) return;

        String valStr = rawVal.toString().trim();
        String jsonValExtract = "json_value(" + predRoot + ",'$." + jsonFieldPath + "')";

        // A. HANDLE WILDCARDS / LISTS (e.g. $.child*.code)
        if (jsonFieldPath.contains("*")) {
            String fieldTranslated = jsonFieldPath.replace("*", "[*]");

            // 1. INTERCEPT ARRAY NULL CHECKS FIRST (Matches your Criteria API fix)
            if ("~null".equalsIgnoreCase(valStr) || "~notnull".equalsIgnoreCase(valStr)) {

                // Extract the array of properties (e.g., '["Alice", "Bob"]' or '[""]')
                String jsonExtractProps = "json_extract(" + predRoot + ", '$." + fieldTranslated + "')";
                // Search inside that array specifically for an empty string
                String jsonSearchEmpty = "json_search(" + jsonExtractProps + ", 'one', '')";

                if ("~null".equalsIgnoreCase(valStr)) {
                    // Matches if omitted entirely (IS NULL) OR saved as an empty string
                    pred.add(" (" + jsonExtractProps + " IS NULL OR " + jsonSearchEmpty + " IS NOT NULL) ");
                } else {
                    // Matches if exists AND does not contain empty strings
                    pred.add(" (" + jsonExtractProps + " IS NOT NULL AND " + jsonSearchEmpty + " IS NULL) ");
                }
                return; // Exit early for wildcard nulls
            }

            // 2. NORMAL ARRAY SEARCH
            if ("in".equals(operator)) {
                java.util.List<String> inList = (rawVal instanceof java.util.List)
                        ? ((java.util.List<?>) rawVal).stream().map(Object::toString).collect(java.util.stream.Collectors.toList())
                        : java.util.Arrays.asList(rawVal.toString().split(","));

                java.util.List<String> orPreds = new java.util.ArrayList<>();
                for (String v : inList) {
                    String pName = "p" + sqlParams.size();
                    sqlParams.put(pName, v.trim());
                    orPreds.add("json_search(lower(" + predRoot + "),'one',lower(:" + pName + "),null,'$." + fieldTranslated + "') is not null");
                }
                if (!orPreds.isEmpty()) {
                    pred.add(" (" + String.join(" OR ", orPreds) + ") ");
                }
            } else {
                String pName = "p" + sqlParams.size();
                sqlParams.put(pName, "%" + valStr + "%");
                pred.add(" json_search(lower(" + predRoot + "),'one',lower(:" + pName + "),null,'$." + fieldTranslated + "') is not null ");
            }
            return; // Exit early for wildcards
        }

        // B. HANDLE SPECIAL NULL/NOT-NULL VALUES (Upgraded for Flat Fields)
        if ("~null".equalsIgnoreCase(valStr)) {
            // Upgraded: Also covers empty strings "" instead of just strict SQL NULLs
            pred.add(" (" + jsonValExtract + " IS NULL OR " + jsonValExtract + " = '') ");
            return; // Exit early
        } else if ("~notnull".equalsIgnoreCase(valStr)) {
            pred.add(" (" + jsonValExtract + " IS NOT NULL AND " + jsonValExtract + " != '') ");
            return; // Exit early
        }

        // Generate base parameter name for this condition to guarantee uniqueness
        String pName = "p" + sqlParams.size();

        // C. HANDLE STANDARD FORM ITEMS / OPERATORS
        switch (operator) {
            case "in":
            case "notin":
                java.util.List<String> inList = (rawVal instanceof java.util.List)
                        ? ((java.util.List<?>) rawVal).stream().map(Object::toString).collect(java.util.stream.Collectors.toList())
                        : java.util.Arrays.stream(rawVal.toString().split(",")).map(String::trim).collect(java.util.stream.Collectors.toList());

                sqlParams.put(pName, inList.isEmpty() ? java.util.Arrays.asList("") : inList);

                if ("in".equals(operator)) pred.add(" (" + jsonValExtract + " IN (:" + pName + ")) ");
                else pred.add(" (" + jsonValExtract + " NOT IN (:" + pName + ")) ");
                break;

            case "contain":
            case "contains":
                sqlParams.put(pName, "%" + valStr + "%");
                pred.add(" (upper(" + jsonValExtract + ") like upper(:" + pName + ")) ");
                break;

            case "notcontain":
                sqlParams.put(pName, "%" + valStr + "%");
                pred.add(" (upper(" + jsonValExtract + ") not like upper(:" + pName + ")) ");
                break;

            case "from":
                sqlParams.put(pName, rawVal);
                pred.add(" (" + jsonValExtract + " >= :" + pName + ") ");
                break;

            case "to":
                sqlParams.put(pName, rawVal);
                pred.add(" (" + jsonValExtract + " <= :" + pName + ") ");
                break;

            case "between":
                String[] bounds = rawVal.toString().split(",");
                if (bounds.length == 2) {
                    String pName2 = "p" + (sqlParams.size() + 1);
                    sqlParams.put(pName, bounds[0].trim());
                    sqlParams.put(pName2, bounds[1].trim());
                    pred.add(" (" + jsonValExtract + " BETWEEN :" + pName + " AND :" + pName2 + ") ");
                }
                break;

            default:
                // D. FALLBACK TO LEGACY TYPE-BASED EXACT MATCHES
                if (itemType == null) {
                    sqlParams.put(pName, "%" + valStr + "%");
                    pred.add(" (upper(" + jsonValExtract + ") like upper(:" + pName + "))");
                } else if (java.util.Arrays.asList("select", "radio").contains(itemType)) {
                    sqlParams.put(pName, valStr);
                    pred.add(" (upper(" + jsonValExtract + ") like upper(:" + pName + ")) ");
                } else if (java.util.Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(itemType)) {
                    sqlParams.put(pName, rawVal);
                    pred.add(" (" + jsonValExtract + " = :" + pName + ") ");
                } else if (java.util.Objects.equals("checkbox", itemType)) {
                    if (Boolean.parseBoolean(rawVal + "")) {
                        pred.add(" (" + jsonValExtract + " is true) ");
                    } else {
                        pred.add(" (" + jsonValExtract + " is false or json_value(" + rootCol + ",'$." + jsonFieldPath + "') is null) ");
                    }
                } else if (java.util.Objects.equals("text", itemType)) {
                    if ("input".equals(formSubType)) {
                        sqlParams.put(pName, valStr);
                    } else {
                        sqlParams.put(pName, "%" + valStr + "%");
                    }
                    pred.add(" (upper(" + jsonValExtract + ") like upper(:" + pName + ")) ");
                } else {
                    sqlParams.put(pName, valStr);
                    pred.add(" (upper(" + jsonValExtract + ") like upper(:" + pName + "))");
                }
                break;
        }
    }

    // #4.1 TO CHARTIZE DB DATA (NULL-SAFE & OPTIMIZED)
    private Map<String, Object> __transformResultset(boolean __isSeries, boolean __showAgg, List<Object[]> result) {
        Map<String, Object> data = new HashMap<>();
        data.put("series", __isSeries);
        data.put("showAgg", __showAgg);

        if (result == null || result.isEmpty()) {
            data.put("data", new ArrayList<>());
            return data;
        }

        if (__isSeries) {
            Set<String> aCat = new HashSet<>();
            Set<String> aSeries = new HashSet<>();
            Map<String, Object> dataMap = new HashMap<>();

            // 1. Safely parse names, series, and build dataMap
            for (Object[] d : result) {
                String rawName = d[0] != null ? d[0].toString() : "n/a_:_n/a";
                String[] n = rawName.split("_:_");

                String cat = n[0];
                String ser = n.length > 1 ? n[1] : "n/a"; // Protects against missing delimiter

                aCat.add(cat);
                aSeries.add(ser);
                dataMap.put(cat + "_:_" + ser, d[1] != null ? d[1] : 0);
            }

            List<String> listACat = new ArrayList<>(aCat);
            List<String> listASeries = new ArrayList<>(aSeries);
            listACat.sort(Comparator.naturalOrder());
            listASeries.sort(Comparator.naturalOrder());

            List<List<Object>> dataset = new ArrayList<>();
            List<Object> header = new ArrayList<>();
            header.add("Series");
            header.addAll(listACat);
            dataset.add(header);

            // 2. Build Dataset
            for (String ser : listASeries) {
                List<Object> row = new ArrayList<>();
                row.add(ser);
                for (String cat : listACat) {
                    row.add(dataMap.getOrDefault(cat + "_:_" + ser, 0));
                }
                dataset.add(row);
            }
            data.put("data", dataset);

            // 3. Optimized Aggregation (Primitive Math instead of Streams)
            if (__showAgg) {
                List<Object> totalRow = new ArrayList<>();
                List<Object> totalColumn = new ArrayList<>();
                totalRow.add("Total");
                totalColumn.add("Total");

                for (String ser : listASeries) {
                    double rowSum = 0d;
                    for (String cat : listACat) {
                        rowSum += safeAsDouble(dataMap.get(cat + "_:_" + ser));
                    }
                    totalRow.add(rowSum);
                }

                double gTotal = 0d;
                for (String cat : listACat) {
                    double colSum = 0d;
                    for (String ser : listASeries) {
                        colSum += safeAsDouble(dataMap.get(cat + "_:_" + ser));
                    }
                    totalColumn.add(colSum);
                    gTotal += colSum;
                }

                data.put("_arow", totalRow);
                data.put("_acol", totalColumn);
                data.put("_a", gTotal);
            }

        } else {
            List<Map<String, Object>> d1 = new ArrayList<>();
            double totalSum = 0d;

            for (Object[] e : result) {
                Map<String, Object> map = new HashMap<>();
                map.put("name", e[0] != null ? e[0] : "n/a");
                map.put("value", e[1] != null ? e[1] : 0);
                d1.add(map);

                if (__showAgg) {
                    totalSum += safeAsDouble(e[1]);
                }
            }

            data.put("data", d1);
            if (__showAgg) {
                data.put("_a", totalSum);
            }
        }

        return data;
    }

    private String __computeStatusText(Form form, Long currentTierId, String currentStatus, String lang) {
        if (currentStatus == null || currentStatus.isEmpty()) {
            return currentStatus;
        }

        // 1. Format system statuses cleanly upfront (e.g., "always_approve" -> "Approved")
        String statusLower = currentStatus.toLowerCase();
        String cleanStatus;
        String cleanStatus1 = currentStatus.substring(0, 1).toUpperCase() + currentStatus.substring(1).toLowerCase();
        // 1. Translate system statuses based on Language
        if ("ms".equals(lang)) {
            switch (statusLower) {
                case "drafted": cleanStatus = "Didraf"; break;
                case "submitted": cleanStatus = "Dihantar"; break;
                case "resubmitted": cleanStatus = "Dihantar semula"; break;
                case "returned": cleanStatus = "Dikembalikan"; break; // Added for completeness
                case "always_approve": cleanStatus = "Diproses"; break;
                default:
                    cleanStatus = cleanStatus1;
            }
        } else {
            switch (statusLower) {
                case "always_approve": cleanStatus = "Processed"; break;
                default:
                    // Capitalizes "submitted", "drafted", "returned"
                    cleanStatus = cleanStatus1;
            }
        }

        // ======= NEW SHORT-CIRCUIT =======
        // Immediately return without the tier name for these specific statuses
        if ("drafted".equalsIgnoreCase(currentStatus) || "submitted".equalsIgnoreCase(currentStatus) || currentTierId == 0L) {
            return cleanStatus;
        }
        // =================================

        try {
            // 2. Ensure form and tiers exist, and currentTier is valid
            if (form != null && form.getTiers() != null && currentTierId != null && currentTierId >= 0 && !form.getTiers().isEmpty()) {

                // [NEW] Find the actual Tier object by matching the ID
                var tier = form.getTiers().stream()
                        .filter(t -> t.getId().equals(currentTierId))
                        .findFirst()
                        .orElse(null);

                if (tier != null) {
                    String tierName = tier.getName() != null ? tier.getName() : "Unknown Tier";

                    // Start with our cleanly formatted system status
                    String statusLabel = cleanStatus;

                    // 3. IF it's a custom action, override the system label with the mapped TierAction label
                    if (tier.getActions() != null && tier.getActions().containsKey(currentStatus)) {
                        var action = tier.getActions().get(currentStatus);
                        if (action != null && action.getLabel() != null && !action.getLabel().isEmpty()) {
                            statusLabel = action.getLabel();
                        }
                    }

                    return tierName + ": " + statusLabel;
                }
            }
        } catch (Exception e) {
            // Failsafe: Catch any unexpected mapping failures safely
        }

        // 4. Ultimate Fallback (If tier logic completely fails or currentTier is null)
        return cleanStatus;
    }

    /**
     * Untuk LAMBDA
     */
    @Transactional(readOnly = true)
    public Map<String, Object> chartize(Long formId, Map cm, String email, Lambda lambda) {

        ChartizeObj c = MAPPER.convertValue(cm, ChartizeObj.class);

        User user = userRepository.findFirstByEmailAndAppId(email, lambda.getApp().getId())
                .orElseGet(() -> {
                    User fallbackUser = new User();
                    String safeEmail = email != null ? email : "anonymous";
                    fallbackUser.setEmail(safeEmail);
                    fallbackUser.setName(safeEmail); // Set other required fields if your User entity allows it
                    return fallbackUser;
                });

        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        if (form.getX().get("extended") != null) {
            Long extendedId = form.getX().get("extended").asLong();
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        return _chartizeDbData(c.agg, c.by, c.value, !Helper.isNullOrEmpty(c.series), c.series, c.showAgg, form, user, c.status, c.prevStatus,  c.filter, false, null, null);

    }

    /**
     * For LAMBDA (untuk convert list data chart kepada map based)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> chartAsMap(Map<String, Object> cdata) {
        if ((boolean) cdata.get("series")) {
            List<Object> _arow = (List<Object>) cdata.get("_arow");
            List<List<Object>> data = (List<List<Object>>) cdata.get("data");
            List<Object> column = data.get(0);
            Map<String, Object> result = new HashMap<>();
            for (int i = 1; i < data.size(); i++) { // skip header row
                Map<String, Object> m = new HashMap<>();
                List<Object> row = data.get(i);
                String series = row.get(0) + "";
                for (int j = 1; j < row.size(); j++) { // skip series column
                    m.put(column.get(j) + "", row.get(j));
                }
                if ((boolean) cdata.get("showAgg")) {
                    m.put("Total", _arow.get(i));
                }
                result.put(series, m);
            }
            return result;
//            return result; // [{"a":123,"b":121,"Total":244},{"a":123,"b":121,"Total":244}]
        } else {
            List<Map<String, Object>> data = (List<Map<String, Object>>) cdata.get("data");
            Map<String, Object> map = data.stream().collect(Collectors.toMap(x -> x.get("name") + "", x -> x.get("value")));
            if ((boolean) cdata.get("showAgg")) {
                map.put("_a", cdata.get("_a")); //_a
            }
            return map; // {"a":123,"b":123,"c":123,"_a":369}
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChartMapDataNative(Long chartId, Map<String, Object> filtersNew, String email, HttpServletRequest req) {
        Map<String, Object> cdata = getChartDataNative(chartId, filtersNew, email, req);
        return chartAsMap(cdata);
    }

    @Transactional(readOnly = true)
    public List<ObjectNode> findListByDatasetData(Long datasetId, String searchText, String email, Map<String,
            Object> filters, String cond, List<String> sorts, List<Long> ids, boolean anonymous,
                                                Pageable pageable, HttpServletRequest req) {

        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        // Build specification like before
        Specification<Entry> spec =
                buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req);

        // If size is not provided → return ALL results (PageRequest unlimited)
        Pageable effectivePageable =
                req.getParameter("size") != null
                        ? pageable
                        : PageRequest.of(0, Integer.MAX_VALUE);

        Map<String, Set<String>> fieldsMap = getFieldsMap(dataset);

        // Use your optimized findPaged()
        Page<EntryDto> entryList = customEntryRepository.findDataPaged(spec, fieldsMap, effectivePageable);

        // if user doesnt include $prev in fields, then $prev will be null here
        return entryList.map(e -> {
            ObjectNode o = e.getData().deepCopy();
            o.set("$prev", e.getPrev());
            return o;
        }).getContent();

    }

    /*
     * Mungkin Return {"series":{"code":value}} atau {"code":"value"}
     * */
    @Transactional(readOnly = true)
    public Page<EntryApprovalTrail> findApprovalTrailById(long id, Pageable pageable) {
        return entryApprovalTrailRepository.findTrailByEntryId(id, pageable);
    }

    //    @Async UPDATED ON 14-MARCH-2024
    @Async("asyncExec")
    public void trail(Long entryId, JsonNode snap, String action, Long formId, String email, String remark, Integer snapTier, Long snapTierId, String snapStatus, Boolean snapEdit) {
        entryTrailRepository.save(new EntryTrail(entryId, snap, email, formId, action, remark, snapTier, snapTierId, snapStatus, snapEdit));
    }

    //    @Async UPDATED ON 14-MARCH-2024
    @Async("asyncExec")
    public void trailApproval(Long entryId, JsonNode data, Tier tier, String status, String remark, String email) {

        EntryApprovalTrail eat = new EntryApprovalTrail(data, tier, status, remark, email, entryId);
        entryApprovalTrailRepository.save(eat);
    }

    //    @Async UPDATED ON 14-MARCH-2024
    @Async("asyncExec")
    public void trailApproval(EntryApprovalTrail eat) {
        entryApprovalTrailRepository.save(eat);
    }

    public String getPrincipalEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserPrincipal) {
                return ((UserPrincipal) principal).getEmail();
            } else {
                // Optionally log or handle unexpected principal types
                return authentication.getName(); // fallback
            }
        } else {
            return "anonymous";
        }
    }

    record MailerHolder(Long mailerId, Tier tier) {}

    public record ChartizeObj(String agg, String by, String value,
                              String series, boolean showAgg,
                              JsonNode status, JsonNode prevStatus, Map<String, Object> filter) {
    }

    @Async("asyncExec")
    public void bulkResyncEntryData_ModelPicker(Long datasetId) {

        Set<Item> itemList = new HashSet<>();

        itemList.addAll(itemRepository.findByDatasourceIdAndItemType(datasetId, List.of("modelPicker")));

        List<EntryDto> entryDtoList = findListByDataset(datasetId, "%", null, null, null, null, null,
                PageRequest.of(0, Integer.MAX_VALUE), null).getContent();

        for (EntryDto le : entryDtoList) {
            JsonNode jnode = le.getData();
//            final JsonNode maskedDataNode = applyMask(dataset, jnode);
            self.resyncEntryData(itemList, "$id", jnode);
        }
    }

    record ModelUpdateHolder(Long id, String path, JsonNode jsonNode) {
    }

    @Async("asyncExec")
    public void resyncEntryData_ModelPicker(Long oriFormId, JsonNode entryDataNode) {
        for (Long did : datasetRepository.findIdsByFormId(oriFormId)) {
            Set<Item> itemList = new HashSet<>();
            itemList.addAll(itemRepository.findByDatasourceIdAndItemType(did, List.of("modelPicker")));
            Dataset dataset = datasetRepository.findById(did).get();
            final JsonNode maskedDataNode = applyMask(dataset, entryDataNode);
            self.resyncEntryData(itemList, "$id", maskedDataNode);
        }
    }

    private final TransactionTemplate transactionTemplate;

    public JsonNode applyMask(Dataset dataset, JsonNode jsonNode) {
        // Defensive null checks
        if (dataset == null || jsonNode == null) {
            return jsonNode;
        }

        boolean fieldMask = keyValueRepository.getValue("platform", "dataset-field-mask")
                .map("true"::equals)
                .orElse(false);

        boolean skipMask = dataset.getX() != null
                && dataset.getX().at("/skipMask").asBoolean(false);

        if (!fieldMask || skipMask || dataset.getItems() == null || dataset.getItems().isEmpty()) {
            // No masking, return original node
            return jsonNode;
        }

        Form form = dataset.getForm();
        Form prevForm = form != null ? form.getPrev() : null;

        // Build field extraction list
        Set<String> textToExtract = new HashSet<>(Set.of(
                "$.$id", "$.$code", "$.$counter",
                "$prev$.$id", "$prev$.$code", "$prev$.$counter"
        ));

        for (DatasetItem i : dataset.getItems()) {
            textToExtract.add(i.getPrefix() + "." + i.getCode());
            Helper.addIfNonNull(textToExtract, i.getPre());

            if ("$".equals(i.getPrefix()) && form != null) {
                Item item = form.getItems().get(i.getCode());
                if (item != null) {
                    Helper.addIfNonNull(textToExtract, item.getPlaceholder(), item.getF());
                }
            } else if ("$prev$".equals(i.getPrefix()) && prevForm != null) {
                Item item = prevForm.getItems().get(i.getCode());
                if (item != null) {
                    Helper.addIfNonNull(textToExtract, item.getPlaceholder(), item.getF());
                }
            }
        }

        Helper.addIfNonNull(textToExtract, dataset.getX() == null ? null
                : dataset.getX().at("/defGroupField").asText()
                .replace("prev.", "$prev$.")
                .replace("data.", "$.")
        );

        for (DatasetAction a : dataset.getActions()) {
            Helper.addIfNonNull(textToExtract, a.getPre(), a.getF(), a.getParams());
        }

        // Build fields map for variable extraction
        Map<String, Set<String>> fieldsMap =
                extractVariables(Set.of("$", "$prev$", "$_"), String.join(",", textToExtract));

        // Apply field filtering directly on JSON
        return filterJsonNode(jsonNode, fieldsMap.getOrDefault("$", Set.of()));
    }

    Stream<Entry> findByFormIdAndPath(Long formId, String selectPath,Object refColValue, boolean multi){
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));
        if (multi){
            return entryRepository.findByFormIdAndDataPathMultiWithId(formId, selectPath, refColValue, form.isLive());
        }else{
            return entryRepository.findByFormIdAndDataPathWithId(formId, selectPath, refColValue, form.isLive());
        }
    }

    Stream<EntryApproval> findByTierIdAndApprovalPath(Long tierId, String selectPath,Object refColValue, boolean multi){
        if (multi){
            return entryRepository.findByTierIdAndApprovalDataPathMultiWithId(tierId, selectPath, refColValue);
        }else{
            return entryRepository.findByTierIdAndApprovalDataPathWithId(tierId, selectPath, refColValue);
        }
    }

    // Update localized data when original data is updated.
    // itemList is ModelPicker/Lookup item that use dataset with the form
    // entryDataNode is the data Node

    @Transactional(readOnly = true)
    public void resyncEntryData(Set<Item> itemList, String refCol, JsonNode entryDataNode) {

        for (Item item : itemList) {
            Set<Long> entryIds = new HashSet<>();

            final Long formId = item.getForm().getId();

            final String fieldCode = item.getCode();

            final boolean isMulti = "checkboxOption".equals(item.getType()) || "multiple".equals(item.getSubType());

            SectionItem sectionItem = sectionItemRepository.findByFormIdAndCode(formId, item.getCode());

            if (sectionItem == null) continue;

            Section section = sectionItem.getSection();

            final String sectionCode = section.getCode();

            List<ModelUpdateHolder> updateList = new ArrayList<>();

            JsonNode entryValNode = entryDataNode.path(refCol);

            String entryValText = entryValNode.asText(null);

            // Wrap stream processing in a transaction (Issues with @Transactional when run from lambda)
            transactionTemplate.execute(status -> {
                try {

                    if ("list".equals(section.getType())) {

                        final String selectPath = isMulti
                                ? "$." + sectionCode + "[*]." + fieldCode + "[*]." + refCol
                                : "$." + sectionCode + "[*]." + fieldCode + "." + refCol;

                        try (Stream<Entry> entryStream = findByFormIdAndPath(formId, selectPath, MAPPER.convertValue(entryValNode, Object.class), true)) {
                            entryStream.forEach(entry -> {

                                entryIds.add(entry.getId());

                                JsonNode dataNode = entry.getData();  // safe
                                JsonNode sectionNode = dataNode.path(sectionCode);

                                this.entityManager.detach(entry);

                                // Utk list, get List and update each item @ $.<section_key>[index]
                                if (sectionNode != null && !sectionNode.isNull() && !sectionNode.isMissingNode() && sectionNode.isArray()) {

                                    for (int z = 0; z < sectionNode.size(); z++) {
                                        // jn ialah jsonnode each child item
                                        JsonNode child = sectionNode.get(z);
                                        JsonNode lookupNode = child.path(fieldCode);

                                        String updatePath = "$." + sectionCode + "[" + z + "]." + fieldCode;

                                        processUpdatePath(lookupNode, updatePath, refCol,
                                                entryDataNode, entryValText, isMulti,
                                                entry.getId(), updateList);

                                    }
                                }
                            });
                        }

                    } else if ("section".equals(section.getType())) {

                        final String selectPath = isMulti
                                ? "$." + fieldCode + "[*]." + refCol
                                : "$." + fieldCode + "." + refCol;

                        // cannot just use json_value with wildcard because it will only true if first element match
                        try (Stream<Entry> entryStream = findByFormIdAndPath(formId, selectPath, MAPPER.convertValue(entryValNode, Object.class), isMulti)) {
                            entryStream.forEach(entry -> {

                                entryIds.add(entry.getId());

                                JsonNode dataNode = entry.getData();  // safe
                                JsonNode lookupNode = dataNode.path(fieldCode);

                                this.entityManager.detach(entry);

                                String updatePath = "$." + fieldCode;

                                processUpdatePath(lookupNode, updatePath, refCol,
                                        entryDataNode, entryValText, isMulti,
                                        entry.getId(), updateList);

                            });
                        }

                    } else if ("approval".equals(section.getType())) {

                        final String selectPath = isMulti
                                ? "$." + fieldCode + "[*]." + refCol
                                : "$." + fieldCode + "." + refCol;

                        List<Tier> tlist = tierRepository.findBySectionId(section.getId());

                        for (Tier t : tlist) {
                            try (Stream<EntryApproval> entryStream = findByTierIdAndApprovalPath(t.getId(), selectPath, MAPPER.convertValue(entryDataNode.at("/" + refCol), Object.class), isMulti)) {
                                entryStream.forEach(entryApproval -> {

                                    entryIds.add(entryApproval.getEntry().getId());

                                    JsonNode approvalNode = entryApproval.getData();
                                    JsonNode lookupNode = approvalNode.path(fieldCode);

                                    this.entityManager.detach(entryApproval);

                                    String updatePath = "$." + fieldCode;

                                    processUpdatePath(lookupNode, updatePath, refCol,
                                            entryDataNode, entryValText, isMulti,
                                            entryApproval.getId(), updateList);

                                });
                            }
                        }
                    }

                    if (updateList.size() > 0) {
                        // if field ada value & !null and field ada id
                        for (ModelUpdateHolder update : updateList) {
                            if (update != null) {
                                if ("approval".equals(section.getType())) {
                                    entryRepository.updateApprovalDataFieldScope(update.id, update.path, "[" + MAPPER.valueToTree(update.jsonNode).toString() + "]");
                                } else {
                                    entryRepository.updateDataFieldScope(update.id, update.path, "[" + MAPPER.valueToTree(update.jsonNode).toString() + "]");
                                }
                            }
                        }
                    }

                    if (entryIds.size() > 0) {

                        List<Long> allIds = new ArrayList<>(entryIds); // convert set to list
                        int batchSize = 100;
                        for (int iids = 0; iids < allIds.size(); iids += batchSize) {
                            List<Long> batchIds = allIds.subList(iids, Math.min(iids + batchSize, allIds.size()));

                            // fetch the entries in this batch
                            List<Entry> entries = entryRepository.findAllById(batchIds);

                            // update each entry
                            for (Entry e : entries) {
                                updateApprover(e, e.getEmail());
                            }

                            // batch save
                            entryRepository.saveAll(entries);

                            // free memory
                            entityManager.flush();
                            entityManager.clear();
                        }
                    }

                } catch (Exception e) {
                    status.setRollbackOnly();
                    throw e;
                }
                return null;
            });
        }
    }

    public String execJs(String cacheId, String fn, Map<String, Object> bindingMaps) {
        // 1. Sort keys to ensure the generated Source string is deterministic for GraalVM caching
        List<String> sortedKeys = new ArrayList<>(bindingMaps != null ? bindingMaps.keySet() : Collections.emptyList());
        Collections.sort(sortedKeys);

        // 2. Build the JS wrapper function dynamically based on the binding keys
        StringBuilder varDeclarations = new StringBuilder();
        for (String key : sortedKeys) {
            varDeclarations.append("  var ").append(key).append(" = typeof __bind_").append(key)
                    .append(" !== 'undefined' && __bind_").append(key).append(" !== null ? JSON.parse(__bind_")
                    .append(key).append(") : null;\n");
        }

        String scriptWrapper = "function __runJs() {\n" + varDeclarations + "  return (" + fn + ");\n}";

        // 3. Execute inside an isolated Polyglot Context
        try (Context ctx = Context.newBuilder("js")
                .engine(sharedGraalEngine)
                .allowHostAccess(access)
                .build()) {

            // --- FIX: Moved inside the try block to catch the checked IOException ---
            Source fnSource = Source.newBuilder("js", scriptWrapper, "execJs-" + cacheId + ".js").build();

            // Evaluate dayjs or other base scripts if initialized
            if (dayjsSource != null) {
                ctx.eval(dayjsSource);
            }

            // Evaluate our wrapped function
            ctx.eval(fnSource);
            Value bindings = ctx.getBindings("js");

            // 4. Serialize Java objects to JSON strings and inject into bindings
            if (bindingMaps != null) {
                for (Map.Entry<String, Object> entry : bindingMaps.entrySet()) {
                    String jsonVal = null;
                    if (entry.getValue() != null) {
                        try {
                            jsonVal = MAPPER.writeValueAsString(entry.getValue());
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException("Failed to serialize binding key '" + entry.getKey() + "' to JSON", e);
                        }
                    }
                    bindings.putMember("__bind_" + entry.getKey(), jsonVal);
                }
            }

            // 5. Execute the function and stringify the output
            Value runJs = bindings.getMember("__runJs");
            Value resultVal = runJs.execute();

            if (resultVal.isNull()) {
                return null;
            }

            Value jsonObj = bindings.getMember("JSON");
            Value jsonStrVal = jsonObj.invokeMember("stringify", resultVal);

            return jsonStrVal.isNull() ? null : jsonStrVal.asString();

            // The IOException from .build() is automatically caught here:
        } catch (Exception e) {
            logger.error("Error executing JS snippet [cacheId={}]: {}", cacheId, e.getMessage(), e);
            throw new RuntimeException("JS execution failed for cacheId: " + cacheId, e);
        }
    }

    public void processUpdatePath(JsonNode lookupNode, String updatePath, String refCol,
                                  JsonNode entryDataNode, String entryValText, boolean isMulti,
                                  Long entryId, List<ModelUpdateHolder> updateList) {
        if (lookupNode != null
                && !lookupNode.isNull()
                && !lookupNode.isMissingNode()
                && !lookupNode.isEmpty()) {

            if (isMulti) {
                // multiple lookup inside section
                if (lookupNode.isArray()) {
                    // if really multiple lookup
                    for (int x = 0; x < lookupNode.size(); x++) {
                        // Have to cater for numeric (id) or string(other field), so just convert to string
                        String dataVal = lookupNode.path(x).path(refCol).asText(null);

                        if (dataVal != null && dataVal.equals(entryValText)) {
                            updateList.add(new ModelUpdateHolder(entryId, updatePath + "[" + x + "]", entryDataNode));
                        }
                    }
                }
            } else {
                //if lookup biasa dlm section
                String dataVal = lookupNode.path(refCol).asText(null);
                if (dataVal != null && dataVal.equals(entryValText)) {
                    updateList.add(new ModelUpdateHolder(entryId, updatePath, entryDataNode));
                }
            }

        }
    }


}



