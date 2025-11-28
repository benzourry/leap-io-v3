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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//import org.jboss.aerogear.security.otp.Totp;
//import org.jboss.aerogear.security.otp.api.Base32;
//import org.jboss.aerogear.security.otp.api.Clock;

@RestController
@RequestMapping("api/app")
//@CrossOrigin(allowCredentials = "true")
public class AppController {
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
//        this.clientService = clientService;
        this.MAPPER = MAPPER;
    }

    // Only cache app with path:
    @PostMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
    public App save(@RequestBody App app,
                    @RequestParam("email") String email,
                    Principal principal) {
        // existing app && principal is not app creator
        if (app.getId() != null && !allowAccess(principal, app)){
            System.out.println("App update failure: App email:"+app.getEmail()+", Principal:"+principal.getName());
            throw new AuthorizationServiceException("Unauthorized modification by non-creator :" + principal.getName());
        }
        return this.appService.save(app, email);
    }

    public boolean allowAccess(Principal principal, App app){
        String emails = ","+Optional.ofNullable(app.getEmail()).orElse("").replaceAll("\\s","")+",";
        String appManagers = "";
        String managers = "";
        Optional<String> v = keyValueService.getValue("platform","managers");

        if (app.getGroup()!=null){
            appManagers = ","+Optional.ofNullable(app.getGroup().getManagers()).orElse("").replaceAll("\\s","")+",";
        }

        if (v.isPresent()){
            managers = ","+v.orElse("").replaceAll("\\s","")+",";
        }
        return (
                emails.contains(","+principal.getName()+",") ||
                appManagers.contains(","+principal.getName()+",") ||
                managers.contains(","+principal.getName()+",")
        );
    }

    @GetMapping("{appId}")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
//    @Cacheable(value = "app", key = "#appId")
    public App findById(@PathVariable("appId") Long appId) {
        return this.appService.findById(appId);
    }

    @GetMapping("path/{key:.+}")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
//    @Cacheable(value = "appRun", key = "#key")
    public App findByKey(@PathVariable("key") String key) {
        return this.appService.findByKey(key);
    }


    @GetMapping("{appId}/navis-all")
    public List<NaviGroup> findNaviByAppId(@PathVariable("appId") Long appId) {
        return this.appService.findNaviByAppIdAndEmail(appId, null);
    }

    @GetMapping("{appId}/navis")
//    @Cacheable(value = "appNavi", key = "#appId")
    public List<NaviGroup> findNaviByAppId(@PathVariable("appId") Long appId,
                                           @RequestParam(value="email",required = false) String email) {
        return this.appService.findNaviByAppIdAndEmail(appId, email);
    }

    @PostMapping("{appId}/delete")
