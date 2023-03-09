package com.benzourry.leap.service;

import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.filter.EntryFilter;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.script.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.FileReader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

//import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class EntryService {
    final EntryRepository entryRepository;

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

    final NotificationService notificationService;

    final EndpointService endpointService;

    final ChartQuery chartQuery;

    final AppService appService;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(EntryService.class);

    public EntryService(EntryRepository entryRepository,
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
                        NotificationService notificationService,
                        EndpointService endpointService,
                        ChartQuery chartQuery, AppService appService) {
        this.entryRepository = entryRepository;
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
        this.notificationService = notificationService;
        this.endpointService = endpointService;
        this.chartQuery = chartQuery;
        this.appService = appService;
    }

    public Entry assignApprover(Long entryId, Long atId, String email) throws Exception {
        Optional<Entry> entryOpt = entryRepository.findById(entryId);
        if (!entryOpt.isPresent()) {
            throw new Exception("Entry does not exist");
        }
        Entry entry = entryOpt.get();
        Tier gat = tierRepository.getReferenceById(atId);
        Map<Long, String> approver = entry.getApprover();

        approver.put(atId, email);
        entry.setApprover(approver);
        entryRepository.save(entry);

        /*
          EMAIL NOTIFICATION TO INFORM ADMIN & APPLICANT ON PTJ ENDORSEMENT
          sendMail(admin);
         */

        List<Long> emailTemplates = gat.getAssignMailer();

        emailTemplates.forEach(t -> triggerMailer(t, entry, gat));

        return entry;

    }

    /**
     * FOR LAMBDA USAGE
     **/
    public Entry byId(Long id, Lambda lambda) throws Exception {
        Entry entry = entryRepository.getReferenceById(id);
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return entry;
        } else {
            throw new Exception("Lambda trying to view external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Page<Entry> dataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.getReferenceById(datasetId);
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return findListByDataset(datasetId, "", email, filters, null, filters!=null && filters.get("ids")!=null?(List<Long>)filters.get("ids"):null, PageRequest.of(0, Integer.MAX_VALUE), null);
        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public List<JsonNode> flatDataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset d = datasetRepository.getReferenceById(datasetId);
        if (Objects.equals(d.getApp().getId(), lambda.getApp().getId())) {
            return customEntryRepository.findDataPaged(EntryFilter.builder()
                    .filters(filters)
                    .form(d.getForm())
                    .formId(d.getForm().getId())
                    .build().filter(), PageRequest.of(0, Integer.MAX_VALUE));
        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Long count(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.getReferenceById(datasetId);
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return countByDataset(datasetId, "", email, filters, null);
        } else {
            throw new Exception("Lambda trying to count external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Stream<Entry> streamDataset(Long datasetId, Map filters, String email, Lambda lambda) throws Exception {
        Dataset dataset = datasetRepository.getReferenceById(datasetId);
        if (Objects.equals(dataset.getApp().getId(), lambda.getApp().getId())) {
            return findListByDatasetStream(datasetId, "", email, filters, null,filters!=null && filters.get("ids")!=null?(List<Long>)filters.get("ids"):null, null);
        } else {
            throw new Exception("Lambda trying to list external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public Entry save(Entry entry, Lambda lambda) throws Exception {
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return save(entry.getForm().getId(), entry, null, entry.getEmail());
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
        Form form = formRepository.getReferenceById(formId);
        if (Objects.equals(form.getApp().getId(), lambda.getApp().getId())) {
            return save(formId, e, prevId, e.getEmail());
        } else {
            throw new Exception("Lambda trying to update external entry");
        }
    }

    /**
     * FOR LAMBDA
     **/
    public void delete(Long id, Lambda lambda) throws Exception {
        Entry entry = entryRepository.getReferenceById(id);
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
    public Entry update(Long entryId, JsonNode obj, String root, Lambda lambda) {
        return updateField(entryId, obj, root, lambda.getApp().getId());
    }

    /**
     * FOR LAMBDA
     **/
    public Entry update(Long entryId, Map obj, Lambda lambda) {
        ObjectMapper om = new ObjectMapper();
        return updateField(entryId, om.convertValue(obj, JsonNode.class), null, lambda.getApp().getId());
    }

    /**
     * FOR LAMBDA
     **/
    public Entry approval(Long entryId, Map filters, String email, Lambda lambda) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        EntryApproval ea = mapper.convertValue(filters, EntryApproval.class);
        //Long entryId, EntryApproval gaa, String email
        Entry entry = entryRepository.getReferenceById(entryId);
        if (Objects.equals(entry.getForm().getApp().getId(), lambda.getApp().getId())) {
            return actionApp(entryId, ea,email);
        }else{
            throw new Exception("Lambda trying to approve external entry");
        }
    }


    public Entry save(Long formId, Entry entry, Long prevId, String email) {
        boolean newEntry = false;
        Form form = formService.findFormById(formId);
        if (entry.getId() != null) {
            Entry e = entryRepository.getReferenceById(entry.getId());
            entry.setPrevEntry(e.getPrevEntry());
        }
        if (prevId != null) {
            // if prevId=null dont do any assignment/re-assignment of prevData.
            Optional<Entry> prevEntryOpt = entryRepository.findById(prevId);
            if (prevEntryOpt.isPresent()) {
                entry.setPrevEntry(prevEntryOpt.get());
            }
        }
        entry.setForm(form);

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

//            long counter = form.getCounter();
            form.setCounter(form.getCounter() + 1);
            formRepository.save(form);
            newEntry = true;
        }

        final Entry fEntry = entryRepository.save(entry);

        if (newEntry) {
            form.getAddMailer().forEach(m -> triggerMailer(m, fEntry, null));
        } else {
            form.getUpdateMailer().forEach(m -> triggerMailer(m, fEntry, null));
        }

        try {
            EntryApprovalTrail eat = new EntryApprovalTrail(null, null, "saved", "Saved by " + email, new Date(), email, fEntry.getId());
            entryApprovalTrailRepository.save(eat);
        }catch (Exception e){}

        return entry;
    }


//    public void updateApproverInTier(Long formId,Long tierId)
//    public Entry findById(Long id) {
//
//        return entryRepository.findById(id).get();
//    }

    public Entry updateApprover(Entry entry, String email) {
        Map<Long, String> approver = entry.getApprover();
        ObjectMapper mapper = new ObjectMapper();

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
                    User user;
                    if (userOpt.isPresent()) {
                        user = userOpt.get();
                    } else {
                        user = new User();
                        user.setEmail(entry.getEmail());
                    }
                    userMap = mapper.convertValue(user, Map.class);
                    dataMap.put("user", userMap);
                }
                dataMap.put("data", mapper.convertValue(entry.getData(), HashMap.class));
                dataMap.put("prev", mapper.convertValue(entry.getPrev(), HashMap.class));

                dataMap.put("now", Instant.now().toEpochMilli());
                String compiled = MailService.compileTpl(at.getApprover(), dataMap);
                List<String> emails = Arrays.asList(compiled.split(",")).stream()
                        .filter(Objects::nonNull)
                        .filter(Predicate.not(String::isBlank))
                        .toList();
                a = String.join(",", emails);
            } else if ("GROUP".equals(at.getType()) && at.getApproverGroup() != null) {
                Long groupId = at.getApproverGroup();
                List<String> emails = appUserRepository.findEmailsByGroupId(groupId).stream()
                        .filter(Objects::nonNull)
                        .filter(Predicate.not(String::isBlank))
                        .toList();
                a = String.join(",", emails);
            }
            approver.put(at.getId(), a);
        });

//        System.out.println("APPROVER:" + approver);
        entry.setApprover(approver);
        return entry;
//        return updateApproverNew(entry,email,null,true);
    }

    public void triggerMailer(Long mailer, Entry entry, Tier gat) {
        if (mailer != null) {
//            logger.info("mailer != null");
            try {
                EmailTemplate template = emailTemplateRepository.findByIdAndEnabled(mailer, Constant.ENABLED);//.findByCodeAndEnabled(mailer, Constant.ENABLED);
                if (template != null) {
//                    logger.info("template != null");
                    Map<String, Object> contentMap = new HashMap<>();
                    Map<String, Object> subjectMap = new HashMap<>();
                    ObjectMapper mapper = new ObjectMapper();
//                    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
                    contentMap.put("_", mapper.convertValue(entry, Map.class));
                    subjectMap.put("_", mapper.convertValue(entry, Map.class));
                    Map<String, Object> result = mapper.convertValue(entry.getData(), Map.class);
                    Map<String, Object> prev = mapper.convertValue(entry.getPrev(), Map.class);


//                    String url = "https://" + entry.getForm().getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
                    String url = "https://";
                    if (entry.getForm().getApp().getAppDomain() != null) {
                        url += entry.getForm().getApp().getAppDomain() + "/#";
                    } else {
                        url += entry.getForm().getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
                    }

                    contentMap.put("uiUri", url);
                    contentMap.put("viewUri", url + "/form/" + entry.getForm().getId() + "/view?entryId=" + entry.getId());
                    contentMap.put("editUri", url + "/form/" + entry.getForm().getId() + "/edit?entryId=" + entry.getId());

                    if (result != null) {

                        contentMap.put("code", result.get("$code"));
                        subjectMap.put("code", result.get("$code"));

                        contentMap.put("id", result.get("$id"));
                        subjectMap.put("id", result.get("$id"));

                        contentMap.put("counter", result.get("$counter"));
                        subjectMap.put("counter", result.get("$counter"));
                    }

                    if (prev != null) {

                        contentMap.put("prev_code", prev.get("$code"));
                        subjectMap.put("prev_code", prev.get("$code"));

                        contentMap.put("prev_id", prev.get("$id"));
                        subjectMap.put("prev_id", prev.get("$id"));

                        contentMap.put("prev_counter", prev.get("$counter"));
                        subjectMap.put("prev_counter", prev.get("$counter"));
                    }

                    contentMap.put("data", result);
                    subjectMap.put("data", result);

                    contentMap.put("prev", prev);
                    subjectMap.put("prev", prev);

//                    contentMap.put("entry", entry);
//                    subjectMap.put("entry", entry);

                    Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), entry.getForm().getApp().getId());
                    if (u.isPresent()) {
                        Map userMap = mapper.convertValue(u.get(), Map.class);
                        contentMap.put("user", userMap);
                        subjectMap.put("user", userMap);
                    }

                    if (gat != null) {
//                        gat = entry.getForm().getTiers().get(entry.getCurrentTier());
                        contentMap.put("tier", gat);
                        subjectMap.put("tier", gat);
                    }

                    if (entry.getApproval() != null && gat != null) {
                        EntryApproval approval_ = entry.getApproval().get(gat.getId());
                        if (approval_ != null) {
                            Map<String, Object> approval = mapper.convertValue(approval_.getData(), Map.class);
                            subjectMap.put("approval_", approval_);
                            contentMap.put("approval_", approval_);
                            subjectMap.put("approval", approval);
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
                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).collect(toList());
                            if (!adminEmails.isEmpty()) {
                                recipients.addAll(adminEmails);
                            }
//                            if (!Helper.isNullOrEmpty(entry.getForm().getAdmin().getUsers())) {
//                                recipients.addAll(Arrays.asList(entry.getForm().getAdmin().getUsers().replaceAll(" ", "").split(",")));
//                            }
                        }

                    }
                    if (gat != null && template.isToApprover()) {
                        if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
                            recipients.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
                        }
                    }
                    if (!Objects.isNull(template.getToExtra())) {
                        String extra = MailService.compileTpl(template.getToExtra(), contentMap);
                        if (!extra.isEmpty()) {
                            recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                                    .filter(str -> !str.isBlank())
                                    .collect(toList()));
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
                                    .map(appUser -> appUser.getUser().getEmail()).collect(toList());
                            if (!adminEmails.isEmpty()) {
                                recipientsCc.addAll(adminEmails);
                            }
//                            if (!Helper.isNullOrEmpty(entry.getForm().getAdmin().getUsers())) {
//                                recipientsCc.addAll(Arrays.asList(entry.getForm().getAdmin().getUsers().replaceAll(" ", "").split(",")));
//                            }
                        }

                    }
                    if (gat != null && template.isCcApprover()) {
                        if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
                            recipientsCc.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
                        }
                    }
                    if (!Objects.isNull(template.getCcExtra())) {
                        String ccextra = MailService.compileTpl(template.getCcExtra(), contentMap);
                        if (!ccextra.isEmpty()) {
                            recipientsCc.addAll(Arrays.stream(ccextra.replaceAll(" ", "").split(","))
                                    .filter(str -> !str.isBlank())
                                    .collect(toList()));
                        }
                    }


                    String[] rec = recipients.toArray(new String[0]);
                    String[] recCc = recipientsCc.toArray(new String[0]);

