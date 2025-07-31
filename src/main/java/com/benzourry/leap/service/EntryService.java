package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
//import com.benzourry.leap.dto.EntryDTO;
import com.benzourry.leap.exception.JsonSchemaValidationException;
import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.filter.EntryFilter;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import javax.script.*;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.benzourry.leap.utility.Helper.*;
import static java.util.stream.Collectors.toList;

//import com.fasterxml.jackson.databind.ObjectMapper;

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

    final ApiKeyRepository apiKeyRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private KeyValueRepository keyValueRepository;



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
                        ScreenRepository screenRepository,
                        UserRepository userRepository,
                        AppUserRepository appUserRepository,
                        EmailTemplateRepository emailTemplateRepository,
                        ItemRepository itemRepository, SectionItemRepository sectionItemRepository, NotificationService notificationService,
                        EndpointService endpointService,
                        ApiKeyRepository apiKeyRepository,
                        KeyValueRepository keyValueRepository,
                        ChartQuery chartQuery, AppService appService, PlatformTransactionManager transactionManager) {
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
        this.keyValueRepository = keyValueRepository;
//        this.transactionTemplate = transactionTemplate;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public Entry assignApprover(Long entryId, Long atId, String email) {
        Entry entry = entryRepository.findById(entryId).orElseThrow();
//        if (entryOpt.isEmpty()) {
//            throw ;
//        }
//        Entry entry = entryOpt.get();
        Tier gat = tierRepository.findById(atId).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", atId));
        Map<Long, String> approver = entry.getApprover();

        approver.put(atId, email);
        entry.setApprover(approver);
        entryRepository.save(entry); //should already have $id

        /*
          EMAIL NOTIFICATION TO INFORM ADMIN & APPLICANT ON PTJ ENDORSEMENT
          sendMail(admin);
         */

        List<Long> emailTemplates = gat.getAssignMailer();

        emailTemplates.forEach(t -> triggerMailer(t, entry, gat, email));

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
    public Page<Entry> dataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        String cond = String.valueOf(Optional.ofNullable(filters.get("@cond")).orElse("AND"));
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return findListByDataset(datasetId, "", email, filters, cond, null, filters.get("ids") != null ? (List<Long>) filters.get("ids") : null, PageRequest.of(0, Integer.MAX_VALUE), null);
        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    @Transactional(readOnly = true)
    public List<JsonNode> flatDataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset d = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));
        if (Objects.equals(d.getApp().getId(), lambda.getApp().getId())) {
            return customEntryRepository.findDataPaged(EntryFilter.builder()
                    .filters(filters)
                    .form(d.getForm())
                    .formId(d.getForm().getId())
                    .action(false)
                    .build().filter(), PageRequest.of(0, Integer.MAX_VALUE));
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
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return countByDataset(datasetId, "", email, filters, cond, null);
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
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return findListByDatasetStream(datasetId, "", email, filters, cond, null, filters.get("ids") != null ? (List<Long>) filters.get("ids") : null, null);
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
                trail(entry.getId(), entry.getData(), EntryTrail.UPDATED, entry.getForm().getId(), getPrincipalEmail(), "Change data owner to "+email,
                        entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
            } catch (Exception e) {}

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
        ObjectMapper mapper = new ObjectMapper();
        Entry e = mapper.convertValue(entry, Entry.class);
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));
        if (form.getX().get("extended") != null) {
            Long extendedId = form.getX().get("extended").asLong();
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", extendedId));
        }
        if (Objects.equals(form.getApp().getId(), lambda.getApp().getId())) {
            return save(form.getId(), e, prevId, e.getEmail(), true);
        } else {
            throw new Exception("Lambda trying to update external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public void delete(Long id, Lambda lambda) throws Exception {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            deleteEntry(id, lambda.getEmail());
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
//        System.out.println("update #1");
        return updateField(entryId, obj, root, lambda.getApp().getId());
    }

    /**
     * FOR LAMBDA
     **/
    public Entry update(Long entryId, Map obj, Lambda lambda) throws Exception {
        ObjectMapper om = new ObjectMapper();
//        System.out.println("update #2");
        return updateField(entryId, om.convertValue(obj, JsonNode.class), null, lambda.getApp().getId());
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
        ObjectMapper mapper = new ObjectMapper();
        EntryApproval ea = mapper.convertValue(approval, EntryApproval.class);
        //// CHECK TOK KLAK
        //Long id, EntryApproval gaa, String email
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return actionApp(entryId, ea, silent, email);
        } else {
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

    @Transactional
    public Entry save(Long formId, Entry entry, Long prevId, String email, boolean trail) throws Exception {
        boolean newEntry = false;
//        Form form = formService.findFormById(formId);
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));
        if (form.getX().get("extended") != null) {
            Long extendedId = form.getX().get("extended").asLong();
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        if (form.getPrev()!=null && prevId==null){
            throw new Exception("Previous entry Id is required for form with previous form");
        }

        // load validation setting from KV config
        Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation");

        boolean serverValidation = false;
        if (validateOpt.isPresent()){
            serverValidation = "true".equals(validateOpt.get());
        }

        boolean skipValidate = form.getX()!=null
                && form.getX().at("/skipValidate").asBoolean(false);

        /** NEW!!!!!!!!!! Check before deploy! Server-side data validation ***/
        if (form.isValidateSave() && serverValidation && !skipValidate){
            String jsonSchema = formService.getJsonSchema(form);
            Helper.ValidationResult result = Helper.validateJson(jsonSchema, entry.getData());
            if (!result.valid()){
                System.out.println("INVALID JSON: "+result.errorMessagesAsString());
                throw new JsonSchemaValidationException(result.errors());
            }
        }

        JsonNode snap = entry.getData();
        if (entry.getId() != null) { // entry is not new
            var entryId = entry.getId();
            Entry entryFromDb = entryRepository.findById(entry.getId()).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));
            snap = entryFromDb.getData();
            entry.setPrevEntry(entryFromDb.getPrevEntry()); // ensure no prevEntry reassign

            if (entryFromDb.getForm()!=null){
                form = entryFromDb.getForm(); // ensure not reassign form if already set
            }
        }else {
            if (prevId != null) {
                // if prevId=null dont do any assignment/re-assignment of prevData.
                // only do prev assignment when entry is new and prevId not null
                Optional<Entry> prevEntryOpt = entryRepository.findById(prevId);
                if (prevEntryOpt.isPresent()) {
                    entry.setPrevEntry(prevEntryOpt.get());
                } else {
                    entry.setPrevEntry(null);
                }
            }
        }

        entry.setForm(form); // set form, either from formId, or existing entry form when update.

        if (form.getX().at("/autoSync").asBoolean(false) && entry.getId() != null){
            // resync only when entry was saved and form is set to autosync
            resyncEntryData_ModelPicker(form.getId(),entry.getData());
        }

//        if (prevId!=null){
//            Entry prev = findById(prevId, form.getPrev());
//            entry.setPrev(prevId);
//        }

        if (entry.getEmail() == null) {
            entry.setEmail(email);
        }