//    @Caching(evict = {
//            @CacheEvict(value = "app", key = "#app.id")
//    })
    public Map<String, Object> delete(@PathVariable("appId") Long appId,
                       @RequestParam("email") String email) {
        this.appService.delete(appId,email);
        return Map.of(
                "success", true,
                "message", "App deleted successfully."
        );
    }

    @PostMapping("clone")
    public App clone(@RequestBody App app,
                     @RequestParam("email") String email) {
        return this.appService.cloneApp(app, email);
    }

    @PostMapping("{appId}/live")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneDesign.class),
    })
    public App setLive(@PathVariable("appId") Long appId,
                     @RequestParam("status") Boolean status) {
        return this.appService.setLive(appId, status);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    public Page<App> getList(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                             Pageable pageable) {
        return this.appService.getList(searchText, pageable);
    }

    @GetMapping("super")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
//    })
    public Page<App> getAdminList(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                  @RequestParam(value = "live", required = false) Boolean live,
                                  Pageable pageable) {
        return this.appService.getSuperAdminList(searchText, live, pageable);
    }

    @GetMapping("top")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    public Page<App> getTopList(Pageable pageable) {
        return this.appService.getTopList(pageable);
    }

    @GetMapping("my")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    public Page<App> getMyList(@RequestParam(value = "email", required = false) String email,
                               @RequestParam(value = "searchText", defaultValue = "") String searchText,
                               @RequestParam(value = "live", required = false) Boolean live,
                               Pageable pageable) {
        return this.appService.getMyList(email, searchText, live, pageable);
    }


    @GetMapping("status")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    public Page<App> getByStatusList(@RequestParam(value = "status", required = false) List<String> status,
                                     @RequestParam(value = "searchText", defaultValue = "") String searchText,
                                     Pageable pageable) {
        return this.appService.getByStatusList(status, searchText, pageable);
    }

    // check = utk check path bila mok set app path, check-code-key: utk check app exist or x.
    // boleh MERGE jd 'check-code-key' jk.
    @GetMapping("check-by-key")
    public boolean check(@RequestParam(value = "appPath") String appPath) {
//        System.out.println("CHECK BY KEY CTRL:"+appPath);
//        Map<String, Object> data = new HashMap<>();
//        data.put("exist",this.appService.checkByKey(appPath));
        return this.appService.checkByKey(appPath);
    }

//    @GetMapping("check-code-key")
//    public boolean checkByKey(@RequestParam(value ="appPath") String appPath){
//        return this.appService.checkByKey(appPath);
//    }

//    @PostMapping("{appId}/activate")
//    public Map activate(@PathVariable("appId") Long appId,@RequestParam(value="code") String code, @RequestParam("email") String requesterEmail){
//        Map<String, Object> data = new HashMap<>();
//        // totp that will expired in 2 days 2 * 24 * 60 * 60
//        Totp totp = new Totp(Base32.encode((appId+requesterEmail).getBytes()), new Clock(172800));
//        if (Helper.isValidLong(code)){
//            data.put("result",totp.verify(code));
//        }else{
//            data.put("result",false);
//        }
//
//        return data;
//    }

    @PostMapping("{appId}/request")
    public CloneRequest request(@PathVariable("appId") Long appId,
                                @RequestParam("email") String requesterEmail) {
        return appService.requestCopy(appId, requesterEmail);
    }

    @PostMapping("request/{id}/activate")
    public CloneRequest activate(@PathVariable("id") Long id) {
        return appService.status(id, "activated");
    }

    @PostMapping("request/{id}/reject")
    public CloneRequest reject(@PathVariable("id") Long id) {
        return appService.status(id, "rejected");
    }

    @GetMapping("{appId}/request")
    public Page<CloneRequest> request(@PathVariable("appId") Long appId, Pageable pageable) {
        return appService.getCopyRequestList(appId, pageable);
    }

    @GetMapping("{appId}/activation-check")
    public Map<String, Object> activationCheck(@PathVariable("appId") Long appId,
                                               @RequestParam("email") String requesterEmail) {
        Map<String, Object> data = new HashMap<>();
        data.put("result", appService.activationCheck(appId, requesterEmail));
        return data;
    }

    @GetMapping("{appId}/export")
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
    public AppWrapper export(@PathVariable("appId") Long appId, HttpServletResponse response) {

        App app = appService.findById(appId);

        String filename = URLEncoder
                .encode(app.getTitle().replaceAll("[^a-zA-Z0-9.]",""), StandardCharsets.UTF_8)
                .toLowerCase();

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename("app-"+appId+"-"+filename+".appmeta.json")
                .build();

//        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
//                .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
//                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
//                .contentType(MediaType.parseMediaType(MediaType.APPLICATION_JSON_VALUE));

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        AppWrapper appWrapper = appService.exportApp(appId);

        return appWrapper;

    }

//    @PostMapping("{appId}/secret")
//    public Map secret(@PathVariable("appId") Long appId, @RequestParam("email") String requesterEmail){
//        Map<String, Object> data = new HashMap<>();
//        Totp totp = new Totp(Base32.encode((appId+requesterEmail).getBytes()));
//        data.put("result",totp.now());
//        return data;
//    }

    @PostMapping("{appId}/import")
    public Map<String, Object> importApp(@PathVariable("appId") Long appId,
                                         @RequestParam("file") MultipartFile file,
                                          @RequestParam("email") String email,
                                          HttpServletRequest request) throws Exception {

        Map<String, Object> data = new HashMap<>();


        ByteArrayInputStream stream = new ByteArrayInputStream(file.getBytes());
        String myString = IOUtils.toString(stream, "UTF-8");

        AppWrapper appwrapper = MAPPER.readValue(myString, AppWrapper.class);

        App newApp = appService.importApp(appId, appwrapper, email);


        try {
            data.put("app", newApp);
            data.put("success", true);
            data.put("message", "success");
        } catch (IllegalStateException e) {
            data.put("message", "failed");
        }
        return data;
    }


    @PostMapping("logo")
    public Map<String, Object> uploadLogo(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "appId", required = false) Long appId,
                                          HttpServletRequest request) throws Exception {

        Map<String, Object> data = new HashMap<>();

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        BufferedImage croppedImage = Helper.cropImageSquare(file.getBytes());
        int type = croppedImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : croppedImage.getType();

        /// RESIZE IMAGE
        String unique = Instant.now().getEpochSecond() + "";

//        72,96,192,512
        int[] sizes = {16, 72, 96, 192, 512};


        File dir = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + "/");
        dir.mkdirs();
