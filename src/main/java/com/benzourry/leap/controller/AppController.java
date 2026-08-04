package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.mixin.AppMixin;
import com.benzourry.leap.mixin.MetadataMixin;
import com.benzourry.leap.mixin.NaviMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.CodeAutoRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.service.KeyValueService;
import com.benzourry.leap.service.NotificationService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/app")
//@CrossOrigin(allowCredentials = "true")
public class AppController {

    private static final Logger logger = LoggerFactory.getLogger(AppController.class);
    final AppService appService;
    final NotificationService notificationService;
    final CodeAutoRepository codeAutoRepository;
    final KeyValueService keyValueService;
    private final ObjectMapper MAPPER;

    public AppController(AppService appService,
                         NotificationService notificationService,
                         KeyValueService keyValueService,
                         CodeAutoRepository codeAutoRepository, ObjectMapper MAPPER) {
        this.appService = appService;
        this.keyValueService = keyValueService;
        this.notificationService = notificationService;
        this.codeAutoRepository = codeAutoRepository;
        this.MAPPER = MAPPER;
    }

    // Only cache app with path:
    @PostMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
    @PreAuthorize("@authz.isDesigner()")
    public App save(@RequestBody App app,
                    @CurrentUser UserPrincipal principal) {
        // existing app && principal is not app creator
        if (app.getId() != null && !allowAccess(principal, app)){
            logger.error("App update failure: App email:"+app.getEmail()+", Principal:"+principal.getName());
            throw new AuthorizationServiceException("Unauthorized modification by non-creator :" + principal.getName());
        }
        return this.appService.save(app, principal.getEmail());
    }