//                    logger.info("recipients:::"+recipients.toString());
                    if (template.isPushable()) {
                        pushService.sendMailPush(entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, template, subjectMap, contentMap, entry.getForm().getApp());
                    }

                    mailService.sendMail(entry.getForm().getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, template, subjectMap, contentMap, entry.getForm().getApp());
//            notificationService.notify(-1, rec, template, contentMap);
                } else {
//                    logger.info("template == null");
                }
            } catch (Exception e) {
                e.printStackTrace();
//            logger.warn("submitEntry:" + e.getMessage());
            }
        }
    }

    public Entry findById(Long id, Long formId, boolean anonymous) {

        Form form = formService.findFormById(formId);

        Entry entry;

        boolean isPublic = form.isPublicEp();
//        System.out.println("fromPrivate:"+anonymous);
        if (anonymous && !isPublic) {
            // access to private dataset from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Form Entry: Access to private form entry from public endpoint is not allowed");
        } else {
//            return findListByDataset(datasetId, searchText, email, filters, pageable, req);
            entry = entryRepository.getReferenceById(id);
        }


        return entry;
    }


    @Transactional
    public Map<String, Object> blastEmailByDataset(Long datasetId, String searchText, String email, Map filters, EmailTemplate emailTemplate, List<Long> ids, HttpServletRequest req) {

        Map<String, Object> data = new HashMap<>();
        Dataset d = datasetRepository.getReferenceById(datasetId);
        ObjectMapper mapper = new ObjectMapper();
//        Page<Entry> list = findListByDataset(datasetId, searchText, email, filters, PageRequest.of(0, Integer.MAX_VALUE), req);

        AtomicInteger index = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();


        try (Stream<Entry> entryStream = findListByDatasetStream(datasetId, searchText, email, filters,null, ids, req)) {
            entryStream.forEach(entry -> {
                total.getAndIncrement();
                Map<String, Object> contentMap = new HashMap<>();
                Map<String, Object> subjectMap = new HashMap<>();
                contentMap.put("_", entry);
                subjectMap.put("_", entry);
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
                    subjectMap.put("code", result.get("$code"));

                    contentMap.put("id", result.get("$id"));
                    subjectMap.put("id", result.get("$id"));

                    contentMap.put("counter", result.get("$counter"));
                    subjectMap.put("counter", result.get("$counter"));
                }


                if (prev != null) {

                    contentMap.put("prev_code", prev.get("$code"));
                    subjectMap.put("prev_code", prev.get("$code"));

                    contentMap.put("prev_id", prev.get("$id"));
                    subjectMap.put("prev_id", prev.get("$id"));

                    contentMap.put("prev_counter", prev.get("$counter"));
                    subjectMap.put("prev_counter", prev.get("$counter"));
                }

                contentMap.put("data", result);
                subjectMap.put("data", result);

                contentMap.put("prev", prev);
                subjectMap.put("prev", prev);

                assert result != null;
                recipients.add(result.get(d.getBlastTo()) + "");


                try {
                    Optional<User> u = userRepository.findFirstByEmailAndAppId(entry.getEmail(), d.getApp().getId());
                    if (u.isPresent()) {
                        Map userMap = mapper.convertValue(u.get(), Map.class);
                        contentMap.put("user", userMap);
                        subjectMap.put("user", userMap);
                    }

                    Tier gat = entry.getForm().getTiers().get(entry.getCurrentTier());
                    if (gat != null) {
                        contentMap.put("tier", gat);
                        subjectMap.put("tier", gat);
                    }

                    if (entry.getApproval() != null && gat != null) {
                        EntryApproval approval_ = entry.getApproval().get(gat.getId());
                        if (approval_ != null) {
                            Map<String, Object> approval = mapper.convertValue(approval_.getData(), Map.class);
                            subjectMap.put("approval_", approval_);
                            contentMap.put("approval_", approval_);
                            subjectMap.put("approval", approval);
                            contentMap.put("approval", approval);
                        }
                    }

                    if (emailTemplate.isToUser()) {
                        recipients.add(entry.getEmail());
                    }
                    if (emailTemplate.isToAdmin()) {
                        if (entry.getForm().getAdmin() != null) {
                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).collect(toList());
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
                        String extra = MailService.compileTpl(emailTemplate.getToExtra(), contentMap);
                        if (!extra.isEmpty()) {
                            recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                                    .filter(str -> !str.isBlank())
                                    .collect(toList()));
                        }
                    }

                    if (emailTemplate.isCcUser()) {
                        recipientsCc.add(entry.getEmail());
                    }
                    if (emailTemplate.isCcAdmin()) {
                        if (entry.getForm().getAdmin() != null) {
                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).collect(toList());
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
                        String extra = MailService.compileTpl(emailTemplate.getCcExtra(), contentMap);
                        if (!extra.isEmpty()) {
                            recipientsCc.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
                                    .filter(str -> !str.isBlank())
                                    .collect(toList()));
                        }
                    }
                } catch (Exception e) {
                    System.out.println("ERROR BLAST@@@######::" + e.getMessage());
                }

                String[] rec = recipients.toArray(new String[0]);
                String[] recCc = recipientsCc.toArray(new String[0]);

                if (emailTemplate.isPushable()) {
                    pushService.sendMailPush(d.getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, subjectMap, contentMap, d.getApp());
                }

                mailService.sendMail(d.getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, subjectMap, contentMap, d.getApp());
                index.getAndIncrement();
                this.entityManager.detach(entry);
            });
        }


//        Pageable pageRequest = PageRequest.of(0, 100);
//        Page<Entry> onePage = findListByDataset(datasetId, searchText, email, filters, pageRequest, req);

//        while (!onePage.isEmpty()) {
//            pageRequest = pageRequest.next();
//
//            onePage.forEach(entry -> {
//                Map<String, Object> contentMap = new HashMap<>();
//                Map<String, Object> subjectMap = new HashMap<>();
//                contentMap.put("_", entry);
//                subjectMap.put("_", entry);
//                Map<String, Object> result = mapper.convertValue(entry.getData(), Map.class);
//                Map<String, Object> prev = mapper.convertValue(entry.getPrev(), Map.class);
//
//                String url = "https://" + entry.getForm().getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN + "/#";
//                contentMap.put("uiUri", url);
//                contentMap.put("viewUri", url + "/form/" + entry.getForm().getId() + "/view?entryId=" + entry.getId());
//                contentMap.put("editUri", url + "/form/" + entry.getForm().getId() + "/edit?entryId=" + entry.getId());
//
//                List<String> recipients = new ArrayList<>();// Arrays.asList(entry.getEmail());
//                List<String> recipientsCc = new ArrayList<>();
//
//
//                if (result != null) {
//                    contentMap.put("code", result.get("$code"));
//                    subjectMap.put("code", result.get("$code"));
//
//                    contentMap.put("id", result.get("$id"));
//                    subjectMap.put("id", result.get("$id"));
//
//                    contentMap.put("counter", result.get("$counter"));
//                    subjectMap.put("counter", result.get("$counter"));
//                }
//
//
//                if (prev != null) {
//
//                    contentMap.put("prev_code", prev.get("$code"));
//                    subjectMap.put("prev_code", prev.get("$code"));
//
//                    contentMap.put("prev_id", prev.get("$id"));
//                    subjectMap.put("prev_id", prev.get("$id"));
//
//                    contentMap.put("prev_counter", prev.get("$counter"));
//                    subjectMap.put("prev_counter", prev.get("$counter"));
//                }
//
//                contentMap.put("data", result);
//                subjectMap.put("data", result);
//
//                contentMap.put("prev", prev);
//                subjectMap.put("prev", prev);
//
//                assert result != null;
//                recipients.add(result.get(d.getBlastTo()) + "");
//
//
//                try {
//                    Optional<User> u = userRepository.findByEmailAndAppId(entry.getEmail(), d.getApp().getId());
//                    if (u.isPresent()) {
//                        Map userMap = mapper.convertValue(u.get(), Map.class);
//                        contentMap.put("user", userMap);
//                        subjectMap.put("user", userMap);
//                    }
//
//                    Tier gat = entry.getForm().getTiers().get(entry.getCurrentTier());
//                    if (gat != null) {
//                        contentMap.put("tier", gat);
//                        subjectMap.put("tier", gat);
//                    }
//
//                    if (entry.getApproval() != null && gat != null) {
//                        EntryApproval approval_ = entry.getApproval().get(gat.getId());
//                        if (approval_ != null) {
//                            Map<String, Object> approval = mapper.convertValue(approval_.getData(), Map.class);
//                            subjectMap.put("approval_", approval_);
//                            contentMap.put("approval_", approval_);
//                            subjectMap.put("approval", approval);
//                            contentMap.put("approval", approval);
//                        }
//                    }
//
//                    if (emailTemplate.isToUser()) {
//                        recipients.add(entry.getEmail());
//                    }
//                    if (emailTemplate.isToAdmin()) {
//                        if (entry.getForm().getAdmin() != null) {
//                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
//                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).collect(toList());
//                            if (!adminEmails.isEmpty()) {
//                                recipients.addAll(adminEmails);
//                            }
//                        }
//                    }
//                    if (gat != null && emailTemplate.isToApprover()) {
//                        if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
//                            recipients.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
//                        }
//                    }
//                    if (!Objects.isNull(emailTemplate.getToExtra())) {
//                        String extra = MailService.compileTpl(emailTemplate.getToExtra(), contentMap);
//                        if (!extra.isEmpty()) {
//                            recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
//                                    .filter(str -> !str.isBlank())
//                                    .collect(toList()));
//                        }
//                    }
//
//                    if (emailTemplate.isCcUser()) {
//                        recipients.add(entry.getEmail());
//                    }
//                    if (emailTemplate.isCcAdmin()) {
//                        if (entry.getForm().getAdmin() != null) {
//                            List<String> adminEmails = appUserRepository.findByGroupId(entry.getForm().getAdmin().getId(), Pageable.unpaged())
//                                    .getContent().stream().map(appUser -> appUser.getUser().getEmail()).collect(toList());
//                            if (!adminEmails.isEmpty()) {
//                                recipientsCc.addAll(adminEmails);
//                            }
//                        }
//
//                    }
//                    if (gat != null && emailTemplate.isCcApprover()) {
//                        if (!entry.getApprover().isEmpty() && entry.getApprover().get(gat.getId()) != null) {
//                            recipientsCc.addAll(Arrays.asList(entry.getApprover().get(gat.getId()).replaceAll(" ", "").split(",")));
//                        }
//                    }
//                    if (!Objects.isNull(emailTemplate.getCcExtra())) {
//                        String extra = MailService.compileTpl(emailTemplate.getCcExtra(), contentMap);
//                        if (!extra.isEmpty()) {
//                            recipients.addAll(Arrays.stream(extra.replaceAll(" ", "").split(","))
//                                    .filter(str -> !str.isBlank())
//                                    .collect(toList()));
//                        }
//                    }
//                } catch (Exception e) {
//                    System.out.println("ERROR BLAST@@@######::" + e.getMessage());
//                }
//
//                String[] rec = recipients.toArray(new String[0]);
//                String[] recCc = recipientsCc.toArray(new String[0]);
//
//                if (emailTemplate.isPushable()) {
//                    pushService.sendMailPush(d.getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, subjectMap, contentMap, d.getApp());
//                }
//
//                mailService.sendMail(d.getApp().getAppPath() + "_" + Constant.LEAP_MAILER, rec, recCc, null, emailTemplate, subjectMap, contentMap, d.getApp());
//                index.getAndIncrement();
//            });
//
//            onePage = findListByDataset(datasetId, searchText, email, filters, pageRequest, req);
//
//        }


        data.put("totalCount", total.get());
        data.put("totalSent", index.get());
        data.put("success", total.get() == index.get());
        data.put("partial", total.get() > index.get());

        return data;

    }


    public Entry findFirstByParam(Long formId, Map filters, HttpServletRequest req, boolean anonymous) throws Exception {
        Form form = formRepository.getReferenceById(formId);

        boolean isPublic = form.isPublicEp();
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
//        presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));
//
            final Map newFilter = new HashMap();
            if (filters != null) {
                newFilter.putAll(filters);
            }
//            if (filters != null) {
//            if (d.getPresetFilters() != null) {
//                filters.putAll(presetFilters);
//            }
            if (filtersReq.size() > 0) {
                newFilter.putAll(filtersReq);
            }
//            }
            Page<Entry> entry = entryRepository.findAll(EntryFilter.builder()
                    .formId(formId)
                    .form(form)
                    .filters(newFilter)
                    .build().filter(), PageRequest.of(0, 1));

            return entry.getContent().stream()
                    .findFirst().orElseThrow(() -> new Exception("No entry found using parameters:" + filters));
        }
    }

