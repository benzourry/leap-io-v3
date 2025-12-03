package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.filter.AppFilter;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.AtomicDouble;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
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
    public final ScreenActionRepository screenActionRepository;
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
    public final NotificationRepository notificationRepository;
    public final RestorePointRepository restorePointRepository;
    public final EntryTrailRepository entryTrailRepository;
    public final CognaPromptHistoryRepository cognaPromptHistoryRepository;
    public final CognaRepository cognaRepository;
    public final TierRepository tierRepository;
    private final ObjectMapper MAPPER;
    final MailService mailService;

    public AppService(AppRepository appRepository, FormRepository formRepository,
                      TabRepository tabRepository,
                      CloneRequestRepository cloneRequestRepository,
                      DatasetRepository datasetRepository,
                      DashboardRepository dashboardRepository,
                      ScreenRepository screenRepository,
                      ScreenActionRepository screenActionRepository,
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
                      NotificationRepository notificationRepository,
                      RestorePointRepository restorePointRepository,
                      EntryTrailRepository entryTrailRepository,
                      CognaPromptHistoryRepository cognaPromptHistoryRepository,
                      CognaRepository cognaRepository,
                      TierRepository tierRepository,
                      MailService mailService,
                      ObjectMapper MAPPER) {
        this.appRepository = appRepository;
        this.formRepository = formRepository;
        this.tabRepository = tabRepository;
        this.cloneRequestRepository = cloneRequestRepository;
        this.datasetRepository = datasetRepository;
        this.dashboardRepository = dashboardRepository;
        this.screenRepository = screenRepository;
        this.screenActionRepository = screenActionRepository;
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
        this.notificationRepository = notificationRepository;
        this.restorePointRepository = restorePointRepository;
        this.entryTrailRepository = entryTrailRepository;
        this.cognaPromptHistoryRepository = cognaPromptHistoryRepository;
        this.cognaRepository = cognaRepository;
        this.tierRepository = tierRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.MAPPER = MAPPER;
    }

    public App save(App app, String email) {

//        Ensure the path is not conflict
        if (app.getAppPath() != null) {
            if (app.getId() == null) { // if new app
                if (checkByKey("path:" + app.getAppPath())) {
                    throw new RuntimeException("App path " + app.getAppPath() + " is already taken.");
                }
            } else { // if not new
                App byKey = findByKey("path:" + app.getAppPath());
                if (byKey != null && !Objects.equals(byKey.getId(), app.getId())) {
                    throw new RuntimeException("App path " + app.getAppPath() + " is already taken.");
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
            .orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));
    }

    @Transactional
    public App setLive(Long appId, Boolean status) {
        App app = appRepository.findById(appId).orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));
        ObjectNode x = (ObjectNode) app.getX();
        if (x == null) {
            x = MAPPER.createObjectNode();
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
                .orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));
        String[] emails = app.getEmail().split(",");
        if (emails.length > 1) {
            List<String> newEmails = Arrays.asList(emails);
            newEmails.forEach(e -> e.trim());
            newEmails.remove(email.trim());
            app.setEmail(String.join(",", newEmails));
            appRepository.save(app);
        } else {
            if (app.getEmail().toLowerCase().trim().equals(email.toLowerCase().trim())) {
                // if the email is the only email, delete the app
                // but first delete all the entries and users
                // then delete the app
                // this will ensure that the app is not deleted if there are still entries or users
                // otherwise it will throw an error
                deleteEntry(appId);
                deleteUsers(appId);
                deleteApp(appId);
            }
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
        formRepository.saveAllAndFlush(formList.parallelStream().map(f -> {
            f.setAdmin(null);
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
            System.out.println("Removing form: " + f.getId() + " - " + f.getTitle());
            entryRepository.deleteApproverByFormId(f.getId());
            entryRepository.deleteApprovalByFormId(f.getId());
            entryRepository.deleteTrailByFormId(f.getId());
            entryRepository.deleteApprovalTrailByFormId(f.getId());
            entryRepository.deleteByFormId(f.getId());
        });

        List<EntryAttachment> entryAttachmentList = entryAttachmentRepository.findByAppId(appId);


        // UNTUK DELETE FILE
        entryAttachmentList.forEach(entryAttachment -> {
            String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
            if (entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            String fileName = entryAttachment.getFileUrl();
            if (fileName == null || fileName.contains("..")) {
                return;
            }

            File dest = new File(destStr, fileName);
            if (dest.exists() && !dest.delete()) {
//                log.warn("Failed to delete file: {}", dest.getAbsolutePath());
            }

            entryAttachmentRepository.delete(entryAttachment);
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

    public Page<App> getSuperAdminList(String searchText, Boolean live, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.appRepository.findAll(AppFilter.builder()
                .searchText(searchText)
                .live(live)
//                .status(Arrays.asList("local", "published"))
                .build().filter(), pageable);
    }

    public Page<App> getMyList(String email, String searchText, Boolean live, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.appRepository.findAll(AppFilter.builder()
                .email(email)
                .searchText(searchText)
                .live(live)
                .status(Arrays.asList("local", "published"))
                .build().filter(), pageable);
    }

    public Page<AppUser> findUserByAppId(Long appId, String searchText, List<String> status, Long group, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";

        if (group != null) {
            return appUserRepository.findByGroupIdAndParams(group, searchText, status, Optional.ofNullable(status).orElse(List.of()).isEmpty(), pageable);
//            return appUserRepository.findByAppIdAndParam(appId, searchText, status, group, pageable);
        } else {
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

    public Map<String, Object> regUserBulk(List<Long> groups, Long appId, String emaillist, Boolean autoReg, List<String> tags) {
        Map<String, Object> data = new HashMap<>();
        if (!emaillist.isBlank()) {
            Arrays.asList(emaillist.split(",")).forEach(em -> {
                String[] splitted = em.split("\\|");
                String email = splitted[0].trim();
                String name = email;
                if (splitted.length > 1) {
                    name = splitted[1].trim();
                }
                if (!email.isBlank()) {
                    regUser(groups, appId, email, null, name, autoReg, tags);
                }
            });
            data.put("success", true);
            data.put("message", "Users successfully added");
        } else {
            data.put("success", false);
            data.put("message", "Email list cannot be blank");
        }
        return data;
    }

    @Transactional
    public Map<String, Object> regUser(List<Long> groups, Long appId, String email, Long userId, String name, Boolean autoReg, List<String> tags) {

        Map<String, Object> data = new HashMap<>();

        if (email != null) {
            email = email.trim();
        }

        App app = appRepository.getReferenceById(appId);
        final boolean fAutoReg = app.getEmail().contains(email) || autoReg;

        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);
        if (userId != null) {
            userOpt = userRepository.findById(userId);
        }

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

            UserGroup g = userGroupRepository.findById(gId).orElseThrow(() -> new ResourceNotFoundException("UserGroup", "id", gId));

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

        userMap = MAPPER.convertValue(user, Map.class);
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
                case "path" -> this.appRepository.checkByPath(p[1].replaceAll("--dev", ""));
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
                case "path" -> this.appRepository.findByAppPath(p[1].replaceAll("--dev", ""));
                default -> this.appRepository.findByAppPath(key);
            };
        }
        return app;
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
        App app = findByKey(path);
        if (app == null) {
            return Collections.emptyMap();
        }

        String baseUrl;
        if (app.getAppDomain() != null) {
            baseUrl = "https://" + app.getAppDomain();
        } else {
            String devSuffix = app.isLive() ? "" : "--dev";
            baseUrl = "https://" + app.getAppPath() + devSuffix + "." + Constant.UI_BASE_DOMAIN;
        }

        String logoTemplate = app.getLogo() != null
                ? IO_BASE_DOMAIN + "/api/app/" + app.getAppPath() + "/logo/{0}"
                : baseUrl + "/assets/icons/icon-{0}.png";

        // Build icons list directly
        List<Map<String, String>> icons = Arrays.asList(72, 96, 192, 512).stream()
                .map(size -> Map.of(
                        "src", MessageFormat.format(logoTemplate, size),
                        "sizes", size + "x" + size,
                        "type", "image/png"
                ))
                .toList(); // Java 16+, otherwise use collect(Collectors.toList())

        String themeColor = Optional.ofNullable(app.getTheme()).orElse("#2c2c2c");

        Map<String, Object> manifest = new HashMap<>();
        manifest.put("name", app.getTitle());
        manifest.put("short_name", app.getTitle());
        manifest.put("display", "standalone");
        manifest.put("theme_color", themeColor);
        manifest.put("background_color", themeColor);
        manifest.put("start_url", baseUrl);
        manifest.put("scope", baseUrl + "/");
        manifest.put("icons", icons);

        return manifest;
    }

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

        List<NaviGroup> group = findNaviByAppIdAndEmail(appId, email);

        List<Long> datasetInNavi = new ArrayList<>();
        List<Long> screenInNavi = new ArrayList<>();
        List<Long> formInNavi = new ArrayList<>();
        List<Long> formSingleInNavi = new ArrayList<>();
        List<Long> viewSingleInNavi = new ArrayList<>();
        List<Long> dashboardInNavi = new ArrayList<>();
        List<Long> lookupInNavi = new ArrayList<>();

        for (NaviGroup g : group) {
            for (NaviItem i : g.getItems()) {
                String type = i.getType();
                Long id = i.getScreenId();

                if (id != null) {
                    if ("form".equals(i.getType())) {
                        formInNavi.add(id);
                    } else if ("form-single".equals(type)) {
                        formSingleInNavi.add(id);
                    } else if ("view-single".equals(type)) {
                        viewSingleInNavi.add(id);
                    } else if ("dashboard".equals(type)) {
                        dashboardInNavi.add(id);
                    } else if ("lookup".equals(type)) {
                        lookupInNavi.add(id);
                    } else if ("dataset".equals(type)) {
                        datasetInNavi.add(id);
                    } else if ("screen".equals(type)) {
                        screenInNavi.add(id);
                    }
                }
            }
        }

        PageRequest unlimited = PageRequest.of(0, Integer.MAX_VALUE);

        if(!formInNavi.isEmpty()){
            formRepository.findByIdsAndEmail(formInNavi, email, unlimited)
                .forEach(f -> obj.put("form-" + f.getId(), f));
        }
        if (!formSingleInNavi.isEmpty()){
            formRepository.findByIdsAndEmail(formSingleInNavi, email, unlimited)
                .forEach(d -> obj.put("form-single-" + d.getId(), d));
        }
        if (!viewSingleInNavi.isEmpty()){
            formRepository.findByIdsAndEmail(viewSingleInNavi, email, unlimited)
                .forEach(d -> obj.put("view-single-" + d.getId(), d));
        }
        if (!datasetInNavi.isEmpty()){
            datasetRepository.findByIdsAndEmail(datasetInNavi, email)
                .forEach(d -> obj.put("dataset-" + d.getId(), d));
        }
        if (!dashboardInNavi.isEmpty()){
            dashboardRepository.findByIdsAndEmail(dashboardInNavi, email)
                .forEach(d -> obj.put("dashboard-" + d.getId(), d));
        }
        if (!screenInNavi.isEmpty()){
            screenRepository.findByIdsAndEmail(screenInNavi, email)
                .forEach(d -> obj.put("screen-" + d.getId(), d));
        }
        if (!lookupInNavi.isEmpty()){
            lookupRepository.findByIdsAndEmail("%", lookupInNavi, email, unlimited)
                .forEach(d -> obj.put("lookup-" + d.getId(), d));
        }

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
    public Map getSummary(Long appId) {

        Map<String, Object> obj = new HashMap<>();

        long formCount = formRepository.countByAppId(appId);
        long datasetCount = datasetRepository.countByAppId(appId);
        long dashboardCount = dashboardRepository.countByAppId(appId);
        long screenCount = screenRepository.countByAppId(appId);
        boolean hasNavi = naviGroupRepository.countByAppId(appId) > 0;// app.getNavis().size()>0;
        //bilangan lambda
        long lambdaCount = lambdaRepository.countByAppId(appId);
        //bilangan lookup,
        long lookupCount = lookupRepository.countByAppId(appId);
        //bilangan mailer
        long mailerCount = emailTemplateRepository.countByAppId(appId);
        //bilangan endpoint
        long endpointCount = endpointRepository.countByAppId(appId);
        //bilangan bucket, bilangan file, saiz
        long bucketCount = bucketRepository.countByAppId(appId);
        //bilangan cogna

        //// Need this to ensure any Map.of using LinkedHashMap
        Map<String, Object> map = new LinkedHashMap<>();

        long userCount = Optional.ofNullable(userRepository.countByAppId(appId)).orElse(0L);
        long entryCount = Optional.ofNullable(entryRepository.statTotalCount(appId)).orElse(0L);
        long entrySize = Optional.ofNullable(entryRepository.statTotalSize(appId)).orElse(0L);
        long attachmentCount = Optional.ofNullable(entryAttachmentRepository.statTotalCountByAppId(appId)).orElse(0L);
        long attachmentSize = Optional.ofNullable(entryAttachmentRepository.statTotalSizeByAppId(appId)).orElse(0L);
        long notificationTotal = Optional.ofNullable(notificationRepository.countByAppId(appId)).orElse(0L);
        long restorePointCount = Optional.ofNullable(restorePointRepository.countByAppId(appId)).orElse(0L);
        long entryTrailCount = Optional.ofNullable(entryTrailRepository.countByAppId(appId)).orElse(0L);
        long entryApprovalTrailCount = 0;
        long cognaPromptHistoryCount = Optional.ofNullable(cognaPromptHistoryRepository.countByAppId(appId)).orElse(0L);

        Map<String, Object> bucketStat = Map.of("typeCount", Optional.ofNullable(entryAttachmentRepository.statCountByFileTypeByAppId(appId)).orElse(List.of()),
                "typeSize", Optional.ofNullable(entryAttachmentRepository.statSizeByFileTypeByAppId(appId)).orElse(List.of()),
                "labelCount", Optional.ofNullable(entryAttachmentRepository.statCountByItemLabelByAppId(appId)).orElse(List.of()),
                "labelSize", Optional.ofNullable(entryAttachmentRepository.statSizeByItemLabelByAppId(appId)).orElse(List.of()),
                "totalSize", attachmentSize,
                "totalCount", attachmentCount,
                "bucketCount", bucketCount);

        List<Map> entryCountByYearMonth = entryRepository.statCountByYearMonth(appId);

        List<Map> entryCountByYearMonthCumulative = entryRepository.statCountByYearMonthCumulative(appId);

        Map<String, Object> entryStat = Map.of("entryCount", entryCount,
                "formCount", entryRepository.statCountByForm(appId),
                "monthlyCount", sortNameValue(entryCountByYearMonth),
                "monthlyCountCumulative", sortNameValue(entryCountByYearMonthCumulative)
        );


        List<Map> userCountByYearMonth = userRepository.statCountByYearMonth(appId);
        List<Map> userCountByYearMonthCumulative = userRepository.statCountByYearMonthCumulative(appId);

        Map<String, Object> userStat = Map.of(
                "totalCount", userCount,
                "typeCount", userRepository.statCountByType(appId),
                "monthlyCount", sortNameValue(userCountByYearMonth),
                "monthlyCountCumulative", sortNameValue(userCountByYearMonthCumulative)
        );


        List<Map> attachmentCountByYearMonth = entryAttachmentRepository.statCountByYearMonth(appId);

        List<Map> attachmentCountByYearMonthCumulative = entryAttachmentRepository.statCountByYearMonthCumulative(appId);

        List<Map> attachmentSizeByYearMonth = entryAttachmentRepository.statSizeByYearMonth(appId);

        List<Map> attachmentSizeByYearMonthCumulative = entryAttachmentRepository.statSizeByYearMonthCumulative(appId);

        Map<String, Object> attachmentStat = Map.of(
//                "totalCount", userCount,
//                "typeCount", userRepository.statCountByType(appId),
                "monthlyCount", sortNameValue(attachmentCountByYearMonth),
                "monthlyCountCumulative", sortNameValue(attachmentCountByYearMonthCumulative),
                "monthlySize", sortNameValue(attachmentSizeByYearMonth),
                "monthlySizeCumulative", sortNameValue(attachmentSizeByYearMonthCumulative)
        );


        obj.put("design", Map.of(
                "form", formCount,
                "dataset", datasetCount,
                "dashboard", dashboardCount,
                "screen", screenCount,
                "navi", hasNavi,
                "lambda", lambdaCount,
                "lookup", lookupCount,
                "mailer", mailerCount,
                "endpoint", endpointCount
        ));
        obj.put("data", Map.of(
                "entry", entryCount,
                "entrySize", entrySize,
                "attachment", attachmentCount,
                "attachmentSize", attachmentSize,
                "notification", notificationTotal,
                "users", userCount,
                "restorePoint", restorePointCount,
                "entryTrail", entryTrailCount,
                "approvalTrail", entryApprovalTrailCount,
                "promptLog", cognaPromptHistoryCount
        ));

        obj.put("users", userStat);
        obj.put("bucket", bucketStat);
        obj.put("entry", entryStat);
        obj.put("attachment", attachmentStat);


        return obj;
    }


    public Map<String, Object> getPlatformSummary() {
        List<Map> appStatByLive = appRepository.statCountByLive();

        List<Map> entryStatByYearMonth = entryRepository.statCountByYearMonth();

        List<Map> entryStatByYearMonthCumulative = entryRepository.statCountByYearMonthCumulative(); // cumulateNameValue(entryStatByYearMonth);

        List<Map> entryStatByApp = entryRepository.statCountByApp();

        List<Map> userStatByApp = userRepository.statCountByApp();

        List<Map> userStatByYearMonth = userRepository.statCountByYearMonth(null);
        List<Map> userStatByYearMonthCumulative = userRepository.statCountByYearMonthCumulative(null);// cumulateNameValue(userRepository.statCountByYearMonth());

        List<Map> attachmentStatByApp = entryAttachmentRepository.statSizeByApp();

        List<Map> attachmentStatByYearMonth = entryAttachmentRepository.statSizeByYearMonth(null);
        List<Map> attachmentStatByYearMonthCumulative = entryAttachmentRepository.statSizeByYearMonthCumulative(null);

//        List<Map> attachmentStatByYearMonthCumulative = cumulateNameValue(entryAttachmentRepository.statSizeByYearMonth());


        Map<String, Object> data = new HashMap<>();

        data.put("appStatByLive", sortNameValue(appStatByLive));
        data.put("entryStatByYearMonth", sortNameValue(entryStatByYearMonth));
        data.put("entryStatByYearMonthCumulative", sortNameValue(entryStatByYearMonthCumulative));
        data.put("entryStatByApp", sortNameValue(entryStatByApp));
        data.put("userStatByApp", sortNameValue(userStatByApp));
        data.put("userStatByYearMonth", sortNameValue(userStatByYearMonth));
        data.put("userStatByYearMonthCumulative", sortNameValue(userStatByYearMonthCumulative));
        data.put("attachmentStatByApp", sortNameValue(attachmentStatByApp));
        data.put("attachmentStatByYearMonth", sortNameValue(attachmentStatByYearMonth));
//        data.put("attachmentStatByYearMonthCum",sortNameValue(attachmentStatByYearMonthCum));
        data.put("attachmentStatByYearMonthCumulative", sortNameValue(attachmentStatByYearMonthCumulative));

        return data;
    }

    @NotNull
    private static List<Map> cumulateNameValue(List<Map> normalList) {
        AtomicDouble attachmentai = new AtomicDouble(0);
        return normalList.stream().map(i -> {
            Double d = ((Number) Optional.ofNullable(i.get("value")).orElse(0L)).doubleValue();
            return new TreeMap<>(Map.of("name", Optional.ofNullable(i.get("name")).orElse("n/a"), "value", attachmentai.addAndGet(d)));
        }).collect(Collectors.toList());
    }

    @NotNull
    private static List<Map> sortNameValue(List<Map> unsorted) {
        return unsorted.stream().map(i -> new TreeMap<>(Map.of("name", Optional.ofNullable(i.get("name")).orElse("n/a"), "value", i.get("value")))).collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<Map> getPages(Long appId) {
        List<Map> dataList = new ArrayList<>();
        List<Form> forms = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "sortOrder"))).getContent();
        List<Dataset> datasets = datasetRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "sortOrder")));
        List<Dashboard> dashboards = dashboardRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "sortOrder")));
        List<Screen> screens = screenRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "sortOrder")));
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

        User user = userRepository.findFirstByEmailAndAppId(email, appId).get();
        user.setOnce(val);
        userRepository.save(user);

        data = MAPPER.convertValue(user, Map.class);
        List<AppUser> groups = appUserRepository.findByUserIdAndStatus(user.getId(), "approved");
        Map<Long, UserGroup> groupMap = groups.stream().collect(
                Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
        data.put("groups", groupMap);

        return data;
    }

    public Map<String, Object> removeAcc(Long appId, String email) {
        Map<String, Object> data = new HashMap<>();

        appUserRepository.findByAppIdAndEmail(appId, email).forEach(appUserRepository::delete);

        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);
        userOpt.ifPresent(userRepository::delete);

        data.put("success", true);

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

    public Map blastBulkUser(Long appId, Map<String, String> data, List<Long> userIdList) {
        App app = appRepository.findById(appId).orElseThrow();
        List<User> userList = userRepository.findAllById(userIdList);
        userList.forEach(user -> {
            mailService.sendMail(app.getAppPath() + "_" + Constant.LEAP_MAILER, new String[]{user.getEmail()}, null, null, data.get("subject"), data.get("content"), app);
        });
        return Map.of("success", true, "rows", userList.size());
    }

    public List<ApiKey> getApiKeys(Long appId) {
        return apiKeyRepository.findByAppId(appId);
    }

    @Transactional
    public Map<String, Object> removeApiKey(long apiKeyId) {
        Map<String, Object> data = new HashMap();
        apiKeyRepository.deleteById(apiKeyId);
        data.put("success", true);
        return data;
    }

    @Transactional
    public ApiKey generateNewApiKey(Long appId) {
        ApiKey apiKey = new ApiKey();
        apiKey.setAppId(appId);
        String apiKeyStr = RandomStringUtils.randomAlphanumeric(16);
        apiKey.setApiKey(apiKeyStr);
        apiKey.setTimestamp(new Date());
        return apiKeyRepository.save(apiKey);
    }

    @Transactional
    public App cloneApp(App targetApp, String email) {

        Long appId = targetApp.getId();

        // increase count of clone to get the popularity
        App oriApp = appRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));
        oriApp.setClone(Optional.ofNullable(oriApp.getClone()).orElse(0L) + 1);
        appRepository.save(oriApp);


        targetApp.setId(null);

        App newApp = appRepository.save(targetApp);

        //// COPY USER GROUP
        List<UserGroup> groupListOld = userGroupRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY LOOKUP AND ENTRIES
        List<Lookup> lookupListOld = lookupRepository.findByAppId("%", appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        Map<Long, List<LookupEntry>> lookupEntries = new HashMap<>();
        lookupListOld.forEach(lookup -> {
            if ("db".equals(lookup.getSourceType())) {
                lookupEntries.put(lookup.getId(), lookupEntryRepository.findByLookupId(lookup.getId(), null, null, null, null, PageRequest.of(0, Integer.MAX_VALUE))
                        .getContent());
            }
        });

        //// COPY MAILER TEMPLATE
        List<EmailTemplate> emailListOld = emailTemplateRepository.findByAppId(appId, "%", PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY Lambda
        List<Lambda> lambdaListOld = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY Bucket
        List<Bucket> bucketListOld = bucketRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY FORM LIST
        List<Form> formListOld = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY DATASET
        List<Dataset> datasetListOld = datasetRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "sortOrder")));

        //// COPY DASHBOARD
        List<Dashboard> dashboardListOld = dashboardRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));

        ///// COPY SCREEN
        List<Screen> screenListOld = screenRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));

        //// COPY NAVIGROUP
        List<NaviGroup> naviGroupListOld = naviGroupRepository.findByAppId(appId);

        //// COPY Cogna
        List<Cogna> cognaListOld = cognaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY Endpoint
        List<Endpoint> endpointListOld = endpointRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY Schedule
        List<Schedule> scheduleListOld = scheduleRepository.findByAppId(appId);


        App importedApp = __importApp(newApp.getId(),
                AppWrapper.builder()
                        .app(oriApp)
                        .roles(groupListOld)
                        .lookups(lookupListOld)
                        .lookupEntries(lookupEntries)
                        .mailers(emailListOld)
                        .lambdas(lambdaListOld)
                        .buckets(bucketListOld)
                        .forms(formListOld)
                        .datasets(datasetListOld)
                        .dashboards(dashboardListOld)
                        .screens(screenListOld)
                        .navis(naviGroupListOld)
                        .cognas(cognaListOld)
                        .endpoints(endpointListOld)
                        .schedules(scheduleListOld)
                        .build(), email, true);

        appRepository.save(importedApp);


        return newApp;
    }


    public AppWrapper exportApp(Long appId) {


        App app = appRepository.findById(appId).orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));

        //// COPY LOOKUP AND ENTRIES
        List<Lookup> lookupList = lookupRepository.findByAppId("%", appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        Map<Long, List<LookupEntry>> lookupEntries = new HashMap<>();
        lookupList.forEach(lookup -> {
            if ("db".equals(lookup.getSourceType())) {
                lookupEntries.put(lookup.getId(), lookupEntryRepository.findByLookupId(lookup.getId(), null, null, null, null, PageRequest.of(0, Integer.MAX_VALUE))
                        .getContent());
            }
        });

        //// COPY MAILER TEMPLATE
        List<EmailTemplate> emailList = emailTemplateRepository.findByAppId(appId, "%", PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY Lambda
        List<Lambda> lambdaList = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY USER GROUP
        List<UserGroup> groupList = userGroupRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY Bucket
        List<Bucket> bucketList = bucketRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY FORM LIST
        List<Form> formList = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        //// COPY DATASET
        List<Dataset> datasetList = datasetRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC, "sortOrder")));

        //// COPY DASHBOARD
        List<Dashboard> dashboardList = dashboardRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));

        List<Cogna> cognaList = cognaRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE)).getContent();

        ///// COPY SCREEN
        List<Screen> screenList = screenRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));

        List<Endpoint> endpointList = endpointRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE)).getContent();

        //// COPY NAVIGROUP
        List<NaviGroup> naviGroupList = naviGroupRepository.findByAppId(appId);

        List<Schedule> scheduleList = scheduleRepository.findByAppId(appId);

        return AppWrapper.builder()
                .app(app)
                .baseIo(IO_BASE_DOMAIN)
                .baseUi(UI_BASE_DOMAIN)
                .lookups(lookupList)
                .mailers(emailList)
                .endpoints(endpointList)
                .roles(groupList)
                .buckets(bucketList)
                .lambdas(lambdaList)
                .cognas(cognaList)
                .forms(formList)
                .datasets(datasetList)
                .dashboards(dashboardList)
                .screens(screenList)
                .navis(naviGroupList)
                .schedules(scheduleList)
                .lookupEntries(lookupEntries)
                .build();
    }

    @Transactional
    public App importApp(Long appId, AppWrapper appwrapper, String email) {
        return __importApp(appId, appwrapper, email, false);
    }

    private App __importApp(Long appId, AppWrapper appwrapper, String email, boolean keepOldIfNotFound) {

        App sourceApp = appwrapper.getApp(); // sourceapp

        App targetApp = appRepository.findById(appId).orElse(sourceApp);

        // source, target, exclude
        BeanUtils.copyProperties(sourceApp, targetApp, "id", "appPath", "appDomain", "title", "description", "email", "group");
//        targetApp.setId(null);

        /****START IMPORT CODES****/
        App newApp = appRepository.save(targetApp);
        newApp.setEmail(email);
        if (!Optional.ofNullable(email).orElse("").contains("@unimas")) {
            newApp.setUseUnimas(false);
            newApp.setUseUnimasid(false);
        }
        newApp.setStatus("local");

        appRepository.save(newApp);


        //// COPY USER GROUP
//        Page<UserGroup> groupPaged = userGroupRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<UserGroup> groupListOld = Optional.ofNullable(appwrapper.getRoles()).orElse(List.of());
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


        //// COPY LOOKUP AND ENTRIES
//        Page<Lookup> lookupPaged = lookupRepository.findByAppId("%", appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Lookup> lookupListOld = Optional.ofNullable(appwrapper.getLookups()).orElse(List.of());
        List<Lookup> lookupListNew = new ArrayList<>();
        Map<Long, Lookup> lookupMap = new HashMap<>();
        lookupListOld.forEach(lookup -> {
            Lookup d2 = new Lookup();
            BeanUtils.copyProperties(lookup, d2, "id");
            d2.setApp(newApp);
            if (lookup.getAccessList() != null) {
                List<Long> newAccessList = new ArrayList<>();
                lookup.getAccessList().forEach(a -> {
                    if (groupMap.get(a) != null) {
                        newAccessList.add(groupMap.get(a).getId());
                    } else {
                        newAccessList.add(a);
                    }
                });
                d2.setAccessList(newAccessList);
            }
            lookupListNew.add(d2);
            lookupMap.put(lookup.getId(), d2);
        });
        lookupRepository.saveAll(lookupListNew);

        lookupListOld.forEach(lookup -> {
            List<LookupEntry> lookupEntryList = new ArrayList<>();

            if (appwrapper.getLookupEntries() != null) {
                if (appwrapper.getLookupEntries().get(lookup.getId()) != null) {
                    appwrapper.getLookupEntries().get(lookup.getId()).forEach(le -> {
                        LookupEntry le2 = new LookupEntry();
                        BeanUtils.copyProperties(le, le2, "id");
                        le2.setLookup(lookupMap.get(lookup.getId()));
                        lookupEntryList.add(le2);
                    });
                    lookupEntryRepository.saveAll(lookupEntryList);
                }
            }
        });

        //// COPY MAILER TEMPLATE
//        Page<EmailTemplate> emailPaged = emailTemplateRepository.findByAppId(appId, "%", PageRequest.of(0, Integer.MAX_VALUE));
        List<EmailTemplate> emailListOld = Optional.ofNullable(appwrapper.getMailers()).orElse(List.of());
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
//        Page<Lambda> lambdaPaged = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Lambda> lambdaListOld = Optional.ofNullable(appwrapper.getLambdas()).orElse(List.of());
        List<Lambda> lambdaListNew = new ArrayList<>();
        Map<Long, Lambda> lambdaMap = new HashMap<>();
        Map<String, Lambda> lambdaCodeMap = new HashMap<>();
        lambdaListOld.forEach(l -> {
            Lambda l2 = new Lambda();
            BeanUtils.copyProperties(l, l2, "id");
            l2.setCode(l.getCode() + "-" + newApp.getId());
            l2.setEmail(email);
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

            lambdaCodeMap.put(l.getCode(), l2);

            l2.setBinds(newLambdaBindList);

        });
        lambdaRepository.saveAll(lambdaListNew);


        //// COPY Bucket
//        Page<Bucket> bucketPaged = bucketRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Bucket> bucketListOld = Optional.ofNullable(appwrapper.getBuckets()).orElse(List.of());
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
//        Page<Form> f = formRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Form> formListOld = Optional.ofNullable(appwrapper.getForms()).orElse(List.of());
        List<Form> formListNew = new ArrayList<>();
        Map<Long, Form> formMap = new HashMap<>();
        Map<Long, Tab> tabMap = new HashMap<>();
        Map<Long, Section> sectionMap = new HashMap<>();

        formListOld.forEach(oldForm -> {
            Form newForm = new Form();
            BeanUtils.copyProperties(oldForm, newForm, "id", "prev");
            if (oldForm.getAccessList() != null) {
                List<Long> newAccessList = new ArrayList<>();
                oldForm.getAccessList().forEach(a -> {
                    if (groupMap.get(a) != null) {
                        newAccessList.add(groupMap.get(a).getId());
                    } else {
                        newAccessList.add(a);
                    }
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
            oldForm.getItems().forEach((name, oldItem) -> {
                Item newItem = new Item();
                BeanUtils.copyProperties(oldItem, newItem, "id");
                if (oldItem.getX() != null) {
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
            Map<Long, Tier> tierMap = new HashMap<>();
            oldForm.getTiers().forEach(oldTier -> {
                Tier newTier = new Tier();
                BeanUtils.copyProperties(oldTier, newTier, "id");
//                List<Long> submitMailerNew = new ArrayList<>();
                for (Long i : oldTier.getSubmitMailer()) {
                    if (emailMap.get(i) != null) {
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

                // Perlu new HashMap, mn x nya akan klua error shared collection
                Map<String, TierAction> newActionMap = new HashMap<>();//newTier.getActions();
                oldTier.getActions().forEach((name, oldTAction) -> {
                    TierAction newTa = new TierAction();
                    BeanUtils.copyProperties(oldTAction, newTa, "id");
                    for (Long i : oldTAction.getMailer()) {
                        if (emailMap.get(i) != null) {
                            newTa.getMailer().add((emailMap.get(i).getId()));
                        }
                    }
//                    if (oldTAction.getNextTier() != null) {
////                        newTa.setNextTier(tierMap);
//                    }
                    newTa.setTier(newTier);
                    newActionMap.put(name, newTa);
                });

                // Perlu set or akan dapat error setting transient on persistent
                newTier.setActions(newActionMap);

                tierRepository.save(newTier);
                tierMap.put(oldTier.getId(), newTier);
                newTierList.add(newTier);

            });

            formRepository.save(newForm);

            newTierList.forEach(nT -> {
                nT.getActions().forEach((name, nTA) -> {
                    if (nTA.getNextTier() != null) {
                        if (tierMap.get(nTA.getNextTier()) != null) {
                            nTA.setNextTier(tierMap.get(nTA.getNextTier()).getId());
                        } else {
                            nTA.setNextTier(null);
                        }
                    }
                });

//                tierRepository.save(nT);
            });

            formListNew.add(newForm);
//            formMap.put(oldForm.getId(), newForm);
        });
//        formRepository.saveAll(formListNew); //save all form


        //// COPY DATASET
        List<Dataset> datasetListOld = Optional.ofNullable(appwrapper.getDatasets()).orElse(List.of()); // datasetRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE, Sort.by(Sort.Direction.ASC,"sortOrder")));
        Map<Long, Dataset> datasetMap = new HashMap<>();
        List<Dataset> datasetListNew = new ArrayList<>();
        datasetListOld.forEach(oldDataset -> {

            Dataset newDataset = new Dataset();
            BeanUtils.copyProperties(oldDataset, newDataset, "id");

            if (appwrapper.getApp().getId() == null || Objects.equals(appwrapper.getApp().getId(), oldDataset.getAppId())) {
                newDataset.setAppId(newApp.getId());
            }

            if (oldDataset.getAccessList() != null) {
                List<Long> newAccessList = new ArrayList<>();
                oldDataset.getAccessList().forEach(ac -> {
//                    UserGroup ug = groupMap.get(ac);
                    if (groupMap.get(ac) != null) {
                        newAccessList.add(groupMap.get(ac).getId());
                    } else {
                        newAccessList.add(ac);
                    }
                    newDataset.setAccessList(newAccessList);
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
                if (formMap.get(oldDataset.getForm().getId()) != null) { // if form for dataset is within the same app
                    newDataset.setForm(formMap.get(oldDataset.getForm().getId()));
                } else { // if dataset is created with form from other app
                    newDataset.setForm(oldDataset.getForm());
                }
            }
            newDataset.setApp(newApp);
            newDataset.setItems(newDatasetItemList);
            newDataset.setActions(newDatasetActionList);
            newDataset.setFilters(newDatasetFilterList);

            datasetListNew.add(newDataset);
            datasetRepository.save(newDataset);
            datasetMap.put(oldDataset.getId(), newDataset);
            System.out.println("ds-old:" + oldDataset.getId() + ",ds-new:" + newDataset.getId());
        });
//        datasetRepository.saveAll(datasetListNew);
//        datasetMap

        //// COPY DASHBOARD
        List<Dashboard> dashboardListOld = Optional.ofNullable(appwrapper.getDashboards()).orElse(List.of()); // dashboardRepository.findByAppId(appId,PageRequest.ofSize(Integer.MAX_VALUE));
        List<Dashboard> dashboardListNew = new ArrayList<>();
        Map<Long, Dashboard> dashboardMap = new HashMap<>();
        Map<Long, Chart> chartMap = new HashMap<>();
        dashboardListOld.forEach(oldDashboard -> {
            Dashboard newDashboard = new Dashboard();
            BeanUtils.copyProperties(oldDashboard, newDashboard, "id");
            if (oldDashboard.getAccessList() != null) {
                List<Long> newAccessList = new ArrayList<>();
                oldDashboard.getAccessList().forEach(ac -> {
                    if (groupMap.get(ac) != null) {
                        newAccessList.add(groupMap.get(ac).getId());
                    } else {
                        newAccessList.add(ac);
                    }
                    newDashboard.setAccessList(newAccessList);
                });
            }


            Set<Chart> charts = new HashSet<>();
            oldDashboard.getCharts().forEach(oldChart -> {
                Chart newChart = new Chart();

                BeanUtils.copyProperties(oldChart, newChart, "id");
                newChart.setDashboard(newDashboard);
                if (oldChart.getForm() != null) {
                    if (formMap.get(oldChart.getForm().getId()) != null) { // if form for chart is within the same app
                        newChart.setForm(formMap.get(oldChart.getForm().getId()));
                    } else {// if chart is created with form from other app
                        newChart.setForm(oldChart.getForm());
                    }
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
                chartMap.put(oldChart.getId(), newChart);
            });

            newDashboard.setApp(newApp);
            newDashboard.setCharts(charts);
            dashboardListNew.add(newDashboard);
            dashboardMap.put(oldDashboard.getId(), newDashboard);
        });
        dashboardRepository.saveAll(dashboardListNew);


        ///// COPY SCREEN
        List<Screen> screenListOld = Optional.ofNullable(appwrapper.getScreens()).orElse(List.of()); // screenRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE));
        List<Screen> screenListNew = new ArrayList<>();
        Map<Long, Screen> screenMap = new HashMap<>();
        screenListOld.forEach(oldScreen -> {
            Screen newScreen = new Screen();
            BeanUtils.copyProperties(oldScreen, newScreen, "id", "actions");
            if (oldScreen.getAccessList() != null) {
                List<Long> newAccessList = new ArrayList<>();
                oldScreen.getAccessList().forEach(ac -> {
//                    UserGroup ug = groupMap.get(ac);
                    if (groupMap.get(ac) != null) {
                        newAccessList.add(groupMap.get(ac).getId());
                    } else {
                        newAccessList.add(ac);
                    }
                    newScreen.setAccessList(newAccessList);
                });
            }

            newScreen.setForm(null);
            newScreen.setDataset(null);

            Set<Action> actions = new HashSet<>();
            newScreen.setActions(actions);

            newScreen.setApp(newApp);

            screenRepository.save(newScreen);

            screenMap.put(oldScreen.getId(), newScreen);
        });
        /// then set screen actions after persist screen above;
        screenListOld.forEach(oldScreen -> {
            Screen newScreen = screenMap.get(oldScreen.getId());

            if ("page".equals(oldScreen.getType())) {
                if (oldScreen.getForm() != null) {
                    if (formMap.get(oldScreen.getForm().getId()) != null) {
                        newScreen.setForm(formMap.get(oldScreen.getForm().getId()));
                    } else {
                        if (keepOldIfNotFound) {
                            newScreen.setForm(oldScreen.getForm());
                        }
                    }
                }
            } else if (Set.of("list", "calendar", "map").contains(oldScreen.getType())) {
                if (oldScreen.getDataset() != null) {
                    if (datasetMap.get(oldScreen.getDataset().getId()) != null) {
                        newScreen.setDataset(datasetMap.get(oldScreen.getDataset().getId()));
                    } else {
                        if (keepOldIfNotFound) {
                            newScreen.setDataset(oldScreen.getDataset());
                        }
                    }
                }
            }

            Set<Action> sActions = newScreen.getActions();

            Map<String, String> ACTION_REPLACE_HARDCODES = new HashMap<>();

            oldScreen.getActions().forEach(sa -> {
                Action sa2 = new Action();
                BeanUtils.copyProperties(sa, sa2, "id");
                if (appwrapper.getApp().getId() == null || Objects.equals(appwrapper.getApp().getId(), sa.getAppId())) {
                    sa2.setAppId(newApp.getId());
                }

                if (List.of("screen", "static").contains(sa.getNextType())) {
                    if (screenMap.get(sa.getNext()) != null) {
                        sa2.setNext(screenMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("form", "view", "view-single", "edit", "edit-single", "prev", "facet").contains(sa.getNextType())) {
                    if (formMap.get(sa.getNext()) != null) {
                        sa2.setNext(formMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("dataset").contains(sa.getNextType())) {
                    if (datasetMap.get(sa.getNext()) != null) {
                        sa2.setNext(datasetMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("dashboard").contains(sa.getNextType())) {
                    if (dashboardMap.get(sa.getNext()) != null) {
                        sa2.setNext(dashboardMap.get(sa.getNext()).getId());
                    }
                } else if (List.of("user").contains(sa.getNextType())) {
                    if (groupMap.get(sa.getNext()) != null) {
                        sa2.setNext(groupMap.get(sa.getNext()).getId());
                    }
                }
                sa2.setScreen(newScreen);
                Action action = screenActionRepository.save(sa2);
                sActions.add(action);

                ACTION_REPLACE_HARDCODES.put("$go['" + sa.getId() + "']", "$go['" + action.getId() + "']");
                ACTION_REPLACE_HARDCODES.put("$go[\"" + sa.getId() + "\"]", "$go['" + action.getId() + "']");
                ACTION_REPLACE_HARDCODES.put("$popup['" + sa.getId() + "']", "$popup['" + action.getId() + "']");
                ACTION_REPLACE_HARDCODES.put("$popup[\"" + sa.getId() + "\"]", "$popup['" + action.getId() + "']");
            });

            newScreen.setActions(sActions);

            /** REPLACE HARDCODED **/
            if (newScreen.getData() != null) {
                Map<String, Object> map = Optional.ofNullable(MAPPER.convertValue(newScreen.getData(), Map.class)).orElse(Map.of());

                map.keySet().forEach(k -> {
                    if (map.get(k) instanceof String) {
                        String newV = Helper.replaceMulti((String) map.get(k), ACTION_REPLACE_HARDCODES);
                        map.put(k, newV);
                    }
                });
                /** END REPLACE HARDCODED **/

                newScreen.setData(MAPPER.valueToTree(map));
            }

            screenListNew.add(newScreen);
        });

        screenRepository.saveAll(screenListNew);

        // setting datasource, etc

        // kemungkinan tok penyebab double. SAH
        formListNew.forEach(newForm -> {
            newForm.getItems().forEach((name, item) -> {
                if (item.getDataSource() != null) {
                    Long newDs = null;

                    if (List.of("modelPicker", "dataset").contains(item.getType())) {
                        if (datasetMap.get(item.getDataSource()) != null) {
                            System.out.println("ada dataset:" + item.getDataSource());
                            newDs = datasetMap.get(item.getDataSource()).getId();
                        }
                    } else if (List.of("screen").contains(item.getType())) {
                        if (screenMap.get(item.getDataSource()) != null) {
                            System.out.println("ada screen:" + item.getDataSource());
                            newDs = screenMap.get(item.getDataSource()).getId();
                        }
                    } else {
                        if (lookupMap.get(item.getDataSource()) != null) {
                            System.out.println("ada lookup:" + item.getDataSource());
                            newDs = lookupMap.get(item.getDataSource()).getId();
                        }
                    }
                    System.out.println("form:" + newForm.getTitle() + "/" + newForm.getId() + "item:" + item.getLabel());
                    System.out.println("item f# old-ds:" + item.getDataSource() + ", new-ds:" + newDs);
                    item.setDataSource(newDs);
                }
            });

//            formRepository.save(newForm);
        });
        formRepository.saveAll(formListNew);


        //// COPY NAVIGROUP
        List<NaviGroup> naviGroupListOld = Optional.ofNullable(appwrapper.getNavis()).orElse(List.of()); // naviGroupRepository.findByAppId(appId);
        List<NaviGroup> naviGroupListNew = new ArrayList<>();

        naviGroupListOld.forEach(oldNaviGroup -> {
            NaviGroup newNaviGroup = new NaviGroup();
            BeanUtils.copyProperties(oldNaviGroup, newNaviGroup, "id");
            List<Long> newGroupAccessList = new ArrayList<>();
            if (oldNaviGroup.getAccessList() != null) {
                oldNaviGroup.getAccessList().forEach(ngA -> {
                    if (groupMap.get(ngA) != null) {
                        newGroupAccessList.add(groupMap.get(ngA).getId());
                    }
                });
                newNaviGroup.setAccessList(newGroupAccessList);
            }

            newNaviGroup.setApp(newApp);
            List<NaviItem> naviItemListOld = oldNaviGroup.getItems();
            List<NaviItem> naviItemListNew = new ArrayList<>();

            naviItemListOld.forEach(oldNaviItem -> {
                NaviItem newNaviItem = new NaviItem();
                BeanUtils.copyProperties(oldNaviItem, newNaviItem, "id");
                if (appwrapper.getApp().getId() == null || Objects.equals(appwrapper.getApp().getId(), oldNaviItem.getAppId())) {
                    newNaviItem.setAppId(newApp.getId());
                }

                if ("form".equals(oldNaviItem.getType()) ||
                        "form-single".equals(oldNaviItem.getType()) ||
                        "view-single".equals(oldNaviItem.getType())) {
                    if (formMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(formMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                else if ("dataset".equals(oldNaviItem.getType())) {
                    System.out.println("naviitem dataset:" + oldNaviItem.getScreenId());
                    if (datasetMap.get(oldNaviItem.getScreenId()) != null) {
                        System.out.println("naviitem dataset ##:" + (datasetMap.get(oldNaviItem.getScreenId()).getId()));
                        newNaviItem.setScreenId(datasetMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                else if ("lookup".equals(oldNaviItem.getType())) {
                    if (lookupMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(lookupMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                else if ("screen".equals(oldNaviItem.getType())) {
                    if (screenMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(screenMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                else if ("dashboard".equals(oldNaviItem.getType())) {
                    if (dashboardMap.get(oldNaviItem.getScreenId()) != null) {
                        newNaviItem.setScreenId(dashboardMap.get(oldNaviItem.getScreenId()).getId());
                    }
                }
                else if ("external".equals(oldNaviItem.getType())) {
                    newNaviItem.setUrl(oldNaviItem.getUrl());//.put("url",i.get("url"));
                }

                newNaviItem.setGroup(newNaviGroup);
                naviItemListNew.add(newNaviItem);
            });

            newNaviGroup.setItems(naviItemListNew);
            naviGroupListNew.add(newNaviGroup);
        });

        naviGroupRepository.saveAll(naviGroupListNew);


        //// COPY Cogna
//        Page<Lambda> lambdaPaged = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Cogna> cognaListOld = Optional.ofNullable(appwrapper.getCognas()).orElse(List.of());
        List<Cogna> cognaListNew = new ArrayList<>();
        Map<Long, Cogna> cognaMap = new HashMap<>();
        Map<String, Cogna> cognaCodeMap = new HashMap<>();
        cognaListOld.forEach(c -> {
            Cogna c2 = new Cogna();
            BeanUtils.copyProperties(c, c2, "id");
            c2.setCode(c.getCode() + "-" + newApp.getId());
            c2.setEmail(email);
            c2.setApp(newApp);
            cognaListNew.add(c2);
            cognaMap.put(c.getId(), c2);
            cognaCodeMap.put(c.getCode(), c2);

            Set<CognaSource> newCognaSourceList = new HashSet<>();
            c.getSources().forEach(oldSource -> {
                CognaSource newCognaSource = new CognaSource();
                BeanUtils.copyProperties(oldSource, newCognaSource, "id");
                if (appwrapper.getApp().getId() == null || Objects.equals(appwrapper.getApp().getId(), oldSource.getAppId())) {
                    newCognaSource.setAppId(newApp.getId());
                }
                if ("dataset".equals(oldSource.getType())) {
                    if (datasetMap.get(newCognaSource.getSrcId()) != null) {
                        newCognaSource.setSrcId(datasetMap.get(newCognaSource.getSrcId()).getId());
                    }
                }
                else if ("bucket".equals(oldSource.getType())) {
                    if (bucketMap.get(newCognaSource.getSrcId()) != null) {
                        newCognaSource.setSrcId(bucketMap.get(newCognaSource.getSrcId()).getId());
                    }
                }
                newCognaSource.setCogna(c2);
                newCognaSourceList.add(newCognaSource);
            });
            c2.setSources(newCognaSourceList);

            Set<CognaTool> newCognaToolList = new HashSet<>();
            c.getTools().forEach(oldTool -> {
                CognaTool newCognaTool = new CognaTool();
                BeanUtils.copyProperties(oldTool, newCognaTool, "id");
                newCognaTool.setCogna(c2);
                newCognaToolList.add(newCognaTool);
            });
            c2.setTools(newCognaToolList);

            Set<CognaSub> newCognaSubList = new HashSet<>();
            c.getSubs().forEach(oldSub -> {
                CognaSub newCognaSub = new CognaSub();
                BeanUtils.copyProperties(oldSub, newCognaSub, "id");
                newCognaSub.setCogna(c2);
                newCognaSubList.add(newCognaSub);
            });
            c2.setSubs(newCognaSubList);

            Set<CognaMcp> newCognaMcpList = new HashSet<>();
            c.getMcps().forEach(oldMcp -> {
                CognaMcp newCognaMcp = new CognaMcp();
                BeanUtils.copyProperties(oldMcp, newCognaMcp, "id");
                newCognaMcp.setCogna(c2);
                newCognaMcpList.add(newCognaMcp);
            });
            c2.setMcps(newCognaMcpList);


        });
        cognaRepository.saveAll(cognaListNew);


        //// COPY Endpoint
//        Page<Lambda> lambdaPaged = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Endpoint> endpointListOld = Optional.ofNullable(appwrapper.getEndpoints()).orElse(List.of());
        List<Endpoint> endpointListNew = new ArrayList<>();
        Map<Long, Endpoint> endpointMap = new HashMap<>();
        Map<String, Endpoint> endpointCodeMap = new HashMap<>();
        endpointListOld.forEach(e -> {
            Endpoint e2 = new Endpoint();
            BeanUtils.copyProperties(e, e2, "id");
            e2.setCode(e.getCode() + "-" + newApp.getId());
            e2.setEmail(email);
            e2.setApp(newApp);
            endpointListNew.add(e2);
            endpointMap.put(e.getId(), e2);
            endpointCodeMap.put(e.getCode(), e2);

        });
        endpointRepository.saveAll(endpointListNew);

        //// COPY Schedule
//        Page<Lambda> lambdaPaged = lambdaRepository.findByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
        List<Schedule> scheduleListOld = Optional.ofNullable(appwrapper.getSchedules()).orElse(List.of());
        List<Schedule> scheduleListNew = new ArrayList<>();
        Map<Long, Schedule> scheduleMap = new HashMap<>();
        scheduleListOld.forEach(sched -> {
            Schedule sched2 = new Schedule();
            BeanUtils.copyProperties(sched, sched2, "id");
//            c2.setCode(c.getCode()+"-copy");
            sched2.setApp(newApp);
            scheduleListNew.add(sched2);
            scheduleMap.put(sched.getId(), sched2);

            if (datasetMap.get(sched.getDatasetId()) != null) {
                sched2.setDatasetId(datasetMap.get(sched.getDatasetId()).getId());
            }

            if (emailMap.get(sched.getMailerId()) != null) {
                sched2.setMailerId(emailMap.get(sched.getMailerId()).getId());
            }
        });
        scheduleRepository.saveAll(scheduleListNew);


        if (newApp.getStartPage() != null) {
            String[] spArr = newApp.getStartPage().split("/");

            String startPage = null;
            Long newPageId;

            if ("form".equals(spArr[0])) {
                if (formMap.get(Long.parseLong(spArr[1])) != null) {
                    newPageId = formMap.get(Long.parseLong(spArr[1])).getId();
                    startPage = "form/" + newPageId + "/" + spArr[2];
                }
            } else if ("dataset".equals(spArr[0])) {
                if (datasetMap.get(Long.parseLong(spArr[1])) != null) {
                    newPageId = datasetMap.get(Long.parseLong(spArr[1])).getId();
                    startPage = "dataset/" + newPageId;
                }
            } else if ("dashboard".equals(spArr[0])) {
                if (dashboardMap.get(Long.parseLong(spArr[1])) != null) {
                    newPageId = dashboardMap.get(Long.parseLong(spArr[1])).getId();
                    startPage = "dashboard/" + newPageId;
                }
            } else if ("screen".equals(spArr[0])) {
                if (screenMap.get(Long.parseLong(spArr[1])) != null) {
                    newPageId = screenMap.get(Long.parseLong(spArr[1])).getId();
                    startPage = "screen/" + newPageId;
                }
            } else if ("web".equals(spArr[0])) {
                if (lambdaCodeMap.get(spArr[1]) != null) {
                    startPage = "web/" + lambdaCodeMap.get(spArr[1]).getCode();
                }
            }
            newApp.setStartPage(startPage);
        }


        /** START REPLACE HARDCODED**/
        Map<String, String> REPLACE_HARDCODES = new HashMap<>();
        datasetMap.forEach((oldId, ds) -> {
            REPLACE_HARDCODES.put("datasetId=" + oldId, "datasetId=" + ds.getId());
            REPLACE_HARDCODES.put("dataset/" + oldId, "dataset/" + ds.getId());
            REPLACE_HARDCODES.put("dataset_" + oldId, "dataset_" + ds.getId());
            REPLACE_HARDCODES.put("_entry.dataset(" + oldId, "_entry.dataset(" + ds.getId());
        });
        formMap.forEach((oldId, form) -> {
            REPLACE_HARDCODES.put("formId=" + oldId, "formId=" + form.getId());
            REPLACE_HARDCODES.put("form/" + oldId + "/", "form/" + form.getId() + "/");
        });
        screenMap.forEach((oldId, screen) -> {
            REPLACE_HARDCODES.put("screen/" + oldId, "screen/" + screen.getId());
        });
        dashboardMap.forEach((oldId, dashboard) -> {
            REPLACE_HARDCODES.put("dashboard/" + oldId, "dashboard/" + dashboard.getId());
        });
        chartMap.forEach((oldId, chart) -> {
            REPLACE_HARDCODES.put("chart/" + oldId, "chart/" + chart.getId());
            REPLACE_HARDCODES.put("chart-map/" + oldId, "chart-map/" + chart.getId());
        });
        lookupMap.forEach((oldId, lookup) -> {
            REPLACE_HARDCODES.put("lookup/" + oldId, "lookup/" + lookup.getId());
            REPLACE_HARDCODES.put("_lookup.list(" + oldId, "_lookup.list(" + lookup.getId());
            REPLACE_HARDCODES.put("lookup_" + oldId, "lookup_" + lookup.getId());
        });
        groupMap.forEach((oldId, group) -> {
            REPLACE_HARDCODES.put("user/" + oldId, "user/" + group.getId());
            REPLACE_HARDCODES.put("$user$.groups['" + oldId + "']", "$user$.groups['" + group.getId() + "']");
            REPLACE_HARDCODES.put("$user$.groups[\"" + oldId + "\"]", "$user$.groups['" + group.getId() + "']");
        });
        lambdaCodeMap.forEach((oldCode, lambda) -> {
            REPLACE_HARDCODES.put("~/" + oldCode, "~/" + lambda.getCode());
            REPLACE_HARDCODES.put("web/" + oldCode, "web/" + lambda.getCode());
        });
        endpointCodeMap.forEach((oldCode, ep) -> {
            REPLACE_HARDCODES.put("$endpoint$('" + oldCode + "'", "$endpoint$('" + ep.getCode() + "'");
            REPLACE_HARDCODES.put("$endpoint$(\"" + oldCode + "\"", "$endpoint$('" + ep.getCode() + "'");
            REPLACE_HARDCODES.put("_endpoint.run('" + oldCode + "'", "_endpoint.run('" + ep.getCode() + "'");
            REPLACE_HARDCODES.put("_endpoint.run(\"" + oldCode + "\"", "_endpoint.run('" + ep.getCode() + "'");
        });


        Map<String, String> UI_HARDCODES = new HashMap<>(REPLACE_HARDCODES);
        UI_HARDCODES.put("{{$base$}}/api", "{{$baseApi$}}");
        UI_HARDCODES.put(appwrapper.getBaseIo() + "/api", "{{$baseApi$}}");
        UI_HARDCODES.put(appwrapper.getBaseIo(), "{{$base$}}");

        // replace dlm form
        formListNew.forEach(newForm -> {
            newForm.setF(Helper.replaceMulti(newForm.getF(), UI_HARDCODES));
            newForm.setOnSave(Helper.replaceMulti(newForm.getOnSave(), UI_HARDCODES));
            newForm.setOnSubmit(Helper.replaceMulti(newForm.getOnSubmit(), UI_HARDCODES));
            newForm.setOnView(Helper.replaceMulti(newForm.getOnView(), UI_HARDCODES));
            newForm.getItems().forEach((name, item) -> {
                item.setPost(Helper.replaceMulti(item.getPost(), UI_HARDCODES));
                item.setPre(Helper.replaceMulti(item.getPre(), UI_HARDCODES));
                item.setPlaceholder(Helper.replaceMulti(item.getPlaceholder(), UI_HARDCODES));
            });
            newForm.getTabs().forEach(tab -> {
                tab.setPre(Helper.replaceMulti(tab.getPre(), UI_HARDCODES));
                if (tab.getX() != null) {
                    Map<String, Object> map = Optional.ofNullable(MAPPER.convertValue(tab.getX(), Map.class)).orElse(Map.of());
                    map.keySet().forEach(k -> {
                        if (map.get(k) instanceof String) {
                            String newV = Helper.replaceMulti((String) map.get(k), UI_HARDCODES);
                            map.put(k, newV);
                        }
                    });
                    tab.setX(MAPPER.valueToTree(map));
                }
            });
            newForm.getTiers().forEach(tier -> {
                tier.setPre(Helper.replaceMulti(tier.getPre(), UI_HARDCODES));
                tier.setPost(Helper.replaceMulti(tier.getPost(), UI_HARDCODES));
                tier.getActions().forEach((actionCode, actionObj) -> {
                    actionObj.setPre(Helper.replaceMulti(actionObj.getPre(), UI_HARDCODES));
                });
            });
            newForm.getSections().forEach(section -> {
                section.setPre(Helper.replaceMulti(section.getPre(), UI_HARDCODES));
            });
        });
        formRepository.saveAll(formListNew);
        // replace dlm dataset
        datasetListNew.forEach(ds -> {
            if (ds.getX() != null) {
                Map<String, Object> map = Optional.ofNullable(MAPPER.convertValue(ds.getX(), Map.class)).orElse(Map.of());
                map.keySet().forEach(k -> {
                    if (map.get(k) instanceof String) {
                        String newV = Helper.replaceMulti((String) map.get(k), UI_HARDCODES);
                        map.put(k, newV);
                    }
                });
                ds.setX(MAPPER.valueToTree(map));
            }
            ds.getActions().forEach(dsa -> {
                dsa.setPre(Helper.replaceMulti(dsa.getPre(), UI_HARDCODES));
                dsa.setF(Helper.replaceMulti(dsa.getF(), UI_HARDCODES));
                dsa.setUrl(Helper.replaceMulti(dsa.getUrl(), UI_HARDCODES));
            });
        });
        datasetRepository.saveAll(datasetListNew);
        // replace dlm screen
        screenListNew.forEach(newScreen -> {
            if (newScreen.getData() != null) {
                Map<String, Object> map = Optional.ofNullable(MAPPER.convertValue(newScreen.getData(), Map.class)).orElse(Map.of());
                map.keySet().forEach(k -> {
                    if (map.get(k) instanceof String) {
                        String newV = Helper.replaceMulti((String) map.get(k), UI_HARDCODES);
                        map.put(k, newV);
                    }
                });
                newScreen.setData(MAPPER.valueToTree(map));
            }
        });
        screenRepository.saveAll(screenListNew);

        // replace dlm lambda
        lambdaListNew.forEach(newLambda -> {
            if (newLambda.getBinds() != null) {
                newLambda.getBinds().forEach(lb -> {
                    if ("dataset".equals(lb.getType()) && datasetMap.get(lb.getSrcId()) != null) {
                        lb.setSrcId(datasetMap.get(lb.getSrcId()).getId());
                    }
                    if ("lookup".equals(lb.getType()) && lookupMap.get(lb.getSrcId()) != null) {
                        lb.setSrcId(lookupMap.get(lb.getSrcId()).getId());
                    }
                });
            }

            if (newLambda.getData() != null) {
                Map<String, Object> map = Optional.ofNullable(MAPPER.convertValue(newLambda.getData(), Map.class)).orElse(Map.of());
                map.keySet().forEach(k -> {
                    if (map.get(k) instanceof String) {
                        String newV = Helper.replaceMulti((String) map.get(k), REPLACE_HARDCODES);
                        map.put(k, newV);
                    }
                });
                newLambda.setData(MAPPER.valueToTree(map));
            }
        });
        // replace dlm NaviGroup/Item
        naviGroupListNew.forEach(navig -> {
            navig.setPre(Helper.replaceMulti(navig.getPre(), UI_HARDCODES));
            navig.getItems().forEach(navii -> {
                navii.setPre(Helper.replaceMulti(navii.getPre(), UI_HARDCODES));
                navii.setUrl(Helper.replaceMulti(navii.getUrl(), UI_HARDCODES));
            });
        });
        naviGroupRepository.saveAll(naviGroupListNew);
        // replace dlm App F()
        newApp.setF(Helper.replaceMulti(newApp.getF(), UI_HARDCODES));

        System.out.println(UI_HARDCODES);

        /** END REPLACE HARDCODED */

        return appRepository.save(newApp);
    }
}
