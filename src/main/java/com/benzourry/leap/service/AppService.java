package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.filter.AppFilter;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.benzourry.leap.config.Constant.IO_BASE_DOMAIN;

/**
 * Created by User on 10/11/2018.
 */
@Service
public class AppService {
    public final AppRepository appRepository;
    public final FormRepository formRepository;
    public final TabRepository tabRepository;
    public final DatasetRepository datasetRepository;
    public final DashboardRepository dashboardRepository;
    public final ScreenRepository screenRepository;
    public final CloneRequestRepository cloneRequestRepository;
    public final LookupRepository lookupRepository;
    public final LookupEntryRepository lookupEntryRepository;
    public final EmailTemplateRepository emailTemplateRepository;
    public final UserGroupRepository userGroupRepository;
    public final LambdaRepository lambdaRepository;
    public final NaviGroupRepository naviGroupRepository;
    public final NaviItemRepository naviItemRepository;
    public final AppUserRepository appUserRepository;
    public final EntryRepository entryRepository;
    public final EntryAttachmentRepository entryAttachmentRepository;
    public final EndpointRepository endpointRepository;
    public final ScheduleRepository scheduleRepository;
    public final UserRepository userRepository;
    public final PushSubRepository pushSubRepository;
    public final BucketRepository bucketRepository;
    public final ItemRepository itemRepository;
    public final SectionRepository sectionRepository;
    public final ApiKeyRepository apiKeyRepository;

//    @Autowired
//    public UserOldRepository userOldRepository;

    final MailService mailService;

    public AppService(AppRepository appRepository, FormRepository formRepository,
                      TabRepository tabRepository,
                      CloneRequestRepository cloneRequestRepository,
                      DatasetRepository datasetRepository,
                      DashboardRepository dashboardRepository,
                      ScreenRepository screenRepository,
                      LookupRepository lookupRepository,
                      LookupEntryRepository lookupEntryRepository,
                      UserGroupRepository userGroupRepository,
                      LambdaRepository lambdaRepository,
                      NaviGroupRepository naviGroupRepository,
                      NaviItemRepository naviItemRepository,
                      EmailTemplateRepository emailTemplateRepository,
                      AppUserRepository appUserRepository,
                      EntryRepository entryRepository,
                      EntryAttachmentRepository entryAttachmentRepository,
                      EndpointRepository endpointRepository,
                      ScheduleRepository scheduleRepository,
                      UserRepository userRepository,
                      PushSubRepository pushSubRepository,
                      ItemRepository itemRepository,
                      SectionRepository sectionRepository,
                      BucketRepository bucketRepository,
                      ApiKeyRepository apiKeyRepository,
                      MailService mailService) {
        this.appRepository = appRepository;
        this.formRepository = formRepository;
        this.tabRepository = tabRepository;
        this.cloneRequestRepository = cloneRequestRepository;
        this.datasetRepository = datasetRepository;
        this.dashboardRepository = dashboardRepository;
        this.screenRepository = screenRepository;
        this.lookupRepository = lookupRepository;
        this.lookupEntryRepository = lookupEntryRepository;
        this.userGroupRepository = userGroupRepository;
        this.lambdaRepository = lambdaRepository;
        this.naviGroupRepository = naviGroupRepository;
        this.naviItemRepository = naviItemRepository;
        this.appUserRepository = appUserRepository;
        this.entryRepository = entryRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.endpointRepository = endpointRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.mailService = mailService;
        this.userRepository = userRepository;
        this.scheduleRepository = scheduleRepository;
        this.bucketRepository = bucketRepository;
        this.itemRepository = itemRepository;
        this.sectionRepository = sectionRepository;
        this.pushSubRepository = pushSubRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    public App save(App app, String email) {

//        Ensure the path is not conflict
        if (app.getAppPath()!=null){
            if (app.getId()==null){ // if new app
                    if(checkByKey("path:"+app.getAppPath())){
                        throw new RuntimeException("App path "+app.getAppPath()+" is already taken.");
                    }
            }else{ // if not new
                App byKey = findByKey("path:"+app.getAppPath());
                if (byKey!=null && !Objects.equals(byKey.getId(), app.getId())){
                    throw new RuntimeException("App path "+app.getAppPath()+" is already taken.");
                }
            }
        }

        if (Optional.ofNullable(app.getId()).isEmpty()) {
            app.setEmail(email);
        }
        return this.appRepository.save(app);
    }


    public App findById(Long appId) {
        return this.appRepository.findById(appId)
                .orElseThrow(()->new ResourceNotFoundException("App","id",appId));
    }

    @Transactional
    public App setLive(Long appId,Boolean status){
        App app = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id", appId));
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode x = (ObjectNode)app.getX();
        if (x==null){
            x = mapper.createObjectNode();
        }
        x.put("live", status);
        app.setX(x);
        return appRepository.save(app);
    }

    public List<NaviGroup> findNaviByAppIdAndEmail(Long appId, String email) {

        if (email != null) {
            return naviGroupRepository.findByAppIdAndEMail(appId, email);
        } else {
            return naviGroupRepository.findByAppId(appId);
        }
    }

    @Transactional
    public void delete(Long appId, String email) {
        App app = appRepository.findById(appId)
                .orElseThrow(()->new ResourceNotFoundException("App","id",appId));
        String [] emails = app.getEmail().split(",");
        if (emails.length>1){
            List<String> newEmails = Arrays.asList(emails);
            newEmails.forEach(e-> e.trim());
            newEmails.remove(email.trim());
            app.setEmail(String.join(",", newEmails));
            appRepository.save(app);
        }else{
            deleteEntry(appId);
            deleteUsers(appId);
            deleteApp(appId);
        }

    }

    @Transactional
    public void deleteApp(Long appId) {

        // use this to ensure cascade. cascade not working via jpql
        List<Screen> screenList = screenRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        screenRepository.deleteAll(screenList);

        List<Dataset> datasetList = datasetRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        datasetRepository.deleteAll(datasetList);

        List<Dashboard> dashboardList = dashboardRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        dashboardRepository.deleteAll(dashboardList);

        List<NaviGroup> naviGroupList = naviGroupRepository.findByAppId(appId);
        naviGroupRepository.deleteAll(naviGroupList);

        List<Lookup> lookupList = lookupRepository.findByAppId("%", appId, PageRequest.ofSize(Integer.MAX_VALUE)).getContent();

        lookupList.forEach(l -> lookupEntryRepository.deleteByLookupId(l.getId()));
        lookupRepository.deleteAll(lookupList);

        List<Form> formList = formRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE)).getContent();
        formRepository.saveAllAndFlush(formList.parallelStream().map(f->{
            f.setAdmin(null);
//            f.setAccess(null);
            return f;
        }).toList());
        formRepository.deleteAll(formList);