//    public Entry findFirstByEmail(Long formId, String email) throws Exception {
//        Page<Entry> entry = entryRepository.findAll(EntryFilter.builder()
//                .formId(formId)
//                .email(email)
//                .build().filter(), PageRequest.of(0, 1));
//
//        return entry.getContent().stream()
//                .findFirst().orElseThrow(()->new Exception("No entry found for formId="+formId+" and email="+email));
//    }

    public Page<Entry> findByFormId(Long formId, Pageable pageable) {
        return entryRepository.findByFormId(formId, pageable);
    }

    @Transactional
    public Entry retractApp(Long id, String email) {

        Entry entry = entryRepository.findById(id).get();


        entry.setCurrentStatus(Entry.STATUS_DRAFTED);
//        entry.getResearch().setDocFlag(Constant.DRAFT_FLAG);
        entry.setCurrentTier(null);
        entry.setCurrentTierId(null);
        entry.setFinalTierId(null);
        entry.getApproval().clear();

//        endpointService.runEndpoint(entry.getForm().getRetractCb(),null);
        entry.getForm().getRetractMailer().forEach(t -> triggerMailer(t, entry, null));
//        triggerMailer(entry.getForm().getRetractMailer(), entry, null);
//        entry.setApproval(new HashMap<>());
//        entry.getApproval().clear();
        EntryApprovalTrail eat = new EntryApprovalTrail();
//        eat.setTier(gat);
        eat.setTimestamp(new Date());
        eat.setRemark("RETRACTED by User " + Optional.ofNullable(email).orElse(""));
        eat.setStatus(Entry.STATUS_DRAFTED);
        eat.setEmail(Optional.ofNullable(email).orElse(entry.getEmail()));
        eat.setEntryId(id);

        entryApprovalTrailRepository.save(eat);

        return entryRepository.save(entry);

    }

    /*
     * $update$(id,{'name':'asdaa'},'prev')
     * */
    @Transactional
    public Entry updateField(Long entryId, JsonNode obj, String root, Long appId) {
        Entry entry = entryRepository.getReferenceById(entryId);
//        ObjectNode data = (ObjectNode) entry.getData(); ///((ObjectNode) nodeParent).put('subfield', "my-new-value-here");

        ObjectMapper mapper = new ObjectMapper();

        if (entry.getForm().getApp().getId().equals(appId) || appId == null) {
            // dari app yg sama atau appId == null
            Map<String, Object> map1;
            boolean isPrev = "prev".equals(root);
            if (isPrev) {
                map1 = mapper.convertValue(entry.getPrev(), Map.class);
            } else {
                map1 = mapper.convertValue(entry.getData(), Map.class);
            }
            Map<String, Object> map2 = mapper.convertValue(obj, Map.class);

            Map<String, Object> merged = new HashMap<>(map1);
            merged.putAll(map2);
//            System.out.println("#####Map from update field");
//            System.out.println(map2);
//        System.out.println(mapper.writeValueAsString(merged));
            if (isPrev) {
//                entry.setPrev(mapper.valueToTree(merged));
            } else {
                entry.setData(mapper.valueToTree(merged));
            }
            // So, update field will also update approver n trigger new entry mailer
//            entryRepository.save(entry);
            save(entry.getForm().getId(), entry, null, entry.getEmail());
            try {
                EntryApprovalTrail eat = new EntryApprovalTrail(obj, null, "Field UPDATED", "Field(s) updated: "+map2.keySet() , new Date(), "anonymous", entryId);
                entryApprovalTrailRepository.save(eat);
            }catch (Exception e){}
        } else {
            // bukan app yg sama
        }


        return entry;
    }

    public Page<EntryAttachment> findFilesById(long id, Pageable pageable) {
        return this.entryAttachmentRepository.findByEntryId(id, pageable);
    }

    @Transactional
    public Map<String, Object> undelete(long entryId, long trailId, String email) {
        int row = this.entryRepository.undeleteEntry(entryId);
        int r_approval=0;
        if (row>0){
            r_approval = this.entryApprovalRepository.undeleteEntry(entryId);
        }
        EntryApprovalTrail entryApprovalTrail = entryApprovalTrailRepository.getReferenceById(trailId);
        entryApprovalTrail.setStatus("Entry REMOVED history");
        entryApprovalTrailRepository.save(entryApprovalTrail);
        try {
            EntryApprovalTrail eat = new EntryApprovalTrail(null, null, "Entry RESTORED", "Entry restored by "+ email, new Date(), email, entryId);
            entryApprovalTrailRepository.save(eat);
        }catch (Exception e){}

        return Map.of("entry",row,"approval",r_approval);

    }


    record MailerHolder(Long mailerId, Tier tier) {}

    @Transactional
    public Entry actionApp(Long entryId, EntryApproval gaa, String email) {
        Entry entry = entryRepository.getReferenceById(entryId);
        entry = updateApprover(entry, email);
        Tier gat = tierRepository.getReferenceById(gaa.getTier().getId());

        Optional<User> user = userRepository.findFirstByEmailAndAppId(email, entry.getForm().getApp().getId());

        user.ifPresent(gaa::setApprover);

        gaa.setEntry(entry);
        gaa.setTier(gat);
        gaa.setTierId(gat.getId());
        gaa.setEmail(email);

        gaa.setTimestamp(new Date());
        entry.setCurrentTierId(gat.getId());

        int currentTier = Math.toIntExact(gat.getSortOrder());
//        List<Long> emailTemplate = new ArrayList<>();

        List<MailerHolder> emMap = new ArrayList<>();
//        Map<Long, Tier> emailTemplateMap = new HashMap<>();
//        Long emailTemplate = null;

        if (gat.getActions() != null) {
            if (gat.getActions().get(gaa.getStatus()) != null || gat.isAlwaysApprove()) {
                TierAction ta = gat.getActions().get(gaa.getStatus());

                if (ta != null) {
                    if ("nextTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;
                        System.out.println("+++++++++++++++++++++++++ Dlm isAlwaysApprove ++++++++++++++++++");

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null && !ta.isUserEdit()) {
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                                ngat.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, ngat));
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
//                            emailTemplateMap.put(ngat.getSubmitMailer(), ngat);
                            }
                        }

                    } else if ("prevTier".equals(ta.getAction())) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) - 1;

                    } else if ("goTier".equals(ta.getAction())) {
                        Tier t1 = tierRepository.getReferenceById(ta.getNextTier());
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

//                    ta.getMailer().forEach(i -> emailTemplateMap.put(i, gat));
                    ta.getMailer().forEach(i -> emMap.add(new MailerHolder(i, gat)));
                } else {
                    if (gat.isAlwaysApprove()) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;
                        System.out.println("+++++++++++++++++++++++++ Dlm isAlwaysApprove ++++++++++++++++++");

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null) { //&& !ta.isUserEdit()
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                                ngat.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, ngat));
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
//                            emailTemplateMap.put(ngat.getSubmitMailer(), ngat);
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
//        System.out.println("$$$$$$$$$GAT ID =:"+ gat.getId());

        entry = entryRepository.save(entry);

        EntryApprovalTrail eat = new EntryApprovalTrail(gaa);
        entryApprovalTrailRepository.save(eat);

        for (MailerHolder m : emMap) {
            triggerMailer(m.mailerId, entry, m.tier);
        }

        return entry;
    }


    public Map<String, Object> actionApps(List<Long> ids, EntryApproval gas, String email) {
        Map<String, Object> data = new HashMap<>();
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<String> failedMessage = new ArrayList<>();
        ids.forEach(id->{
            try {
                Entry e = actionApp(id,gas,email);
                success.getAndIncrement();
            }catch (Exception e){
                failed.getAndIncrement();
                failedMessage.add(e.getMessage());
            }
        });
        if (ids.size()==success.get()){
            data.put("success", "true");
        }else if (success.get()<ids.size() && success.get()>0){
            data.put("success","partial");
        }else if (ids.size()>0 && success.get()==0){
            data.put("success","false");
        }
        data.put("successCount", success.get());
        data.put("errorCount", failed.get());
        data.put("logs", failedMessage);
        return data;
    }

    @Transactional
    public Entry saveApproval(Long entryId, EntryApproval gaa, String email) {
        Entry entry = entryRepository.getReferenceById(entryId);
        Tier gat = tierRepository.getReferenceById(gaa.getTier().getId());
//        Long gatId = gat.getId();
        Optional<User> user = userRepository.findFirstByEmailAndAppId(email, entry.getForm().getApp().getId());

        user.ifPresent(gaa::setApprover);
        // Previously prevStatus is if prevStatus == 'nextTier' then move it back if the new update is prevTier or curTier.
        // But it no longer relevant since we are using sortOrder as index for tier movement
        // Planned for removal, and use actionApp to cater both action and save-approval.
        String prevStatus = entry.getApproval().get(gat.getId()).getStatus(); //why keep prevStatus??

        gaa.setEntry(entry);
        gaa.setTier(gat); //disetara dgn actionApp
        gaa.setTierId(gat.getId()); //disetara dgn actionApp
        gaa.setEmail(email); //disetara dgn actionApp

        gaa.setTimestamp(new Date()); //disetara dgn actionApp
        entry.setCurrentTierId(gat.getId()); //disetara dgn actionApp
        entryApprovalRepository.save(gaa);

        entry.getApproval().put(gat.getId(), gaa);
        entry = entryRepository.save(entry);

        int currentTier = Math.toIntExact(gat.getSortOrder());

//        Map<Long, Tier> emailTemplateMap = new HashMap<>();
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
//                            emailTemplateMap.put(ngat.getSubmitMailer(), ngat);
                            }
                        }

                    } else {
                        if (gat.getActions().get(prevStatus) != null && "nextTier".equals(gat.getActions().get(prevStatus).getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder());
                        }

                        if ("prevTier".equals(ta.getAction())) {
                            currentTier = Math.toIntExact(gat.getSortOrder()) - 1;

                        } else if ("goTier".equals(ta.getAction())) {
                            Tier t1 = tierRepository.getReferenceById(ta.getNextTier());
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

//                    ta.getMailer().forEach(i -> emailTemplateMap.put(i, gat));
                    ta.getMailer().forEach(i -> emMap.add(new MailerHolder(i, gat)));
                } else {
                    if (gat.isAlwaysApprove()) {
                        currentTier = Math.toIntExact(gat.getSortOrder()) + 1;

                        if (currentTier < entry.getForm().getTiers().size()) {
                            // if there's tier ahead, trigger notification of next tier
                            Tier ngat = entry.getForm().getTiers().get(currentTier);
                            if (ngat.getSubmitMailer() != null) {// && !ta.isUserEdit()
                                // if next tier has submitmailer, and nextStatus is SUBMITTED, trigger submitted (next tier) email
//                                ngat.getSubmitMailer().forEach(t -> emailTemplateMap.put(t, ngat));
                                ngat.getSubmitMailer().forEach(t -> emMap.add(new MailerHolder(t, ngat)));
//                            emailTemplateMap.put(ngat.getSubmitMailer(), ngat);
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

        EntryApprovalTrail eat = new EntryApprovalTrail(gaa);
        eat.setRemark("Approval updated");
        entryApprovalTrailRepository.save(eat);

        if (entry.getForm().getUpdateApprovalMailer() != null) {
//            emailTemplateMap.put(entry.getForm().getUpdateApprovalMailer(), gat);
            emMap.add(new MailerHolder(entry.getForm().getUpdateApprovalMailer(), gat));
        }

//        for (Map.Entry<Long, Tier> et : emailTemplateMap.entrySet()) {
//            triggerMailer(et.getKey(), entry, et.getValue());
//        }
        for (MailerHolder m : emMap) {
            triggerMailer(m.mailerId, entry, m.tier);
        }
        return entry;

    }

    @Transactional
    public void deleteEntry(Long id, String email) {
        bucketService.deleteFileByEntryId(id);
//        System.out.println("removedEntry:"+data);
//        List<EntryAttachment> eaList = entryAttachmentRepository.findByEntryId(id);
//        eaList.forEach(ea->{
//
//        });
        try {
            EntryApprovalTrail eat = new EntryApprovalTrail(null, null, "Entry REMOVED", "Entry removed by "+ email, new Date(), email, id);
            entryApprovalTrailRepository.save(eat);
        }catch (Exception e){}

        entryRepository.deleteById(id);
    }


    public void deleteEntries(List<Long> ids, String email) {
        ids.forEach(id->deleteEntry(id, email));
    }


    public Entry reset(Long id) {
        Entry entry = entryRepository.findById(id)
                .orElseThrow();

        entry.setCurrentTier(0);

        entry.setCurrentStatus(Entry.STATUS_SUBMITTED);

        return entryRepository.save(entry);
    }

    public Entry updateApproval(Long id, Long entryId, EntryApproval ea) {
        Entry entry = entryRepository.findById(entryId)
                .orElseThrow();

        ea.setEntry(entry);

        entry.getApproval().put(id, ea);

        return entryRepository.save(entry);
    }

    public Entry removeApproval(Long id, Long entryId) {
        Entry entry = entryRepository.findById(entryId).get();

        entry.getApproval().remove(id);
        entryApprovalRepository.deleteById(id);
        return entryRepository.save(entry);
    }

    public Entry submit(Long id) {
        Date dateNow = new Date();
        Entry entry = entryRepository.findById(id).get();

        entry.setSubmissionDate(dateNow);
        entry.setResubmissionDate(dateNow);

        entry.setCurrentTier(0);

        List<Long> mailer = null;
//        Long cb = null;
        Tier gat = null;
//        System.out.println("isEmpty::::"+entry.getForm().getTiers().isEmpty());
        if (!entry.getForm().getTiers().isEmpty()) {
            gat = entry.getForm().getTiers().get(0);
            mailer = gat.getSubmitMailer();
            entry = updateApprover(entry, entry.getEmail());
//            cb = gat.getSubmitCb();
        }
        entry.setCurrentStatus(Entry.STATUS_SUBMITTED);
        entry.setCurrentEdit(false);

        entry = entryRepository.save(entry);

//        logger.info("APPROVER Submit:"+ entry.getApprover());
        EntryApprovalTrail eat = new EntryApprovalTrail();
//        eat.setTier(gat);
        eat.setTimestamp(new Date());
        eat.setStatus(Entry.STATUS_SUBMITTED);
        eat.setRemark("SUBMITTED by User " + entry.getEmail());
        eat.setEmail(entry.getEmail());
        eat.setEntryId(id);

        entryApprovalTrailRepository.save(eat);

        if (mailer != null) {
            for (Long i : mailer) {
                triggerMailer(i, entry, gat);
            }
        }

//        mailer.forEach(t->);
//        ;

//        endpointService.runEndpoint(cb,null);

        return entry;

    }

    public Entry resubmit(Long id) {
        Date dateNow = new Date();
        Entry entry = entryRepository.findById(id).get();

        entry.setResubmissionDate(dateNow);

//        entry.setCurrentTier(0);

        List<Long> mailer = null;
//        Long cb = null;
        Tier gat = null;
        if (!entry.getForm().getTiers().isEmpty()) {
            gat = entry.getForm().getTiers().get(entry.getCurrentTier());
            mailer = gat.getResubmitMailer();
            entry = updateApprover(entry, entry.getEmail());
        }
        entry.setCurrentStatus(Entry.STATUS_RESUBMITTED);
        entry.setCurrentEdit(false);

        entry = entryRepository.save(entry);

        EntryApprovalTrail eat = new EntryApprovalTrail();
        eat.setTier(gat);
        eat.setTimestamp(new Date());
        eat.setStatus(Entry.STATUS_RESUBMITTED);
        eat.setRemark("RESUBMITTED by User " + entry.getEmail());
        eat.setEmail(entry.getEmail());
        eat.setEntryId(id);

        entryApprovalTrailRepository.save(eat);

//        assert mailer != null;
        if (mailer != null) {
            for (Long i : mailer) {
                triggerMailer(i, entry, gat);
            }
        }

//        endpointService.runEndpoint(cb, null);

        return entry;

    }

//    public Page<Entry> findActionByEmail(Long formId, String searchText, String email, List<String> status, Pageable pageable) {
//        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
//        return entryRepository.findActionByEmail(formId, searchText, email, status, pageable);
//    }
//find
//    public Page<Entry> getActionArchiveList(Long formId, String searchText, String email, List<String> status, Pageable pageable) {
//        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
//        return entryRepository.getActionArchiveList(formId, searchText, email, status, pageable);
//    }

    public long countEntry(Dataset dataset, String email) {
        long cn = 0L;

        long formId = dataset.getForm().getId();

        ObjectMapper mapper = new ObjectMapper();

//        Map<String, Object> dataMap = new HashMap<>();
//        dataMap.put("user", userRepository.findByEmail(email).get());
//        User user = userRepository.findByEmail(email).get();
//        Map userMap = mapper.convertValue(user, Map.class);
//        dataMap.put("user", userMap);
//        dataMap.put("now", Instant.now().toEpochMilli());

        Map<String, Object> dataMap = new HashMap<>();
        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, dataset.getApp().getId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Map userMap = mapper.convertValue(user, Map.class);
            dataMap.put("user", userMap);
        }
        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());


        Map presetFilters = mapper.convertValue(dataset.getPresetFilters(), HashMap.class);
        presetFilters.replaceAll((k, v) -> MailService.compileTpl(Optional.ofNullable(v).orElse("").toString(), dataMap));

//        HashMap filters = mapper.convertValue(dataset.getPresetFilters(), HashMap.class);
//        Form form = dataset.getForm();
//        Form prevForm = form.getPrev();

//        filters.putAll(mapper.convertValue(d.getPresetFilters(), HashMap.class));
//        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
        if (dataset.getType() != null) {
            return switch (dataset.getType()) {
                case "all" -> entryRepository.count(EntryFilter.builder()
                        .formId(formId)
                        .form(dataset.getForm())
                        .filters(presetFilters)
                        .status(mapper.convertValue(dataset.getStatusFilter(), HashMap.class))
//                                    .status(Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s", "").split(",")))
                        .build().filter());
//                    entryRepository.countAll(formId, "%",
//                    Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s","").split(",")));


                case "admin" -> entryRepository.count(EntryFilter.builder()
                        .formId(formId)
                        .form(dataset.getForm())
                        .filters(presetFilters)
                        .admin(email)
                        .status(mapper.convertValue(dataset.getStatusFilter(), HashMap.class))
//                            .status(Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s", "").split(",")))
                        .build().filter());
//                    entryRepository.countAdminByEmail(formId, "%", email,
//                    Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s","").split(",")));
                case "user" -> entryRepository.count(EntryFilter.builder()
                        .formId(formId)
                        .form(dataset.getForm())
                        .filters(presetFilters)
                        .email(email)
                        .status(mapper.convertValue(dataset.getStatusFilter(), HashMap.class))
//                                    .status(Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s", "").split(",")))
                        .build().filter());
//                    entryRepository.countUserByEmail(formId, "%", email,
//                    Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s","").split(",")));
                case "action" -> entryRepository.count(EntryFilter.builder()
                        .formId(formId)
                        .form(dataset.getForm())
                        .filters(presetFilters)
                        .approver(email)
                        .action(true)
                        .status(mapper.convertValue(dataset.getStatusFilter(), HashMap.class))
                        //                                    .status(Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s", "").split(",")))
                        .build().filter());

//                    return entryRepository.countActionByEmail(formId, "%", email
//                            Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").replaceAll("\\s", "").split(","))
//                    );
                default -> 0;
            };
        } else {
            return 0;
        }

    }

    public Map<String, Long> getStart(Long appId, String email) {
        Map<String, Long> data = new HashMap<>();

//        List<Dataset> dsList = datasetRepository.findByAppId(appId);
//        List<Screen> sList = screenRepository.findByAppId(appId);

        List<NaviGroup> group = appService.findNaviByAppIdAndEmail(appId, email);
//        Map<String, Long> items = new HashMap<>();
        List<Long> dsInNavi = new ArrayList<>();
        List<Long> sInNavi = new ArrayList<>();
        group.forEach(g -> {
            g.getItems().forEach(i -> {
                if ("dataset".equals(i.getType())) {
                    dsInNavi.add(i.getScreenId());
                }
                if ("screen".equals(i.getType())) {
                    sInNavi.add(i.getScreenId());
                }
            });
        });

        List<Dataset> dsList = datasetRepository.findByIds(dsInNavi);
        List<Screen> sList = screenRepository.findByIds(sInNavi);


        dsList.forEach(ds -> data.put(ds.getId() + "", countEntry(ds, email)));

        sList.forEach(s -> {
//            System.out.println(s.getId());
            if ("list".equals(s.getType()) && s.getDataset() != null) {
                data.put("screen_" + s.getId(), countEntry(s.getDataset(), email));
            }
        });

        return data;
    }

//    @Async("asyncExec")
//    public CompletableFuture<Map<String, Object>> updateApproverAllTierOld(Long formId) {
//        Map<String, Object> data = new HashMap<>();
//        Page<Entry> list = entryRepository.findByFormId(formId, PageRequest.of(0, Integer.MAX_VALUE));
//        list.getContent().forEach(e -> entryRepository.save(updateApprover(e, e.getEmail())));
////        entryRepository.saveAll(list);
//        return CompletableFuture.completedFuture(data);
//    }

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

        long start = System.currentTimeMillis();

//        Pageable pageRequest = PageRequest.of(0, 200);
//        Page<Entry> onePage = entryRepository.findByFormId(formId, pageRequest);
        try (Stream<Entry> entryStream = entryRepository.findByFormId(formId)) {
            entryStream.forEach(e -> {
                entryRepository.saveAndFlush(updateApprover(e, e.getEmail()));
                this.entityManager.detach(e);
            });
        }

//        while (!onePage.isEmpty()) {
//            List<Entry> newList = new ArrayList<>();
//            pageRequest = pageRequest.next();
////            System.out.println("$$$$$$->next()");
//            //DO SOMETHING WITH ENTITIES
//            onePage.forEach(e -> {
//                newList.add(updateApprover(e, e.getEmail()));
////                entryRepository.save(updateApprover(e, e.getEmail()));
//            });
//
//            entryRepository.saveAll(newList);
//
//            onePage = entryRepository.findByFormId(formId, pageRequest);
//
//        }
        long finish = System.currentTimeMillis();
        System.out.println("finish in:" + (finish - start));
        return CompletableFuture.completedFuture(data);
    }

    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> updateApproverBulk(Long formId, Long tierId, boolean updateApproved) {
//        Page<Entry> list = entryRepository.findByFormId(formId, PageRequest.of(0, Integer.MAX_VALUE));
        //Form form = formRepository.findById(formId).get();

        Map<String, Object> data = new HashMap<>();

//        List<Entry> newEntryList;

        final List<String> errors = new ArrayList<>();
//        final List<String> success = new ArrayList<>();
//        final List<String> notEmpty = new ArrayList<>();

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
                            if (e.getApproval().get(tierId) != null || updateApproved) {

                                entryRepository.saveAndFlush(updateApprover(e, e.getEmail()));
                                successTotal.getAndIncrement();

                                if (e.getApproval().get(tierId) != null) {
                                    notEmptyTotal.getAndIncrement();
//                                    notEmpty.add(e.getId() + ": Entry approval has been performed (forced approver update)");
                                }
                            } else {
                                notEmptyTotal.getAndIncrement();
//                                notEmpty.add(e.getId() + ": Entry approval has been performed (ignored)");
                            }
                        }
                    });