//        dir.mkdir();

        for (int size : sizes) {
            BufferedImage image = Helper.resizeImageWithHint(croppedImage, size, size, type);

            File dest = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + "/" + size + ".png");

            ImageIO.setUseCache(false);

            if (size == 192) {
                File defaultLogo = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + ".png");
                ImageIO.write(image, "png", defaultLogo);
            }

            ImageIO.write(image, "png", dest);
        }

        // REMOVE OLD ICON AND SET ICON
        if (appId != null) {
            App app = appService.findById(appId);
            if ((app != null && app.getLogo() != null)) {
                try {
                    File oldDir = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + app.getLogo() + "/");
                    FileUtils.forceDelete(oldDir);
                    File oldFile = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + app.getLogo() + ".png");
                    FileUtils.forceDelete(oldFile);
                } catch (Exception e) {
//                    System.out.println(e.getMessage());
                }

                app.setLogo(unique);
                appService.save(app, app.getEmail());
            }
        }

        try {
            data.put("fileSize", fileSize);
            data.put("fileName", originalFilename);
            data.put("fileType", contentType);
            data.put("fileUrl", unique);
            data.put("message", "success");
        } catch (IllegalStateException e) {
            data.put("message", "failed");
        }
        return data;
    }


    @PostMapping("delete-logo")
    public Map<String, Object> deleteLogo(@RequestParam(value = "appId", required = false) Long appId){

        Map<String, Object> data = new HashMap<>();

        App app = appService.findById(appId);

        String unique = app.getLogo();

        // delete logo directory
        File dir = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + "/");
        dir.delete();

        // delete default logo
        File defaultLogo = new File(Constant.UPLOAD_ROOT_DIR + "/logo/" + unique + ".png");
        defaultLogo.delete();

        app.setLogo(null);

        appService.save(app, app.getEmail());


        try {
            data.put("message", "success");
        } catch (IllegalStateException e) {
            data.put("message", "failed");
        }
        return data;
    }

    @GetMapping("logo/{path:.+}")
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

    @GetMapping("{appPath:.+}/logo/{size}")
    public FileSystemResource getFileInline(@PathVariable("appPath") String path,
                                            @PathVariable("size") Integer size,
                                            HttpServletResponse response) {
        App app = appService.findByKey(path);

        if (app==null) return null;

        Integer logoSize = Optional.ofNullable(size).orElse(72);

        FileSystemResource fsr =  new FileSystemResource(Constant.UPLOAD_ROOT_DIR + "/logo/" + app.getLogo() + "/" + logoSize + ".png");

        if (!fsr.exists()) return null;

        return fsr;

    }


    @GetMapping("{path:.+}/manifest.json")
    public Map getManifest(@PathVariable("path") String path) {
        return appService.getManifest(path);
    }


    @PostMapping("navi/add-group/{id}")
    public NaviGroup addNaviGroup(@PathVariable("id") Long id, @RequestBody NaviGroup group) {
        return appService.addNaviGroup(id, group);
    }


    @PostMapping("navi/add-item/{id}")
    public NaviItem addNaviItem(@PathVariable("id") Long id, @RequestBody NaviItem naviItem) {
        return appService.addNaviItem(id, naviItem);
    }

    @PostMapping("navi/delete-group/{id}")
    public Map<String, Object> removeNaviGroup(@PathVariable("id") Long id) {
        return appService.removeNaviGroup(id);
    }

    @PostMapping("navi/delete-item/{id}")
    public Map<String, Object> removeNaviItem(@PathVariable("id") Long id) {
        return appService.removeNaviItem(id);
    }

    @PostMapping("navi/save-item-order")
    public List<Map<String, Long>> saveItemOrder(@RequestBody List<Map<String, Long>> formItemList) {
        return appService.saveItemOrder(formItemList);
    }


    @PostMapping("navi/move-item")
    public NaviItem moveItem(@RequestParam("itemId") long itemId,
                             @RequestParam("newGroupId") long newGroupId,
                             @RequestParam("sortOrder") long sortOrder) {
        return appService.moveItem(itemId, newGroupId, sortOrder);
    }

    @PostMapping("navi/save-group-order")
    public List<Map<String, Long>> saveSectionOrder(@RequestBody List<Map<String, Long>> groupList) {
        return appService.saveGroupOrder(groupList);
    }

    @GetMapping("{appId}/navi-data")
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

    @GetMapping("{appId}/counts")
    public Map getCounts(@PathVariable("appId") Long appId) {
        return appService.getCounts(appId);
    }


    @GetMapping("{appId}/summary")
    public Map getAppSummary(@PathVariable("appId") Long appId) {

        // bilangan form,dataset, dashboard, screen, users
        Map<String, Object> summary = appService.getSummary(appId);

        return summary;
    }

    @GetMapping("platform-summary")
    public Map getPlatformSummary() {

        // bilangan form,dataset, dashboard, screen, users
        Map<String, Object> summary = appService.getPlatformSummary();

        return summary;
    }


    @GetMapping("{appId}/notification")
    public Page<Notification> findNotiByAppIdAndEmail(@PathVariable("appId") Long appId,
                                                      @RequestParam(value = "searchText", required = false) String searchText,
                                                      @RequestParam(value = "tplId", required = false) Long tplId,
                                                      @RequestParam(value = "email", required = false) String email,
                                                      Pageable pageable) {
        return this.notificationService.findByAppIdAndParam(appId,searchText,email,tplId,pageable);
//        return this.notificationService.findByAppIdAndEmail(appId, email, PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(), Sort.by("timestamp").descending()));
    }

    @PostMapping("notification-read/{nId}")
    public Notification markNotiByAppIdAndEmail(@PathVariable("nId") Long nId,
                                                @RequestParam("email") String email) {
        return this.notificationService.markRead(nId, email);
    }

    @GetMapping("{appId}/pages")
    public List<Map> getPages(@PathVariable("appId") Long appId) {
        return appService.getPages(appId);
    }


    @GetMapping("{appId}/user-by-email")
    public List<AppUser> appUserByEmail(@PathVariable("appId") Long appId,
                                        @RequestParam("email") String email){
        return appService.findByAppIdAndEmail(appId,email);
    }

    @GetMapping("{appId}/user")
    public Page<AppUser> userByAppId(@PathVariable("appId") Long appId,
                                     @RequestParam(value="searchText",defaultValue = "") String searchText,
                                     @RequestParam(value="status",required = false) List<String> status,
                                     @RequestParam(value="group",required = false) Long group,
                                     Pageable pageable){
        return appService.findUserByAppId(appId,searchText,status,group,pageable);
    }

    @GetMapping("{appId}/user-all")
    public Page<AppUser> userByAppId(@PathVariable("appId") Long appId,
                                     @RequestParam(value="searchText",defaultValue = "") String searchText,
                                     @RequestParam(value="status",required = false) List<String> status,
                                     Pageable pageable){
        return appService.findAllByAppId(appId,searchText,status,pageable);
    }

    record AppUserPayload(String email, List<Long> groups, String name, boolean autoReg, List<String> tags){}

    @PostMapping("{appId}/user")
    public Map saveAppUser(@RequestBody AppUserPayload payload,
                           @PathVariable("appId") Long appId){
        return appService.regUser(payload.groups, appId, payload.email,null, payload.name, payload.autoReg, payload.tags);
    }

    @PostMapping("{appId}/register")
    public Map regAppUser(@RequestBody AppUserPayload payload,
                          @PathVariable("appId") Long appId,
                          @CurrentUser UserPrincipal userPrincipal){
        return appService.regUser(payload.groups, appId, payload.email, userPrincipal.getId(), payload.name, payload.autoReg, payload.tags);
    }

    @PostMapping("user/update-user/{userId}")
    public User updateUser(@RequestBody User payload,
                           @PathVariable("userId") Long userId){
        return appService.updateUser(userId, payload);
    }

    @PostMapping("user/remove-bulk")
    public Map removeBulk(@RequestBody List<Long> userIdList){
        return appService.removeBulkUser(userIdList);
    }

    record UserBlastPayload(Map<String, String> data, List<Long> userIdList){}

    @PostMapping("{appId}/user/blast")
    public Map removeBulk(@PathVariable("appId") Long appId,
                          @RequestBody UserBlastPayload userBlastPayload){
        return appService.blastBulkUser(appId,userBlastPayload.data, userBlastPayload.userIdList);
    }

    record UserProviderPayload(String provider, List<Long> userIdList){}

    @PostMapping("user/change-provider-bulk")
    public Map changeProviderBulk(@RequestBody UserProviderPayload userProviderPayload){
        return appService.changeProviderBulkUser(userProviderPayload.provider, userProviderPayload.userIdList);
    }

    @PostMapping("{appId}/user-bulk")
    public Map saveAppUserBulk(@RequestBody AppUserPayload payload,
                               @PathVariable("appId") Long appId){
        List<Long> userGroups = payload.groups;
        String emails = payload.email;
        return appService.regUserBulk(userGroups,appId,emails,payload.autoReg,payload.tags);
    }

    @PostMapping("{appId}/once-done")
    public Map<String, Object> onceDone(@PathVariable("appId") Long appId,
                                        @RequestParam("email") String email,
                                        @RequestParam("val") Boolean val){
        return appService.onceDone(appId,email, val);
    }

    @PostMapping("{appId}/remove-acc")
    public Map<String, Object> removeAccount(@PathVariable("appId") Long appId,
                                             @RequestParam("email") String email){
        return appService.removeAcc(appId,email);
    }

    @GetMapping("autocomplete")
    public List<CodeAuto> loadAutoComplete(@RequestParam("type") String type){
        return codeAutoRepository.findByType(type);
    }

    @GetMapping("time")
    public Instant getServerTime(){
        return Instant.now();
    }

    @GetMapping("{appId}/api-keys")
    public List<ApiKey> getApiKeys(@PathVariable("appId") Long appId){
        return appService.getApiKeys(appId);
    }

    @PostMapping("delete-api-key/{apiKeyId}")
    public Map<String, Object> deleteApiKey(@PathVariable("apiKeyId") Long apiKeyId){
        return appService.removeApiKey(apiKeyId);
    }

    @PostMapping("{appId}/generate-key")
    public ApiKey generateKey(@PathVariable("appId") Long appId){
        return appService.generateNewApiKey(appId);
    }

}
