package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.filter.AppFilter;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import static com.benzourry.leap.config.Constant.UI_BASE_DOMAIN;

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
                      BucketRepository bucketRepository,
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
        this.pushSubRepository = pushSubRepository;
    }

    public App save(App app, String email) {
        if (Optional.ofNullable(app.getId()).isEmpty()) {
            app.setEmail(email);
        }
        return this.appRepository.save(app);
    }


    public App findById(Long appId) {
//        System.out.println("findById");
        return this.appRepository.findById(appId)
                .orElseThrow(()->new ResourceNotFoundException("App","id",appId));
    }

//    public App findByIdAndEmail(Long appId, final String email){
//
//        App a = this.appRepository.findById(appId).get();
//        a.setNavis(
//        a.getNavis().stream().filter(g->{
//            if (g.getAccess()!=null && g.getAccess().getUsers()!=null){
//                return g.getAccess().getUsers().contains(email);
//            }else{
//                return true;
//            }}).collect(toList())
//        );
//        return a;
//    }

    public List<NaviGroup> findNaviByAppIdAndEmail(Long appId, String email) {

        if (email != null) {
            return naviGroupRepository.findByAppIdAndEMail(appId, email);
        } else {
            return naviGroupRepository.findByAppId(appId);
        }
    }

//    public List<NaviGroup> findNaviByAppIdAndUserId(Long appId, Long userId) {
//
//        if (userId != null) {
//            return naviGroupRepository.findByAppIdAndUserId(appId, userId);
//        } else {
//            return naviGroupRepository.findByAppId(appId);
//        }
//    }


    @Transactional
    public void delete(Long appId, String email) {
        App app = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id",appId));
        String [] emails = app.getEmail().split(",");
        if (emails.length>1){
            List newEmails = Arrays.asList(emails);
            newEmails.remove(emails);
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
        List<Screen> screenList = screenRepository.findByAppId(appId);
        screenRepository.deleteAll(screenList);

        List<Dataset> datasetList = datasetRepository.findByAppId(appId);
        datasetRepository.deleteAll(datasetList);

        List<Dashboard> dashboardList = dashboardRepository.findByAppId(appId);
        dashboardRepository.deleteAll(dashboardList);

        List<NaviGroup> naviGroupList = naviGroupRepository.findByAppId(appId);
        naviGroupRepository.deleteAll(naviGroupList);

        List<Lookup> lookupList = lookupRepository.findByAppId("%", appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        lookupList.forEach(l -> lookupEntryRepository.deleteByLookupId(l.getId()));
        lookupRepository.deleteAll(lookupList);

        List<Form> formList = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        formRepository.saveAllAndFlush(formList.stream().map(f->{
            f.setAdmin(null);
//            f.setAccess(null);
            return f;
        }).toList());
        formRepository.deleteAll(formList);

        List<Lambda> lambdaList = lambdaRepository.findByAppId(appId, PageRequest.of(0,Integer.MAX_VALUE)).getContent();
        lambdaRepository.deleteAll(lambdaList);

//        lookupRepository.deleteByAppId(appId);
        emailTemplateRepository.deleteByAppId(appId);
        endpointRepository.deleteByAppId(appId);
        userGroupRepository.deleteByAppId(appId);
        bucketRepository.deleteByAppId(appId);
        scheduleRepository.deleteByAppId(appId);

        this.appRepository.deleteById(appId);
    }

    @Transactional
    public void deleteEntry(Long appId) {
        List<Form> formList = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        formList.forEach(f -> {
            entryRepository.deleteApproverByFormId(f.getId());
            entryRepository.deleteApprovalByFormId(f.getId());
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

    public Page<App> getAdminList(String email, String searchText, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.appRepository.findAll(AppFilter.builder()
                .email(email)
                .searchText(searchText)
                .status(Arrays.asList("local", "published"))
                .build().filter(), pageable);
//        return this.appRepository.findByEmail(email,searchText,pageable);
    }

//    public Page<App> getSharedList(String email,String searchText, Pageable pageable) {
//        searchText = "%"+searchText.toUpperCase()+"%";
//        return this.appRepository.findAll(AppFilter.builder()
//        .emailNot(email)
//        .searchText(searchText)
//        .shared(true)
//        .build().filter(), pageable);
////        return this.appRepository.findByEmail(email,searchText,pageable);
//    }


    public Page<AppUser> findUserByAppId(Long appId, String searchText, List<String> status, Long group, Pageable pageable) {
        searchText = "%" + searchText + "%";
        if (group!=null){
            System.out.println("with group");
            return appUserRepository.findByAppIdAndParam(appId, searchText, status, group, pageable);
        }else{
            System.out.println("no group");
            return appUserRepository.findAllByAppId(appId, searchText, status, pageable);

        }
    }

    public Page<AppUser> findAllByAppId(Long appId, String searchText, List<String> status, Pageable pageable) {
        searchText = "%" + searchText + "%";
        return appUserRepository.findAllByAppId(appId, searchText, status, pageable);
    }

    public List<AppUser> findByAppIdAndEmail(Long appId, String email) {
        return appUserRepository.findByAppIdAndEmail(appId, email);
    }

    public Map<String, Object> regUserBulk(List<UserGroup> groups, Long appId, String emaillist,Boolean autoReg){
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
                    regUser(groups,appId,email,name,autoReg);
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

    public Map<String, Object> regUser(List<UserGroup> groups, Long appId, String email, String name, Boolean autoReg) {

        Map<String, Object> data = new HashMap<>();

        App app = appRepository.getReferenceById(appId);
        final boolean fAutoReg = app.getEmail().contains(email) || autoReg;
//        User user = userRepository.findByEmailAndAppId(email,appId).get();
//        System.out.println("USER_EMAIL:"+email);
        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);
        User user;

        if (userOpt.isPresent()) {
//            System.out.println("---->user.isPresent"); //sbb usually user saved first when signin
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
        groups.forEach(g -> {

            AppUser appUser;
            Optional<AppUser> appUserOptional = appUserRepository.findByUserIdAndGroupId(user.getId(), g.getId());
            appUser = appUserOptional.orElseGet(AppUser::new);

            appUser.setUser(user);
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

//        user.setStatus(pending.get() > 0 ? "pending" : "approved");
        user.setStatus(approved.get() > 0 ? "approved" : "pending");
        userRepository.save(user);

        Map<String, Object> userMap;
        ObjectMapper mapper = new ObjectMapper();
//        User user = userRepository.findByEmailAndAppId(email, appId).get();
//        user.setOnce(val);
//        userRepository.save(user);

        userMap = mapper.convertValue(user, Map.class);
        List<AppUser> appUserListApproved = appUserList.stream().filter(au -> "approved".equals(au.getStatus())).collect(Collectors.toList());
        Map<Long, UserGroup> groupMap = appUserListApproved.stream().collect(
                Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
        userMap.put("groups", groupMap);

        data.put("appUserList", appUserList);
        data.put("user", userMap);
        return data;
    }


//    public boolean check(String appPath) {
//        long count = 0;
//
//        if (!Helper.isNullOrEmpty(appPath)){
//            count = this.appRepository.checkByPath(appPath);
//        }
//        return count>0;
//    }

    public boolean checkByKey(String appPath) {
        long count = 0;

        if (!Helper.isNullOrEmpty(appPath)) {
            String[] p = appPath.split(":");
            count = switch (p[0]) {
                case "domain" -> this.appRepository.checkByDomain(p[1]);
                case "path" -> this.appRepository.checkByPath(p[1]);
                default -> this.appRepository.checkByPath(p[0]);
            };
        }
//        System.out.println("AppPath["+appPath+"] Count:"+count);
        return count > 0;
    }

//    public App findByPath(String path) {
//        return this.appRepository.findByAppPath(path);
//    }
//
//    public App findByDomain(String domain) {
//        return this.appRepository.findByAppDomain(domain);
//    }


    public App findByKey(String key) {
        // System.out.println("findByKey");
        App app = null;
        if (!Helper.isNullOrEmpty(key)) {
            String[] p = key.split(":");
            app = switch (p[0]) {
                case "domain" -> this.appRepository.findByAppDomain(p[1]);
                case "path" -> this.appRepository.findByAppPath(p[1]);
                default -> this.appRepository.findByAppPath(key);
            };
        }
        //System.out.println("AppId+AppName:"+app.getId()+"+"+app.getTitle());
        return app;
    }


//    public Map<String, Object> findByKeyAndEmail(String key, String email) {
//        Map<String, Object> data = new HashMap<>();
//        App app = null;
//        if (!Helper.isNullOrEmpty(key)) {
//            String[] p = key.split(":");
//            app = switch (p[0]) {
//                case "domain" -> this.appRepository.findByAppDomain(p[1]);
//                case "path" -> this.appRepository.findByAppPath(p[1]);
//                default -> this.appRepository.findByAppPath(key);
//            };
//        }
//
//        List<AppUser> appUserList = findByAppIdAndEmail(app.getId(), email);
//        data.put("roles", appUserList);
//        data.put("app", app);
//        return data;
//    }


//    public App findByKeyAndEmail(String key) {
//        App app = null;
//        if (!Helper.isNullOrEmpty(key)){
//            String [] p = key.split(":");
//            switch (p[0]){
//                case "domain":
//                    app = this.appRepository.findByAppDomain(p[1]);
//                    break;
//                case "path":
//                    app = this.appRepository.findByAppPath(p[1]);
//                    break;
//                default:
//                    app = this.appRepository.findByAppPath(key);
//            }
//        }
//        return app;
//    }


    public App cloneApp(App app, String email) {

        Long appId = app.getId();

        // increase count of clone to get the popularity
        App k = appRepository.getReferenceById(appId);
        k.setClone(Optional.ofNullable(k.getClone()).orElse(0L) + 1);
        appRepository.save(k);


        app.setId(null);
        App newApp = appRepository.save(app);
        newApp.setEmail(email);
        if (!Optional.ofNullable(email).orElse("").contains("@unimas")) {
            newApp.setUseUnimas(false);
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
//        formRepository.saveAll(formListNew); //save all form

        formListOld.forEach(oldForm -> {
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
                    newSection.setParent(tabMap.get(oldSection.getParent()).getId());
                }
                newSection.setForm(newForm);
                newSection.setItems(siSet);
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
//            formListNew.add(newForm);
        });
//        formRepository.saveAll(formListNew); //save all form


        //// COPY DATASET
        List<Dataset> datasetListOld = datasetRepository.findByAppId(appId);
        Map<Long, Dataset> datasetMap = new HashMap<>();
        List<Dataset> datasetListNew = new ArrayList<>();
        datasetListOld.forEach(oldDataset -> {

            Dataset newDataset = new Dataset();
            BeanUtils.copyProperties(oldDataset, newDataset, "id");
            if (oldDataset.getAccess() != null) {
                newDataset.setAccess(groupMap.get(oldDataset.getAccess().getId()));
            }

            List<DatasetItem> newDatasetItemList = new ArrayList<>();
            Set<DatasetFilter> newDatasetFilterList = new HashSet<>();
            oldDataset.getItems().forEach(oldDatasetItem -> {
                DatasetItem newDatasetItem = new DatasetItem();
                BeanUtils.copyProperties(oldDatasetItem, newDatasetItem, "id");
                newDatasetItem.setDataset(newDataset);
                newDatasetItemList.add(newDatasetItem);
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
            newDataset.setFilters(newDatasetFilterList);

            datasetListNew.add(newDataset);
            datasetMap.put(oldDataset.getId(), newDataset);
        });
        datasetRepository.saveAll(datasetListNew);


        ///// COPY SCREEN
        List<Screen> screenListOld = screenRepository.findByAppId(appId);
        List<Screen> screenListNew = new ArrayList<>();
        Map<Long, Screen> screenMap = new HashMap<>();
        screenListOld.forEach(oldScreen -> {
            Screen newScreen = new Screen();
            BeanUtils.copyProperties(oldScreen, newScreen, "id", "actions");
            if (oldScreen.getAccess() != null) {
                newScreen.setAccess(groupMap.get(oldScreen.getAccess().getId()));
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
                } else {
                    if (formMap.get(sa.getNext()) != null) {
                        sa2.setNext(formMap.get(sa.getNext()).getId());
                    }
                }
                sa2.setScreen(newScreen);
                sActions.add(sa2);
            });

            newScreen.setActions(sActions);
            screenListNew.add(newScreen);
        });

        screenRepository.saveAll(screenListNew);


        //// COPY DASHBOARD
        List<Dashboard> dashboardListOld = dashboardRepository.findByAppId(appId);
        List<Dashboard> dashboardListNew = new ArrayList<>();
        Map<Long, Dashboard> dashboardMap = new HashMap<>();
        dashboardListOld.forEach(oldDashboard -> {
            Dashboard newDashboard = new Dashboard();
            BeanUtils.copyProperties(oldDashboard, newDashboard, "id");
            if (oldDashboard.getAccess() != null) {
                newDashboard.setAccess(groupMap.get(oldDashboard.getAccess().getId()));
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
                    if (datasetMap.get(oldNaviItem.getScreenId()) != null) {
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
//        System.out.println(app.getTitle());
        cloneRequest.setApp(app);
        cloneRequest = cloneRequestRepository.save(cloneRequest);
        String[] to = Optional.ofNullable(app.getEmail()).orElse("").replace(" ", "").split(",");

        try {
            mailService.sendMail(Constant.LEAP_MAILER, to, null, null,
                    "Request to copy [" + app.getTitle() + "]",
                    "Hi, please note that <b>" + requesterEmail + "</b> has requested to copy <b>" + cloneRequest.getApp().getTitle() + "</b>. <br/>You may activate or reject requester request by going to Copy Request at the left pane of your application editor.");
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
                    "Hi, please note that your request to copy <b>" + cloneRequest.getApp().getTitle() + "</b> has been <b>" + status + "</b>.<br/>You may visit the repository and copy the application by clicking on the 'Protected' button and 'Copy App' on the bottom right of the popup dialog.");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return cloneRequest;
    }

    public Map<String, Object> getManifest(String path) {

        App k = findByKey(path);//.getOne(appId);
        Map<String, Object> manifest = new HashMap();
        if (k!=null) {

            String url = "https://" + k.getAppPath() + "." + UI_BASE_DOMAIN;

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
            manifest.put("theme_color", k.getTheme());
            manifest.put("background_color", k.getTheme());
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

    public Map getNaviData(Long appId) {

        Map<String, Object> obj = new HashMap<>();
        List<Form> forms = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Dataset> datasets = datasetRepository.findByAppId(appId);
        List<Dashboard> dashboards = dashboardRepository.findByAppId(appId);
        List<Screen> screens = screenRepository.findByAppId(appId);
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

        System.out.println("$$$$$$$$$$$$$$$$$$$$");
        System.out.println(formInNavi);
        List<Form> forms = formRepository.findByIdsAndEmail(formInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        System.out.println("&&&&&&&&&&&&&&&&&&&&");
        List<Form> formSingles = formRepository.findByIdsAndEmail(formSingleInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Form> viewSingles = formRepository.findByIdsAndEmail(viewSingleInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        System.out.println(datasetInNavi);
        List<Dataset> datasets = datasetRepository.findByIdsAndEmail(datasetInNavi, email);
        System.out.println(datasets);
        List<Dashboard> dashboards = dashboardRepository.findByIdsAndEmail(dashboardInNavi, email);
        List<Screen> screens = screenRepository.findByIdsAndEmail(screenInNavi, email);
        List<Lookup> lookups = lookupRepository.findByIdsAndEmail("%", lookupInNavi, email, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        System.out.println("^^^^^^^^^^^^^^^^^^^");


        forms.forEach(f -> obj.put("form-" + f.getId(), f));

        formSingles.forEach(d -> obj.put("form-single-" + d.getId(), d));

        viewSingles.forEach(d -> obj.put("view-single-" + d.getId(), d));

        datasets.forEach(d -> obj.put("dataset-" + d.getId(), d));

        dashboards.forEach(d -> obj.put("dashboard-" + d.getId(), d));

        screens.forEach(d -> obj.put("screen-" + d.getId(), d));

        lookups.forEach(d -> obj.put("lookup-" + d.getId(), d));

        return obj;
    }

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

    public List<Map> getPages(Long appId) {
        List<Map> dataList = new ArrayList<>();
        List<Form> forms = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE)).getContent();
        List<Dataset> datasets = datasetRepository.findByAppId(appId);
        List<Dashboard> dashboards = dashboardRepository.findByAppId(appId);
        List<Screen> screens = screenRepository.findByAppId(appId);

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

        appUserRepository.findByAppIdAndEmail(appId,email).forEach(au-> appUserRepository.delete(au));

        User user = userRepository.findFirstByEmailAndAppId(email, appId).get();
        userRepository.delete(user);


        data.put("success",true);

        return data;
    }


}