//                    success.add(e.getId() + ": Successfully re-set the approver");
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


        Form form = formRepository.findById(formId).get();

        String script = form.getItems().get(field).getF();
        ObjectMapper mapper = new ObjectMapper();

        final List<String> errors = new ArrayList<>();
        final List<String> success = new ArrayList<>();
        final List<String> notEmpty = new ArrayList<>();

        final AtomicInteger total = new AtomicInteger();

        try {

            HostAccess access = HostAccess.newBuilder(HostAccess.ALL).targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class))).build();
            ScriptEngine engine = GraalJSScriptEngine.create(null,
                    Context.newBuilder("js")
                            .allowHostAccess(access)
            );

            Resource resource = new ClassPathResource("dayjs.min.js");
            FileReader fr = new FileReader(resource.getFile());
            engine.eval(fr);

            CompiledScript compiled = ((Compilable) engine).compile("function fef($_,$user$){ var $ = JSON.parse(dataModel); var $prev$ = JSON.parse(prevModel); var $_ = JSON.parse(entryModel); return " + script + "}");

            long start = System.currentTimeMillis();

            Map<String, Map> userMap = new HashMap<>();

            System.out.println("##########%%%%%%%%%%");
            try (Stream<Entry> entryStream = entryRepository.findByFormId(formId)) {
                entryStream.forEach(e -> {
                    total.incrementAndGet();
                    Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

                    JsonNode node = e.getData();
                    if (force || node.get(field) == null || node.get(field).isNull()) {
                        if (e.getEmail() != null) { //even not null user still not found how?
                            Map user = null;

                            boolean userOk;
                            if (script.contains("$user$")) {
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
                                    }
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
                            errors.add("Entry " + e.getId() + ": No email");
                        }
                    } else {
                        notEmpty.add(e.getId() + ": Field not empty");
                    }
                });
            }

