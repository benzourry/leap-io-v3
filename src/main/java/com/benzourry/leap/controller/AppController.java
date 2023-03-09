package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.AppMixin;
import com.benzourry.leap.mixin.NaviMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.service.NotificationService;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
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


//    private final OAuth2AuthorizedClientService clientService;


    public AppController(AppService appService,
                         NotificationService notificationService) {
        this.appService = appService;
        this.notificationService = notificationService;
//        this.clientService = clientService;
    }

    // Only cache app with path:
    @PostMapping
    public App save(@RequestBody App app, @RequestParam("email") String email, Principal principal) {
        // existing app && principal is not app creator
        if (app.getId() != null && !app.getEmail().contains(principal.getName())){
            throw new AuthorizationServiceException("Unauthorized modification by non-creator :" + principal.getName());
        }
        return this.appService.save(app, email);
    }

//    @GetMapping("init-navi")
//    public Map initAll(){
//        Map<String,Object> data = new HashMap<>();
//        this.appService.initAllApp();
//        data.put("success",true);
//        return data;
//    }
//
//    @GetMapping("migrate")
//    public Map migrateAll(){
//        Map<String,Object> data = new HashMap<>();
//        this.appService.migrateAll();
//        data.put("success",true);
//        return data;
//    }

    @GetMapping("{appId}")
//    @Cacheable(value = "app", key = "#appId")
    public App findById(@PathVariable Long appId) {
//        if (email == null) {
        return this.appService.findById(appId);
//        }else{
//            return this.appService.findByIdAndEmail(appId, email);
//        }
    }

//    @GetMapping("{appId}")
//    public App findByIdAndEmail(@PathVariable Long appId, Reques){
//        return this.appService.findById(appId);
//    }

    //    @GetMapping("path/{path}")
//    public App findByPath(@PathVariable String path){
//        return this.appService.findByPath(path);
//    }
//
//    @GetMapping("domain/{domain}")
//    public App findByDomain(@PathVariable String domain){
//        return this.appService.findByDomain(domain);
//    }
    @GetMapping("path/{key:.+}")
//    @Cacheable(value = "appRun", key = "#key")
    public App findByKey(@PathVariable String key) {
        return this.appService.findByKey(key);
    }

//    @GetMapping({"key/{key:.+}","domain/{key:.+}","path/{key:.+}"})
//    public App findByKey2(@PathVariable String key){
//        return this.appService.findByKey(key);
//    }

    @GetMapping("{appId}/navis-all")
    public List<NaviGroup> findNaviByAppId(@PathVariable Long appId) {
        return this.appService.findNaviByAppIdAndEmail(appId, null);
    }

    @GetMapping("{appId}/navis")
//    @Cacheable(value = "appNavi", key = "#appId")
    public List<NaviGroup> findNaviByAppId(@PathVariable Long appId, @RequestParam(required = false) String email) {
        return this.appService.findNaviByAppIdAndEmail(appId, email);
    }

    @PostMapping("{appId}/delete")