//        Map<Long, String> approver = new HashMap<>();

        ///// ##### Map data and compileTpl ##### /////
        entry = updateApprover(entry, email);

        // TOTALLY NEW ENTRY
        if (entry.getId() == null) {
            entry.setCurrentStatus(Entry.STATUS_DRAFTED);

            form.setCounter(form.getCounter() + 1);
            formRepository.save(form);

            newEntry = true;
        }

        final Entry fEntry = entryRepository.save(entry);

        if (trail) {
            trail(entry.getId(), snap, newEntry ? EntryTrail.CREATED : EntryTrail.SAVED, form.getId(), email, "Saved by " + email,
                    entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        }


        if (newEntry) {
            form.getAddMailer().forEach(m -> triggerMailer(m, fEntry, null, email));
        } else {
            form.getUpdateMailer().forEach(m -> triggerMailer(m, fEntry, null, email));
        }


        try {
            trailApproval(fEntry.getId(), null, null, "saved", "Saved by " + email, getPrincipalEmail());
        } catch (Exception e) {
        }

        return entryRepository.save(fEntry); // 2nd save to save $id, $code, $counter set at @PostPersist
    }


    @Transactional
    public Entry updateApprover(Entry entry, String email) {
        Map<Long, String> approver = entry.getApprover();
        ObjectMapper mapper = new ObjectMapper();
        Entry entryHolder = new Entry();
        BeanUtils.copyProperties(entry, entryHolder, "form", "prevEntry");
        Map entryMap = mapper.convertValue(entryHolder, HashMap.class);
        Map entryDataMap = mapper.convertValue(entry.getData(), HashMap.class);
        Map prevDataMap = mapper.convertValue(entry.getPrev(), HashMap.class);

        entry.getForm().getTiers().forEach(at -> {
            String a = "";
            if ("DYNAMIC".equals(at.getType())) {
                try {
                    a = formService.getOrgMapApprover(at, email, entry);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            } else if ("FIXED".equals(at.getType())) {
                Map<String, Object> dataMap = new HashMap<>();
                // check $user$ load user only if use $user$
                Map userMap;
                if (at.getApprover().contains("$user$")) {
                    Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId());
                    User user = userOpt.orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(entry.getEmail());
                        return newUser;
                    });
                    userMap = mapper.convertValue(user, Map.class);
                    dataMap.put("user", userMap);
                }
                dataMap.put("data", entryDataMap);
                dataMap.put("prev", prevDataMap);
                dataMap.put("_", entryMap);

                // perlu pake $$.("???").name or $$.???.name sebab nya dh convert ke HashMap<String, Object>
                if (entry.getApproval() != null) {
                    dataMap.put("approval_", mapper.convertValue(entry.getApproval(), HashMap.class));
                    Map<Long, JsonNode> apprData = new HashMap<>();
                    entry.getApproval().keySet().forEach(ap -> {
                        apprData.put(ap, entry.getApproval().get(ap).getData());
                    });
                    dataMap.put("approval", mapper.convertValue(apprData, HashMap.class));
                }


                dataMap.put("now", Instant.now().toEpochMilli());
                String compiled = Helper.compileTpl(at.getApprover(), dataMap);
                List<String> emails = Arrays.stream(compiled.split(","))
                        .filter(Objects::nonNull)
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .toList();
                a = String.join(",", emails);
            } else if ("GROUP".equals(at.getType()) && at.getApproverGroup() != null) {
                Long groupId = at.getApproverGroup();
                /*
                Possible parameter: {'$approver$.attributes.departmentCode':'{{$.faculty.code}}'}
                                    {'$tags$~in':'{{$user$.faculty.code}}'}
                * */
                List<String> emails = appUserRepository.findEmailsByGroupId(groupId).stream()
                        .filter(Objects::nonNull)
                        .filter(java.util.function.Predicate.not(String::isBlank))
                        .toList();
                a = String.join(",", emails);
            }
            approver.put(at.getId(), a);
        });

        entry.setApprover(approver);
        return entry;
    }

    @Transactional
    public void triggerMailer(Long mailer, Entry entry, Tier gat, String initBy) {
        if (mailer != null) {
            try {
                EmailTemplate template = emailTemplateRepository.findByIdAndEnabled(mailer, Constant.ENABLED);//.findByCodeAndEnabled(mailer, Constant.ENABLED);
                if (template != null) {
                    Map<String, Object> contentMap = new HashMap<>();
                    ObjectMapper mapper = new ObjectMapper();
                    contentMap.put("_", mapper.convertValue(entry, Map.class));
                    Map<String, Object> result = mapper.convertValue(entry.getData(), Map.class);
                    Map<String, Object> prev = mapper.convertValue(entry.getPrev(), Map.class);


//                    String url = "https://" + entry.getForm().getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
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

                    if (result != null) {

                        contentMap.put("code", result.get("$code"));
                        contentMap.put("id", result.get("$id"));
                        contentMap.put("counter", result.get("$counter"));                    }

                    if (prev != null) {

                        contentMap.put("prev_code", prev.get("$code"));
                        contentMap.put("prev_id", prev.get("$id"));
                        contentMap.put("prev_counter", prev.get("$counter"));
                    }

                    contentMap.put("data", result);

                    contentMap.put("prev", prev);

                    Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId());
                    if (u.isPresent()) {
                        Map userMap = mapper.convertValue(u.get(), Map.class);
                        contentMap.put("user", userMap);
                    }

                    if (gat != null) {
                        contentMap.put("tier", gat);
                    }

                    if (entry.getApproval() != null && gat != null) {
                        EntryApproval approval_ = entry.getApproval().get(gat.getId());
                        if (approval_ != null) {
                            Map<String, Object> approval = mapper.convertValue(approval_.getData(), Map.class);
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
                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).toList();
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
                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                    .getContent().stream()
                                    .filter(appUser -> appUser.getUser() != null)
                                    .map(appUser -> appUser.getUser().getEmail()).toList();
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
//            notificationService.notify(-1, rec, template, contentMap);
                } else {
//                    logger.info("template == null");
                }
            } catch (Exception e) {
                System.out.println("Error trigger mailer: " + e.getMessage());
//                e.printStackTrace();
//            logger.warn("submitEntry:" + e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public Entry findByIdOld(Long id, Long formId, boolean anonymous, HttpServletRequest req) {

//        Form form = formService.findFormById(formId);
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        Entry entry;

        boolean isPublic = form.isPublicEp();
//        System.out.println("fromPrivate:"+anonymous);
        if (anonymous && !isPublic) {
            // access to private dataset from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Form Entry: Access to private form entry from public endpoint is not allowed");
        } else {
            String apiKeyStr = Helper.getApiKey(req);
            if (apiKeyStr != null) {
                ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
                if (apiKey != null && apiKey.getAppId() != null && !form.getApp().getId().equals(apiKey.getAppId())) {
                    throw new OAuth2AuthenticationProcessingException("Invalid API Key: API Key used is not designated for the app of the dataset");
                }
            }

            entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        }

        return entry;
    }

    @Transactional(readOnly = true)
    public Entry findById(Long id, boolean anonymous, HttpServletRequest req) {

//        Form form = formService.findFormById(formId);
//        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        Form form = entry.getForm(); // form get from entry

        boolean isPublic = form.isPublicEp();
//        System.out.println("fromPrivate:"+anonymous);
        if (anonymous && !isPublic) {
            // access to private dataset from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Form Entry: Access to private form entry from public endpoint is not allowed");
        } else {
            String apiKeyStr = Helper.getApiKey(req);
            if (apiKeyStr != null) {
                ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
                if (apiKey != null && apiKey.getAppId() != null && !form.getApp().getId().equals(apiKey.getAppId())) {
                    throw new OAuth2AuthenticationProcessingException("Invalid API Key: API Key used is not designated for the app of the dataset");
                }
            }

//            entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        }

        return entry;
    }

    public boolean checkAccess(List<Long> accessList, String email, Long appId) throws Exception {
        if (accessList!=null && accessList.size()>0) {

            List<Long> userAuthoritiesList = appUserRepository
                    .findIdsByAppIdAndEmailAndStatus(appId, email, "approved");
            accessList.retainAll(userAuthoritiesList);

            if (accessList.size() == 0) {
                throw new Exception("User doesn't have access to the dataset");
            }

            return true;
        }
        return true;
    }


    @Transactional
    public Map<String, Object> blastEmailByDataset(Long datasetId, String searchText, String email, Map filters, String cond, EmailTemplate emailTemplate, List<Long> ids, HttpServletRequest req, String initBy, UserPrincipal userPrincipal) throws Exception {

        Map<String, Object> data = new HashMap<>();
        Dataset d = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        if (!d.isCanBlast()){
            throw new Exception("Unauthorized email blast request");
        }

        checkAccess(d.getAccessList(), userPrincipal.getEmail(), d.getAppId());

        ObjectMapper mapper = new ObjectMapper();
//        Page<Entry> list = findListByDataset(datasetId, searchText, email, filters, PageRequest.of(0, Integer.MAX_VALUE), req);

        AtomicInteger index = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();


        try (Stream<Entry> entryStream = findListByDatasetStream(datasetId, searchText, email, filters, cond, null, ids, req)) {
//            System.out.println("----- dlm tryResource; filter:" + filters);
            entryStream.forEach(entry -> {
                total.getAndIncrement();
                Map<String, Object> contentMap = new HashMap<>();
                contentMap.put("_", entry);
                Map<String, Object> result = mapper.convertValue(entry.getData(), Map.class);
                Map<String, Object> prev = mapper.convertValue(entry.getPrev(), Map.class);

                String url = "https://" + entry.getForm().getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
                contentMap.put("uiUri", url);
                contentMap.put("viewUri", url + "/form/" + entry.getForm().getId() + "/view?entryId=" + entry.getId());
                contentMap.put("editUri", url + "/form/" + entry.getForm().getId() + "/edit?entryId=" + entry.getId());

                List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
                List<String> recipientsCc = new ArrayList<>();


                if (result != null) {
                    contentMap.put("code", result.get("$code"));
                    contentMap.put("id", result.get("$id"));
                    contentMap.put("counter", result.get("$counter"));
                }


                if (prev != null) {

                    contentMap.put("prev_code", prev.get("$code"));
                    contentMap.put("prev_id", prev.get("$id"));
                    contentMap.put("prev_counter", prev.get("$counter"));
                }

                contentMap.put("data", result);
                contentMap.put("prev", prev);

                assert result != null;
                if (d.isCanBlast() && d.getBlastTo() != null) {
                    String blastToStr = result.get(d.getBlastTo()) + "";
                    recipients.addAll(Arrays.stream(blastToStr.replaceAll(" ", "").split(","))
                            .filter(str -> !str.isBlank())
                            .toList());
                }

                try {
                    Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), d.getApp().getId());
                    if (u.isPresent()) {
                        Map userMap = mapper.convertValue(u.get(), Map.class);
                        contentMap.put("user", userMap);
                    }

                    Tier gat = null;
                    if (entry.getForm().getTiers().size() > 0 && entry.getCurrentTier() != null) {
                        gat = entry.getForm().getTiers().get(entry.getCurrentTier());
                        if (gat != null) {
                            contentMap.put("tier", gat);
                        }
                    }

                    if (entry.getApproval() != null && gat != null) {
                        EntryApproval approval_ = entry.getApproval().get(gat.getId());
                        if (approval_ != null) {
                            Map<String, Object> approval = mapper.convertValue(approval_.getData(), Map.class);
                            contentMap.put("approval_", approval_);
                            contentMap.put("approval", approval);
                        }
                    }

                    if (emailTemplate.isToUser()) {
                        recipients.add(entry.getEmail());
                    }
                    if (emailTemplate.isToAdmin()) {
                        if (entry.getForm().getAdmin() != null) {
                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).toList();
                            if (!adminEmails.isEmpty()) {
                                recipients.addAll(adminEmails);
                            }
                        }
                    }
                    if (gat != null && emailTemplate.isToApprover()) {
                        if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
                            recipients.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
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
                        if (entry.getForm().getAdmin() != null) {
                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).toList();
                            if (!adminEmails.isEmpty()) {
                                recipientsCc.addAll(adminEmails);
                            }
                        }

                    }
                    if (gat != null && emailTemplate.isCcApprover()) {
                        if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
                            recipientsCc.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
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
                    System.out.println("ERROR BLAST@@@######::" + e.getMessage());
                }

                String[] rec = recipients.toArray(new String[0]);
                String[] recCc = recipientsCc.toArray(new String[0]);

                if (emailTemplate.isPushable()) {
                    pushService.sendMailPush(d.getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, contentMap, d.getApp());
                }

                mailService.sendMail(d.getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, contentMap, d.getApp(), initBy, entry.getId());
                index.getAndIncrement();
                this.entityManager.detach(entry);
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
//            formId = extendedId;
            Long extendedId = form.getX().get("extended").asLong();
//            form = formService.findFormById(extendedId);
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

//        System.out.println("fromPrivate:"+anonymous);
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

//        Map presetFilters = mapper.convertValue(d.getPresetFilters(), HashMap.class);
//        presetFilters.replaceAll((k, v) -> Helper.compileTpl(v.toString(), dataMap));
//
            final Map newFilter = new HashMap();
            if (filters != null) {
                newFilter.putAll(filters);
            }

            if (filtersReq.size() > 0) {
                newFilter.putAll(filtersReq);
            }
            Page<Entry> entry = entryRepository.findAll(EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .filters(newFilter)
                    .action(false)
                    .build().filter(), PageRequest.of(0, 1));

            return entry.getContent().stream()
                    .findFirst().orElseThrow(() -> new ResourceNotFoundException("Entry", "filters", filters));
        }
    }

    public Page<Entry> findByFormId(Long formId, Pageable pageable) {
        return entryRepository.findByFormId(formId, pageable);
    }

    @Transactional
    public Entry retractApp(Long id, String email) {

        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        trail(entry.getId(), entry.getData(), EntryTrail.RETRACTED, entry.getForm().getId(), email, "Retracted by " + email,
                entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());


        entry.setCurrentStatus(Entry.STATUS_DRAFTED);
        entry.setCurrentTier(null);
        entry.setCurrentTierId(null);
        entry.setFinalTierId(null);
        entry.getApproval().clear();

        entry.getForm().getRetractMailer().forEach(t -> triggerMailer(t, entry, null, email));

        trailApproval(id, null, null, Entry.STATUS_DRAFTED, "RETRACTED by User " + Optional.ofNullable(email).orElse(""), getPrincipalEmail());

        return entryRepository.save(entry);

    }

    /*
     * $update$(id,{'name':'asdaa'},'prev')
     * */
    @Transactional
    public Entry updateField(Long entryId, JsonNode obj, String root, Long appId) throws Exception {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

        String principal = getPrincipalEmail();
        JsonNode snap = entry.getData();
//        ObjectNode data = (ObjectNode) entry.getData(); ///((ObjectNode) nodeParent).put('subfield', "my-new-value-here");

        ObjectMapper mapper = new ObjectMapper();

        if (entry.getForm().getApp().getId().equals(appId) || appId == null) {
            // dari app yg sama atau appId == null
//            Map<String, Object> map1;
            JsonNode node1;
            boolean isPrev = "prev".equals(root);
            if (isPrev) {
                node1 = entry.getPrev();
            } else {
                node1 = entry.getData();
            }
            Map<String, Object> map2 = mapper.convertValue(obj, Map.class);

            if (isPrev) {
            } else {
                entry.setData(deepMerge(node1, obj));
            }

            Long previousEntryId = Optional.ofNullable(entry.getPrevEntry())
                    .map(prev -> prev.getId())
                    .orElse(null);

            save(entry.getForm().getId(), entry, previousEntryId, entry.getEmail(), false);

            trail(entryId, snap, EntryTrail.UPDATED, entry.getForm().getId(), principal, "Field(s) updated: " + map2.keySet() + " by " + principal,
                    entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());

        } else {
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
            trail(entryId, null, EntryTrail.RESTORED, entryTrail.getFormId(), email, "Entry restored by " + email, null, null, null, null);
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

            trail(entryId, entry.getData(), EntryTrail.REVERTED, entry.getForm().getId(), email, "Entry data reverted by " + email,
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
            if (diffcp){
                remark = "Action taken on behalf of " + email + " by " + cp;
            }
            trail(entry.getId(), entry.getData(), EntryTrail.APPROVAL, entry.getForm().getId(), cp, remark, entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        } catch (Exception e) {
        }

        entry = updateApprover(entry, email);
        Tier gat = tierRepository.findById(gaa.getTier().getId()).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", gaa.getTier().getId()));

        Optional<User> user = userRepository.findFirstByEmailAndAppId(email, entry.getForm().getApp().getId());

        user.ifPresent(gaa::setApprover);



        gaa.setEntry(entry);
        gaa.setTier(gat);
        gaa.setTierId(gat.getId());
        gaa.setEmail(email);

        gaa.setTimestamp(new Date());
        entry.setCurrentTierId(gat.getId());

        int currentTier = Math.toIntExact(gat.getSortOrder());

        List<MailerHolder> emMap = new ArrayList<>();

        if (gat.getActions() != null) {
            if (gat.getActions().get(gaa.getStatus()) != null || gat.isAlwaysApprove()) {
                TierAction ta = gat.getActions().get(gaa.getStatus());

                if (ta != null) {
                    if ("nextTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                                ngat.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, ngat));
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
                            }
                        }

                    } else if ("prevTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) - 1;

                    } else if ("goTier".equals(ta.getAction())) {
                        Tier t1 = tierRepository.findById(ta.getNextTier()).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", ta.getNextTier()));
                        if (t1.getSubmitMailer() != null && !ta.isUserEdit()) {
                            // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                            t1.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, t1));
                            t1.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, t1)));
//                        emailTemplateMap.put(t1.getSubmitMailer(), t1);
                        }
//                    if (t1.getResubmitMailer() != null && Entry.STATUS_RESUBMITTED.equals(ta.getNextStatus())){
//                        // if next tier has resubmitmailer, and nextStatus is RESUBMITTED, trigger resubmitted (next tier) email
//                        emailTemplateMap.put(t1.getResubmitMailer(), t1);
//                    }
                        currentTier = Math.toIntExact(t1.getSortOrder());
                    } else if ("curTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder());
                    }

                    entry.setCurrentStatus(ta.getCode());
                    entry.setCurrentEdit(ta.isUserEdit());

                    ta.getMailer().forEach(i -> emMap.add(new MailerHolder(i, gat)));
                } else {
                    if (gat.isAlwaysApprove()) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null) { //&& !ta.isUserEdit()
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
                            }
                        }
                        entry.setCurrentStatus("always_approve");
                        entry.setCurrentEdit(false);
                    }
                }
            }
        }

        if (currentTier == entry.getForm().getTiers().size()) {
            // to mark this is final tier (the status could be approved, rejected, returned, resubmitted)
            entry.setFinalTierId(gat.getId());
        }

        entry.setCurrentTier(currentTier);

        entry.getApproval().put(gat.getId(), gaa);

        entry = entryRepository.save(entry);

        updateApprover(entry, entry.getEmail());

        EntryApprovalTrail eat = new EntryApprovalTrail(gaa);

        trailApproval(eat);

        if (!silent) {
            for (MailerHolder m : emMap) {
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
        ids.forEach(id -> {
            try {
                Entry e = actionApp(id, gas, false, email);
                success.getAndIncrement();
            } catch (Exception e) {
                failed.getAndIncrement();
                failedMessage.add(e.getMessage());
            }
        });
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
            if (diffcp){
                remark = "Action taken on behalf of " + email + " by " + cp;
            }
            trail(entry.getId(), entry.getData(), EntryTrail.APPROVAL, entry.getForm().getId(), email, "Action taken by " + email, entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
        } catch (Exception e) {
        }

//        entry = updateApprover(entry, email); // knak comment???
        Tier gat = tierRepository.findById(gaa.getTier().getId()).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", gaa.getTier().getId()));

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
        entry.setCurrentTierId(gat.getId()); //disetara dgn actionApp
        entryApprovalRepository.save(gaa); // ONLY HERE

        entry.getApproval().put(gat.getId(), gaa); // ONLY HERE
        entry = entryRepository.save(entry); // ONLY HERE

        int currentTier = Math.toIntExact(gat.getSortOrder());

        List<MailerHolder> emMap = new ArrayList<>();

        if (gat.getActions() != null) {
            if (gat.getActions().get(gaa.getStatus()) != null || gat.isAlwaysApprove()) {
                TierAction ta = gat.getActions().get(gaa.getStatus());

                if (ta != null) {
                    if ("nextTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                                ngat.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, ngat));
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
                            }
                        }

                    } else {
                        if (gat.getActions().get(prevStatus) != null && "nextTier".equals(gat.getActions().get(prevStatus).getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder());
                        }

                        if ("prevTier".equals(ta.getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder()) - 1;

                        } else if ("goTier".equals(ta.getAction())) {
                            Tier t1 = tierRepository.findById(ta.getNextTier()).orElseThrow(() -> new ResourceNotFoundException("Tier", "id", ta.getNextTier()));
                            if (t1.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                                t1.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, t1));
                                t1.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, t1)));
//                            emailTemplateMap.put(t1.getSubmitMailer(), t1);
                            }
                            currentTier = Math.toIntExact(t1.getSortOrder());
                        } else if ("curTier".equals(ta.getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder());
                        }
                    }

                    entry.setCurrentStatus(ta.getCode());
                    entry.setCurrentEdit(ta.isUserEdit());

                    ta.getMailer().forEach(i -> emMap.add(new MailerHolder(i, gat)));
                } else {
                    if (gat.isAlwaysApprove()) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null) {// && !ta.isUserEdit()
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
                            }
                        }
                        entry.setCurrentStatus("always_approve");
                        entry.setCurrentEdit(false);
                    }
                }
            }
        }

        if (currentTier == entry.getForm().getTiers().size()) {
            // to mark this is final tier (the status could be approved, rejected, returned, resubmitted)
            entry.setFinalTierId(gat.getId());
        }

        entry.setCurrentTier(currentTier);

        entry = entryRepository.save(entry);

        updateApprover(entry, entry.getEmail());

        EntryApprovalTrail eat = new EntryApprovalTrail(gaa);
        eat.setRemark("Approval updated "); // ONLY HERE

        trailApproval(eat);

        if (entry.getForm().getUpdateApprovalMailer() != null) { // ONLY HERE
            emMap.add(new MailerHolder(entry.getForm().getUpdateApprovalMailer(), gat));
        }

        for (MailerHolder m : emMap) {
            triggerMailer(m.mailerId, entry, m.tier, email);
        }
        return entry;
    }

    @Transactional
    public void deleteEntry(Long id, String email) {
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));
        JsonNode snap = entry.getData();
        bucketService.deleteFileByEntryId(id);

        try {
            trail(id, snap, EntryTrail.REMOVED, entry.getForm().getId(), email, "Entry removed by " + email, entry.getCurrentTier(), entry.getCurrentTierId(), entry.getCurrentStatus(), entry.isCurrentEdit());
//            EntryTrail eat = new EntryApprovalTrail(null, null, "Entry REMOVED", "Entry removed by "+ email, new Date(), email, id);
//            entryApprovalTrailRepository.save(eat);
        } catch (Exception e) {
        }

        entryRepository.deleteById(id);
    }

    @Transactional
    public void deleteEntries(List<Long> ids, String email) {
        ids.forEach(id -> deleteEntry(id, email));
    }

    public Entry reset(Long id) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow();

        entry.setCurrentTier(0);

        entry.setCurrentStatus(Entry.STATUS_SUBMITTED);

        return entryRepository.save(entry);
    }

    public Entry removeApproval(Long tierId, Long entryId, String email) {
        Entry entry = entryRepository.findById(entryId).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", entryId));

//        EntryApproval ea = null;
        if (entry.getApproval() != null && entry.getApproval().get(tierId) != null) {
//            ea = entry.getApproval().get(tierId);

            if (!Optional.ofNullable(entry.getApprover().get(tierId)).orElse("")
                    .contains(email)){
                throw new RuntimeException("User ["+email+"] not allowed to remove approval data.");
            }

            try {
                trailApproval(tierId, null, null, EntryApprovalTrail.DELETE, "Approval removed by " + entry.getEmail(), getPrincipalEmail());
            } catch (Exception e) {
            }

            entry.getApproval().remove(tierId);
            entryApprovalRepository.deleteById(tierId);
        }

        return entryRepository.save(entry);
    }

    @Transactional
    public Entry submit(Long id, String email) {
        Date dateNow = new Date();
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));

        /* NEW!!! Perlu check n test bena2 sebelum deploy! Data validation on server-side */

        Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation");

        boolean serverValidation = false;
        if (validateOpt.isPresent()){
            serverValidation = "true".equals(validateOpt.get());
        }

        boolean skipValidate = entry.getForm().getX()!=null
                && entry.getForm().getX().at("/skipValidate").asBoolean(false);

        if (serverValidation && !skipValidate) {
            String jsonSchema = formService.getJsonSchema(entry.getForm());
            Helper.ValidationResult result = Helper.validateJson(jsonSchema, entry.getData());
            if (!result.valid()) {
                System.out.println("INVALID JSON: "+result.errorMessagesAsString());
                throw new JsonSchemaValidationException(result.errors());
            }
        }

        entry.setSubmissionDate(dateNow);
        entry.setResubmissionDate(dateNow); // Why set resubmission here?

        entry.setCurrentTier(0);

        List<Long> mailer = null;

        Tier gat = null;

        if (!entry.getForm().getTiers().isEmpty()) {
            gat = entry.getForm().getTiers().get(0);
            mailer = gat.getSubmitMailer();
            entry = updateApprover(entry, entry.getEmail());
        }
        entry.setCurrentStatus(Entry.STATUS_SUBMITTED);
        entry.setCurrentEdit(false);

        entry = entryRepository.save(entry);

        trailApproval(id, null, null, Entry.STATUS_SUBMITTED, "SUBMITTED by User " + entry.getEmail(), getPrincipalEmail());

        if (mailer != null) {
            for (Long i : mailer) {
                triggerMailer(i, entry, gat, email);
            }
        }
        return entry;
    }

    @Transactional
    public Entry resubmit(Long id, String email) {
        Date now = new Date();
        Entry entry = entryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Entry", "id", id));


        Optional<String> validateOpt = keyValueRepository.getValue("platform", "server-entry-validation");

        boolean serverValidation = false;
        if (validateOpt.isPresent()){
            serverValidation = "true".equals(validateOpt.get());
        }

        boolean skipValidate = entry.getForm().getX()!=null
                && entry.getForm().getX().at("/skipValidate").asBoolean(false);

        if (serverValidation && !skipValidate) {
            String jsonSchema = formService.getJsonSchema(entry.getForm());
            Helper.ValidationResult result = Helper.validateJson(jsonSchema, entry.getData());
            if (!result.valid()) {
                System.out.println("INVALID JSON: "+result.errorMessagesAsString());
                throw new JsonSchemaValidationException(result.errors());
            }
        }

        List<Tier> tiers = entry.getForm().getTiers();

        // No tiers: handle with submit
        if (tiers.isEmpty()) {
//            throw new RuntimeException("Form ["+entry.getForm().getId()+"] has no tier specified");
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
        Tier tier = tiers.get(currentTier);
        entry.setResubmissionDate(now);
        entry.setCurrentStatus(Entry.STATUS_RESUBMITTED);
        entry.setCurrentEdit(false);
        entry = updateApprover(entry, entry.getEmail());
        entry = entryRepository.save(entry);

        trailApproval(id,null,tier,Entry.STATUS_RESUBMITTED, "RESUBMITTED by User " + entry.getEmail(),getPrincipalEmail());

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
        group.forEach(g -> g.getItems().forEach(i -> {
            if ("dataset".equals(i.getType())) {
                dsInNavi.add(i.getScreenId());
            }
            if ("screen".equals(i.getType())) {
                sInNavi.add(i.getScreenId());
            }
        }));

        List<Dataset> dsList = datasetRepository.findByIds(dsInNavi);
        List<Screen> sList = screenRepository.findByIds(sInNavi);


        dsList.forEach(ds -> data.put(ds.getId() + "", countEntry(ds, email)));

        sList.forEach(s -> {
            if ("list".equals(s.getType()) && s.getDataset() != null) {
                data.put("screen_" + s.getId(), countEntry(s.getDataset(), email));
            }
        });

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
//        Form form = formService.findFormById(formId);
//        // if form is extended form, then use original form
//        Long extendedId = form.getX().get("extended").asLong();
//        if (extendedId != null) {
//            formId = extendedId;
//            form = formService.findFormById(formId);
//        }


        long start = System.currentTimeMillis();

//        Pageable pageRequest = PageRequest.of(0, 200);
//        Page<Entry> onePage = entryRepository.findByFormId(formId, pageRequest);
        try (Stream<Entry> entryStream = entryRepository.findByFormId(formId)) {
            entryStream.forEach(e -> {
                entryRepository.saveAndFlush(updateApprover(e, e.getEmail()));
                this.entityManager.detach(e);
            });
        }

        long finish = System.currentTimeMillis();
        System.out.println("finish in:" + (finish - start));
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

        try (Stream<Entry> entryStream = entryRepository.findByFormId(formId)) {
            entryStream.forEach(e -> {

                entryTotal.getAndIncrement();

                try {
                    e.getForm().getTiers().forEach(at -> {
                        if (Objects.equals(at.getId(), tierId)) {
                            if (e.getApproval().get(tierId) == null || updateApproved) {

                                entryRepository.saveAndFlush(updateApprover(e, e.getEmail()));
                                successTotal.getAndIncrement();

                                if (e.getApproval().get(tierId) != null) {
                                    notEmptyTotal.getAndIncrement();
//                                    notEmpty.add(e.getId() + ": Entry approval has been performed (forced approver update)");
                                }
                            } else {
                                notEmptyTotal.getAndIncrement();
//                                errors.add(e.getId() + ": No approval yet");
//                                notEmpty.add(e.getId() + ": Entry approval has been performed (ignored)");
                            }
                        }
                    });

                } catch (Exception ex) {
                    System.out.println("BULK UPDATE APPROVER:" + ex.getMessage() + ":" + e.getId());
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
//        data.put("notEmptyLog", notEmpty);
        data.put("success", true);

        return CompletableFuture.completedFuture(data);
    }

    @Async("asyncExec")
    @Transactional(readOnly = true)
    public CompletableFuture<Map<String, Object>> execVal(Long formId, String field, boolean force) {
        Map<String, Object> data = new HashMap<>();

        Form loadform = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        // perlu baca awal script nya, in case field nya dalam extended form
        String script = loadform.getItems().get(field).getF();

        // if form is extended form, then use original form
        if (loadform.getX().get("extended") != null) {
//            formId = extendedId;
            Long extendedId = loadform.getX().get("extended").asLong();
//            loadform = formService.findFormById(extendedId);
            loadform = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
        }

        final Form form = loadform;

        ObjectMapper mapper = new ObjectMapper();

        final List<String> errors = new ArrayList<>();
        final List<String> success = new ArrayList<>();
        final List<String> notEmpty = new ArrayList<>();

        final AtomicInteger total = new AtomicInteger();

        try {

            HostAccess access = HostAccess.newBuilder(HostAccess.ALL).targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class))).build();
            ScriptEngine engine = GraalJSScriptEngine.create(Engine.newBuilder()
                            .option("engine.WarnInterpreterOnly", "false")
                            .build(),
                    Context.newBuilder("js")
                            .allowHostAccess(access)
            );

            try {
                Resource resource = new ClassPathResource("dayjs.min.js");
                FileReader fr = new FileReader(resource.getFile());
                engine.eval(fr);
            }catch (IOException e) {
                System.out.println("WARNING: Error loading dayjs.min.js with errors: " + e.getMessage());
            }

            CompiledScript compiled = ((Compilable) engine).compile("function fef($_,$user$){ var $ = JSON.parse(dataModel); var $prev$ = JSON.parse(prevModel); var $_ = JSON.parse(entryModel); return " + script + "}");

            long start = System.currentTimeMillis();

            Map<String, Map> userMap = new HashMap<>();

            try (Stream<Entry> entryStream = entryRepository.findByFormId(form.getId())) {
                entryStream.forEach(e -> {
                    total.incrementAndGet();
                    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

                    JsonNode node = e.getData();
                    if (force || node.get(field) == null || node.get(field).isNull()) {

                        Map user = null;
                        boolean userOk = false;
                        if (script.contains("$user$")) {
                            if (e.getEmail() != null) { //even not null user still not found how?
                                if (userMap.get(e.getEmail()) != null) {
                                    user = userMap.get(e.getEmail());
                                    userOk = true;
                                } else {
                                    Optional<User> u = userRepository.findFirstByEmailAndAppId(e.getEmail(), form.getApp().getId());
                                    if (u.isPresent()) {
                                        user = mapper.convertValue(u.get(), Map.class);
                                        userOk = true; // $user$ in script and user exist
                                    } else {
                                        userOk = false; // $user$ in script but user not exist
                                        errors.add("Entry " + e.getId() + ": Contain $user$ but user not exist");
                                    }
                                }
                            } else {
                                userOk = false;
                                errors.add("Entry " + e.getId() + ": Contain $user$ but entry has no email");
                            }
                        } else {
                            userOk = true; // if $user$ not in script, just proceed
                        }

                        if (userOk) {
                            Object val;
                            try {

                                bindings.putAll(Map.of(
                                        "dataModel", mapper.writeValueAsString(e.getData()),
                                        "prevModel", mapper.writeValueAsString(e.getPrev()),
                                        "entryModel", mapper.writeValueAsString(e)));

                                compiled.eval(bindings);

                                Invocable inv = (Invocable) compiled.getEngine();

                                val = inv.invokeFunction("fef", e, user);

                                ObjectNode o = (ObjectNode) node;
                                o.set(field, mapper.valueToTree(val));
//                                    e.setData(o);
                                // Mn update pake jpql pake json_set cuma scalar value xpat object
//                                    entryRepository.updateField(e.getId(),"$."+field, mapper.writeValueAsString(val));
                                entryRepository.updateDataField(e.getId(), o.toString());

                                this.entityManager.detach(e);

                                success.add(e.getId() + ": Success");
                            } catch (Exception ex) {
                                errors.add(e.getId() + ":" + ex.getMessage());
                            }
                        } else {
                            errors.add(e.getId() + ": Contain $user$ but user not exist");
                        }
                    } else {
                        notEmpty.add(e.getId() + ": Field not empty");
                    }
                });
            }

            long finish = System.currentTimeMillis();
            System.out.println("completed in (stream + update):" + (finish - start));
        } catch (Exception e) {
            e.printStackTrace();
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
    public Page<Entry> findListByDatasetCheck(Long datasetId, String searchText, String email, Map<String,
            Object> filters, String cond, List<String> sorts, List<Long> ids, boolean anonymous,
                                              Pageable pageable, HttpServletRequest req) {
        Dataset d = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new RuntimeException("Dataset does not exist, ID=" + datasetId));


        boolean isPublic = d.isPublicEp();

        if (anonymous && !isPublic) {
            throw new OAuth2AuthenticationProcessingException("Private Dataset: Access to private dataset from public endpoint is not allowed");
        }

        String apiKeyStr = Helper.getApiKey(req);
        if (apiKeyStr != null) {
            ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
            if (apiKey != null && apiKey.getAppId() != null && !d.getApp().getId().equals(apiKey.getAppId())) {
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
            throw new OAuth2AuthenticationProcessingException("Private Dataset: Access to private dataset from public endpoint is not allowed");
        } else {

            String apiKeyStr = Helper.getApiKey(req);
            if (apiKeyStr != null) {
                ApiKey apiKey = apiKeyRepository.findFirstByApiKey(apiKeyStr);
                if (apiKey != null && apiKey.getAppId() != null && !d.getApp().getId().equals(apiKey.getAppId())) {
                    throw new OAuth2AuthenticationProcessingException("Invalid API Key: API Key used is not designated for the app of the dataset");
                }
            }

            return findListByDatasetStream(datasetId, searchText, email, filters, cond, sorts, ids, req);
        }
    }

    public Specification<Entry> buildSpecification(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, HttpServletRequest req) {

        if (searchText != null && searchText.isEmpty()) {
            searchText = null;
        }

        if (email != null) {
            email = email.trim();
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        Dataset d = datasetRepository.findById(datasetId).orElseThrow(() -> new ResourceNotFoundException("Dataset", "id", datasetId));

        Map<String, Object> dataMap = new HashMap<>();

        userRepository.findFirstByEmailAndAppId(email, d.getApp().getId())
                .ifPresent(user -> {
                    Map<String, Object> userMap = mapper.convertValue(user, Map.class);
                    dataMap.put("user", userMap);
                });

        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());
        dataMap.put("conf", Map.of());

        Map filtersReq = new HashMap();
        if (req != null) {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getKey().contains("$") && !isNullOrEmpty(req.getParameter(entry.getKey())) ) {
                    filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }
        }

        Map<String, Object> pF = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        if (pF.containsKey("@cond")) {
            cond = pF.get("@cond") + "";
            pF.remove("@cond");
        }

        Map presetFilters = pF.entrySet().stream()
                .filter(x -> x.getKey().startsWith("$"))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue() + ""));

        presetFilters.replaceAll((k, v) -> Helper.compileTpl(v.toString(), dataMap));

        System.out.println(presetFilters);

        final Map newFilter = new HashMap();

        if (filters != null) {
            newFilter.putAll(filters);
        }
        if (d.getPresetFilters() != null) {
            newFilter.putAll(presetFilters);
        }
        if (filtersReq.size() > 0) {
            newFilter.putAll(filtersReq);
        }

        System.out.println("--- ### newFilter:" + newFilter);

        Map statusFilter = mapper.convertValue(d.getStatusFilter(), HashMap.class);

        List<String> sortFin = new ArrayList<>();

        Optional.ofNullable(sorts).ifPresent(sortFin::addAll);

        if (d.getDefSortField() != null) {
            sortFin.add(d.getDefSortField() + "~" + (d.getDefSortDir() != null ? d.getDefSortDir() : "asc"));
        }

        JsonNode qFilter = null;
        try {
            qFilter = mapper.readTree(d.getX().at("/qFilter").asText());
        } catch (Exception e) {
//            e.printStackTrace();
        }

        Form form = d.getForm();

        // if form is extended form, then use original form
        if (form.getX().get("extended") != null) {
//            formId = extendedId;
            Long extendedId = form.getX().get("extended").asLong();
//            form = formService.findFormById(extendedId);
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", extendedId));
        }



        return switch (d.getType()) {
            case "all" -> EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
                    .action(false)
                    .qBuilder(qFilter)
                    .dataMap(dataMap)
                    .filters(newFilter)
                    .cond(cond)
                    .build().filter(); // entryRepository.findAll(formId, searchText, status, pageable);
            case "admin" -> EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .admin(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
                    .action(false)
                    .qBuilder(qFilter)
                    .dataMap(dataMap)
                    .filters(newFilter)
                    .cond(cond)
                    .build().filter(); // entryRepository.findAdminByEmail(formId, searchText, email, status, pageable);
            case "user" -> EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .email(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
                    .action(false)
                    .qBuilder(qFilter)
                    .dataMap(dataMap)
                    .filters(newFilter)
                    .cond(cond)
                    .build().filter(); //findUserByEmail(formId, searchText, email, status, pageable);
            case "action" -> EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .approver(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
                    .qBuilder(qFilter)
                    .dataMap(dataMap)
                    .filters(newFilter)
                    .cond(cond)
                    .action(true)
                    .build().filter(); //findUserByEmail(formId, searchText, email, status, pageable);
            default -> null;
        };
    }

    @Transactional(readOnly = true)
    public Page<Entry> findListByDataset(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, Pageable pageable, HttpServletRequest req) {
        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(()->new ResourceNotFoundException("Dataset","Id",datasetId));

        Page<Entry> page = entryRepository.findAll(buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req), pageable);

        Set<String> textToExtract = new HashSet<>(Set.of("$.$id","$.$code","$.$counter","$prev$.$id","$prev$.$code","$prev$.$counter"));

        Form form = dataset.getForm();
        Form prevForm = form.getPrev();

        Optional<String> fieldMaskOpt = keyValueRepository.getValue("platform", "dataset-field-mask");

        boolean fieldMask = false;
        if (fieldMaskOpt.isPresent()){
            fieldMask = "true".equals(fieldMaskOpt.get());
        }

        boolean skipMask = dataset.getX()!=null
                && dataset.getX().at("/skipMask").asBoolean(false);

        // if ada items, then perform filter
        if (dataset.getItems()!=null && dataset.getItems().size()>0 && fieldMask && !skipMask) {
            textToExtract.add("$.$id,$.$code,$.$counter");
            dataset.getItems().stream().forEach(i -> {
                textToExtract.add(i.getPrefix() + "." + i.getCode());
                Optional.ofNullable(i.getPre()).ifPresent(textToExtract::add);

                if ("$".equals(i.getPrefix())) {
                    Item item = form.getItems().get(i.getCode());
                    if (item != null) {
                        Optional.ofNullable(item.getPlaceholder()).ifPresent(textToExtract::add);
                        Optional.ofNullable(item.getF()).ifPresent(textToExtract::add);
                    }
                } else if ("$prev$".equals(i.getPrefix()) && prevForm != null) {
                    Item item = prevForm.getItems().get(i.getCode());
                    if (item != null) {
                        Optional.ofNullable(item.getPlaceholder()).ifPresent(textToExtract::add);
                        Optional.ofNullable(item.getF()).ifPresent(textToExtract::add);
                    }
                }
            });


            dataset.getActions().forEach(a -> Helper.addIfNonNull(textToExtract,
                    a.getPre(), a.getF(), a.getParams()));

            Map<String, Set<String>> fieldsMap = extractVariables(Set.of("$", "$prev$", "$_"), String.join(",", textToExtract));

            List<Entry> filteredContent = page.getContent().stream()
                    .map(entry -> {
                        entityManager.detach(entry); // explicitly detach the entity

                        Entry copy = new Entry();
                        BeanUtils.copyProperties(entry, copy, "data", "prev");
                        // Filter JsonNode
                        JsonNode filtered = filterJsonNode(entry.getData(), fieldsMap.get("$"));
                        copy.setData(filtered);

                        if (entry.getPrevEntry() != null) {
                            entityManager.detach(entry.getPrevEntry()); // explicitly detach the entity

                            Entry copyPrev = new Entry();
                            BeanUtils.copyProperties(entry.getPrevEntry(), copyPrev, "data");

                            JsonNode filteredPrev = filterJsonNode(entry.getPrevEntry().getData(), fieldsMap.get("$prev$"));
                            copyPrev.setData(filteredPrev);
                            copy.setPrevEntry(copyPrev);
                        }
                        return copy;
                    })
                    .toList();

            return new PageImpl<>(
                    filteredContent,
                    page.getPageable(),
                    page.getTotalElements()
            );
        }else{
            System.out.println("mask not enabled");
            // if not, the keluarkan semua
            return page;
        }
    }


//    @Transactional(readOnly = true)
//    public Page<EntryDTO> findListByDataset(Long datasetId, String searchText, String email, Map filters, String cond, List<String> sorts, List<Long> ids, Pageable pageable, HttpServletRequest req) {
//        Dataset dataset = datasetRepository.findById(datasetId).orElseThrow(()->new ResourceNotFoundException("Dataset","Id",datasetId));
//
//        Page<Entry> page = entryRepository.findAll(buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req), pageable);
//
//        Set<String> textToExtract = new HashSet<>(Set.of("$.$id","$.$code","$.$counter","$prev$.$id","$prev$.$code","$prev$.$counter"));
//
//        Form form = dataset.getForm();
//        Form prevForm = form.getPrev();
//
//        Optional<String> fieldMaskOpt = keyValueRepository.getValue("platform", "dataset-field-mask");
//
//        boolean fieldMask = false;
//        if (fieldMaskOpt.isPresent()){
//            fieldMask = "true".equals(fieldMaskOpt.get());
//        }
//
//        boolean skipMask = dataset.getX()!=null
//                && dataset.getX().at("/skipMask").asBoolean(false);
//
//        // if ada items, then perform filter
//        if (dataset.getItems()!=null && dataset.getItems().size()>0 && fieldMask && !skipMask) {
//            dataset.getItems().stream().forEach(i -> {
//                textToExtract.add(i.getPrefix() + "." + i.getCode());
//                Optional.ofNullable(i.getPre()).ifPresent(textToExtract::add);
//
//                if ("$".equals(i.getPrefix())) {
//                    Item item = form.getItems().get(i.getCode());
//                    if (item != null) {
//                        Optional.ofNullable(item.getPlaceholder()).ifPresent(textToExtract::add);
//                        Optional.ofNullable(item.getF()).ifPresent(textToExtract::add);
//                    }
//                } else if ("$prev$".equals(i.getPrefix()) && prevForm != null) {
//                    Item item = prevForm.getItems().get(i.getCode());
//                    if (item != null) {
//                        Optional.ofNullable(item.getPlaceholder()).ifPresent(textToExtract::add);
//                        Optional.ofNullable(item.getF()).ifPresent(textToExtract::add);
//                    }
//                }
//            });
//
//
//            dataset.getActions().forEach(a -> Helper.addIfNonNull(textToExtract,
//                    a.getPre(), a.getF(), a.getParams()));
//
//            Map<String, Set<String>> fieldsMap = extractVariables(Set.of("$", "$prev$", "$_"), String.join(",", textToExtract));
//
//            List<EntryDTO> filteredContent = page.getContent().stream()
//                    .map(entry -> {
//                        Entry copy = new Entry();
//                        BeanUtils.copyProperties(entry, copy, "data", "prev");
//                        // Filter JsonNode
//                        JsonNode filteredData = filterJsonNode(entry.getData(), fieldsMap.get("$"));
//                        copy.setData(filteredData);
//                        JsonNode filteredPrev = null;
//
//                        if (entry.getPrevEntry() != null) {
//                            Entry copyPrev = new Entry();
//                            BeanUtils.copyProperties(entry.getPrevEntry(), copyPrev, "data");
//                            filteredPrev = filterJsonNode(entry.getPrevEntry().getData(), fieldsMap.get("$prev$"));
//                            copyPrev.setData(filteredPrev);
//                            copy.setPrevEntry(copyPrev);
//                        }
//
//                        return new EntryDTO(entry, filteredData, filteredPrev);
//
////                        return copy;
//                    })
//                    .toList();
//
//            return new PageImpl<>(
//                    filteredContent,
//                    page.getPageable(),
//                    page.getTotalElements()
//            );
//        }else{
//            System.out.println("mask not enabled");
//            // if not, the keluarkan semua
//            return page.map(entry -> new EntryDTO(entry, entry.getData(), entry.getPrev()));
//        }
//    }

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
        dashboard.getCharts().stream()
                .forEach(c -> data.put(c.getId(), getChartDataNative(c.getId(), filters, email, req)));

        return data;
    }
    @Transactional(readOnly = true)
    public Map getDashboardMapDataNativeNew(Long dashboardId, Map<String, Object> filters, String email, HttpServletRequest req) {

        Dashboard dashboard = dashboardService.getDashboard(dashboardId);
        Map<Object, Object> data = new HashMap<>();
        dashboard.getCharts()
                .forEach(c -> data.put(c.getId(), getChartMapDataNative(c.getId(), filters, email, req)));

        return data;
    }

    @Transactional(readOnly = true)
    public List<Object[]> _queryChartizeDbData(String __agg,
                                               String __codeField,
                                               String __valueField,
                                               boolean __isSeries,
                                               String __seriesField,
                                               boolean __showAgg,
                                               Form __form, User __user,
                                               JsonNode __status,
                                               Map<String, Object> __filters) {

        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> tplDataMap = new HashMap<>();
        if (__user != null) {
            Map userMap = mapper.convertValue(__user, Map.class);
            tplDataMap.put("user", userMap);
        }

        tplDataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        tplDataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        tplDataMap.put("todayEnd", calendarEnd.getTimeInMillis());

        Map<String, String> jsonRootMap = Map.of("$_", "e",
                "$", "e.data",
                "$prev$", "e2.data",
                "$$", "eac.data",
                "$$_", "eac",
                "data", "e.data", // utk kegunaan fieldCode, fieldValue, fieldSeries dgn format data#field
                "prev", "e2.data",
                "approval", "eac.data");


        Map<String, String> statusFilter = mapper.convertValue(__status, HashMap.class);

        List<String> cond = new ArrayList<>();

        String statusCond = "";
        if (!Helper.isNullOrEmpty(statusFilter)) {
            statusFilter.keySet().forEach(s -> {
                if (statusFilter.get(s) != null && !statusFilter.get(s).isEmpty()) {
                    if ("-1".equals(s)) {
                        cond.add("(e.current_tier_id is null and e.current_status in ('" + statusFilter.get(s).replace(",", "','") + "'))");
                    } else {
                        cond.add("(e.current_tier_id = " + s + " and e.current_status in ('" + statusFilter.get(s).replace(",", "','") + "'))");
                    }
                }
            });
            statusCond = " AND (" + String.join(" or ", cond) + ")";
        }

        List<String> pred = new ArrayList<>();
        String filterCond = "";
//
        if (!Helper.isNullOrEmpty(__filters)) {
            __filters.replaceAll((k, v) -> Helper.compileTpl(v.toString(), tplDataMap));
        }

        if (!Helper.isNullOrEmpty(__filters)) {
            __filters.keySet().forEach(f -> {
                if (__filters.get(f) != null) {
                    String[] splitted1 = f.split("\\.");

                    String rootCol = splitted1[0]; // $, $prev$, $_, $$, $$_


                    // $ = data, $prev$ = prev, $$ = approval
                    // $$.484.college

                    String predRoot = jsonRootMap.get(rootCol);

                    Long tierId;
                    String fieldFull = ""; //$$.123.lookup.code -> lookup.code
                    String fieldCode = ""; //$$.123.lookup.code -> lookup
                    // what if list section?
                    // $.address*.country.code
                    // $.address*.date~from
                    Form form = null;
                    if ("$$".equals(rootCol)) {
                        String[] splitted = f.split("\\.", 3);
                        tierId = Long.parseLong(splitted[1]);
                        fieldFull = splitted[2];
                        fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
                        form = __form;
                    } else if ("$".equals(rootCol) || "$prev$".equals(rootCol)) {
                        String[] splitted = f.split("\\.", 2);
                        fieldFull = splitted[1];
                        fieldCode = (fieldFull.split("\\.")[0]).split("~")[0];
//                            predRoot = "$".equals(rootCol) ? "data": mapJoinPrev.get("data"); // new HibernateInlineExpression(cb, realRoot);
                        form = "$".equals(rootCol) ? __form : __form != null ? __form.getPrev() : null;
                    }


                    if (Arrays.asList("$$", "$", "$prev$").contains(rootCol)) {

                        if (form != null && form.getItems() != null && form.getItems().get(fieldCode) != null && !fieldFull.contains("*")) {
                            if (Arrays.asList("select", "radio").contains(form.getItems().get(fieldCode).getType())) {
                                pred.add(" (upper(json_value(" + predRoot + ",'$." + fieldFull + "')) like upper('" + __filters.get(f) + "')) ");
                            } else if (Arrays.asList("date", "number", "scale", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
                                // if $$.484.college~from
                                if (__filters.get(f) != null && !__filters.get(f).toString().isEmpty()) {
                                    if (fieldFull.contains("~")) {
                                        String[] splitField = fieldFull.split("~");
                                        if ("from".equals(splitField[1])) {
                                            pred.add(" (json_value(" + predRoot + ",'$." + splitField[0] + "') >= " + __filters.get(f) + ") ");
                                        }
                                        if ("to".equals(splitField[1])) {
                                            pred.add(" (json_value(" + predRoot + ",'$." + splitField[0] + "') <= " + __filters.get(f) + ") ");
                                        }
                                    } else {
                                        pred.add(" (json_value(" + predRoot + ",'$." + fieldFull + "') = " + __filters.get(f) + ") ");
                                    }
                                }

                            } else if (Objects.equals("checkbox", form.getItems().get(fieldCode).getType())) {
                                if (Boolean.parseBoolean(__filters.get(f) + "")) {
                                    pred.add(" (json_value(" + predRoot + ",'$." + fieldFull + "') is true) ");
                                } else {
                                    pred.add(" (json_value(" + predRoot + ",'$." + fieldFull + "') is false or json_value(" + rootCol + ",'$." + fieldFull + "') is null) ");
                                }
                            } else if (Objects.equals("text", form.getItems().get(fieldCode).getType())) {
                                String searchText = "%" + __filters.get(f) + "%";
                                // if short text, dont add wildcard to the condition. Problem with email, example ymyati@unimas vs yati@unimas
                                if ("input".equals(form.getItems().get(fieldCode).getSubType())) {
                                    searchText = __filters.get(f) + "";
                                }
                                pred.add(" (upper(json_value(" + predRoot + ",'$." + fieldFull + "')) like upper('" + __filters.get(f) + "')) ");

                            } else {
                                pred.add(" (upper(json_value(" + predRoot + ",'$." + fieldFull + "')) like upper('" + __filters.get(f) + "'))");
                            }
                        } else if (fieldFull.contains("*")) {
                            // after checkboxOption so checkboxOPtion can be executed first, then, check for section
                            // ideally check if fieldcode contain * or not, if not: the above, else: here

                            // CHECK EITHER CHECKBOXOPTION OR SECTION
                            // THEN GET FIELD_CODE, ETC

                            String fieldTranslated = fieldFull.replace("*", "[*]");

                            pred.add(" json_search(lower(" + predRoot + "),'one',lower('%" + __filters.get(f) + "%'),null,'$." + fieldTranslated + "') is not null ");

                        } else {
                            /// IF NOT a part of form
                            pred.add(" (upper(json_value(" + predRoot + ",'$." + fieldFull + "')) like upper('" + __filters.get(f) + "'))");

                        }
                    } else if ("$$_".equals(rootCol)) {
                        String[] splitted = f.split("\\.", 3);
                        tierId = Long.parseLong(splitted[1]);
                        fieldFull = splitted[2];
                        fieldCode = fieldFull.split("\\.")[0];
//                            MapJoin<Entry, Long, EntryApproval> mapJoin = root.joinMap("approval", JoinType.LEFT);
                        predRoot = jsonRootMap.get("$$_");
//                            predicates.add(cb.equal(mapJoin.key(), tierId));

                        if ("timestamp".equals(fieldCode)) {
                            if (__filters.get(f) != null && !__filters.get(f).toString().isEmpty()) {
                                if (fieldFull.contains("~")) {
                                    String[] splitField = fieldFull.split("~");
                                    if ("from".equals(splitField[1])) {
                                        pred.add(" (" + predRoot + ".timestamp >= " + __filters.get(f) + ") ");
                                    }
                                    if ("to".equals(splitField[1])) {
                                        pred.add(" (" + predRoot + ".timestamp <= " + __filters.get(f) + ") ");
                                    }
                                } else {
                                    pred.add(" (" + predRoot + ".timestamp = " + __filters.get(f) + ") ");
                                }
                            }
                        } else if ("status".equals(fieldCode)) {
                            pred.add(" (" + predRoot + ".status = " + (__filters.get(f) + "").trim() + ") ");
                        } else if ("remark".equals(fieldCode)) {
                            pred.add(" ( upper(" + predRoot + ".remark) like upper('%" + __filters.get(f) + "%') ) ");
                        }
                    } else if ("$_".equals(rootCol)) {
                        fieldCode = f.split("\\.")[1];
                        predRoot = jsonRootMap.get("$_");
                        if ("email".equals(fieldCode)) {
                            pred.add(" ( upper(" + predRoot + ".email) = upper(" + (__filters.get(f) + "").trim() + ") ) ");
                        } else if ("currentTier".equals(fieldCode)) {
                            pred.add(" (" + predRoot + ".current_tier = " + __filters.get(f) + ") ");
                        } else if ("currentStatus".equals(fieldCode)) {
                            pred.add(" ( upper(" + predRoot + ".current_status) = upper(" + __filters.get(f) + ") ) ");
                        }
                    }
                }

            });
            if (pred.size() > 0) {
                filterCond = " AND (" + String.join(" and ", pred) + ")";
            }
        }

        // fieldcode valuecode using format data#code
        String[] code = __codeField.split("[#.]", 2);

        String jsonRoot = jsonRootMap.get(code[0]);//
        String field = code[1];

        String codeSql = "coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a')";
        String codeApprovalJoin = "";
        String codeApprovalTierId = "";
        if ("approval".equals(code[0])) { //###!!! Something wrong HERE!!!!!
            String[] codeSplitted = code[1].split("\\.", 2);
            codeApprovalJoin = " left join entry_approval eac on e.id = eac.entry ";
            codeApprovalTierId = " and eac.tier = " + codeSplitted[0];
            jsonRoot = "eac.data";
            field = codeSplitted[1];
            codeSql = "coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a')";
        }

        if ("$_".equals(code[0])) {
            codeSql = "coalesce(" + jsonRoot + "." + field + ", 'n/a')";
        }


        String __codeFieldMulti = "";
        String __codeFieldPrevMulti = "";
        if (__codeField.contains("*.")){ // ie: $.accounts*.account_name
            String [] splitted = __codeField.split(Pattern.quote("*.")); // [$.accounts,account_name]
            String splitted_root = splitted[0]
                    .replace("$prev$","$")
                    .replace("$$","$")
                    +"[*]";
            String splitted_col = "$."+splitted[1];
            String splitted_col_clean = splitted[1].replaceAll("[.]+","_");
//            System.out.println(__codeField);

            // if $prev$, then used the joined e2 field
            if (splitted[0].contains("$prev$")){
                __codeFieldMulti = "";
                __codeFieldPrevMulti = " join json_table(" + jsonRoot + ", '"+splitted_root+"' columns("+splitted_col_clean+" varchar(2000) path '"+splitted_col+"')) as code_field_multi ";
            }else{
                __codeFieldMulti = " join json_table(" + jsonRoot + ", '"+splitted_root+"' columns("+splitted_col_clean+" varchar(2000) path '"+splitted_col+"')) as code_field_multi ";
                __codeFieldPrevMulti = "";
            }
            codeSql = "coalesce(code_field_multi." + splitted_col_clean + ", 'n/a')";
        }


        String distinct = "count".equals(__agg) ? "distinct" : "";
//        String[] value = __valueField.split("#");

        // $.name -> e.data, $.name
        // $prev$.name -> e2.data, $.name
        //$$.401.name -> eac, $.name
        String[] value = __valueField.split("[#.]", 2); // split $$, 401.name
        jsonRoot = jsonRootMap.get(value[0]);//
        field = value[1];

//        String path2 = value2[1].split("\\.",2)[1]; // 401.name -> 401, name -> name


//        String valueSql = "(" + distinct + " json_value(" + jsonRootMap.get(value[0]) + ", '$." + value[1] + "'))";
        String valueSql = "(" + distinct + " json_value(" + jsonRootMap.get(value[0]) + ", '$." + value[1] + "'))";
        String valueApprovalJoin = "";
        String valueApprovalTierId = "";
//        if ("approval".equals(value[0])) {
        if ("approval".equals(value[0])) {
            String[] valueSplitted = value[1].split("\\.", 2);
            valueApprovalJoin = " left join entry_approval eav on e.id = eav.entry ";
            valueApprovalTierId = " and eav.tier = " + valueSplitted[0];
            jsonRoot = "eav.data";
            field = valueSplitted[1];
            valueSql = "(" + distinct + " json_value(" + jsonRoot + ", '$." + field + "'))";
        }
        if ("$_".equals(value[0])) {
//            valueStatusJoin = "left join "
            valueSql = "(" + distinct + " coalesce(" + jsonRoot + "." + field + ", 'n/a'))";
        }

        String __valueFieldMulti = "";
        String __valueFieldPrevMulti = "";
        if (__valueField.contains("*.")){ // ie: $.accounts*.account_name
            String [] splitted = __valueField.split(Pattern.quote("*.")); // [$.accounts,account_name]
            String splitted_root = splitted[0]
                    .replace("$prev$","$")
                    .replace("$$","$")
                    +"[*]";
            String splitted_col = "$."+splitted[1];
            String splitted_col_clean = splitted[1].replaceAll("[.]+","_");

            // if $prev$, then used the joined e2 field
            if (splitted[0].contains("$prev$")){
                __valueFieldMulti = "";
                __valueFieldPrevMulti = " join json_table(" + jsonRoot + ", '"+splitted_root+"' columns("+splitted_col_clean+" varchar(2000) path '"+splitted_col+"')) as value_field_multi ";
            }else{
                __valueFieldMulti = " join json_table(" + jsonRoot + ", '"+splitted_root+"' columns("+splitted_col_clean+" varchar(2000) path '"+splitted_col+"')) as value_field_multi ";
                __valueFieldPrevMulti = "";
            }

            //            __valueFieldMulti = " join json_table(" + jsonRootMap.get(value[0]) + ", '"+splitted_root+"' columns("+splitted[1]+" varchar(2000) path '"+splitted_col+"')) as value_field_multi ";
            valueSql = "(" + distinct + " coalesce(value_field_multi." + splitted_col_clean + ", 'n/a'))";
        }



//        System.out.println("valueSQL:" + valueSql);

        String[] series;
        String seriesSql;
        String seriesJoin = "";
        String seriesTierId = "";
        String __seriesFieldMulti = "";
        String __seriesFieldPrevMulti = "";
        if (__isSeries) {
            series = __seriesField.split("[#.]", 2);
            if ("approval".equals(series[0])) {
                String[] codeSplitted = series[1].split("\\.", 2);
                seriesJoin = " left join entry_approval es on e.id = es.entry ";
                seriesTierId = " and es.tier = " + codeSplitted[0];
                seriesSql = "coalesce(json_value(es.data" + ", '$." + codeSplitted[1] + "'), 'n/a')";

            } else if ("$_".equals(series[0])) {
                seriesSql = "coalesce(" + jsonRootMap.get(series[0]) + "." + series[1] + ", 'n/a')";
            } else {
                seriesSql = "coalesce(json_value(" + jsonRootMap.get(series[0]) + ", '$." + series[1] + "'), 'n/a')";
            }

            ///// PERLU SEMAK LAGIK N TEST LAGIK
            if (__seriesField.contains("*.")){ // ie: $.accounts*.account_name
                String [] splitted = __seriesField.split(Pattern.quote("*.")); // [$.accounts,account_name]
                String splitted_root = splitted[0]
                        .replace("$prev$","$")
                        .replace("$$","$")
                        +"[*]";
                String splitted_col = "$."+splitted[1];
                String splitted_col_clean = splitted[1].replaceAll("[.]+","_");

                // if $prev$, then used the joined e2 field
                if (splitted[0].contains("$prev$")){
                    __seriesFieldMulti = "";
                    __seriesFieldPrevMulti = " join json_table(" + jsonRootMap.get(series[0]) + ", '"+splitted_root+"' columns("+splitted_col_clean+" varchar(2000) path '"+splitted_col+"')) as series_field_multi ";
                }else{
                    __seriesFieldMulti = " join json_table(" + jsonRootMap.get(series[0]) + ", '"+splitted_root+"' columns("+splitted_col_clean+" varchar(2000) path '"+splitted_col+"')) as series_field_multi ";
                    __seriesFieldPrevMulti = "";
                }

//                __seriesFieldMulti = " join json_table(" + jsonRootMap.get(series[0]) + ", '"+splitted_root+"' columns("+splitted[1]+" varchar(2000) path '"+splitted_col+"')) as series_field_multi ";
                seriesSql = "coalesce(series_field_multi." + splitted_col_clean + ", 'n/a')";
            }
            codeSql = "concat(" + codeSql + ",'_:_'," + seriesSql + ")";
        }




        String prevJoin = " left join entry e2 on e.prev_entry = e2.id ";

        // dev mode should be able to access to the live data for troubleshooting
        String liveCond = "";
        if (__form.isLive()) {// if live , only fetch live=true
            liveCond = " AND e.live = " + __form.isLive();
//            predicates.add(cb.equal(root.get("live"), __form.isLive()));
        } else {// if dev
            // if devInData=null or devInData=false (default value)
            if (__form.getApp() != null && __form.getApp().getX() != null && !__form.getApp().getX().at("/devInData").asBoolean(false)) {
                liveCond = " AND e.live = " + __form.isLive();
//                predicates.add(cb.equal(root.get("live"), form.isLive()));
            }
            //else, dont add predicate, fetch everything
        }


        String sql = "select " + codeSql + " as name, " +
                __agg + valueSql + " as value " +
                " from entry e " +
                __codeFieldMulti +
                codeApprovalJoin +
                valueApprovalJoin +
                __valueFieldMulti +
                seriesJoin +
                __seriesFieldMulti +
                prevJoin +
                __codeFieldPrevMulti +
                __valueFieldPrevMulti +
                __seriesFieldPrevMulti +
                " where e.form=" + __form.getId() +
                codeApprovalTierId + valueApprovalTierId + seriesTierId +
                statusCond +
                filterCond + " and e.deleted = false " + liveCond +
                " group by " + codeSql +
                " order by " + codeSql + " ASC";

        System.out.println("Final sql []:"+sql);

        return dynamicSQLRepository.runQuery(sql, Map.of(), true);
    }


    public Map<String, Object> _chartizeDbData(String __agg,
                                               String __codeField,
                                               String __valueField,
                                               boolean __isSeries,
                                               String __seriesField,
                                               boolean __showAgg,
                                               Form __form, User __user,
                                               JsonNode __status,
                                               Map<String, Object> __filters) {
        Map<String, Object> data = new HashMap<>();

        try {
            List<Object[]> result = _queryChartizeDbData(__agg, __codeField, __valueField, __isSeries, __seriesField, __showAgg, __form, __user, __status, __filters);
            data = __transformResultset(__isSeries, __showAgg, result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return data;
    }

    private Map<String, Object> __transformResultset(boolean __isSeries, boolean __showAgg, List<Object[]> result) {
        Map<String, Object> data = new HashMap<>();
        if (__isSeries) {
            /*
             * 2011:F, 10
             * 2011:M, 12,
             * 2012:F, 8,
             * 2012:M, 6
             *
             * convert to
             * series, F,    M
             * 2011,  10  , 12
             * 2012,  8  , 6
             * */

            Set<String> aCat = new HashSet<>();
            Set<String> aSeries = new HashSet<>();

            result.forEach(d -> {
                String[] n = d[0].toString().split("_:_"); //split series.category
                aCat.add(n[0]);
                aSeries.add(n[1]);
            });

            List<String> listACat = new ArrayList<>(aCat);
            List<String> listASeries = new ArrayList<>(aSeries);
            listACat.sort(Comparator.comparing(Object::toString));
            listASeries.sort(Comparator.comparing(Object::toString));

            Map<String, Object> dataMap = new HashMap<>();
            result.forEach(v -> dataMap.put(v[0].toString(), Optional.ofNullable(v[1]).orElse(0)));

            List<List<Object>> dataset = new ArrayList<>();
            List<Object> header = new ArrayList<>();
            header.add("Series");
            // add header to dataset
            header.addAll(listACat);

//                            header.sort((a, b) -> Double.compare(b, a));
            dataset.add(header);


            // add data to dataset
            listASeries.forEach(ser -> {
                List<Object> row = new ArrayList<>();
//                                System.out.println("ser:"+ser);
                row.add(ser);
                listACat.forEach(cat -> row.add(Optional.ofNullable(dataMap.get(cat + "_:_" + ser)).orElse(0)));
                dataset.add(row);
            });

            if (__showAgg) {
                List<Object> totalRow = new ArrayList();
                List<Object> totalColumn = new ArrayList();

                totalRow.add("Total");
                listASeries.forEach(ser -> {
                    List<Double> row = new ArrayList<>();
                    listACat.forEach(cat -> row.add(((Number) Optional.ofNullable(dataMap.get(cat + "_:_" + ser)).orElse(0)).doubleValue()));
                    totalRow.add(row.stream().reduce(0d, Double::sum));
                });

                totalColumn.add("Total");
                listACat.forEach(cat -> {
                    List<Double> col = new ArrayList<>();
                    listASeries.forEach(ser -> col.add(((Number) Optional.ofNullable(dataMap.get(cat + "_:_" + ser)).orElse(0)).doubleValue()));
                    totalColumn.add(col.stream().reduce(0d, Double::sum));
                });

                data.put("_acol", totalColumn); //_acol
                data.put("_arow", totalRow); //_arow
                Double gTotal = totalColumn.stream()
                        .skip(1)
                        .map(e -> (Double) e)
//                                        .mapToDouble(Double::valueOf)
                        .reduce(0d, Double::sum);
                data.put("_a", gTotal); //_a
            }

            data.put("data", dataset); //

        } else {
            List<Map<String, Object>> d1 = result.stream().map(e -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", e[0]);
                        map.put("value", e[1]);
                        return map;
                    })
                    .collect(toList());
            data.put("data", d1);
            if (__showAgg) {
                Double sum = d1.stream().map(e -> ((Number) e.get("value")).doubleValue())
                        .mapToDouble(Double::valueOf)
                        .sum();
                data.put("_a", sum); //_a
            }
        }
        data.put("series", __isSeries);
        data.put("showAgg", __showAgg);
        return data;
    }


//    private Map<String, Object> __transformResultset(boolean isSeries, boolean showAgg, List<Object[]> result) {
//        Map<String, Object> output = new LinkedHashMap<>();
//
//        if (isSeries) {
//            processSeriesData(output, showAgg, result);
//        } else {
//            processSimpleData(output, showAgg, result);
//        }
//
//        output.put("series", isSeries);
//        output.put("showAgg", showAgg);
//        return output;
//    }
//
//    private void processSeriesData(Map<String, Object> output, boolean showAgg, List<Object[]> result) {
//        // Extract unique series and categories
//        Set<String> series = new TreeSet<>();
//        Set<String> categories = new TreeSet<>();
//        Map<String, Number> valueMap = new HashMap<>();
//
//        for (Object[] row : result) {
//            if (row.length < 2) continue;
//
//            String key = row[0].toString();
//            String[] parts = key.split("_:_");
//            if (parts.length != 2) continue;
//
//            String seriesId = parts[0];
//            String category = parts[1];
//            Number value = parseNumber(row[1]);
//
//            series.add(seriesId);
//            categories.add(category);
//            valueMap.put(key, value);
//        }
//
//        // Build dataset structure
//        List<List<Object>> dataset = new ArrayList<>();
//        List<String> header = new ArrayList<>(categories);
//        header.add(0, "Series");
//        dataset.add(new ArrayList<>(header));
//
//        for (String seriesId : series) {
//            List<Object> row = new ArrayList<>();
//            row.add(seriesId);
//            for (String category : categories) {
//                Number value = valueMap.getOrDefault(seriesId + "_:_" + category, 0);
//                row.add(value);
//            }
//            dataset.add(row);
//        }
//
//        output.put("data", dataset);
//
//        if (showAgg) {
//            calculateAggregations(output, series, categories, valueMap);
//        }
//    }
//
//    private void calculateAggregations(Map<String, Object> output, Set<String> series, Set<String> categories, Map<String, Number> valueMap) {
//        // Row totals (series totals)
//        List<Object> rowTotals = new ArrayList<>();
//        rowTotals.add("Total");
//        categories.forEach(category -> {
//            double sum = series.stream()
//                    .mapToDouble(s -> valueMap.getOrDefault(s + "_:_" + category, 0).doubleValue())
//                    .sum();
//            rowTotals.add(sum);
//        });
//
//        // Column totals (category totals)
//        List<Object> colTotals = new ArrayList<>();
//        colTotals.add("Total");
//        series.forEach(s -> {
//            double sum = categories.stream()
//                    .mapToDouble(c -> valueMap.getOrDefault(s + "_:_" + c, 0).doubleValue())
//                    .sum();
//            colTotals.add(sum);
//        });
//
//        // Grand total
//        double grandTotal = categories.stream()
//                .flatMapToDouble(c -> series.stream()
//                        .mapToDouble(s -> valueMap.getOrDefault(s + "_:_" + c, 0).doubleValue()))
//                .sum();
//
//        output.put("_arow", rowTotals);
//        output.put("_acol", colTotals);
//        output.put("_a", grandTotal);
//    }
//
//    private void processSimpleData(Map<String, Object> output, boolean showAgg, List<Object[]> result) {
//        List<Map<String, Object>> dataItems = result.stream()
//                .filter(row -> row.length >= 2)
//                .map(row -> {
//                    Map<String, Object> item = new HashMap<>();
//                    item.put("name", row[0]);
//                    item.put("value", parseNumber(row[1]));
//                    return item;
//                })
//                .collect(Collectors.toList());
//
//        output.put("data", dataItems);
//
//        if (showAgg) {
//            double total = dataItems.stream()
//                    .mapToDouble(item -> ((Number) item.getOrDefault("value", 0)).doubleValue())
//                    .sum();
//            output.put("_a", total);
//        }
//    }
//
//    private Number parseNumber(Object value) {
//        if (value instanceof Number) {
//            return (Number) value;
//        }
//        try {
//            return Double.parseDouble(value.toString());
//        } catch (NumberFormatException e) {
//            return 0;
//        }
//    }

    /**
     * Untuk LAMBDA
     */
    @Transactional(readOnly = true)
    public Map<String, Object> chartize(Long formId, Map cm, String email, Lambda lambda) {
        ObjectMapper mapper = new ObjectMapper();

        ChartizeObj c = mapper.convertValue(cm, ChartizeObj.class);

        User user = userRepository.findFirstByEmailAndAppId(email, lambda.getApp().getId()).orElse(null);

        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        if (form.getX().get("extended") != null) {
//            formId = extendedId;
            Long extendedId = form.getX().get("extended").asLong();
            form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", formId));
//            form = formService.findFormById(extendedId);
        }

        return _chartizeDbData(c.agg, c.by, c.value, !Helper.isNullOrEmpty(c.series), c.series, c.showAgg, form, user, c.status, c.filter);

    }

    @Transactional(readOnly = true)
    public Map<String, Object> getChartDataNative(Long chartId, Map<String, Object> filters, String email, HttpServletRequest req) {

        ObjectMapper mapper = new ObjectMapper();

        Chart c = dashboardService.getChart(chartId);

        boolean flipAxis = c.getX() != null && c.getX().at("/swap").asBoolean(false);

//        User user = null;
//        if (userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).isPresent()) {
//            user = userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).get();
//        }

        Map<String, Object> data = new HashMap<>();


        Map<String, Object> dataMap = new HashMap<>();

        User user = userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).orElse(null);

        if (Optional.ofNullable(user).isPresent()){
            Map<String, Object> userMap = mapper.convertValue(user, Map.class);
            dataMap.put("user", userMap);
        }
//        userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId())
//                .ifPresent(user -> {
//                    Map<String, Object> userMap = mapper.convertValue(user, Map.class);
//                    dataMap.put("user", userMap);
//                });

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
            Map<String, Object> pF = mapper.convertValue(c.getPresetFilters(), HashMap.class);
            Map<String, Object> presetFilters = Optional.ofNullable(pF).orElse(new HashMap<>())
                    .entrySet().stream()
                    .filter(x -> x.getKey().startsWith("$"))
                    .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

            presetFilters.replaceAll((k, v) -> Helper.compileTpl(v.toString(), dataMap));

            Map<String, Object> filtersNew = new HashMap<>();

            Optional.ofNullable(presetFilters).ifPresent(filtersNew::putAll);
            Optional.ofNullable(filtersReq).ifPresent(filtersNew::putAll);
            Optional.ofNullable(filters).ifPresent(filtersNew::putAll);

            System.out.println(filtersNew);

            Form form = c.getForm();
            // if form is extended form, then use original form
            if (form.getX().get("extended") != null) {
//            formId = extendedId;
                Long extendedId = form.getX().get("extended").asLong();
                form = formRepository.findById(extendedId).orElseThrow(() -> new ResourceNotFoundException("Form (extended from)", "id", extendedId));
//                form = formService.findFormById(extendedId);
            }


            if (c.getFieldCode() != null) {
                List<Object[]> allList = new ArrayList<>();
                if (c.getFieldCode().split(",").length > 1) {
                    Map<String, String> fieldLabels = new HashMap<>();
                    String[] fieldCodes = c.getFieldCode().split(",");
                    for (String fieldCode : fieldCodes) {

                        String[] fsplit = fieldCode.split("#");
                        if (fsplit.length > 1) {
                            String[] actualFieldCode = fsplit[1].split("\\.");
                            Map<String, Item> items = "prev".equals(fsplit[0]) ? form.getPrev().getItems() : c.getForm().getItems();
                            if (items.containsKey(actualFieldCode[0])) {
                                fieldLabels.put(fieldCode, items.get(actualFieldCode[0]).getLabel());
                            }

                        }

                        // return
                        List<Object[]> co = _queryChartizeDbData(c.getAgg(), fieldCode, c.getFieldValue(), c.isSeries(),
                                c.getFieldSeries(), c.isShowAgg(), form, user, c.getStatusFilter(), filtersNew);

                        co.forEach(o -> {
                            String label = fieldLabels.get(fieldCode);
                            String separator = "_:_";
                            if (label != null) {
                                o[0] = flipAxis ? label.trim() + separator + o[0] : o[0] + separator + label.trim();
                            } else {
                                o[0] = flipAxis ? fieldCode + separator + o[0] : o[0] + separator + fieldCode;
                            }
                        });
                        allList.addAll(co);
                    }
                    return __transformResultset(true, c.isShowAgg(), allList);
                } else {
                    return _chartizeDbData(c.getAgg(), c.getFieldCode(), c.getFieldValue(), c.isSeries(),
                            c.getFieldSeries(), c.isShowAgg(), form, user, c.getStatusFilter(), filtersNew);
                }

            }

        } else if ("rest".equals(c.getSourceType())) {

            RestTemplate rt = new RestTemplate();

            try {
                ResponseEntity<Object> re = rt.getForEntity(c.getEndpoint(), Object.class);
                data.put("data", re.getBody());
            } catch (Exception e) {
            }
            //return re.getBody();
        }
        return data;
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
//            List<Map<String, Object>> result = new ArrayList<>();
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
    public List<JsonNode> findListByDatasetData(Long datasetId, String searchText, String email, Map<String,
            Object> filters, String cond, List<String> sorts, List<Long> ids, boolean anonymous,
                                                Pageable pageable, HttpServletRequest req) {

        Page<Entry> entryList = entryRepository.findAll(buildSpecification(datasetId, searchText, email, filters, cond, sorts, ids, req), req.getParameter("size") != null ? pageable : PageRequest.of(0, Integer.MAX_VALUE));

        return entryList.getContent().stream().map(e -> {
            JsonNode node = e.getData();
            ObjectNode o = (ObjectNode) node;
//            o.put("$id", e.getId());
            o.set("$prev", e.getPrev());
            return o;
        }).collect(Collectors.toList());

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

//        EntryApprovalTrail eat = new EntryApprovalTrail(ea);
        entryApprovalTrailRepository.save(eat);
    }

//    @Deprecated
//    public String getPrincipal() {
//        String name = "anonymous";
//        if (SecurityContextHolder.getContext().getAuthentication()!=null) {
//            if ("anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
//                name = "anonymous";
//            } else {
//                name = ((UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getName();
//            }
//        }else{
//            name = "anonymous";
//        }
//        return name;
//    }

//    public String getPrincipalEmail() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication!=null && !(authentication instanceof AnonymousAuthenticationToken)){
//            try {
//                UserPrincipal up = (UserPrincipal) authentication.getPrincipal();
//                return up.getEmail();
//            }catch (Exception e){}
//            return authentication.getName();
//        }else{
//            return "anonymous";
//        }
//    }

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
                              JsonNode status, Map<String, Object> filter) {
    }


    @Async("asyncExec")
    @Transactional(readOnly = true)
    public void bulkResyncEntryData_ModelPicker(Long datasetId) {

        String refCol = "/$id";

        ObjectMapper mapper = new ObjectMapper();

        Set<Item> itemList = new HashSet<>();

        itemList.addAll(itemRepository.findByDatasourceIdAndItemType(datasetId, List.of("modelPicker")));

        Map<Long, JsonNode> newLEntryMap = new HashMap<>();

        List<Entry> ler = findListByDataset(datasetId, "%", null, null, null, null, null, PageRequest.of(0, Integer.MAX_VALUE), null).getContent();

        ler.forEach(le -> {
            JsonNode jnode = le.getData();
            newLEntryMap.put(le.getId(), jnode);
            resyncEntryData(itemList,"$id", jnode);
        });
    }

    record ModelUpdateHolder(Long id, String path, JsonNode jsonNode) {}

    public void resyncEntryData_ModelPicker(Long oriFormId, JsonNode entryDataNode){
        Set<Item> itemList = new HashSet<>();

        datasetRepository.findIdsByFormId(oriFormId)
            .forEach(did -> {
                itemList.addAll(itemRepository.findByDatasourceIdAndItemType(did, List.of("modelPicker")));
            });

        resyncEntryData(itemList,"$id", entryDataNode);
    }

    private final TransactionTemplate transactionTemplate;

    // Update localized data when original data is updated.
    @Async("asyncExec")
    @Transactional(readOnly = true)
    public void resyncEntryData(Set<Item> itemList, String refCol, JsonNode entryDataNode) {

        ObjectMapper mapper = new ObjectMapper();

        Set<Long> entryIds = new HashSet<>();

        itemList.forEach(i -> {
            Long formId = i.getForm().getId();

            SectionItem si = sectionItemRepository.findByFormIdAndCode(formId, i.getCode());

            List<ModelUpdateHolder> updateList = new ArrayList<>();

            if (si != null) {
                Section s = si.getSection();

                // Wrap stream processing in a transaction (Issues with @Transactional when run from lambda)
                transactionTemplate.execute(status -> {
                    try {

                        if ("list".equals(s.getType())) {
                            String selectPath = "";
                            if (List.of("multiple").contains(i.getSubType())) {
                                selectPath = "$." + s.getCode() + "[*]." + i.getCode() + "[*]." + refCol;
                            } else {
                                selectPath = "$." + s.getCode() + "[*]." + i.getCode() + "." + refCol;
                            }

                            try (Stream<Entry> entryStream = entryRepository.findByFormIdAndDataPathWithId(formId, selectPath, entryDataNode.at("/" + refCol))) {
                                entryStream.forEach(entry -> {

                                    entryIds.add(entry.getId());

                                    // Utk list, get List and update each item @ $.<section_key>[index]
                                    if (entry.getData().get(s.getCode()) != null && !entry.getData().get(s.getCode()).isNull() && entry.getData().get(s.getCode()).isArray()) {

                                        for (int z = 0; z < entry.getData().get(s.getCode()).size(); z++) {
                                            // jn ialah jsonnode each child item
                                            JsonNode jn = entry.getData().get(s.getCode()).get(z);

                                            if (jn.get(i.getCode()) != null
                                                    && !jn.get(i.getCode()).isNull()
                                                    && !jn.get(i.getCode()).isEmpty()) {

                                                if (List.of("multiple").contains(i.getSubType())) {
                                                    // multiple lookup inside section
                                                    if (jn.get(i.getCode()).isArray()) {
                                                        // if really multiple lookup
                                                        for (int x = 0; x < jn.get(i.getCode()).size(); x++) {
                                                            if (Objects.equals(
                                                                    jn.get(i.getCode()).get(x).get(refCol).asLong(),
                                                                    entryDataNode.get(refCol).asLong())) {
                                                                updateList.add(new ModelUpdateHolder(entry.getId(), "$." + s.getCode() + "[" + z + "]." + i.getCode() + "[" + x + "]", entryDataNode));
//                                                        entryRepository.updateDataFieldScope(entry.getId(), "$." + s.getCode() + "[" + z + "]." + i.getCode() + "[" + x + "]", "[" + mapper.valueToTree(entryDataNode).toString() + "]");
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    //if lookup biasa dlm section
                                                    if (Objects.equals(
                                                            jn.get(i.getCode()).get(refCol).asLong(),
                                                            entryDataNode.get(refCol).asLong())) {
                                                        updateList.add(new ModelUpdateHolder(entry.getId(), "$." + s.getCode() + "[" + z + "]." + i.getCode(), entryDataNode));
//                                                entryRepository.updateDataFieldScope(entry.getId(), "$." + s.getCode() + "[" + z + "]." + i.getCode(), "[" + mapper.valueToTree(entryDataNode).toString() + "]");
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    try {
                                        entryRepository.save(updateApprover(entry, entry.getEmail()));
                                    } catch (Exception e) {
                                    }

                                    this.entityManager.detach(entry);
                                });
                            }

                        } else if ("section".equals(s.getType())) {
                            String selectPath = "";
                            if (List.of("multiple").contains(i.getSubType())) {
                                selectPath = "$." + i.getCode() + "[*]." + refCol;
                            } else {
                                selectPath = "$." + i.getCode() + "." + refCol;
                            }

                            try (Stream<Entry> entryStream = entryRepository.findByFormIdAndDataPathWithId(formId, selectPath, entryDataNode.at("/" + refCol))) {
                                entryStream.forEach(entry -> {

                                    entryIds.add(entry.getId());

                                    // entry.getData().get(code) --> always return object (modelpicker), so isempty works here.
                                    if (entry.getData().get(i.getCode()) != null
                                            && !entry.getData().get(i.getCode()).isNull()
                                            && !entry.getData().get(i.getCode()).isEmpty()) {

                                        if (List.of("multiple").contains(i.getSubType())) {
                                            // multiple lookup inside section
                                            if (entry.getData().get(i.getCode()).isArray()) {
                                                // if really multiple lookup
                                                for (int z = 0; z < entry.getData().get(i.getCode()).size(); z++) {
                                                    if (Objects.equals(
                                                            entry.getData().get(i.getCode()).get(z).get(refCol).asLong(),
                                                            entryDataNode.get(refCol).asLong())) {
                                                        updateList.add(new ModelUpdateHolder(entry.getId(), "$." + i.getCode() + "[" + z + "]", entryDataNode));
//                                                entryRepository.updateDataFieldScope(entry.getId(), "$." + i.getCode() + "[" + z + "]", "[" + mapper.valueToTree(entryDataNode).toString() + "]");
                                                    }
                                                }
                                            }
                                        } else {
                                            //if lookup biasa dlm section
                                            if (Objects.equals(
                                                    entry.getData().get(i.getCode()).get(refCol).asLong(),
                                                    entryDataNode.get(refCol).asLong())) {
                                                updateList.add(new ModelUpdateHolder(entry.getId(), "$." + i.getCode(), entryDataNode));
//                                        entryRepository.updateDataFieldScope(entry.getId(), "$." + i.getCode(), "[" + mapper.valueToTree(entryDataNode).toString() + "]");
                                            }
                                        }

                                    }

                                    try {
                                        entryRepository.save(updateApprover(entry, entry.getEmail()));
                                    } catch (Exception e) {
                                    }

                                    this.entityManager.detach(entry);
                                });
                            }

                        } else if ("approval".equals(s.getType())) {
                            String selectPath = "";
                            if (List.of("multiple").contains(i.getSubType())) {
                                selectPath = "$." + i.getCode() + "[*]." + refCol;
                            } else {
                                selectPath = "$." + i.getCode() + "." + refCol;
                            }

                            final String finalSelectPath = selectPath;

                            List<Tier> tlist = tierRepository.findBySectionId(s.getId());

                            tlist.stream().forEach(t -> {
                                try (Stream<EntryApproval> entryStream = entryRepository.findByTierIdAndApprovalDataPathWithId(t.getId(), finalSelectPath, entryDataNode.at("/" + refCol))) {
                                    entryStream.forEach(entryApproval -> {

                                        entryIds.add(entryApproval.getEntry().getId());

                                        JsonNode jn = entryApproval.getData();

                                        if (jn.get(i.getCode()) != null
                                                && !jn.get(i.getCode()).isNull()
                                                && !jn.get(i.getCode()).isEmpty()) {

                                            if (List.of("multiple").contains(i.getSubType())) {
                                                // multiple lookup inside section
                                                if (jn.get(i.getCode()).isArray()) {
                                                    // if really multiple lookup
                                                    for (int x = 0; x < jn.get(i.getCode()).size(); x++) {
                                                        if (Objects.equals(
                                                                jn.get(i.getCode()).get(x).get(refCol).asLong(),
                                                                entryDataNode.get(refCol).asLong())) {
                                                            updateList.add(new ModelUpdateHolder(entryApproval.getId(), "$." + i.getCode() + "[" + x + "]", entryDataNode));
//                                                    entryRepository.updateApprovalDataFieldScope2(entryApproval.getId(), "$." + i.getCode() + "[" + x + "]", "[" + mapper.valueToTree(entryDataNode).toString() + "]");
                                                        }
                                                    }
                                                }
                                            } else {
                                                //if lookup biasa dlm section
                                                if (Objects.equals(
                                                        jn.get(i.getCode()).get(refCol).asLong(),
                                                        entryDataNode.get(refCol).asLong())) {
                                                    updateList.add(new ModelUpdateHolder(entryApproval.getId(), "$." + i.getCode(), entryDataNode));
//                                            entryRepository.updateApprovalDataFieldScope2(entryApproval.getId(), "$." + i.getCode(), "[" + mapper.valueToTree(entryDataNode).toString() + "]");
                                                }
                                            }
                                        }
                                        this.entityManager.detach(entryApproval);
                                    });
                                }
                            });
                        }

                        if (updateList.size() > 0) {
                            // if field ada value & !null and field ada id
                            updateList.forEach((update) -> {
                                System.out.println("#######:" + update.id + ",path:" + update.path + ",value:" + update.jsonNode);
//                        System.out.println(update.path);
                                if (update != null) {
                                    if ("approval".equals(s.getType())) {
                                        entryRepository.updateApprovalDataFieldScope2(update.id, update.path, "[" + mapper.valueToTree(update.jsonNode).toString() + "]");
                                    } else {
                                        entryRepository.updateDataFieldScope(update.id, update.path, "[" + mapper.valueToTree(update.jsonNode).toString() + "]");
                                    }
                                }
                            });
                        }
                        if (entryIds.size() > 0) {
                            entryIds.forEach(entryId -> {
                                try {
                                    Entry entry = entryRepository.findById(entryId).orElseThrow();
                                    entryRepository.save(updateApprover(entry, entry.getEmail()));
                                } catch (Exception e) {
                                }
                            });
                        }

                    }catch (Exception e) {
                        status.setRollbackOnly();
                        throw e;
                    }
                    return null;
                });
            }
        });
    }

}