//            this.entryRepository.saveAll(newList);

            long finish = System.currentTimeMillis();
            System.out.println("completed in (stream + update):" + (finish - start));

        } catch (Exception e) {
            e.printStackTrace();
        }


        data.put("total", total.get());
        data.put("successCount", success.size());
//        data.put("successLog", success);
        data.put("errorCount", errors.size());
        data.put("errorLog", errors);
        data.put("notEmptyCount", notEmpty.size());
        data.put("notEmptyLog", notEmpty);
        data.put("success", true);

        return CompletableFuture.completedFuture(data);
    }


//    @Transactional(readOnly = true)
    public Map<String, Object> execVal2(Long formId, String field, boolean force) {
        Map<String, Object> data = new HashMap<>();

        data.put("total", 0);
        data.put("successCount", 0);
//        data.put("successLog", success);
        data.put("errorCount", 0);
        data.put("errorLog", 0);
        data.put("notEmptyCount", 0);
        data.put("notEmptyLog", 0);
        data.put("success", true);

        return data;
    }

    public Page<Entry> findListByDatasetCheck(Long datasetId, String searchText, String email, Map filters, List<String> sorts, List<Long> ids, boolean anonymous, Pageable pageable, HttpServletRequest req) {
//        System.out.println(filters);
        Optional<Dataset> dOpt = datasetRepository.findById(datasetId);
        if (dOpt.isPresent()) {
            Dataset d = dOpt.get();
            boolean isPublic = d.isPublicEp();
            if (anonymous && !isPublic) {
                // access to private dataset from public endpoint is not allowed
                throw new OAuth2AuthenticationProcessingException("Private Dataset: Access to private dataset from public endpoint is not allowed");
            } else {
                return findListByDataset(datasetId, searchText, email, filters, sorts, ids, pageable, req);
            }
        } else {
            throw new RuntimeException("Dataset not exist, ID=" + datasetId);
        }
    }

    public Stream<Entry> streamListByDatasetCheck(Long datasetId, String searchText, String email, Map filters, List<String> sorts, List<Long> ids, boolean anonymous, Pageable pageable, HttpServletRequest req) {
//        System.out.println(filters);
        Optional<Dataset> dOpt = datasetRepository.findById(datasetId);
        if (dOpt.isPresent()) {
            Dataset d = dOpt.get();
            boolean isPublic = d.isPublicEp();
            if (anonymous && !isPublic) {
                // access to private dataset from public endpoint is not allowed
                throw new OAuth2AuthenticationProcessingException("Private Dataset: Access to private dataset from public endpoint is not allowed");
            } else {
                return findListByDatasetStream(datasetId, searchText, email, filters, sorts, ids, req);
            }
        } else {
            throw new RuntimeException("Dataset not exist, ID=" + datasetId);
        }
    }

    public Page<Entry> findListByDataset(Long datasetId, String searchText, String email, Map filters, List<String> sorts, List<Long> ids, Pageable pageable, HttpServletRequest req) {

        if (searchText != null && searchText.isEmpty()) {
            searchText = null;
        }

        ObjectMapper mapper = new ObjectMapper();

        Dataset d = datasetRepository.getReferenceById(datasetId);

        Map<String, Object> dataMap = new HashMap<>();
        if (userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).isPresent()) {
            User user = userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).get();
            Map userMap = mapper.convertValue(user, Map.class);
            dataMap.put("user", userMap);
        }
        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());

//        Map<String, Object> dataMap = new HashMap<>();
//        dataMap.put("user", userRepository.findByEmail(email).get());
//        dataMap.put("now", Instant.now().toEpochMilli());


//        Map joinFilters = new HashMap();

        Map filtersReq = new HashMap();
        if (req != null) {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getKey().contains("$")) {
//                    System.out.println(entry.getKey() + ":"+ req.getParameter(entry.getKey()));
                    filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }
        }

        Map<String, Object> pF = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        Map presetFilters = pF.entrySet().stream()
                .filter(x -> x.getKey().startsWith("$"))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue() + ""));

        presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));

        final Map newFilter = new HashMap();

        if (filters != null) {
            newFilter.putAll(filters);
        }
//        if (filters != null) {
        if (d.getPresetFilters() != null) {
            newFilter.putAll(presetFilters);
        }
        if (filtersReq.size() > 0) {
            newFilter.putAll(filtersReq);
        }
//        }

//        System.out.println(newFilter);

        Map statusFilter = mapper.convertValue(d.getStatusFilter(), HashMap.class);

        List<String> sortFin = new ArrayList<>();

        Optional.ofNullable(sorts).ifPresent(sortFin::addAll);

        if (d.getDefSortField() != null) {
            sortFin.add(d.getDefSortField() + "~" + (d.getDefSortDir() != null ? d.getDefSortDir() : "asc"));
        }

        return switch (d.getType()) {
            case "all" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter(), pageable); // entryRepository.findAll(formId, searchText, status, pageable);
            case "admin" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .admin(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter(), pageable); // entryRepository.findAdminByEmail(formId, searchText, email, status, pageable);
            case "user" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .email(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter(), pageable); //findUserByEmail(formId, searchText, email, status, pageable);
            case "action" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .approver(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .action(true)
                    .build().filter(), pageable); //findUserByEmail(formId, searchText, email, status, pageable);
//                    return entryRepository.findActionByEmail(d.getForm().getId(), searchText, email, pageable);
            default -> null;
        };
    }

    public Stream<Entry> findListByDatasetStream(Long datasetId, String searchText, String email, Map filters, List<String> sorts, List<Long> ids, HttpServletRequest req) {

        if (searchText != null && searchText.isEmpty()) {
            searchText = null;
        }

        ObjectMapper mapper = new ObjectMapper();

        Dataset d = datasetRepository.getReferenceById(datasetId);

        Map<String, Object> dataMap = new HashMap<>();
        if (userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).isPresent()) {
            User user = userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).get();
            Map userMap = mapper.convertValue(user, Map.class);
            dataMap.put("user", userMap);
        }
        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());

//        Map<String, Object> dataMap = new HashMap<>();
//        dataMap.put("user", userRepository.findByEmail(email).get());
//        dataMap.put("now", Instant.now().toEpochMilli());


//        Map joinFilters = new HashMap();

        Map filtersReq = new HashMap();
        if (req != null) {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getKey().contains("$")) {
//                    System.out.println(entry.getKey() + ":"+ req.getParameter(entry.getKey()));
                    filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }
        }

        Map<String, Object> pF = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        Map presetFilters = pF.entrySet().stream()
                .filter(x -> x.getKey().startsWith("$"))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue() + ""));

        presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));

        final Map newFilter = new HashMap();

//        Optional.ofNullable(filters).ifPresent(newFilter::putAll);
//        Optional.ofNullable(presetFilters).ifPresent(newFilter::putAll);
//        Optional.ofNullable(filtersReq).ifPresent(newFilter::putAll);
        if (filters != null) {
            newFilter.putAll(filters);
        }
        if (d.getPresetFilters() != null) {
            newFilter.putAll(presetFilters);
        }
        if (filtersReq.size() > 0) {
            newFilter.putAll(filtersReq);
        }

        Map statusFilter = mapper.convertValue(d.getStatusFilter(), HashMap.class);

        List<String> sortFin = new ArrayList<>();

        Optional.ofNullable(sorts).ifPresent(sortFin::addAll);

        if (d.getDefSortField() != null) {
            sortFin.add(d.getDefSortField() + "~" + (d.getDefSortDir() != null ? d.getDefSortDir() : "asc"));
        }

//        sortFin.addAll(sorts);

        return switch (d.getType()) {
            case "all" -> customEntryRepository.streamAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter()); // entryRepository.findAll(formId, searchText, status, pageable);
            case "admin" -> customEntryRepository.streamAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .admin(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter()); // entryRepository.findAdminByEmail(formId, searchText, email, status, pageable);
            case "user" -> customEntryRepository.streamAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .email(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter()); //findUserByEmail(formId, searchText, email, status, pageable);
            case "action" -> customEntryRepository.streamAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .approver(email)
                    .status(statusFilter)
                    .sort(sortFin)
                    .ids(ids)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .action(true)
                    .build().filter()); //findUserByEmail(formId, searchText, email, status, pageable);