    public boolean allowAccess(UserPrincipal principal, App app) {
        // Fail-fast if the principal or email is unexpectedly null
        if (principal == null || principal.getEmail() == null) {
            return false;
        }

        String email = principal.getEmail();

        if (parseCsv(app.getEmail()).contains(email)) return true;

        if (app.getGroup() != null && parseCsv(app.getGroup().getManagers()).contains(email)) return true;

        return keyValueService.getValue("platform", "managers")
                .map(v -> parseCsv(v).contains(email))
                .orElse(false);
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    @GetMapping("/{appId}")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
//    @Cacheable(value = "app", key = "#appId")
    public App findById(@PathVariable("appId") Long appId) {
        return this.appService.findById(appId);
    }

    @GetMapping("/path/{key:.+}")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
//    @Cacheable(value = "appRun", key = "#key")
    public App findByKey(@PathVariable("key") String key) {
        return this.appService.findByKey(key);
    }


    @GetMapping("/{appId}/navis-all")
    public List<NaviGroup> findNaviByAppId(@PathVariable("appId") Long appId) {
        return this.appService.findNaviByAppIdAndEmail(appId, null);
    }

    @GetMapping("/{appId}/navis")
//    @Cacheable(value = "appNavi", key = "#appId")
    public List<NaviGroup> findNaviByAppId(@PathVariable("appId") Long appId,
                                           @RequestParam(value="email",required = false) String email) {
        return this.appService.findNaviByAppIdAndEmail(appId, email);
    }

    @PostMapping("/{appId}/delete")
//    @Caching(evict = {
//            @CacheEvict(value = "app", key = "#app.id")
//    })
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> delete(@PathVariable("appId") Long appId,
                                      @CurrentUser UserPrincipal principal) {

        App app = appService.findById(appId);
        if (app.getId() != null && !allowAccess(principal, app)){
            logger.error("App delete failure: App email:"+app.getEmail()+", Principal:"+principal.getName());
            throw new AuthorizationServiceException("Unauthorized removal by non-creator :" + principal.getName());
        }

        this.appService.delete(appId, principal.getEmail());
        return Map.of(
                "success", true,
                "message", "App deleted successfully."
        );
    }

    @PostMapping("/clone")
    @PreAuthorize("@authz.isDesigner()")
    public App clone(@RequestBody App app,
                     @CurrentUser UserPrincipal principal) {
        return this.appService.cloneApp(app, principal.getEmail());
    }

    @PostMapping("/{appId}/live")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
    @PreAuthorize("@authz.isDesigner()")
    public App setLive(@PathVariable("appId") Long appId,
                       @RequestParam("status") Boolean status,
                       @CurrentUser UserPrincipal principal) {
        App app = appService.findById(appId);
        if (app.getId() != null && !allowAccess(principal, app)){
            logger.error("App delete failure: App email:"+app.getEmail()+", Principal:"+principal.getName());
            throw new AuthorizationServiceException("Unauthorized update by non-creator/non-manager :" + principal.getName());
        }
        return this.appService.setLive(appId, status);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    @PreAuthorize("@authz.isDesigner()")
    public Page<App> getList(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                             Pageable pageable) {
        return this.appService.getList(searchText, pageable);
    }

    @GetMapping("/super")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
//    })
    @PreAuthorize("@authz.isDesigner()")
    public Page<App> getAdminList(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                  @RequestParam(value = "live", required = false) Boolean live,
                                  Pageable pageable) {
        return this.appService.getSuperAdminList(searchText, live, pageable);
    }

    @GetMapping("/top")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    public Page<App> getTopList(Pageable pageable) {
        return this.appService.getTopList(pageable);
    }

    @GetMapping("/my")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    @PreAuthorize("@authz.isDesigner()")
    public Page<App> getMyList(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                               @RequestParam(value = "live", required = false) Boolean live,
                               @CurrentUser UserPrincipal principal,
                               Pageable pageable) {
        return this.appService.getMyList(principal.getEmail(), searchText, live, pageable);
    }


    @GetMapping("/status")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    @PreAuthorize("@authz.isDesigner()")
    public Page<App> getByStatusList(@RequestParam(value = "status", required = false) List<String> status,
                                     @RequestParam(value = "searchText", defaultValue = "") String searchText,
                                     Pageable pageable) {
        return this.appService.getByStatusList(status, searchText, pageable);
    }

    // check = utk check path bila mok set app path, check-code-key: utk check app exist or x.
    // boleh MERGE jd 'check-code-key' jk.
    @GetMapping("/check-by-key")
    public boolean check(@RequestParam(value = "appPath") String appPath) {
        return this.appService.checkByKey(appPath);
    }

    @PostMapping("/{appId}/request")
    public CloneRequest request(@PathVariable("appId") Long appId,
                                @CurrentUser UserPrincipal principal) {
        return appService.requestCopy(appId, principal.getEmail());
    }

    @PostMapping("/request/{id}/activate")
    public CloneRequest activate(@PathVariable("id") Long id) {
        return appService.status(id, "activated");
    }

    @PostMapping("/request/{id}/reject")
    public CloneRequest reject(@PathVariable("id") Long id) {
        return appService.status(id, "rejected");
    }

    @GetMapping("/{appId}/request")
    public Page<CloneRequest> request(@PathVariable("appId") Long appId, Pageable pageable) {
        return appService.getCopyRequestList(appId, pageable);
    }

    @GetMapping("/{appId}/activation-check")
    public Map<String, Object> activationCheck(@PathVariable("appId") Long appId,
                                               @CurrentUser UserPrincipal principal) {
        Map<String, Object> data = new HashMap<>();
        data.put("result", appService.activationCheck(appId, principal.getEmail()));
        return data;
    }

    @GetMapping("/{appId}/export")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
            @JsonMixin(target = Form.class, mixin = MetadataMixin.Form.class),
            @JsonMixin(target = Dataset.class, mixin = MetadataMixin.Dataset.class),
            @JsonMixin(target = Dashboard.class, mixin = MetadataMixin.Dashboard.class),
            @JsonMixin(target = Chart.class, mixin = MetadataMixin.Chart.class),
            @JsonMixin(target = Screen.class, mixin = MetadataMixin.Screen.class),
            @JsonMixin(target = Lookup.class, mixin = MetadataMixin.Lookup.class),
            @JsonMixin(target = UserGroup.class, mixin = MetadataMixin.Role.class),
            @JsonMixin(target = Endpoint.class, mixin = MetadataMixin.Endpoint.class),
            @JsonMixin(target = EmailTemplate.class, mixin = MetadataMixin.Email.class),
            @JsonMixin(target = Cogna.class, mixin = MetadataMixin.Cogna.class),
            @JsonMixin(target = Bucket.class, mixin = MetadataMixin.Bucket.class),
            @JsonMixin(target = Lambda.class, mixin = MetadataMixin.Lambda.class),
    })
    @PreAuthorize("@authz.isDesigner()")
    public AppWrapper export(@PathVariable("appId") Long appId,
                             @CurrentUser UserPrincipal principal,
                             HttpServletResponse response) {

        App app = appService.findById(appId);
        if (app.getId() != null && !allowAccess(principal, app)){
            logger.error("App export failure: App email:"+app.getEmail()+", Principal:"+principal.getName());
            throw new AuthorizationServiceException("Unauthorized export by non-creator/non-manager :" + principal.getName());
        }


        String filename = URLEncoder
            .encode(app.getTitle().replaceAll("[^a-zA-Z0-9.]",""), StandardCharsets.UTF_8)
            .toLowerCase();

        String currentTimeFmt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("app-"+appId+"-"+filename+"-"+currentTimeFmt+".appmeta.json")
                .build();

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        AppWrapper appWrapper = appService.exportApp(appId);

        return appWrapper;

    }