        List<Lambda> lambdaList = lambdaRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE)).getContent();
        lambdaRepository.deleteAll(lambdaList);

//        lookupRepository.deleteByAppId(appId);
        emailTemplateRepository.deleteByAppId(appId);
        endpointRepository.deleteByAppId(appId);
        userGroupRepository.deleteByAppId(appId);
        bucketRepository.deleteByAppId(appId);
        scheduleRepository.deleteByAppId(appId);
        apiKeyRepository.deleteByAppId(appId);

        this.appRepository.deleteById(appId);
    }

    @Transactional
    public void deleteEntry(Long appId) {
        List<Form> formList = formRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE)).getContent();

        formList.forEach(f -> {
            entryRepository.deleteApproverByFormId(f.getId());
            entryRepository.deleteApprovalByFormId(f.getId());
            entryRepository.deleteTrailByFormId(f.getId());
            entryRepository.deleteApprovalTrailByFormId(f.getId());
            entryRepository.deleteByFormId(f.getId());
        });

        entryAttachmentRepository.deleteByAppId(appId);

        pushSubRepository.deleteByAppId(appId);

    }

    @Transactional
    public void deleteUsers(Long appId) {
        appUserRepository.deleteByAppId(appId);
        userRepository.deleteByAppId(appId);
    }

    public Page<App> getList(String searchText, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.appRepository.findAll(AppFilter.builder().searchText(searchText).status(List.of("published")).build().filter(), pageable);
//        return this.appRepository.findPublished(searchText,pageable);
    }

    public Page<App> getByStatusList(List<String> status, String searchText, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.appRepository.findAll(AppFilter.builder()
                .status(status)
                .searchText(searchText)
                .build().filter(), pageable);
    }

    public Page<App> getAdminList(String email, String searchText, Boolean live, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.appRepository.findAll(AppFilter.builder()
                .email(email)
                .searchText(searchText)
                .live(live)
                .status(Arrays.asList("local", "published"))
                .build().filter(), pageable);
    }

    public Page<AppUser> findUserByAppId(Long appId, String searchText, List<String> status, Long group, Pageable pageable) {
        searchText = "%" + searchText + "%";
        if (group!=null){
            return appUserRepository.findByGroupIdAndParams(group, searchText, status, Optional.ofNullable(status).orElse(List.of()).isEmpty(), pageable);
//            return appUserRepository.findByAppIdAndParam(appId, searchText, status, group, pageable);
        }else{
            return appUserRepository.findAllByAppId(appId, searchText, status, Optional.ofNullable(status).orElse(List.of()).isEmpty(), PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id").ascending()));
        }
    }

    public Page<AppUser> findAllByAppId(Long appId, String searchText, List<String> status, Pageable pageable) {
        searchText = "%" + searchText + "%";
        return appUserRepository.findAllByAppId(appId, searchText, status, status.isEmpty(), pageable);
    }

    public List<AppUser> findByAppIdAndEmail(Long appId, String email) {
        return appUserRepository.findByAppIdAndEmail(appId, email);
    }

    public Map<String, Object> regUserBulk(List<Long> groups, Long appId, String emaillist,Boolean autoReg, List<String> tags){
        Map<String, Object> data = new HashMap<>();
        if (!emaillist.isBlank()){
            Arrays.asList(emaillist.split(",")).forEach(em->{
                String [] splitted = em.split("\\|");
                String email = splitted[0].trim();
                String name = email;
                if (splitted.length>1){
                    name = splitted[1].trim();
                }
                if (!email.isBlank()){
                    regUser(groups,appId,email,name,autoReg,tags);
                }
            });
            data.put("success",true);
            data.put("message","Users successfully added");
        }else{
            data.put("success",false);
            data.put("message","Email list cannot be blank");
        }
        return data;
    }

    @Transactional
    public Map<String, Object> regUser(List<Long> groups, Long appId, String email, String name, Boolean autoReg, List<String> tags) {

        Map<String, Object> data = new HashMap<>();

        if (email!=null){
            email = email.trim();
        }

        App app = appRepository.getReferenceById(appId);
        final boolean fAutoReg = app.getEmail().contains(email) || autoReg;
        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);
        User user;

        if (userOpt.isPresent()) {
            user = userOpt.get();
        } else {
            user = new User();
            user.setEmail(email);
            user.setName(name);
            user.setEmailVerified(false);
            user.setAppId(appId);
            user.setStatus("approved");
            user.setProvider(AuthProvider.undetermine);
            userRepository.save(user);
        }

        // User atas dpt x create duplicate.
        // Tp AppUser diah tok xda checking duplicate lok.
        List<AppUser> appUserList = new ArrayList<>();


        AtomicInteger pending = new AtomicInteger(0);
        AtomicInteger approved = new AtomicInteger(0);
        groups.forEach(gId -> {

            UserGroup g = userGroupRepository.findById(gId).orElseThrow(()->new ResourceNotFoundException("UserGroup","id",gId));

            AppUser appUser;
            Optional<AppUser> appUserOptional = appUserRepository.findByUserIdAndGroupId(user.getId(), g.getId());
            appUser = appUserOptional.orElseGet(AppUser::new);

            appUser.setUser(user);
            appUser.setTags(tags);
            appUser.setGroup(g);
            if (g.isNeedApproval() && !fAutoReg) { // cmne? adakah count lepas if ada approved cgek??
                appUser.setStatus("pending");
                pending.getAndIncrement();
            } else {
                appUser.setStatus("approved");
                approved.getAndIncrement();
            }
            appUserList.add(appUser);
        });

        appUserRepository.saveAll(appUserList);

        user.setStatus(approved.get() > 0 ? "approved" : "pending");
        userRepository.save(user);

        Map<String, Object> userMap;
        ObjectMapper mapper = new ObjectMapper();

        userMap = mapper.convertValue(user, Map.class);
        List<AppUser> appUserListApproved = appUserList.stream().filter(au -> "approved".equals(au.getStatus())).toList();
        Map<Long, UserGroup> groupMap = appUserListApproved.stream().collect(
                Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
        userMap.put("groups", groupMap);

        data.put("appUserList", appUserList);
        data.put("user", userMap);
        return data;
    }

    @Transactional(readOnly = true)
    public boolean checkByKey(String appPath) {
        long count = 0;

        if (!Helper.isNullOrEmpty(appPath)) {
            String[] p = appPath.split(":");
            count = switch (p[0]) {
                case "domain" -> this.appRepository.checkByDomain(p[1]);
                case "path" -> this.appRepository.checkByPath(p[1].replaceAll("--dev",""));
                default -> this.appRepository.checkByPath(p[0]);
            };
        }
        return count > 0;
    }

    @Transactional(readOnly = true)
    public App findByKey(String key) {
        App app = null;
        if (!Helper.isNullOrEmpty(key)) {
            String[] p = key.split(":");
            app = switch (p[0]) {
                case "domain" -> this.appRepository.findByAppDomain(p[1]);
                case "path" -> this.appRepository.findByAppPath(p[1].replaceAll("--dev",""));
                default -> this.appRepository.findByAppPath(key);
            };
        }
        return app;
    }

    @Transactional
    public App cloneApp(App app, String email) {

        Long appId = app.getId();

        // increase count of clone to get the popularity
        App k = appRepository.findById(appId)
                .orElseThrow(()->new ResourceNotFoundException("App","id",appId));
        k.setClone(Optional.ofNullable(k.getClone()).orElse(0L) + 1);
        appRepository.save(k);


        app.setId(null);
        App newApp = appRepository.save(app);
        newApp.setEmail(email);
        if (!Optional.ofNullable(email).orElse("").contains("@unimas")) {
            newApp.setUseUnimas(false);
            newApp.setUseUnimasid(false);
        }
        newApp.setStatus("local");
//        newApp.setShared(false);

        appRepository.save(newApp);

        //// COPY LOOKUP AND ENTRIES
        Page<Lookup> lookupPaged = lookupRepository.findByAppId("%", appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Lookup> lookupListOld = lookupPaged.getContent();
        List<Lookup> lookupListNew = new ArrayList<>();
        Map<Long, Lookup> lookupMap = new HashMap<>();
        lookupListOld.forEach(lookup -> {
            Lookup d2 = new Lookup();
            BeanUtils.copyProperties(lookup, d2, "id");
            d2.setApp(newApp);
            lookupListNew.add(d2);
            lookupMap.put(lookup.getId(), d2);
        });
        lookupRepository.saveAll(lookupListNew);

        lookupListOld.forEach(lookup -> {
            List<LookupEntry> lookupEntryList = new ArrayList<>();

            lookupEntryRepository.findByLookupId(lookup.getId(), null,null, null, null, PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent().forEach(le -> {
                        LookupEntry le2 = new LookupEntry();
                        BeanUtils.copyProperties(le, le2, "id");
                        le2.setLookup(lookupMap.get(lookup.getId()));
                        lookupEntryList.add(le2);
                    });
            lookupEntryRepository.saveAll(lookupEntryList);
        });

        //// COPY MAILER TEMPLATE
        Page<EmailTemplate> emailPaged = emailTemplateRepository.findByAppId(appId, "%", PageRequest.of(0, Integer.MAX_VALUE));
        List<EmailTemplate> emailListOld = emailPaged.getContent();
        List<EmailTemplate> emailListNew = new ArrayList<>();
        Map<Long, EmailTemplate> emailMap = new HashMap<>();
        emailListOld.forEach(em -> {
            EmailTemplate em2 = new EmailTemplate();
            BeanUtils.copyProperties(em, em2, "id");
            em2.setApp(newApp);
            emailListNew.add(em2);
            emailMap.put(em.getId(), em2);
        });
        emailTemplateRepository.saveAll(emailListNew);

        //// COPY Lambda
        Page<Lambda> lambdaPaged = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Lambda> lambdaListOld = lambdaPaged.getContent();
        List<Lambda> lambdaListNew = new ArrayList<>();
        Map<Long, Lambda> lambdaMap = new HashMap<>();
        lambdaListOld.forEach(l -> {
            Lambda l2 = new Lambda();
            BeanUtils.copyProperties(l, l2, "id");
            l2.setCode(l.getCode()+"-copy");
            l2.setApp(newApp);
            lambdaListNew.add(l2);
            lambdaMap.put(l.getId(), l2);

            Set<LambdaBind> newLambdaBindList = new HashSet<>();
            l.getBinds().forEach(oldBind -> {
                LambdaBind newLambdaBind = new LambdaBind();
                BeanUtils.copyProperties(oldBind, newLambdaBind, "id");
                newLambdaBind.setLambda(l2);
                newLambdaBindList.add(newLambdaBind);
            });

//            newDataset.setApp(newApp);
            l2.setBinds(newLambdaBindList);

        });
        lambdaRepository.saveAll(lambdaListNew);

        //// COPY USER GROUP
        Page<UserGroup> groupPaged = userGroupRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<UserGroup> groupListOld = groupPaged.getContent();
        List<UserGroup> groupListNew = new ArrayList<>();
        Map<Long, UserGroup> groupMap = new HashMap<>();
        groupListOld.forEach(em -> {
            UserGroup em2 = new UserGroup();
            BeanUtils.copyProperties(em, em2, "id");
            em2.setApp(newApp);
            groupListNew.add(em2);
            groupMap.put(em.getId(), em2);
        });
        userGroupRepository.saveAll(groupListNew);


        //// COPY Bucket
        Page<Bucket> bucketPaged = bucketRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Bucket> bucketListOld = bucketPaged.getContent();
        List<Bucket> bucketListNew = new ArrayList<>();
        Map<Long, Bucket> bucketMap = new HashMap<>();
        bucketListOld.forEach(bo -> {
            Bucket bo2 = new Bucket();
            BeanUtils.copyProperties(bo, bo2, "id");
            bo2.setAppId(newApp.getId());
            bucketListNew.add(bo2);
            bucketMap.put(bo.getId(), bo2);
        });
        bucketRepository.saveAll(bucketListNew);

        //// COPY FORM LIST
        Page<Form> f = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Form> formListOld = f.getContent();
        List<Form> formListNew = new ArrayList<>();
        Map<Long, Form> formMap = new HashMap<>();
        Map<Long, Tab> tabMap = new HashMap<>();
        Map<Long, Section> sectionMap = new HashMap<>();

        formListOld.forEach(oldForm -> {
            Form newForm = new Form();
            BeanUtils.copyProperties(oldForm, newForm, "id", "prev");
            if (oldForm.getAccessList() != null) {
                List<Long> newAccessList = new ArrayList<>();
                oldForm.getAccessList().forEach(a->{
                    newAccessList.add(groupMap.get(a).getId());
                });
                newForm.setAccessList(newAccessList);
            }
            if (oldForm.getAdmin() != null) {
                newForm.setAdmin(groupMap.get(oldForm.getAdmin().getId()));
            }

            Map<String, Item> items = new HashMap<>();
            List<Section> sections = new ArrayList<>();
            List<Tab> tabs = new ArrayList<>();
            List<Tier> tiers = new ArrayList<>();

            newForm.setItems(items);
            newForm.setSections(sections);
            newForm.setTabs(tabs);
            newForm.setTiers(tiers);

            newForm.setApp(newApp);
            formRepository.save(newForm);
//            formListNew.add(newForm);
            formMap.put(oldForm.getId(), newForm);
        });
//        Save skali lok supaya nya x double
//        formRepository.saveAll(formListNew); //save all form

        formListOld.forEach(oldForm -> {
//            System.out.println("form: "+oldForm.getTitle());
            Form newForm = formMap.get(oldForm.getId());

            if (oldForm.getPrev() != null) {
                newForm.setPrev(formMap.get(oldForm.getPrev().getId()));
            }

            Map<String, Item> newItemMap = newForm.getItems();
            oldForm.getItems().forEach((name, oldItem)->{
                Item newItem = new Item();
                BeanUtils.copyProperties(oldItem, newItem, "id");
                if (oldItem.getX()!=null) {
                    ObjectNode onode = (ObjectNode) oldItem.getX();
                    if (onode.get("bucket") != null && bucketMap.get(onode.get("bucket").asLong()) != null) {
                        onode.put("bucket", bucketMap.get(onode.get("bucket").asLong()).getId());
                    }
                    newItem.setX(onode);
                }

                newItem.setForm(newForm);
                newItemMap.put(name, newItem);
            });

            List<Tab> newTabList = newForm.getTabs();
            oldForm.getTabs().forEach(oldTab -> {
                Tab newTab = new Tab();
                BeanUtils.copyProperties(oldTab, newTab, "id");
                newTab.setForm(newForm);
//                tabRepository.save(newTab);
                newTabList.add(newTab);
                tabMap.put(oldTab.getId(), newTab);
            });
            tabRepository.saveAll(newTabList);

            List<Section> newSectionList = newForm.getSections();
            oldForm.getSections().forEach(oldSection -> {
//                System.out.println("section: "+oldSection.getTitle());
                Section newSection = new Section();
                BeanUtils.copyProperties(oldSection, newSection, "id");
                Set<SectionItem> siSet = new HashSet<>();
                oldSection.getItems().forEach(si -> {
                    SectionItem si2 = new SectionItem();
                    BeanUtils.copyProperties(si, si2, "id");
                    si2.setSection(newSection);
                    siSet.add(si2);
                });

                if (oldSection.getParent() != null) {
                    Optional.ofNullable(tabMap.get(oldSection.getParent()))
                            .ifPresent(o -> newSection.setParent(o.getId()));
                }
                newSection.setForm(newForm);

                newSection.setItems(siSet);
                sectionRepository.save(newSection);
                sectionMap.put(oldSection.getId(), newSection);
                newSectionList.add(newSection);
            });

            List<Tier> newTierList = newForm.getTiers();
            oldForm.getTiers().forEach((oldTier) -> {
                Tier newTier = new Tier();
                BeanUtils.copyProperties(oldTier, newTier, "id");
//                List<Long> submitMailerNew = new ArrayList<>();
                for (Long i : oldTier.getSubmitMailer()) {
                    if (emailMap.get(i) != null) {
//                        submitMailerNew.add()
                        newTier.getSubmitMailer().add(emailMap.get(i).getId());
                    }
                }
                for (Long i : oldTier.getResubmitMailer()) {
                    if (emailMap.get(i) != null) {
                        newTier.getResubmitMailer().add(emailMap.get(i).getId());
                    }
                }

                newTier.setSection(sectionMap.get(oldTier.getId()));
                newTier.setForm(newForm);

                Map<String, TierAction> newActionMap = newTier.getActions();
                oldTier.getActions().forEach((name, oldTAction) -> {
                    TierAction newTa = new TierAction();
                    BeanUtils.copyProperties(oldTAction, newTa, "id");
                    for (Long i : oldTAction.getMailer()) {
                        if (emailMap.get(i) != null) {
                            newTa.getMailer().add((emailMap.get(i).getId()));
                        }
                    }
                    if (oldTAction.getNextTier() != null) {
//                        newTa.setNextTier(tierMap);
                    }
                    newActionMap.put(name, newTa);
                });

                newTierList.add(newTier);
            });

            formRepository.save(newForm);

            // dlm list xda double
//            System.out.println("-----------------");
//            newSectionList.forEach(s->{
//                System.out.println("sec:"+s.getTitle());
//            });
//            System.out.println("-----------------");
//            System.out.println();
            formListNew.add(newForm);
//            formMap.put(oldForm.getId(), newForm);
        });
//        formRepository.saveAll(formListNew); //save all form



        //// COPY DATASET
        List<Dataset> datasetListOld = datasetRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC,"sortOrder")));
        Map<Long, Dataset> datasetMap = new HashMap<>();
        List<Dataset> datasetListNew = new ArrayList<>();
        datasetListOld.forEach(oldDataset -> {

            Dataset newDataset = new Dataset();
            BeanUtils.copyProperties(oldDataset, newDataset, "id");
            if (oldDataset.getAccessList() != null) {
                oldDataset.getAccessList().forEach(ac->{
                    List<Long> accessList = new ArrayList<>();
                    UserGroup ug = groupMap.get(ac);
                    if (groupMap.get(ac)!=null){
                        accessList.add(ug.getId());
                    }
                    newDataset.setAccessList(accessList);
                });
            }

            List<DatasetItem> newDatasetItemList = new ArrayList<>();
            List<DatasetAction> newDatasetActionList = new ArrayList<>();
            Set<DatasetFilter> newDatasetFilterList = new HashSet<>();
            oldDataset.getItems().forEach(oldDatasetItem -> {
                DatasetItem newDatasetItem = new DatasetItem();
                BeanUtils.copyProperties(oldDatasetItem, newDatasetItem, "id");
                newDatasetItem.setDataset(newDataset);
                newDatasetItemList.add(newDatasetItem);
            });
            oldDataset.getActions().forEach(oldDatasetAction -> {
                DatasetAction newDatasetAction = new DatasetAction();
                BeanUtils.copyProperties(oldDatasetAction, newDatasetAction, "id");
                newDatasetAction.setDataset(newDataset);
                newDatasetActionList.add(newDatasetAction);
            });
            oldDataset.getFilters().forEach(oldDatasetFilter -> {
                DatasetFilter newDatasetFilter = new DatasetFilter();
                BeanUtils.copyProperties(oldDatasetFilter, newDatasetFilter, "id");
                newDatasetFilter.setDataset(newDataset);
                newDatasetFilterList.add(newDatasetFilter);
            });

            if (oldDataset.getForm() != null) {
                newDataset.setForm(formMap.get(oldDataset.getForm().getId()));
            }
            newDataset.setApp(newApp);
            newDataset.setItems(newDatasetItemList);
            newDataset.setActions(newDatasetActionList);
            newDataset.setFilters(newDatasetFilterList);

            datasetListNew.add(newDataset);
            datasetRepository.save(newDataset);
            datasetMap.put(oldDataset.getId(), newDataset);
            System.out.println("ds-old:"+ oldDataset.getId()+",ds-new:"+newDataset.getId());
        });
//        datasetRepository.saveAll(datasetListNew);
//        datasetMap

        //// COPY DASHBOARD
        List<Dashboard> dashboardListOld = dashboardRepository.findByAppId(appId,PageRequest.ofSize(Integer.MAX_VALUE));
        List<Dashboard> dashboardListNew = new ArrayList<>();
        Map<Long, Dashboard> dashboardMap = new HashMap<>();
        dashboardListOld.forEach(oldDashboard -> {
            Dashboard newDashboard = new Dashboard();
            BeanUtils.copyProperties(oldDashboard, newDashboard, "id");
            if (oldDashboard.getAccessList() != null) {
                oldDashboard.getAccessList().forEach(ac->{
                    List<Long> accessList = new ArrayList<>();
                    UserGroup ug = groupMap.get(ac);
                    if (groupMap.get(ac)!=null){
                        accessList.add(ug.getId());
                    }
                    newDashboard.setAccessList(accessList);
                });
            }
            Set<Chart> charts = new HashSet<>();
            oldDashboard.getCharts().forEach(oldChart -> {
                Chart newChart = new Chart();

                BeanUtils.copyProperties(oldChart, newChart, "id");
                newChart.setDashboard(newDashboard);
                if (oldChart.getForm() != null) {
                    newChart.setForm(formMap.get(oldChart.getForm().getId()));
                }
                Set<ChartFilter> newChartFilterList = new HashSet<>();
                oldChart.getFilters().forEach(oldChartFilter -> {
                    ChartFilter newChartFilter = new ChartFilter();
                    BeanUtils.copyProperties(oldChartFilter, newChartFilter, "id");
                    newChartFilter.setChart(newChart);
                    newChartFilterList.add(newChartFilter);
                });
                newChart.setFilters(newChartFilterList);

                charts.add(newChart);
            });

            newDashboard.setApp(newApp);
            newDashboard.setCharts(charts);
            dashboardListNew.add(newDashboard);
            dashboardMap.put(oldDashboard.getId(), newDashboard);
        });
        dashboardRepository.saveAll(dashboardListNew);


        ///// COPY SCREEN
        List<Screen> screenListOld = screenRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        List<Screen> screenListNew = new ArrayList<>();
        Map<Long, Screen> screenMap = new HashMap<>();
        screenListOld.forEach(oldScreen -> {
            Screen newScreen = new Screen();
            BeanUtils.copyProperties(oldScreen, newScreen, "id", "actions");
//            if (oldScreen.getAccess() != null) {
//                newScreen.setAccess(groupMap.get(oldScreen.getAccess().getId()));
//            }
            if (oldScreen.getAccessList() != null) {
                oldScreen.getAccessList().forEach(ac->{
                    List<Long> accessList = new ArrayList<>();
                    UserGroup ug = groupMap.get(ac);
                    if (groupMap.get(ac)!=null){
                        accessList.add(ug.getId());
                    }
                    newScreen.setAccessList(accessList);
                });
            }


            if ("page".equals(oldScreen.getType())) {
                if (oldScreen.getForm() != null) {
                    newScreen.setForm(formMap.get(oldScreen.getForm().getId()));
                }
            } else if ("list".equals(oldScreen.getType())) {
                if (oldScreen.getDataset() != null) {
                    newScreen.setDataset(datasetMap.get(oldScreen.getDataset().getId()));
                }
            }

            Set<Action> actions = new HashSet<>();
            newScreen.setActions(actions);

            newScreen.setApp(newApp);

            screenRepository.save(newScreen);

            screenMap.put(oldScreen.getId(), newScreen);
        });
        /// then set screen actions after persist screen above;
        screenListOld.forEach(oldScreen -> {
            Screen newScreen = screenMap.get(oldScreen.getId());
            Set<Action> sActions = newScreen.getActions();

            oldScreen.getActions().forEach(sa -> {
                Action sa2 = new Action();
                BeanUtils.copyProperties(sa, sa2, "id");
                if ("screen".equals(sa.getNextType())) {
                    if (screenMap.get(sa.getNext()) != null) {
                        sa2.setNext(screenMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("form","view","view-single","edit","edit-single","prev").contains(sa.getNextType())) {
                    if (formMap.get(sa.getNext()) != null) {
                        sa2.setNext(formMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("dataset","static").contains(sa.getNextType())) {
                    if (datasetMap.get(sa.getNext()) != null) {
                        sa2.setNext(datasetMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("dashboard").contains(sa.getNextType())) {
                    if (dashboardMap.get(sa.getNext()) != null) {
                        sa2.setNext(dashboardMap.get(sa.getNext()).getId());
                    }
                }
                sa2.setScreen(newScreen);
                sActions.add(sa2);
            });

            newScreen.setActions(sActions);
            screenListNew.add(newScreen);
        });

        screenRepository.saveAll(screenListNew);

        // setting datasource, etc

        // kemungkinan tok penyebab double. SAH
        formListNew.forEach(newForm -> {
            newForm.getItems().forEach((name,item)->{
                if (item.getDataSource()!=null){
                    Long newDs = null;

                    if (List.of("modelPicker","dataset").contains(item.getType())){
                        if (datasetMap.get(item.getDataSource())!=null) {
                            System.out.println("ada dataset:" + item.getDataSource());
                            newDs = datasetMap.get(item.getDataSource()).getId();
                        }
                    }else{
                        if (lookupMap.get(item.getDataSource())!=null){
                            System.out.println("ada lookup:" + item.getDataSource());
                            newDs = lookupMap.get(item.getDataSource()).getId();
                        }
                    }
                    System.out.println("form:"+newForm.getTitle()+"/"+newForm.getId()+"item:"+ item.getLabel());
                    System.out.println("item f# old-ds:"+item.getDataSource()+", new-ds:"+newDs);
                    item.setDataSource(newDs);
                    item.setDataSource(newDs);
//                    itemRepository.save(item);
//                    newForm.getItems().put(name, item);
                }
            });

//            formRepository.save(newForm);
        });
        formRepository.saveAll(formListNew);


        //// COPY NAVIGROUP
        List<NaviGroup> naviGroupListOld = naviGroupRepository.findByAppId(appId);
        List<NaviGroup> naviGroupListNew = new ArrayList<>();

        naviGroupListOld.forEach(oldNaviGroup -> {
            NaviGroup newNaviGroup = new NaviGroup();
            BeanUtils.copyProperties(oldNaviGroup, newNaviGroup, "id");
//            if (oldNaviGroup.getAccess() != null) {
//                newNaviGroup.setAccess(groupMap.get(oldNaviGroup.getAccess().getId()));
//            }
            newNaviGroup.setApp(newApp);
            List<NaviItem> naviItemListOld = oldNaviGroup.getItems();
            List<NaviItem> naviItemListNew = new ArrayList<>();

            naviItemListOld.forEach(oldNaviItem -> {
                NaviItem newNaviItem = new NaviItem();
                BeanUtils.copyProperties(oldNaviItem, newNaviItem, "id");


                if ("form".equals(oldNaviItem.getType()) ||
                        "form-single".equals(oldNaviItem.getType()) ||
                        "view-single".equals(oldNaviItem.getType())) {
                    if (formMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(formMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                if ("dataset".equals(oldNaviItem.getType())) {
                    System.out.println("naviitem dataset:"+oldNaviItem.getScreenId());
                    if (datasetMap.get(oldNaviItem.getScreenId()) != null) {
                        System.out.println("naviitem dataset ##:"+(datasetMap.get(oldNaviItem.getScreenId()).getId()));
                        newNaviItem.setScreenId(datasetMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                if ("lookup".equals(oldNaviItem.getType())) {
                    if (lookupMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(lookupMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                if ("screen".equals(oldNaviItem.getType())) {
                    if (screenMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(screenMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                if ("dashboard".equals(oldNaviItem.getType())) {
                    if (dashboardMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(dashboardMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                if ("external".equals(oldNaviItem.getType())) {
                    newNaviItem.setUrl(oldNaviItem.getUrl());//.put("url",i.get("url"));
                }

                newNaviItem.setGroup(newNaviGroup);
                naviItemListNew.add(newNaviItem);
            });

            newNaviGroup.setItems(naviItemListNew);
            naviGroupListNew.add(newNaviGroup);
        });

        naviGroupRepository.saveAll(naviGroupListNew);
//        newApp.setNavis(naviGroupListNew);

        appRepository.save(newApp);


        return newApp;
    }

    public Page<App> getTopList(Pageable pageable) {
//        searchText = "%"+searchText.toUpperCase()+"%";
        return this.appRepository.findAll(AppFilter.builder()
                .status(List.of("published"))
                .build().filter(), PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "clone")));
    }

    public CloneRequest requestCopy(Long appId, String requesterEmail) {
        App app = appRepository.getReferenceById(appId);
        CloneRequest cloneRequest = new CloneRequest();
        cloneRequest.setEmail(requesterEmail);
        cloneRequest.setType("creator");
        cloneRequest.setStatus("pending");
        cloneRequest.setApp(app);
        cloneRequest = cloneRequestRepository.save(cloneRequest);
        String[] to = Optional.ofNullable(app.getEmail()).orElse("").replace(" ", "").split(",");

        try {
            mailService.sendMail(Constant.LEAP_MAILER, to, null, null,
                    "Request to copy [" + app.getTitle() + "]",
                    "Hi, please note that <b>" + requesterEmail + "</b> has requested to copy <b>" + cloneRequest.getApp().getTitle() + "</b>. <br/>You may activate or reject requester request by going to Copy Request at the left pane of your application editor.", null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cloneRequest;
    }

    public String activationCheck(Long appId, String requesterEmail) {
        return this.cloneRequestRepository.findStatusByAppIdAndEmail(appId, requesterEmail, "creator");
    }

    public Page<CloneRequest> getCopyRequestList(Long appId, Pageable pageable) {
        return this.cloneRequestRepository.findByAppId(appId, Arrays.asList("pending", "activated", "rejected"), pageable);
    }

    public CloneRequest status(Long id, String status) {
        CloneRequest cloneRequest = cloneRequestRepository.getReferenceById(id);
        cloneRequest.setStatus(status);
        cloneRequest = cloneRequestRepository.save(cloneRequest);
        String[] to = Optional.ofNullable(cloneRequest.getEmail()).orElse("").split(",");


        try {
            mailService.sendMail(Constant.LEAP_MAILER, to, null, null,
                    "Your request to copy [" + cloneRequest.getApp().getTitle() + "] has been [" + status + "]",
                    "Hi, please note that your request to copy <b>" + cloneRequest.getApp().getTitle() + "</b> has been <b>" + status + "</b>.<br/>You may visit the repository and copy the application by clicking on the 'Protected' button and 'Copy App' on the bottom right of the popup dialog.", null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cloneRequest;
    }

    public Map<String, Object> getManifest(String path) {

        App k = findByKey(path);//.getOne(appId);
        Map<String, Object> manifest = new HashMap();
        if (k!=null) {

//            String url = "https://" + k.getAppPath() + "." + UI_BASE_DOMAIN;

            String url = "https://";
            if (k.getAppDomain() != null) {
                url += k.getAppDomain();
            } else {
                String dev = k.isLive()?"":"--dev";
                url += k.getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN;
            }

            String logoUrl = k.getLogo() != null ?
                    IO_BASE_DOMAIN + "/api/app/" + k.getAppPath() + "/logo/{0}"
                    : url + "/assets/icons/icon-{0}.png";

            String json = """
                    {
                       "display": "standalone",
                       "icons": [
                            {
                               "src": "$logo72",
                               "sizes": "72x72",
                               "type": "image/png"
                            },
                            {
                               "src": "$logo96",
                               "sizes": "96x96",
                               "type": "image/png"
                            },
                            {
                                "src": "$logo192",
                                "sizes": "192x192",
                                "type": "image/png"
                            },
                            {
                                "src": "$logo512",
                                "sizes": "512x512",
                                "type": "image/png"
                            }
                       ]
                    }
                    """.replace("$logo72", MessageFormat.format(logoUrl, 72))
                    .replace("$logo96", MessageFormat.format(logoUrl, 96))
                    .replace("$logo192", MessageFormat.format(logoUrl, 192))
                    .replace("$logo512", MessageFormat.format(logoUrl, 512));
            ObjectMapper mapper = new ObjectMapper();

            try {
                manifest = mapper.readValue(json, Map.class);
            } catch (IOException e) {
                e.printStackTrace();
            }

            manifest.put("name", k.getTitle());
            manifest.put("short_name", k.getTitle());
            manifest.put("theme_color", Optional.ofNullable(k.getTheme()).orElse("#2c2c2c"));
            manifest.put("background_color", Optional.ofNullable(k.getTheme()).orElse("#2c2c2c"));
            manifest.put("start_url", url);
            manifest.put("scope", url + "/");
        }

        return manifest;
    }

//    public Map<String, Object> getStart(Long appId) {
//        Page<App> apps = appRepository.getStart(appId);
//    }

    public NaviGroup addNaviGroup(Long appId, NaviGroup naviGroup) {
        App app = this.appRepository.getReferenceById(appId);
        naviGroup.setApp(app);
        return naviGroupRepository.save(naviGroup);
    }


    public NaviItem addNaviItem(Long groupId, NaviItem naviItem) {
        NaviGroup group = this.naviGroupRepository.getReferenceById(groupId);
        naviItem.setGroup(group);
        return naviItemRepository.save(naviItem);
    }

    public Map<String, Object> removeNaviGroup(Long groupId) {
        Map<String, Object> data = new HashMap<>();
        this.naviGroupRepository.findById(groupId);

        this.naviGroupRepository.deleteById(groupId);
        data.put("success", true);
        return data;
    }

    public Map<String, Object> removeNaviItem(Long itemId) {
        Map<String, Object> data = new HashMap<>();
        this.naviItemRepository.deleteById(itemId);
        data.put("success", true);
        return data;
    }

    public List<Map<String, Long>> saveItemOrder(List<Map<String, Long>> naviItemList) {
        for (Map<String, Long> element : naviItemList) {
            NaviItem fi = naviItemRepository.getReferenceById(element.get("id"));
            fi.setSortOrder(element.get("sortOrder"));
            naviItemRepository.save(fi);
        }
        return naviItemList;
    }

    public NaviItem moveItem(long siId, long newGroupId, Long sortOrder) {
        NaviItem si = naviItemRepository.getReferenceById(siId);
        NaviGroup newSection = naviGroupRepository.getReferenceById(newGroupId);

        si.setGroup(newSection);
        si.setSortOrder(sortOrder);

        return naviItemRepository.save(si);
    }

    //    @Transactional
    public List<Map<String, Long>> saveGroupOrder(List<Map<String, Long>> elementList) {
        for (Map<String, Long> element : elementList) {
            NaviGroup fs = naviGroupRepository.getReferenceById(element.get("id"));
            fs.setSortOrder(element.get("sortOrder"));
            naviGroupRepository.save(fs);
        }
        return elementList;
    }


//    public App saveNavi(Long appId, List<NaviGroup> navi) {
//        App app = appRepository.getOne(appId);
//        navi.forEach(n-> n.setApp(app));
//        app.setNavis(navi);
//        return appRepository.save(app);
//    }

    @Transactional(readOnly = true)
    public Map getNaviData(Long appId) {

        Map<String, Object> obj = new HashMap<>();
        List<Form> forms = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Dataset> datasets = datasetRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        List<Dashboard> dashboards = dashboardRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        List<Screen> screens = screenRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        List<Lookup> lookups = lookupRepository.findByAppId("%", appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();


        forms.forEach(f -> {
            obj.put("form-" + f.getId(), f);
            if (f.isSingle()) {
                obj.put("form-single-" + f.getId(), f);
                obj.put("view-single-" + f.getId(), f);
            }
        });

        datasets.forEach(d -> obj.put("dataset-" + d.getId(), d));

        dashboards.forEach(d -> obj.put("dashboard-" + d.getId(), d));

        screens.forEach(d -> obj.put("screen-" + d.getId(), d));

        lookups.forEach(d -> obj.put("lookup-" + d.getId(), d));

        return obj;
    }

    @Transactional(readOnly = true)
    public Map getNaviDataByEmail(Long appId, String email) {

        Map<String, Object> obj = new HashMap<>();

        List<NaviGroup> group = findNaviByAppIdAndEmail(appId,email);

        List<Long> datasetInNavi = new ArrayList<>();
        List<Long> screenInNavi = new ArrayList<>();
        List<Long> formInNavi = new ArrayList<>();
        List<Long> formSingleInNavi = new ArrayList<>();
        List<Long> viewSingleInNavi = new ArrayList<>();
        List<Long> dashboardInNavi = new ArrayList<>();
        List<Long> lookupInNavi = new ArrayList<>();

//        System.out.println(group);

        group.forEach(g->{
            g.getItems().forEach(i->{
//                System.out.println(i.getType());
                if ("form".equals(i.getType())||"form-single".equals(i.getType())||"view-single".equals(i.getType())){
                    formInNavi.add(i.getScreenId());
                }
                if ("form-single".equals(i.getType())||"view-single".equals(i.getType())){
                    formSingleInNavi.add(i.getScreenId());
                }
                if ("view-single".equals(i.getType())){
                    viewSingleInNavi.add(i.getScreenId());
                }
                if ("dashboard".equals(i.getType())){
                    dashboardInNavi.add(i.getScreenId());
                }
                if ("lookup".equals(i.getType())){
                    lookupInNavi.add(i.getScreenId());
                }
                if ("dataset".equals(i.getType())){
                    datasetInNavi.add(i.getScreenId());
                }
                if ("screen".equals(i.getType())){
                    screenInNavi.add(i.getScreenId());
                }
            });
        });

        List<Form> forms = formRepository.findByIdsAndEmail(formInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Form> formSingles = formRepository.findByIdsAndEmail(formSingleInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Form> viewSingles = formRepository.findByIdsAndEmail(viewSingleInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Dataset> datasets = datasetRepository.findByIdsAndEmail(datasetInNavi, email);
        List<Dashboard> dashboards = dashboardRepository.findByIdsAndEmail(dashboardInNavi, email);
        List<Screen> screens = screenRepository.findByIdsAndEmail(screenInNavi, email);
        List<Lookup> lookups = lookupRepository.findByIdsAndEmail("%", lookupInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        forms.forEach(f -> obj.put("form-" + f.getId(), f));

        formSingles.forEach(d -> obj.put("form-single-" + d.getId(), d));

        viewSingles.forEach(d -> obj.put("view-single-" + d.getId(), d));

        datasets.forEach(d -> obj.put("dataset-" + d.getId(), d));

        dashboards.forEach(d -> obj.put("dashboard-" + d.getId(), d));

        screens.forEach(d -> obj.put("screen-" + d.getId(), d));

        lookups.forEach(d -> obj.put("lookup-" + d.getId(), d));

        return obj;
    }

    @Transactional(readOnly = true)
    public Map getCounts(Long appId) {

        Map<String, Object> obj = new HashMap<>();
        long forms = formRepository.countByAppId(appId);
        long datasets = datasetRepository.countByAppId(appId);
        long dashboards = dashboardRepository.countByAppId(appId);
        long screens = screenRepository.countByAppId(appId);
        long users = userRepository.countByAppId(appId);
        boolean navi = naviGroupRepository.countByAppId(appId) > 0;// app.getNavis().size()>0;

        obj.put("form", forms);
        obj.put("dataset", datasets);
        obj.put("dashboard", dashboards);
        obj.put("screen", screens);
        obj.put("navi", navi);
        obj.put("users", users);

        return obj;
    }

    @Transactional(readOnly = true)
    public List<Map> getPages(Long appId) {
        List<Map> dataList = new ArrayList<>();
        List<Form> forms = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC,"sortOrder"))).getContent();
        List<Dataset> datasets = datasetRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC,"sortOrder")));
        List<Dashboard> dashboards = dashboardRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC,"sortOrder")));
        List<Screen> screens = screenRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC,"sortOrder")));
        List<Lambda> lambdas = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        Map<String, String> profile = new HashMap<>();
        profile.put("title", "Profile");
        profile.put("path", "profile");
        dataList.add(profile);

        forms.forEach(f -> {
            Map<String, String> i = new HashMap<>();
            i.put("title", "Add " + f.getTitle());
            i.put("path", "form/" + f.getId() + "/add");
            dataList.add(i);

            if (f.isSingle()) {
                Map<String, String> i2 = new HashMap<>();
                i2.put("title", "Edit " + f.getTitle());
                i2.put("path", "form/" + f.getId() + "/edit-single");
                dataList.add(i2);

                Map<String, String> i3 = new HashMap<>();
                i3.put("title", "View " + f.getTitle());
                i3.put("path", "form/" + f.getId() + "/view");
                dataList.add(i3);
            }
        });

        datasets.forEach(d -> {
            Map<String, String> i = new HashMap<>();
            i.put("title", "Dataset " + d.getTitle());
            i.put("path", "dataset/" + d.getId());
            dataList.add(i);
        });

        dashboards.forEach(c -> {
            Map<String, String> i = new HashMap<>();
            i.put("title", "Dashboard " + c.getTitle());
            i.put("path", "dashboard/" + c.getId());
            dataList.add(i);

        });

        screens.forEach(s -> {
            Map<String, String> i = new HashMap<>();
            i.put("title", "Screen " + s.getTitle());
            i.put("path", "screen/" + s.getId());
            dataList.add(i);
        });

        lambdas.forEach(l -> {
            Map<String, String> i = new HashMap<>();
            i.put("title", "Lambda " + l.getName());
            i.put("path", "web/" + l.getCode());
            dataList.add(i);
        });

        return dataList;

    }

    public Map<String, Object> onceDone(Long appId, String email, Boolean val) {
        Map<String, Object> data;
        ObjectMapper mapper = new ObjectMapper();

        User user = userRepository.findFirstByEmailAndAppId(email, appId).get();
        user.setOnce(val);
        userRepository.save(user);

        data = mapper.convertValue(user, Map.class);
        List<AppUser> groups = appUserRepository.findByUserIdAndStatus(user.getId(), "approved");
        Map<Long, UserGroup> groupMap = groups.stream().collect(
                Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
        data.put("groups", groupMap);

        return data;
    }

    public Map<String, Object> removeAcc(Long appId, String email) {
        Map<String, Object> data = new HashMap<>();

        appUserRepository.findByAppIdAndEmail(appId,email).forEach(appUserRepository::delete);

        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);
        userOpt.ifPresent(userRepository::delete);

        data.put("success",true);

        return data;
    }


    public User updateUser(Long userId, User payload) {
        User user = userRepository.getReferenceById(userId);
        user.setName(payload.getName());
        user.setEmail(payload.getEmail());
        user.setProvider(payload.getProvider());
        return userRepository.save(user);
    }

    public Map removeBulkUser(List<Long> userIdList) {
        userRepository.deleteAllById(userIdList);
        return Map.of("success", true);
    }


    public Map changeProviderBulkUser(String provider, List<Long> userIdList) {
        List<User> userList = userRepository.findAllById(userIdList);
        userList.forEach(user -> user.setProvider(AuthProvider.valueOf(provider)));
        userRepository.saveAll(userList);
        return Map.of("success", true, "rows", userList.size());
    }

    public Map blastBulkUser(Long appId,Map<String, String> data, List<Long> userIdList) {
        App app = appRepository.findById(appId).orElseThrow();
        List<User> userList = userRepository.findAllById(userIdList);
        userList.forEach(user -> {
            mailService.sendMail(app.getAppPath() + "_" + Constant.LEAP_MAILER,new String[]{user.getEmail()},null,null,data.get("subject"), data.get("content"), app);
        });
        return Map.of("success", true, "rows", userList.size());
    }


    public List<ApiKey> getApiKeys(Long appId){
        return apiKeyRepository.findByAppId(appId);
    }

    @Transactional
    public Map<String, Object> removeApiKey(long apiKeyId){
        Map<String, Object> data = new HashMap();
        apiKeyRepository.deleteById(apiKeyId);
        data.put("success", true);
        return data;
    }

    @Transactional
    public ApiKey generateNewApiKey(Long appId){
        ApiKey apiKey = new ApiKey();
        apiKey.setAppId(appId);
        String apiKeyStr = RandomStringUtils.randomAlphanumeric(16);
        apiKey.setApiKey(apiKeyStr);
        apiKey.setTimestamp(new Date());
        return apiKeyRepository.save(apiKey);
    }

}