//                    return entryRepository.findActionByEmail(d.getForm().getId(), searchText, email, pageable);
            default -> null;
        };
    }

    public Long countByDataset(Long datasetId, String searchText, String email, Map filters, HttpServletRequest req) {
        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
//        Form form = formService.findFormById(formId);

        ObjectMapper mapper = new ObjectMapper();

        Dataset d = datasetRepository.getReferenceById(datasetId);

//        List<String> status = Arrays.asList(d.getStatus().split(","));

        Map<String, Object> dataMap = new HashMap<>();
        if (userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).isPresent()) {
            User user = userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).get();
            Map userMap = mapper.convertValue(user, Map.class);
            dataMap.put("user", userMap);
        }
        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());

//        Map<String, Object> dataMap = new HashMap<>();
//        dataMap.put("user", userRepository.findByEmail(email).get());
//        dataMap.put("now", Instant.now().toEpochMilli());


//        Map joinFilters = new HashMap();

        Map filtersReq = new HashMap();
        if (req != null) {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getKey().contains("$")) {
                    filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }
        }

//        Map presetFilters = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        Map<String, Object> pF = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        Map presetFilters = pF.entrySet().stream()
                .filter(x -> x.getKey().startsWith("$"))
                .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

        presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));

        final Map newFilter = new HashMap();
        if (filters != null) {
            newFilter.putAll(filters);
        }
//        if (filters != null) {
        if (d.getPresetFilters() != null) {
            newFilter.putAll(presetFilters);
        }
        if (filtersReq.size() > 0) {
            newFilter.putAll(filtersReq);
        }
//        }

        Map statusFilter = mapper.convertValue(d.getStatusFilter(), HashMap.class);
//        statusFilter.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));
//
//        if (d.isCanFilterStatus()){
//            statusFilter = status;
//        }
//        if (status != null) {
//            if (d.getStatusFilter() != null){
//                statusFilter.putAll(status);
////                status.putAll(statusFilter);
//            }
//        }

//        System.out.println(joinFilters);
//        if ("db".equals(d.getForm().getType())) {

        return switch (d.getType()) {
            case "all" -> entryRepository.count(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter()); // entryRepository.findAll(formId, searchText, status, pageable);
            case "admin" -> entryRepository.count(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .admin(email)
                    .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter()); // entryRepository.findAdminByEmail(formId, searchText, email, status, pageable);
            case "user" -> entryRepository.count(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .email(email)
                    .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .build().filter()); //findUserByEmail(formId, searchText, email, status, pageable);
            case "action" -> entryRepository.count(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .approver(email)
                    .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(newFilter)
                    .action(true)
                    .build().filter()); //findUserByEmail(formId, searchText, email, status, pageable);
//                    return entryRepository.findActionByEmail(d.getForm().getId(), searchText, email, pageable);
            default -> null;
        };

//        }

    }

    public Page<Entry> findListNormalizedNgxChart(String type, Long datasetId, String searchText, String email, List<String> status, Map filters, Pageable pageable, HttpServletRequest req) {
        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
        ObjectMapper mapper = new ObjectMapper();
        Dataset d = datasetRepository.getReferenceById(datasetId);

        Form form = d.getForm();

        Page<Entry> paged = switch (type) {
            case "all" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
//                            .status(status)
                    .filters(filters)
                    .build().filter(), pageable);
            case "admin" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .admin(email)
//                            .status(status)
                    .filters(filters)
                    .build().filter(), pageable);
            case "user" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .email(email)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
//                            .status(status)
                    .filters(filters)
                    .build().filter(), pageable);
            case "action" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(d.getForm().getId())
                    .form(d.getForm())
                    .searchText(searchText)
                    .approver(email)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(filters)
                    .action(true)
                    .build().filter(), pageable);
            default -> null;
        };


        List list = paged.getContent().stream()
                .map(entry -> {
                    List<Map> maplist = new ArrayList<>();

                    form.getSections().forEach(s -> {
                        Map<String, Object> sm = new HashMap<>();


                        List<Map> serie = s.getItems().stream()
                                .filter(si -> form.getItems().get(si.getCode()) != null)
                                .filter(si -> !form.getItems().get(si.getCode()).isReadOnly())
                                .map(si -> {
                                    Map<String, Object> smap = new HashMap<>();
                                    smap.put("name", form.getItems().get(si.getCode()).getLabel());
                                    smap.put("value", entry.getData().get(si.getCode()));
                                    return smap;
                                })
                                .collect(Collectors.toList());
                        // dont add the serie if child items is all read-only
                        if (serie.size() != 0) {
                            sm.put("name", s.getTitle());
                            sm.put("series", serie);
                            maplist.add(sm);
                        }
                    });
                    return maplist;

                })
                .collect(Collectors.toList());
        return new PageImpl<>(list);
    }


    public Page<Entry> findListNormalizedNgxChartSeries(String type, Long datasetId, String searchText, String email, List<String> status, Map filters, String serieCol, Pageable pageable, HttpServletRequest request) {
        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
        ObjectMapper mapper = new ObjectMapper();
        Dataset d = datasetRepository.getReferenceById(datasetId);
        Form form = d.getForm();

        Page<Entry> paged = switch (type) {
            case "all" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
//                            .status(status)
                    .filters(filters)
                    .build().filter(), pageable);
            case "admin" -> entryRepository.findAll(EntryFilter.builder().formId(form.getId()).form(form).searchText(searchText).admin(email).status(mapper.convertValue(d.getStatusFilter(), HashMap.class)).filters(filters).build().filter(), pageable);
            case "user" -> entryRepository.findAll(EntryFilter.builder().formId(form.getId()).form(form).searchText(searchText).email(email).status(mapper.convertValue(d.getStatusFilter(), HashMap.class)).filters(filters).build().filter(), pageable);
            case "action" -> entryRepository.findAll(EntryFilter.builder().formId(form.getId()).form(form).searchText(searchText).approver(email).action(true).status(mapper.convertValue(d.getStatusFilter(), HashMap.class)).filters(filters).build().filter(), pageable);
            default -> null;
        };
        List list = paged.getContent().stream()
                .filter(e -> e.getData() != null)
                .filter(e -> e.getData().get(serieCol) != null)
                .map(e -> {
//                        String serieName = e.getData().get(serieCol).asText();

                    Map<String, Object> sm = new HashMap<>();

                    List<Map> serie = form.getItems().keySet().stream()
                            .filter(f -> form.getItems().get(f) != null)
                            .filter(f -> !form.getItems().get(f).isReadOnly())
                            .map(f ->
                                    Map.of("name", form.getItems().get(f).getLabel(),
                                            "value", e.getData().get(f))
                            )
                            .collect(Collectors.toList());

                    sm.put("name", e.getData().get(serieCol));
                    sm.put("series", serie);

                    return sm;

                })
                .collect(Collectors.toList());

        return new PageImpl<>(list);
    }

    public Map findListNormalizedNgxEchartSeries(String type, Long datasetId, String searchText, String email, Map filters, String serieCol, List<String> exclude, Pageable pageable, HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        searchText = searchText.isEmpty() ? "%" : "%" + searchText.toUpperCase() + "%";
        Dataset d = datasetRepository.getReferenceById(datasetId);
        Form form = d.getForm();

//        System.out.println("##@@@!!!$$$::::"+filters.toString());

        Page<Entry> paged = switch (type) {
            case "all" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(filters)
                    .build().filter(), pageable);
            case "admin" -> entryRepository.findAll(EntryFilter.builder().formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .admin(email)
//                            .status(status)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(filters)
                    .build().filter(), pageable);
            case "user" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .email(email)
//                            .status(status)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(filters).build().filter(), pageable);
            case "action" -> entryRepository.findAll(EntryFilter.builder()
                    .formId(form.getId())
                    .form(form)
                    .searchText(searchText)
                    .approver(email)
                    .action(true)
//                            .status(status)
                    .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                    .filters(filters).build().filter(), pageable);
            default -> null;
        };


        Map envelope = new HashMap();
        final List list = new ArrayList();

//        List<String> category = new ArrayList<>();
//        boolean done = false;


        if (paged != null) {
            List<Entry> entryList = paged.getContent();

            List<String> field = new ArrayList<>();
            field.add(serieCol);

            List<String> header = new ArrayList<>();

            header.add(
                    form
                            .getItems()
                            .get(serieCol)
                            .getLabel()
            );

            form.getSections().forEach(s -> {
                field.addAll(
                        s.getItems().stream()
                                .filter(si -> form.getItems().get(si.getCode()) != null)
                                .filter(si -> !form.getItems().get(si.getCode()).isReadOnly())
                                .filter(si -> !exclude.contains(si.getCode()))
                                .filter(si -> !si.getCode().equals(serieCol))
                                .map(SectionItem::getCode)
                                .collect(toList())
                );
                header.addAll(
                        s.getItems().stream()
                                .filter(si -> form.getItems().get(si.getCode()) != null)
                                .filter(si -> !form.getItems().get(si.getCode()).isReadOnly())
                                .filter(si -> !exclude.contains(si.getCode()))
                                .filter(si -> !si.getCode().equals(serieCol))
                                .map(si -> form.getItems().get(si.getCode()).getLabel() + " - " +
                                        s.getTitle())
                                .collect(toList())
                );
            });


            List<List> data = entryList.stream()
                    .filter(e -> e.getData() != null)
//                    .filter(e -> e.getData().get(si.getCode()) != null)
                    .map(e -> field.stream()
                            .map(h -> {

//                                System.out.println("h$$$$$$$$$$$:" + h);
//                                System.out.println("serieCol$$$$$$$$$$$:" + serieCol);
                                if (h.equals(serieCol)) {
                                    if (Arrays.asList("select", "radio").contains(form.getItems().get(serieCol).getType())) {
                                        return e.getData().get(serieCol).get("name").asText();
                                    } else {
                                        return e.getData().get(serieCol).asText();
                                    }
//
                                } else {
                                    return e.getData().get(h);
                                }
                            })
                            .collect(toList()))
                    .collect(toList());

            List res = new ArrayList();
            res.add(header);
            res.addAll(data);

            envelope.put("dataset", res);


        }
        return envelope;
    }

    public Map getDashboardDataNativeNew(Long dashboardId, Map<String, Object> filters, String email, HttpServletRequest req) {

//        ObjectMapper mapper = new ObjectMapper();

        Dashboard dashboard = dashboardService.getDashboard(dashboardId);
        Map<Object, Object> data = new HashMap<>();
        dashboard.getCharts().stream()
//                .filter(c-> "db".equals(c.getSourceType()))
                .forEach(c -> {
                    data.put(c.getId(), getChartDataNative(c.getId(), filters, email, req));
                });

        return data;
    }


    public Map getDashboardMapDataNativeNew(Long dashboardId, Map<String, Object> filters, String email, HttpServletRequest req) {

//        ObjectMapper mapper = new ObjectMapper();

        Dashboard dashboard = dashboardService.getDashboard(dashboardId);
        Map<Object, Object> data = new HashMap<>();
        dashboard.getCharts()
//                .filter(c-> "db".equals(c.getSourceType()))
                .forEach(c -> {
                    data.put(c.getId(), getChartMapDataNative(c.getId(), filters, email, req));
                });

        return data;
    }


    public Map getChartDataNativeOld(Long chartId, Map<String, Object> filters, String email, HttpServletRequest req) {

        ObjectMapper mapper = new ObjectMapper();

        Chart c = dashboardService.getChart(chartId);

        // Create DataMap for {{user}},etc
        Map<String, Object> tplDataMap = new HashMap<>();
        if (userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).isPresent()) {
            User user = userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).get();
            Map userMap = mapper.convertValue(user, Map.class);
            tplDataMap.put("user", userMap);
        } else {
        }
        tplDataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        tplDataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        tplDataMap.put("todayEnd", calendarEnd.getTimeInMillis());


        Map<Object, Object> data = new HashMap<>();

        Map<String, String> jsonRootMap = Map.of("data", "e.data", "prev", "e2.data", "approval", "eac.data");

        if ("db".equals(c.getSourceType())) {

            Map<String, String> statusFilter = mapper.convertValue(c.getStatusFilter(), HashMap.class);

            List<String> cond = new ArrayList<>();

            statusFilter.keySet().forEach(s -> {
                if (statusFilter.get(s) != null && !statusFilter.get(s).isEmpty()) {
                    if ("-1".equals(s)) {

                        cond.add("(e.current_tier_id is null and e.current_status in ('" + statusFilter.get(s).replace(",", "','") + "'))");
                    } else {
                        cond.add("(e.current_tier_id = " + s + " and e.current_status in ('" + statusFilter.get(s).replace(",", "','") + "'))");
                    }
                }
            });
            String statusCond = "(" + String.join(" or ", cond) + ")";

            List<String> pred = new ArrayList<>();
            String filterCond = "";

//            Map filters = new HashMap();
            Map<String, Object> filtersReq = new HashMap();
            if (req != null) {
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    if (entry.getKey().contains("$")) {
                        filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                    }
                }
            }
            Map<String, Object> pF = mapper.convertValue(c.getPresetFilters(), HashMap.class);
            Map<String, Object> presetFilters = pF.entrySet().stream()
                    .filter(x -> x.getKey().startsWith("$"))
                    .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

            presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), tplDataMap));