    @PostMapping("/{appId}/import")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> importApp(@PathVariable("appId") Long appId,
                                         @RequestParam("file") MultipartFile file,
                                         @CurrentUser UserPrincipal principal,
                                          HttpServletRequest request) throws Exception {

        Map<String, Object> data = new HashMap<>();

        ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
        String myString = IOUtils.toString(stream, "UTF-8");

        AppWrapper appwrapper = MAPPER.readValue(myString, AppWrapper.class);

        App newApp = appService.importApp(appId, appwrapper, principal.getEmail());

        try {
            data.put("app", newApp);
            data.put("success", true);
            data.put("message", "success");
        } catch (IllegalStateException e) {
            data.put("message", "failed");
        }
        return data;
    }


    @PostMapping("/logo")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> uploadLogo(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "appId", required = false) Long appId,
                                          @CurrentUser UserPrincipal principal) throws Exception {

        if (file.isEmpty()) {
            return Map.of("message", "failed", "error", "File is empty");
        }

        // 1. Fetch App and Authorize ONCE
        App app = null;
        if (appId != null) {
            app = appService.findById(appId);
            if (app == null) {
                return Map.of("message", "failed", "error", "App not found");
            }
            if (!allowAccess(principal, app)){
                // Fixed logging message and mapped to principal.getEmail()
                logger.error("App logo update failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
                throw new AuthorizationServiceException("Unauthorized update by non-creator/non-manager: " + principal.getEmail());
            }
        }

        // 2. Process Image
        BufferedImage croppedImage = Helper.processLogoToSquare(file.getBytes());
        int type = BufferedImage.TYPE_INT_ARGB;
        String unique = String.valueOf(Instant.now().getEpochSecond());
        int[] sizes = {16, 72, 96, 192, 512};

        File dir = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + "/");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ImageIO.setUseCache(false);

        // 3. Write resized images to disk
        for (int size : sizes) {
            BufferedImage image = Helper.resizeImageWithHint(croppedImage, size, size, type);
            File dest = new File(dir, size + ".png");

            if (size == 192) {
                File defaultLogo = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + ".png");
                ImageIO.write(image, "png", defaultLogo);
            }
            ImageIO.write(image, "png", dest);
        }

        // 4. Handle Old Logo Cleanup and App Update
        if (app != null) {
            // Only attempt deletion if there is an existing logo
            if (app.getLogo() != null && !app.getLogo().isBlank()) {
                try {
                    File oldDir = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + app.getLogo() + "/");
                    if (oldDir.exists()) FileUtils.forceDelete(oldDir);

                    File oldFile = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + app.getLogo() + ".png");
                    if (oldFile.exists()) FileUtils.forceDelete(oldFile);
                } catch (Exception e) {
                    logger.error("Failed to delete old logo files for app {}: {}", appId, e.getMessage());
                }
            }

            // CRITICAL FIX: Update the logo ID regardless of whether the old one was null
            app.setLogo(unique);
            appService.save(app, app.getEmail());
        }

        // 5. Return Response
        Map<String, Object> data = new HashMap<>();
        data.put("fileSize", file.getSize());
        data.put("fileName", file.getOriginalFilename());
        data.put("fileType", file.getContentType());
        data.put("fileUrl", unique);
        data.put("message", "success");

        return data;
    }


    @PostMapping("/delete-logo")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> deleteLogo(@RequestParam(value = "appId") Long appId,
                                          @CurrentUser UserPrincipal principal) {

        // 1. Fetch App and validate existence
        App app = appService.findById(appId);
        if (app == null) {
            return Map.of("message", "failed", "error", "App not found");
        }

        // 2. Perform Authorization Check
        if (!allowAccess(principal, app)) {
            logger.error("App logo delete failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized delete by non-creator/non-manager: " + principal.getEmail());
        }

        String unique = app.getLogo();

        // 3. Early return if there is no logo to delete
        if (unique == null || unique.isBlank()) {
            return Map.of("message", "success", "note", "No logo existed");
        }

        // 4. Safe Deletion using FileUtils (matches your uploadLogo logic)
        try {
            File dir = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + "/");
            if (dir.exists()) {
                FileUtils.forceDelete(dir); // Standard File.delete() fails on non-empty directories
            }

            File defaultLogo = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + ".png");
            if (defaultLogo.exists()) {
                FileUtils.forceDelete(defaultLogo);
            }
        } catch (Exception e) {
            logger.error("Failed to delete logo files from disk for app {}: {}", appId, e.getMessage());
            // We intentionally don't throw an error here, so the DB still clears the logo reference
        }

        // 5. Update Database
        app.setLogo(null);
        appService.save(app, app.getEmail());

        // 6. Clean Return (Removed the impossible IllegalStateException catch block)
        Map<String, Object> data = new HashMap<>();
        data.put("message", "success");
        return data;
    }

    @GetMapping("/logo/{path:.+}")
    @Cacheable("reka.logo")
    public FileSystemResource getFileInline(@PathVariable("path") String path,
                                            HttpServletResponse response) {
        FileSystemResource fsr = new FileSystemResource(Constant.UPLOAD_ROOT_DIR + "/logo/" + path + ".png");
        if (fsr.exists()){
            return fsr;
        }else{
            return null;
        }
    }

    @GetMapping("/{appPath:.+}/logo/{size}")
    public ResponseEntity<FileSystemResource> getFileInline(
            @PathVariable("appPath") String path,
            @PathVariable(value = "size", required = false) Integer size) {

        App app = appService.findByKey(path);
        if (app == null || app.getLogo() == null) {
            return ResponseEntity.notFound().build();
        }

        int logoSize = (size != null) ? size : 72;

        Path logoPath = Paths.get(
                Constant.UPLOAD_ROOT_DIR,
                "logo",
                app.getLogo(),
                logoSize + ".png"
        );

        FileSystemResource resource = new FileSystemResource(logoPath.toFile());

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)))
                .body(resource);
    }

    @GetMapping("/{path:.+}/manifest.json")
    public Map getManifest(@PathVariable("path") String path) {
        return appService.getManifest(path);
    }


    @PostMapping("/navi/add-group/{id}")
    @PreAuthorize("@authz.isDesigner()")
    public NaviGroup addNaviGroup(@PathVariable("id") Long id, @RequestBody NaviGroup group) {
        return appService.addNaviGroup(id, group);
    }


    @PostMapping("/navi/add-item/{id}")
    @PreAuthorize("@authz.isDesigner()")
    public NaviItem addNaviItem(@PathVariable("id") Long id, @RequestBody NaviItem naviItem) {
        return appService.addNaviItem(id, naviItem);
    }

    @PostMapping("/navi/delete-group/{id}")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> removeNaviGroup(@PathVariable("id") Long id) {
        return appService.removeNaviGroup(id);
    }

    @PostMapping("/navi/delete-item/{id}")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> removeNaviItem(@PathVariable("id") Long id) {
        return appService.removeNaviItem(id);
    }

    @PostMapping("/navi/save-item-order")
    @PreAuthorize("@authz.isDesigner()")
    public List<Map<String, Long>> saveItemOrder(@RequestBody List<Map<String, Long>> formItemList) {
        return appService.saveItemOrder(formItemList);
    }


    @PostMapping("/navi/move-item")
    @PreAuthorize("@authz.isDesigner()")
    public NaviItem moveItem(@RequestParam("itemId") long itemId,
                             @RequestParam("newGroupId") long newGroupId,
                             @RequestParam("sortOrder") long sortOrder) {
        return appService.moveItem(itemId, newGroupId, sortOrder);
    }

    @PostMapping("/navi/save-group-order")
    @PreAuthorize("@authz.isDesigner()")
    public List<Map<String, Long>> saveSectionOrder(@RequestBody List<Map<String, Long>> groupList) {
        return appService.saveGroupOrder(groupList);
    }

    @GetMapping("/{appId}/navi-data")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = NaviMixin.FormList.class),
            @JsonMixin(target = Dataset.class, mixin = NaviMixin.DatasetList.class),
            @JsonMixin(target = Dashboard.class, mixin = NaviMixin.DashboardList.class),
            @JsonMixin(target = Screen.class, mixin = NaviMixin.ScreenList.class),
            @JsonMixin(target = Action.class, mixin = NaviMixin.ScreenActionList.class),
            @JsonMixin(target = Lookup.class, mixin = NaviMixin.LookupList.class),
    })
    public Map getNavi(@PathVariable("appId") Long appId,
                       @RequestParam(value="email",required = false) String email) {
        if (email != null) {
            return appService.getNaviDataByEmail(appId, email);
        } else {
            return appService.getNaviData(appId);
        }
    }

    @GetMapping("/{appId}/counts")
    public Map getCounts(@PathVariable("appId") Long appId) {
        return appService.getCounts(appId);
    }


    @GetMapping("/{appId}/summary")
    public Map getAppSummary(@PathVariable("appId") Long appId) {
        // bilangan form,dataset, dashboard, screen, users
        return appService.getSummary(appId);
    }

    @GetMapping("/platform-summary")
    public Map<String, Object> getPlatformSummary() {
        // bilangan form,dataset, dashboard, screen, users
        return appService.getPlatformSummary();
    }

    @GetMapping("/{appId}/notification")
    public Page<Notification> findNotiByAppIdAndEmail(@PathVariable("appId") Long appId,
                                                      @RequestParam(value = "searchText", required = false) String searchText,
                                                      @RequestParam(value = "tplId", required = false) Long tplId,
                                                      @RequestParam(value = "email", required = false) String email,
                                                      @CurrentUser UserPrincipal principal,
                                                      Pageable pageable) {
        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        return this.notificationService.findByAppIdAndParam(appId,searchText, email, tplId,pageable);
    }

    @GetMapping("/{appId}/notification/unread-count")
    public Long countUnreadNotiByAppIdAndEmail(@PathVariable("appId") Long appId,
                                               @RequestParam(value = "email", required = false) String email,
                                               @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        return this.notificationService.countByAppIdAndEmail(appId, email);
    }

    @PostMapping("/notification-read/{nId}")
    public Notification markNotiByAppIdAndEmail(@PathVariable("nId") Long nId,
                                                @RequestParam(value = "email", required = false) String email,
                                                @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        return this.notificationService.markRead(nId, email);
    }

    @GetMapping("/{appId}/pages")
    public List<Map> getPages(@PathVariable("appId") Long appId) {
        return appService.getPages(appId);
    }

    @GetMapping("/{appId}/user-by-email")
    public List<AppUser> appUserByEmail(@PathVariable("appId") Long appId,
                                        @RequestParam("email") String email){
        return appService.findByAppIdAndEmail(appId,email);
    }

    @GetMapping("/{appId}/user")
    public Page<AppUser> userByAppId(@PathVariable("appId") Long appId,
                                     @RequestParam(value="searchText",defaultValue = "") String searchText,
                                     @RequestParam(value="status",required = false) List<String> status,
                                     @RequestParam(value="group",required = false) Long group,
                                     Pageable pageable){
        return appService.findUserByAppId(appId,searchText,status,group,pageable);
    }

    @GetMapping("/{appId}/user-all")
    public Page<AppUser> userByAppId(@PathVariable("appId") Long appId,
                                     @RequestParam(value="searchText",defaultValue = "") String searchText,
                                     @RequestParam(value="status",required = false) List<String> status,
                                     Pageable pageable){
        return appService.findAllByAppId(appId,searchText,status,pageable);
    }

    record AppUserPayload(String email, List<Long> groups, String name, boolean autoReg, List<String> tags){}

    @PostMapping("/{appId}/user")
    public Map saveAppUser(@RequestBody AppUserPayload payload,
                           @PathVariable("appId") Long appId){
        return appService.regUser(payload.groups, appId, payload.email,null, payload.name, payload.autoReg, payload.tags);
    }

    @PostMapping("/{appId}/register")
    public Map regAppUser(@RequestBody AppUserPayload payload,
                          @PathVariable("appId") Long appId,
//                          @RequestParam("email") String email,
                          @CurrentUser UserPrincipal principal){
        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        return appService.regUser(payload.groups, appId, payload.email, principal.getId(), payload.name, payload.autoReg, payload.tags);
    }

    @PostMapping("/user/update-user/{userId}")
    public User updateUser(@RequestBody User payload,
                           @PathVariable("userId") Long userId){
        return appService.updateUser(userId, payload);
    }

    @PostMapping("/user/remove-bulk")
    public Map removeBulk(@RequestBody List<Long> userIdList){
        return appService.removeBulkUser(userIdList);
    }

    record UserBlastPayload(Map<String, String> data, List<Long> userIdList){}

    @PostMapping("/{appId}/user/blast")
    public Map removeBulk(@PathVariable("appId") Long appId,
                          @RequestBody UserBlastPayload userBlastPayload){
        return appService.blastBulkUser(appId,userBlastPayload.data, userBlastPayload.userIdList);
    }

    record UserProviderPayload(String provider, List<Long> userIdList){}

    @PostMapping("/user/change-provider-bulk")
    public Map changeProviderBulk(@RequestBody UserProviderPayload userProviderPayload){
        return appService.changeProviderBulkUser(userProviderPayload.provider, userProviderPayload.userIdList);
    }

    @PostMapping("/{appId}/user-bulk")
    public Map saveAppUserBulk(@RequestBody AppUserPayload payload,
                               @PathVariable("appId") Long appId){
        List<Long> userGroups = payload.groups;
        String emails = payload.email;
        return appService.regUserBulk(userGroups,appId,emails,payload.autoReg,payload.tags);
    }

    @PostMapping("/{appId}/once-done")
    public Map<String, Object> onceDone(@PathVariable("appId") Long appId,
                                        @CurrentUser UserPrincipal principal,
                                        @RequestParam(value = "email", required = false) String email,
                                        @RequestParam("val") Boolean val){
        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        return appService.onceDone(appId, email, val);
    }

    @PostMapping("/{appId}/remove-acc")
    public Map<String, Object> removeAccount(@PathVariable("appId") Long appId,
//                                             @RequestParam(value = "email", required = false) String email,
                                             @CurrentUser UserPrincipal principal){
        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        return appService.removeAcc(appId, principal.getEmail());
    }

    @GetMapping("/autocomplete")
    public List<CodeAuto> loadAutoComplete(@RequestParam("type") String type){
        return codeAutoRepository.findByType(type);
    }

    @GetMapping("/time")
    public Instant getServerTime(){
        return Instant.now();
    }

    @GetMapping("/{appId}/api-keys")
    @PreAuthorize("@authz.isDesigner()")
    public List<ApiKey> getApiKeys(@PathVariable("appId") Long appId,
                                   @CurrentUser UserPrincipal principal){
        // 1. Fetch the App
        App app = appService.findById(appId);

        // 2. Fail-fast if the app doesn't exist
        if (app == null) {
            return List.of(); // Or throw a 404 exception depending on your error handling setup
        }

        // 3. Perform Authorization Check
        if (!allowAccess(principal, app)) {
            logger.error("API keys access failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized access by non-creator/non-manager: " + principal.getEmail());
        }

        return appService.getApiKeys(appId);
    }


    @PostMapping("/delete-api-key/{apiKeyId}")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> deleteApiKey(@PathVariable("apiKeyId") Long apiKeyId,
                                            @CurrentUser UserPrincipal principal) {
        // NOTE: You must retrieve the App associated with this ApiKey.
        // Adjust `getApiKeyById` to match your actual appService/repository method name.
        ApiKey apiKey = appService.getApiKeyById(apiKeyId);
        if (apiKey != null) {
            App app = appService.findById(apiKey.getAppId()); // Assuming ApiKey has a reference to App
            if (!allowAccess(principal, app)) {
                logger.error("API Key delete failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
                throw new AuthorizationServiceException("Unauthorized delete by non-creator/non-manager: " + principal.getEmail());
            }
        }
        return appService.removeApiKey(apiKeyId);
    }

    @PostMapping("/{appId}/generate-key")
    @PreAuthorize("@authz.isDesigner()")
    public ApiKey generateKey(@PathVariable("appId") Long appId,
                              @CurrentUser UserPrincipal principal) {

        App app = appService.findById(appId);
        if (app != null && !allowAccess(principal, app)) {
            logger.error("API Key generation failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized generation by non-creator/non-manager: " + principal.getEmail());
        }
        return appService.generateNewApiKey(appId);
    }

    @GetMapping("/{appId}/secrets")
    @PreAuthorize("@authz.isDesigner()")
    public List<Secret> getSecrets(@PathVariable("appId") Long appId,
                                   @CurrentUser UserPrincipal principal) {

        App app = appService.findById(appId);
        if (app != null && !allowAccess(principal, app)) {
            logger.error("Secrets access failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized access by non-creator/non-manager: " + principal.getEmail());
        }
        return appService.getSecrets(appId);
    }

    @PostMapping("/{appId}/secret")
    @PreAuthorize("@authz.isDesigner()")
    public Secret saveSecrets(@PathVariable("appId") Long appId,
                              @RequestBody Secret secret,
                              @CurrentUser UserPrincipal principal) {

        App app = appService.findById(appId);
        if (app != null && !allowAccess(principal, app)) {
            logger.error("Secret save failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized update by non-creator/non-manager: " + principal.getEmail());
        }
        return appService.saveSecrets(appId, secret);
    }

    @PostMapping("/delete-secret/{secretId}")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> deleteSecret(@PathVariable("secretId") Long secretId,
                                            @CurrentUser UserPrincipal principal) {
        // NOTE: You must retrieve the App associated with this Secret.
        // Adjust `getSecretById` to match your actual appService/repository method name.
        Secret secret = appService.getSecret(secretId);
        if (secret != null) {
            App app = appService.findById(secret.getAppId());
            if (!allowAccess(principal, app)) {
                logger.error("Secret delete failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
                throw new AuthorizationServiceException("Unauthorized delete by non-creator/non-manager: " + principal.getEmail());
            }
        }
        return appService.removeSecret(secretId);
    }

    @GetMapping("/{appId}/logs")
    @PreAuthorize("@authz.isDesigner()")
    public Page<AppLog> getLogs(@PathVariable("appId") Long appId,
                                @RequestParam(value = "searchText", defaultValue = "") String searchText,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "module", required = false) String module,
                                @RequestParam(value = "moduleId", required = false) String moduleId,
                                @RequestParam(value = "email", required = false) String email,
                                @RequestParam(value = "dateFrom", required = false) Long dateFrom,
                                @RequestParam(value = "dateTo", required = false) Long dateTo,
                                Pageable pageable,
                                @CurrentUser UserPrincipal principal) {

        App app = appService.findById(appId);
        if (app != null && !allowAccess(principal, app)) {
            logger.error("Logs access failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized access by non-creator/non-manager: " + principal.getEmail());
        }

        return appService.getLogs(appId, searchText, status, module, moduleId, dateFrom, dateTo, email, pageable);
    }

    @PostMapping("/{appId}/logs/delete")
    @PreAuthorize("@authz.isDesigner()")
    public Map<String, Object> clearLogs(@PathVariable("appId") Long appId,
                                         @RequestBody Map<String, Object> payload,
                                         @CurrentUser UserPrincipal principal) {

        App app = appService.findById(appId);
        if (app != null && !allowAccess(principal, app)) {
            logger.error("Logs delete failure: App email:{}, Principal:{}", app.getEmail(), principal.getEmail());
            throw new AuthorizationServiceException("Unauthorized delete by non-creator/non-manager: " + principal.getEmail());
        }

        String searchText = payload.get("searchText") != null ? payload.get("searchText").toString() : "";
        String status = payload.get("status") != null ? payload.get("status").toString() : null;
        String module = payload.get("module") != null ? payload.get("module").toString() : null;
        String moduleId = payload.get("moduleId") != null ? payload.get("moduleId").toString() : null;
        String email = payload.get("email") != null ? payload.get("email").toString() : null;
        Long dateFrom = payload.get("dateFrom") != null ? Long.parseLong(payload.get("dateFrom").toString()) : null;
        Long dateTo = payload.get("dateTo") != null ? Long.parseLong(payload.get("dateTo").toString()) : null;

        // Swapped System.out.println for standard logging to prevent console blocking in production
        logger.debug("Clearing logs with payload:{}, searchText:{}, status:{}, module:{}, moduleId:{}, email:{}, dateFrom:{}, dateTo:{}",
                payload, searchText, status, module, moduleId, email, dateFrom, dateTo);

        return appService.clearLogs(appId, searchText, status, module, moduleId, email, dateFrom, dateTo);
    }
//
//    @PostMapping("/delete-api-key/{apiKeyId}")
//    @PreAuthorize("@authz.isDesigner()")
//    public Map<String, Object> deleteApiKey(@PathVariable("apiKeyId") Long apiKeyId){
//        return appService.removeApiKey(apiKeyId);
//    }
//
//    @PostMapping("/{appId}/generate-key")
//    @PreAuthorize("@authz.isDesigner()")
//    public ApiKey generateKey(@PathVariable("appId") Long appId){
//        return appService.generateNewApiKey(appId);
//    }
//
//
//    @GetMapping("/{appId}/secrets")
//    @PreAuthorize("@authz.isDesigner()")
//    public List<Secret> getSecrets(@PathVariable("appId") Long appId){
//        return appService.getSecrets(appId);
//    }
//
//    @PostMapping("/{appId}/secret")
//    @PreAuthorize("@authz.isDesigner()")
//    public Secret saveSecrets(@PathVariable("appId") Long appId, @RequestBody Secret secret){
//        return appService.saveSecrets(appId, secret);
//    }
//
//    @PostMapping("/delete-secret/{secretId}")
//    @PreAuthorize("@authz.isDesigner()")
//    public Map<String, Object> deleteSecret(@PathVariable("secretId") Long secretId){
//        return appService.removeSecret(secretId);
//    }
//
//    @GetMapping("/{appId}/logs")
//    @PreAuthorize("@authz.isDesigner()")
//    public Page<AppLog> getLogs(@PathVariable("appId") Long appId,
//                                @RequestParam(value = "searchText", defaultValue = "") String searchText,
//                                @RequestParam(value = "status", required = false) String status,
//                                @RequestParam(value = "module", required = false) String module,
//                                @RequestParam(value = "moduleId", required = false) String moduleId,
//                                @RequestParam(value = "email", required = false) String email,
//                                @RequestParam(value = "dateFrom", required = false) Long dateFrom,
//                                @RequestParam(value = "dateTo", required = false) Long dateTo,
//                                Pageable pageable){
//        return appService.getLogs(appId, searchText, status, module, moduleId, dateFrom, dateTo, email, pageable);
//    }
//
//    @PostMapping("/{appId}/logs/delete")
//    @PreAuthorize("@authz.isDesigner()")
//    public Map<String, Object> clearLogs(@PathVariable("appId") Long appId,
//                                @RequestBody Map<String, Object> payload){
//        String searchText = payload.get("searchText") != null ? payload.get("searchText").toString() : "";
//        String status = payload.get("status") != null ? payload.get("status").toString() : null;
//        String module = payload.get("module") != null ? payload.get("module").toString() : null;
//        String moduleId = payload.get("moduleId") != null ? payload.get("moduleId").toString() : null;
//        String email = payload.get("email") != null ? payload.get("email").toString() : null;
//        Long dateFrom = payload.get("dateFrom") != null ? Long.parseLong(payload.get("dateFrom")+"") : null;
//        Long dateTo = payload.get("dateTo") != null ? Long.parseLong(payload.get("dateTo")+"") : null;
//
//        System.out.println("##########:"+payload+", searchText:"+searchText+", status:"+status+", module:"+module+", moduleId:"+moduleId+", email:"+email+", dateFrom:"+dateFrom+", dateTo:"+dateTo);
//
//        return appService.clearLogs(appId, searchText,
//                status,
//                module,
//                moduleId,
//                email,
//                dateFrom,
//                dateTo);
//    }


}