//    @Caching(evict = {
//            @CacheEvict(value = "app", key = "#app.id")
//    })
    public void delete(@PathVariable Long appId, @RequestParam("email") String email) {
        this.appService.delete(appId,email);
    }

    @PostMapping("clone")
    public App clone(@RequestBody App app, @RequestParam("email") String email) {
        return this.appService.cloneApp(app, email);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppBasic.class)
    })
    public Page<App> getList(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                             Pageable pageable) {
        return this.appService.getList(searchText, pageable);
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
                               Pageable pageable) {
        return this.appService.getAdminList(email, searchText, pageable);
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
    public CloneRequest request(@PathVariable("appId") Long appId, @RequestParam("email") String requesterEmail) {
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
    public Map activationCheck(@PathVariable("appId") Long appId, @RequestParam("email") String requesterEmail) {
        Map<String, Object> data = new HashMap<>();
        data.put("result", appService.activationCheck(appId, requesterEmail));
        return data;
    }

//    @PostMapping("{appId}/secret")
//    public Map secret(@PathVariable("appId") Long appId, @RequestParam("email") String requesterEmail){
//        Map<String, Object> data = new HashMap<>();
//        Totp totp = new Totp(Base32.encode((appId+requesterEmail).getBytes()));
//        data.put("result",totp.now());
//        return data;
//    }

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

    @GetMapping("logo/{path:.+}")
    @Cacheable("reka.logo")
    public FileSystemResource getFileInline(@PathVariable("path") String path, HttpServletResponse response) {
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
        Integer logoSize = Optional.ofNullable(size).orElse(72);

        FileSystemResource fsr =  new FileSystemResource(Constant.UPLOAD_ROOT_DIR + "/logo/" + app.getLogo() + "/" + logoSize + ".png");
//        String unique = app.getLogo().split("/")[0];
        if (fsr.exists()) {
            return fsr;
        } else {
//            throw new FileNotFoundException("File doesn't exist");
            return null;
        }

    }


    @GetMapping("{path:.+}/manifest.json")
    public Map getManifest(@PathVariable("path") String path) {
        return appService.getManifest(path);
    }


//    @PostMapping("{appId}/navi")
//    public App saveNavi(@PathVariable("appId") Long appId, @RequestBody List<NaviGroup> navi){
//        return appService.saveNavi(appId, navi);
//    }

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
    public NaviItem moveItem(@RequestParam long itemId,
                             @RequestParam long newGroupId,
                             @RequestParam long sortOrder) {
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
    public Map getNavi(@PathVariable("appId") Long appId, @RequestParam(required = false) String email) {
        if (email != null) {
            return appService.getNaviDataByEmail(appId, email);
        } else {
            return appService.getNaviData(appId);
        }
    }

//    @GetMapping({"{appId}/navi-data-full", "{appId}/navi-full"})
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Form.class, mixin = NaviMixin.FormListAccess.class),
//            @JsonMixin(target = Dataset.class, mixin = NaviMixin.DatasetListAccess.class),
//            @JsonMixin(target = Dashboard.class, mixin = NaviMixin.DashboardListAccess.class),
//            @JsonMixin(target = Screen.class, mixin = NaviMixin.ScreenListAccess.class),
//            @JsonMixin(target = Action.class, mixin = NaviMixin.ScreenActionList.class),
//            @JsonMixin(target = Lookup.class, mixin = NaviMixin.LookupListAccess.class),
//    })
//    public Map getNaviFull(@PathVariable("appId") Long appId, @RequestParam(required = false) String email) {
//        if (email != null) {
//            return appService.getNaviDataByEmail(appId, email);
//        } else {
//            return appService.getNaviData(appId);
//        }
//    }

    @GetMapping("{appId}/counts")
    public Map getCounts(@PathVariable("appId") Long appId) {
        return appService.getCounts(appId);
    }


    @GetMapping("{appId}/notification")
    public Page<Notification> findNotiByAppIdAndEmail(@PathVariable Long appId,
                                                      @RequestParam("email") String email,
                                                      Pageable pageable) {
        return this.notificationService.findByAppIdAndEmail(appId, email, PageRequest.of(pageable.getPageNumber(),pageable.getPageSize(), Sort.by("timestamp").descending()));
    }

    @PostMapping("notification-read/{nId}")
    public Notification markNotiByAppIdAndEmail(@PathVariable Long nId) {
        return this.notificationService.markRead(nId);
    }

    @GetMapping("{appId}/pages")
    public List<Map> getPages(@PathVariable("appId") Long appId) {
        return appService.getPages(appId);
    }


    @GetMapping("{appId}/user-by-email")
    public List<AppUser> appUserByEmail(@PathVariable Long appId, @RequestParam String email){
        return appService.findByAppIdAndEmail(appId,email);
    }

    @GetMapping("{appId}/user")
    public Page<AppUser> userByAppId(@PathVariable Long appId,
                                     @RequestParam(defaultValue = "") String searchText,
                                     @RequestParam(required = false) List<String> status,
                                     @RequestParam(required = false) Long group,
                                     Pageable pageable){
        return appService.findUserByAppId(appId,searchText,status,group,pageable);
    }

    @GetMapping("{appId}/user-all")
    public Page<AppUser> userByAppId(@PathVariable Long appId,
                                     @RequestParam(defaultValue = "") String searchText,
                                     @RequestParam(required = false) List<String> status,
                                     Pageable pageable){
        return appService.findAllByAppId(appId,searchText,status,pageable);
    }

    @PostMapping("{appId}/user")
    public Map saveAppUser(@RequestBody List<UserGroup> groups,
                           @PathVariable Long appId,
                           @RequestParam String email,
                           @RequestParam String name,
                           @RequestParam boolean autoReg){
        return appService.regUser(groups,appId,email,name,autoReg);
    }

    record AppUserPayload(String emails, List<UserGroup> userGroups){}

    @PostMapping("{appId}/user-bulk")
    public Map saveAppUserBulk(@RequestBody AppUserPayload payload,
                           @PathVariable Long appId,
                           @RequestParam boolean autoReg){
        List<UserGroup> userGroups = payload.userGroups;
        String emails = payload.emails;
        return appService.regUserBulk(userGroups,appId,emails,autoReg);
    }

    @PostMapping("{appId}/once-done")
    public Map<String, Object> onceDone(@PathVariable Long appId,
                           @RequestParam String email,
                         @RequestParam Boolean val){
        return appService.onceDone(appId,email, val);
    }

    @PostMapping("{appId}/remove-acc")
    public Map<String, Object> removeAccount(@PathVariable Long appId,
                           @RequestParam String email){
        return appService.removeAcc(appId,email);
    }

//    @GetMapping("test-at")
//    public Object testAt(){
//        SecurityContext securityContext = SecurityContextHolder.getContext();
//        OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken)
//                securityContext.getAuthentication();
//
//        OAuth2AuthorizedClient client = clientService
//                .loadAuthorizedClient(oauth2Token.getAuthorizedClientRegistrationId(),
//                        oauth2Token.getName());
//
//        System.out.println(client.getRefreshToken().getTokenValue());
//        return null;
//    }

//    @GetMapping("test")
//    public String testAccessToken(@CurrentUser UserPrincipal principal){
//       // principal.
//        System.out.println();
//        final Authentication authenticationObject = SecurityContextHolder.getContext().getAuthentication();
//        String accessToken = "test";
//        System.out.println(SecurityContextHolder.getContext().getAuthentication());
//        if (authenticationObject != null) {
//            final Object detailObject = authenticationObject.getDetails();
//            if (detailObject instanceof OAuth2AuthenticationDetails) {
//                System.out.println("if #1");
//                final OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) detailObject;
//                accessToken = details.getTokenValue();
//            } else if (detailObject instanceof OAuth2AccessToken) {
//                System.out.println("if #2");
//                final OAuth2AccessToken token = (OAuth2AccessToken) detailObject;
//                accessToken = token.getValue();
//            } else {
//                System.out.println("else");
//                accessToken = null;
//            }
//        }
//        return accessToken;
//    }

//    @GetMapping("migrate-user")
//    public Map migrateAllUser(){
//        Map<String,Object> data = new HashMap<>();
//        this.appService.migrateAllUser();
//        data.put("success",true);
//        return data;
//    }

}