//            System.out.println("presetFilter:"+presetFilters);

            Map<String, Object> filtersNew = new HashMap<>();

//            if (filtersNew != null) {
            if (c.getPresetFilters() != null) {
                filtersNew.putAll(presetFilters);
            }
            if (filtersReq.size() > 0) {
                filtersNew.putAll(filtersReq);
            }
            if (filters.size() > 0) {
                filtersNew.putAll(filters);
            }

            if (!filtersNew.isEmpty()) {
                filtersNew.keySet().forEach(f -> {
                    if (filtersNew.get(f) != null) {

                        String[] splitted = f.split("~");

                        String type = splitted[0]; // $range,$bool or $str
                        String rootCol = jsonRootMap.get(splitted[1]);//splitted[1]=="data"?"e.data":"e2.data"; // data or prev
                        String col = splitted[2]; // from, to

                        if (f.contains("$range~")) {
                            if ("from".equals(splitted[3])) {
                                pred.add(" (json_value(" + rootCol + ",'$." + col + "') >= " + filtersNew.get(f) + ") ");
                            }
                            if ("to".equals(splitted[3])) {
                                pred.add(" (json_value(" + rootCol + ",'$." + col + "') <= " + filtersNew.get(f) + ") ");
                            }
                        } else if (f.contains("$bool~")) {
                            if (Boolean.parseBoolean(filtersNew.get(f) + "")) {
                                pred.add(" (json_value(" + rootCol + ",'$." + col + "') is true) ");
                            } else {
                                pred.add(" (json_value(" + rootCol + ",'$." + col + "') is false or json_value(" + rootCol + ",'$." + col + "') is null) ");
                            }
                        } else if (f.contains("$str~")) {
                            pred.add(" (upper(json_value(" + rootCol + ",'$." + col + "')) like upper('" + filtersNew.get(f) + "'))");
                        } else {
                            pred.add(" (json_value(" + rootCol + ",'$." + col + "') like '" + filtersNew.get(f) + "')");
                        }
                    }

                });
                filterCond = " AND (" + String.join(" and ", pred) + ")";
            }


            String[] code = c.getFieldCode().split("#");

            String jsonRoot = jsonRootMap.get(code[0]);// code[0]=="data"?"e.data":"e2.data"; //"e." + code[0];
            String field = code[1];


            String codeSql = "coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a')";
            String codeApprovalJoin = "";
            String codeApprovalTierId = "";
            if ("approval".equals(code[0])) {
                String[] codeSplitted = code[1].split("\\.", 2);
                codeApprovalJoin = " left join entry_approval eac on e.id = eac.entry ";
                codeApprovalTierId = " and eac.tier = " + codeSplitted[0];
                jsonRoot = "eac.data";
                field = codeSplitted[1];
                codeSql = "coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a')";
            }


            String distinct = "count".equals(c.getAgg()) ? "distinct" : "";
            String[] value = c.getFieldValue().split("#");
//        String valueSql = "coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a')";
            String valueSql = "(" + distinct + " json_value(" + jsonRootMap.get(value[0]) + ", '$." + value[1] + "'))";
            String valueApprovalJoin = "";
            String valueApprovalTierId = "";
            if ("approval".equals(value[0])) {
                String[] valueSplitted = value[1].split("\\.", 2);
                valueApprovalJoin = " left join entry_approval eav on e.id = eav.entry ";
                valueApprovalTierId = " and eav.tier = " + valueSplitted[0];
                jsonRoot = "eav.data";
                field = valueSplitted[1];
                valueSql = "(" + distinct + " json_value(" + jsonRoot + ", '$." + field + "'))";
            }


            String[] series;
            String seriesSql;
            String seriesJoin = "";
            String seriesTierId = "";
            if (c.isSeries()) {
                series = c.getFieldSeries().split("#");
                if ("approval".equals(series[0])) {
                    String[] codeSplitted = series[1].split("\\.", 2);
                    seriesJoin = " left join entry_approval es on e.id = es.entry ";
                    seriesTierId = " and es.tier = " + codeSplitted[0];
                    seriesSql = "coalesce(json_value(es.data" + ", '$." + codeSplitted[1] + "'), 'n/a')";

                } else {
                    seriesSql = "coalesce(json_value(" + jsonRootMap.get(series[0]) + ", '$." + series[1] + "'), 'n/a')";
                }
                codeSql = "concat(coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a'),':'," + seriesSql + ")";
            }

            String prevJoin = " left join entry e2 on e.prev_entry = e2.id ";
//
            String sql = "select " + codeSql + " as name," +
//                " " + c.getAgg() + "(distinct json_value(e." + value[0] + ", '$." + value[1] + "')) as value" +
                    " " + c.getAgg() + valueSql + " as value" +
                    " from entry e " + codeApprovalJoin + valueApprovalJoin + seriesJoin + prevJoin +
                    " where e.form=" + c.getForm().getId() +
                    codeApprovalTierId + valueApprovalTierId + seriesTierId +
                    " and " + statusCond +
                    filterCond +
                    " group by " + codeSql +
                    " order by " + codeSql + " ASC";

//            System.out.println("CHART##NATIVE##SQL:::" + sql);

            try {
                if (c.isSeries()) {
                    /*
                     * 2011.F, 10
                     * 2011.M: 12,
                     * 2012.F: 8,
                     * 2012.M: 6
                     *
                     * convert to
                     * series, F,    M
                     * 2011,  10  , 12
                     * 2012,  12  , 6
                     * */

                    List<Object[]> result = dynamicSQLRepository.runQuery(sql, true);

//                            Map<String, Map<String, Object>> matrix0 = new HashMap<>();
                    Set<String> aCat = new HashSet<>();
                    Set<String> aSeries = new HashSet<>();

                    result.forEach(d -> {
                        String[] n = d[0].toString().split(":"); //split series.category
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
                    //                                System.out.println("cat:"+cat);
                    header.addAll(listACat);

//                            header.sort((a, b) -> Double.compare(b, a));
                    dataset.add(header);


                    // add data to dataset
                    listASeries.forEach(ser -> {
                        List<Object> row = new ArrayList<>();
//                                System.out.println("ser:"+ser);
                        row.add(ser);
                        listACat.forEach(cat -> row.add(Optional.ofNullable(dataMap.get(cat + ":" + ser)).orElse(0)));
                        dataset.add(row);
                    });

                    if (c.isShowAgg()) {
                        List<Object> totalRow = new ArrayList();
                        List<Object> totalColumn = new ArrayList();

                        totalRow.add("Total");
                        listASeries.forEach(ser -> {
                            List<Double> row = new ArrayList<>();
                            listACat.forEach(cat -> row.add(((Number) Optional.ofNullable(dataMap.get(cat + ":" + ser)).orElse(0)).doubleValue()));
                            totalRow.add(row.stream().reduce(0d, Double::sum));
                        });

                        totalColumn.add("Total");
                        listACat.forEach(cat -> {
                            List<Double> col = new ArrayList<>();
                            listASeries.forEach(ser -> col.add(((Number) Optional.ofNullable(dataMap.get(cat + ":" + ser)).orElse(0)).doubleValue()));
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
//                                                                        .mapToDouble(Double::valueOf).sum());
                    }
                    data.put("data", dataset); //

                } else {
                    List<Map<String, Object>> d1 = dynamicSQLRepository.runQuery(sql, true)
                            .stream().map(e -> {
                                Map<String, Object> map = new HashMap<>();
                                map.put("name", e[0]);
                                map.put("value", e[1]);
                                return map;
                            })
                            .collect(toList());
                    data.put("data", d1);
                    if (c.isShowAgg()) {
                        Double sum = d1.stream().map(e -> ((Number) e.get("value")).doubleValue())
                                .mapToDouble(Double::valueOf)
                                .sum();
                        data.put("_a", sum); //_a
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
//                });

            return data;
        } else if ("rest".equals(c.getSourceType())) {
//            ObjectMapper mapper = new ObjectMapper();

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

    public Map<String, Object> _chartizeDbData(String __agg,
                                               String __codeField,
                                               String __valueField,
                                               boolean __isSeries,
                                               String __seriesField,
                                               boolean __showAgg,
                                               Form __form, User __user,
                                               JsonNode __status,
                                               Map<String, Object> __filters) {

        ObjectMapper mapper = new ObjectMapper();

//        Chart c = dashboardService.getChart(chartId);

        // Create DataMap for {{user}},etc
        Map<String, Object> tplDataMap = new HashMap<>();
        if (__user != null) {
//            User user = userRepository.findByEmailAndAppId(email, c.getDashboard().getApp().getId()).get();
            Map userMap = mapper.convertValue(__user, Map.class);
//            System.out.println("#user#:"+userMap);
            tplDataMap.put("user", userMap);
        }

        tplDataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        tplDataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        tplDataMap.put("todayEnd", calendarEnd.getTimeInMillis());


        Map<String, Object> data = new HashMap<>();

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
            __filters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), tplDataMap));
        }

        if (!Helper.isNullOrEmpty(__filters)) {
            __filters.keySet().forEach(f -> {
                if (__filters.get(f) != null) {
                    String[] splitted1 = f.split("\\.");

                    String rootCol = splitted1[0]; // $, $prev$, $_, $$, $$_

//                        String realRoot = rootCol.replace("$$", "approval")
//                                .replace("$prev$", "prev")
//                                .replace("$", "data");


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
                            } else if (Arrays.asList("date", "number", "scaleTo10", "scaleTo5").contains(form.getItems().get(fieldCode).getType())) {
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

                            } else if (List.of("checkbox").contains(form.getItems().get(fieldCode).getType())) {
                                if (Boolean.valueOf(__filters.get(f) + "")) {
                                    pred.add(" (json_value(" + predRoot + ",'$." + fieldFull + "') is true) ");
                                } else {
                                    pred.add(" (json_value(" + predRoot + ",'$." + fieldFull + "') is false or json_value(" + rootCol + ",'$." + fieldFull + "') is null) ");
                                }
                            } else if (List.of("text").contains(form.getItems().get(fieldCode).getType())) {
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

        String jsonRoot = jsonRootMap.get(code[0]);// code[0]=="data"?"e.data":"e2.data"; //"e." + code[0];
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
        if ("$_".equals(code[0])){
            codeSql = "coalesce(" + jsonRoot + "." + field + ", 'n/a')";
        }

        String distinct = "count".equals(__agg) ? "distinct" : "";
//        String[] value = __valueField.split("#");

        // $.name -> e.data, $.name
        // $prev$.name -> e2.data, $.name
        //$$.401.name -> eac, $.name
        String[] value = __valueField.split("[#.]", 2); // split $$, 401.name

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
        if ("$_".equals(value[0])){
//            valueStatusJoin = "left join "
            codeSql = "coalesce(" + jsonRoot + "." + field + ", 'n/a')";
        }



        String[] series;
        String seriesSql;
        String seriesJoin = "";
        String seriesTierId = "";
        if (__isSeries) {
            series = __seriesField.split("[#.]", 2);
            if ("approval".equals(series[0])) {
                String[] codeSplitted = series[1].split("\\.", 2);
                seriesJoin = " left join entry_approval es on e.id = es.entry ";
                seriesTierId = " and es.tier = " + codeSplitted[0];
                seriesSql = "coalesce(json_value(es.data" + ", '$." + codeSplitted[1] + "'), 'n/a')";

            } else if ("$_".equals(series[0])){
                seriesSql = "coalesce(" + jsonRootMap.get(series[0]) + "." + series[1] + ", 'n/a')";
            } else {
                seriesSql = "coalesce(json_value(" + jsonRootMap.get(series[0]) + ", '$." + series[1] + "'), 'n/a')";
            }
            codeSql = "concat(coalesce(json_value(" + jsonRoot + ", '$." + field + "'), 'n/a'),':'," + seriesSql + ")";
        }

        String prevJoin = " left join entry e2 on e.prev_entry = e2.id ";

        String sql = "select " + codeSql + " as name, " +
                __agg + valueSql + " as value " +
                " from entry e " + codeApprovalJoin + valueApprovalJoin + seriesJoin + prevJoin +
                " where e.form=" + __form.getId() +
                codeApprovalTierId + valueApprovalTierId + seriesTierId +
                statusCond +
                filterCond +
                " group by " + codeSql +
                " order by " + codeSql + " ASC";
        try {
            List<Object[]> result = dynamicSQLRepository.runQuery(sql, true);
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
             * 2012,  12  , 6
             * */

            Set<String> aCat = new HashSet<>();
            Set<String> aSeries = new HashSet<>();

            result.forEach(d -> {
                String[] n = d[0].toString().split(":"); //split series.category
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
                listACat.forEach(cat -> row.add(Optional.ofNullable(dataMap.get(cat + ":" + ser)).orElse(0)));
                dataset.add(row);
            });

            if (__showAgg) {
                List<Object> totalRow = new ArrayList();
                List<Object> totalColumn = new ArrayList();

                totalRow.add("Total");
                listASeries.forEach(ser -> {
                    List<Double> row = new ArrayList<>();
                    listACat.forEach(cat -> row.add(((Number) Optional.ofNullable(dataMap.get(cat + ":" + ser)).orElse(0)).doubleValue()));
                    totalRow.add(row.stream().reduce(0d, Double::sum));
                });

                totalColumn.add("Total");
                listACat.forEach(cat -> {
                    List<Double> col = new ArrayList<>();
                    listASeries.forEach(ser -> col.add(((Number) Optional.ofNullable(dataMap.get(cat + ":" + ser)).orElse(0)).doubleValue()));
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


//                                                                        .mapToDouble(Double::valueOf).sum());
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

    public record ChartizeObj(String agg, String by, String value,
                              String series, boolean showAgg,
                              JsonNode status, Map<String, Object> filter) {
    }

    /**
     * Untuk LAMBDA
     */
    public Map<String, Object> chartize(Long formId, Map cm, String email, Lambda lambda) {
        ObjectMapper mapper = new ObjectMapper();

        ChartizeObj c = mapper.convertValue(cm, ChartizeObj.class);

        User user = null;
        if (userRepository.findFirstByEmailAndAppId(email, lambda.getApp().getId()).isPresent()) {
            user = userRepository.findFirstByEmailAndAppId(email, lambda.getApp().getId()).get();
        }

        Form form = formRepository.getReferenceById(formId);
        return _chartizeDbData(c.agg, c.by, c.value, !Helper.isNullOrEmpty(c.series), c.series, c.showAgg, form, user, c.status, c.filter);

    }

    public Map<String, Object> getChartDataNative(Long chartId, Map<String, Object> filters, String email, HttpServletRequest req) {

        ObjectMapper mapper = new ObjectMapper();

        Chart c = dashboardService.getChart(chartId);

        User user = null;
        if (userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).isPresent()) {
            user = userRepository.findFirstByEmailAndAppId(email, c.getDashboard().getApp().getId()).get();
        }


        Map<String, Object> data = new HashMap<>();


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
            Map<String, Object> presetFilters = pF.entrySet().stream()
                    .filter(x -> x.getKey().startsWith("$"))
                    .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

//            presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), tplDataMap));

            Map<String, Object> filtersNew = new HashMap<>();

            Optional.ofNullable(presetFilters).ifPresent(filtersNew::putAll);
            Optional.ofNullable(filtersReq).ifPresent(filtersNew::putAll);
            Optional.ofNullable(filters).ifPresent(filtersNew::putAll);

            return _chartizeDbData(c.getAgg(), c.getFieldCode(), c.getFieldValue(), c.isSeries(),
                    c.getFieldSeries(), c.isShowAgg(), c.getForm(), user, c.getStatusFilter(), filtersNew);

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

    /*
     * Mungkin Return {"series":{"code":value}} atau {"code":"value"}
     * */

    public Map<String, Object> getChartMapDataNative(Long chartId, Map<String, Object> filtersNew, String email, HttpServletRequest req) {
        Map<String, Object> cdata = getChartDataNative(chartId, filtersNew, email, req);
        return chartAsMap(cdata);
//        Chart c = dashboardService.getChart(chartId);
//        if (c.isSeries()) {
//            List<Object> _arow = (List<Object>) cdata.get("_arow");
//            List<List<Object>> data = (List<List<Object>>) cdata.get("data");
//            List<Object> column = data.get(0);
////            List<Map<String, Object>> result = new ArrayList<>();
//            Map<String, Object> result = new HashMap<>();
//            for (int i = 1; i < data.size(); i++) { // skip header row
//                Map<String, Object> m = new HashMap<>();
//                List<Object> row = data.get(i);
//                String series = row.get(0)+"";
//                for (int j = 1; j < row.size(); j++) { // skip series column
//                    m.put(column.get(j) + "", row.get(j));
//                }
//                if (c.isShowAgg()) {
//                    m.put("Total", _arow.get(i));
//                }
//                result.put(series,m);
//            }
//            return result;
////            return result; // [{"a":123,"b":121,"Total":244},{"a":123,"b":121,"Total":244}]
//        } else {
//            List<Map<String, Object>> data = (List<Map<String, Object>>) cdata.get("data");
//            Map<String, Object> map = data.stream().collect(Collectors.toMap(x -> x.get("name")+"", x -> x.get("value")));
//            if (c.isShowAgg()) {
//                map.put("_a", cdata.get("_a")); //_a
//            }
//            return map; // {"a":123,"b":123,"c":123,"_a":369}
//        }
    }


    public List<JsonNode> findListByDatasetData(Long datasetId, String searchText, String email, Map filters, Pageable pageable, HttpServletRequest req) {
        if (searchText != null && searchText.isEmpty()) {
            searchText = null;
        }

//        Form form = formService.findFormById(formId);

        ObjectMapper mapper = new ObjectMapper();

        Dataset d = datasetRepository.getReferenceById(datasetId);

        Page<Entry> entryList;

//        List<String> status = Arrays.asList(d.getStatus().split(","));

        Map<String, Object> dataMap = new HashMap<>();
        if (userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).isPresent()) {
            User user = userRepository.findFirstByEmailAndAppId(email, d.getApp().getId()).get();
            Map userMap = mapper.convertValue(user, Map.class);
            dataMap.put("user", userMap);
        }
//        dataMap.put("user", userRepository.findByEmail(email).get());
        dataMap.put("now", Instant.now().toEpochMilli());
        Calendar calendarStart = Helper.calendarWithTime(Calendar.getInstance(), 0, 0, 0, 0);
        dataMap.put("todayStart", calendarStart.getTimeInMillis());
        Calendar calendarEnd = Helper.calendarWithTime(Calendar.getInstance(), 23, 59, 59, 999);
        dataMap.put("todayEnd", calendarEnd.getTimeInMillis());

        Map filtersReq = new HashMap();
        if (req != null) {
            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                if (entry.getKey().contains("$")) {
                    filtersReq.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }
        }

//        Map presetFilters = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        Map<String, Object> pF = mapper.convertValue(d.getPresetFilters(), HashMap.class);
        Map presetFilters = pF.entrySet().stream()
                .filter(x -> x.getKey().startsWith("$"))
                .collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue() + ""));

        presetFilters.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));

        final Map newFilter = new HashMap();
        if (filters != null) {
            newFilter.putAll(filters);
        }
//        if (filters != null) {
        if (d.getPresetFilters() != null) {
            newFilter.putAll(presetFilters);
        }
        if (filtersReq.size() > 0) {
            newFilter.putAll(filtersReq);
        }
//        }


        Map statusFilter = mapper.convertValue(d.getStatusFilter(), HashMap.class);
//        statusFilter.replaceAll((k, v) -> MailService.compileTpl(v.toString(), dataMap));
//
//        if (status != null) {
//            if (d.getStatusFilter() != null){
//                status.putAll(statusFilter);
//            }
//        }

//        if ("db".equals(d.getForm().getType())) {

        switch (d.getType()) {
            case "all":
                entryList = entryRepository.findAll(EntryFilter.builder()
                        .formId(d.getForm().getId())
                        .form(d.getForm())
                        .searchText(searchText)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                        .status(statusFilter)
                        .filters(newFilter)
                        .build().filter(), req.getParameter("size") != null ? pageable : PageRequest.of(0, Integer.MAX_VALUE));
                break;// entryRepository.findAll(formId, searchText, status, pageable);
            case "admin":
                entryList = entryRepository.findAll(EntryFilter.builder()
                        .formId(d.getForm().getId())
                        .form(d.getForm())
                        .searchText(searchText)
                        .admin(email)
                        .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                        .filters(newFilter)
                        .build().filter(), req.getParameter("size") != null ? pageable : PageRequest.of(0, Integer.MAX_VALUE));
                break;// entryRepository.findAdminByEmail(formId, searchText, email, status, pageable);
            case "user":
                entryList = entryRepository.findAll(EntryFilter.builder()
                        .formId(d.getForm().getId())
                        .form(d.getForm())
                        .searchText(searchText)
                        .email(email)
                        .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                        .filters(newFilter)
                        .build().filter(), req.getParameter("size") != null ? pageable : PageRequest.of(0, Integer.MAX_VALUE));
                break;//findUserByEmail(formId, searchText, email, status, pageable);
            case "action":
                entryList = entryRepository.findAll(EntryFilter.builder()
                        .formId(d.getForm().getId())
                        .form(d.getForm())
                        .searchText(searchText)
                        .approver(email)
                        .action(true)
                        .status(statusFilter)
//                            .status(mapper.convertValue(d.getStatusFilter(), HashMap.class))
                        .filters(newFilter)
                        .build().filter(), req.getParameter("size") != null ? pageable : PageRequest.of(0, Integer.MAX_VALUE));

//                    entryList = entryRepository.findActionByEmail(d.getForm().getId(), searchText, email, req.getParameter("size") != null ? pageable : PageRequest.of(0, Integer.MAX_VALUE));
                break;
            default:
                return null;
        }

//        } else {
//            entryList = runRestEndpoint(d.getForm(), req);
//        }

        return entryList.getContent().stream().map(e -> {
            JsonNode node = e.getData();
            ObjectNode o = (ObjectNode) node;
//            o.put("$id", e.getId());
            o.set("$prev", e.getPrev());
            return o;
        }).collect(Collectors.toList());

    }

    public void updateEntry(long formId) {
        List<Entry> list = entryRepository.findByFormId(formId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();
        List<Entry> newList = new ArrayList<>();
        list.forEach(e -> newList.add(updateApprover(e, e.getEmail())));
        entryRepository.saveAll(newList);
    }

    public Page<EntryApprovalTrail> findTrailById(long id, Pageable pageable) {
        return entryApprovalTrailRepository.findTrailByEntryId(id, pageable);
    }



}
