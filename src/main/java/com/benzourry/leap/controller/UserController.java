package com.benzourry.leap.controller;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.UserMixin;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.AccountNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final AppRepository appRepository;
    private final AppUserRepository appUserRepository;
    private final KeyValueRepository keyValueRepository;
    private final AppService appService;
    private final ObjectMapper MAPPER;

    public UserController(UserRepository userRepository,
                          UserGroupRepository userGroupRepository,
                          AppRepository appRepository,
                          KeyValueRepository keyValueRepository,
                          AppService appService,
                          AppUserRepository appUserRepository, ObjectMapper MAPPER){
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.keyValueRepository = keyValueRepository;
        this.appRepository = appRepository;
        this.appService = appService;
        this.appUserRepository = appUserRepository;
        this.MAPPER = MAPPER;
    }

    @GetMapping("/user/{appId}/photo/{email}")
    public UrlResource getUserPhoto(@PathVariable("appId") Long appId,
                                  @PathVariable("email") String email) throws MalformedURLException {

        final UrlResource DEFAULT_AVATAR =
                new FileUrlResource(new URL("classpath:static/avatar-big.png"));

        return userRepository.findFirstByEmailAndAppId(email, appId)
                .map(User::getImageUrl)
                .map(url->{
                    try {
                        UrlResource res = new UrlResource(url);
                        return res.exists() ? res : DEFAULT_AVATAR;
                    } catch (MalformedURLException e) {
                        return DEFAULT_AVATAR;
                    }
                })
                .orElse(DEFAULT_AVATAR);

    }

//    @GetMapping("/user/me")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
//            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
//    })
////    @PreAuthorize("hasRole('USER')")
//    public Map<String, Object> getCurrentUser(@CurrentUser UserPrincipal userPrincipal,
//                                              @RequestParam(value="appId",required = false) Long appId) {
//
//        Map<String, Object> data;
//
//        long userId = userPrincipal.getId();
//
//        if (userId==0){
//            User user = User.anonymous();
//            user.setAppId(appId);
//            data = MAPPER.convertValue(user, Map.class);
//            data.put("groups", Collections.emptyMap());
//            return data;
//        }
//
//        User user = userRepository.findById(userId)
//                .orElseThrow(()->new ResourceNotFoundException("User","id",userId));
//
//        Map<Long, UserGroup> groupMap = new HashMap<>();
//
//        data = MAPPER.convertValue(user, Map.class);
//        List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userId,"approved");
//        groupMap = groups.stream().collect(
//                Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
//
//        /// check if ada appId;
//        Long userPrincipalAppId = userPrincipal.getAppId();
//
//        if (userPrincipalAppId != null && userPrincipalAppId > 0) {
//            Optional<App> app = appRepository.findById(userPrincipalAppId);
//            if (app.isPresent() && app.get().getX()!=null) {
//                if (app.get().getX().at("/userFromApp").isNumber()) {
//                    Long userFromApp = app.get().getX().at("/userFromApp").asLong();
//                    List<AppUser> groups2 = appUserRepository.findByAppIdAndEmailAndStatus(userFromApp, userPrincipal.getEmail(), "approved");
//                    Map<Long, UserGroup> groupMap2 = groups2.stream().collect(
//                            Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
//                    groupMap.putAll(groupMap2);
//                }
//            }
//        } else {
//            Optional<String> managersOpt = keyValueRepository.getValue("platform", "managers");
//            if (managersOpt.isPresent()) {
//                String managers = managersOpt.get();
//                // Remove spaces, tabs, and newlines (\n, \r)
//                String managersEmail = "," + Optional.ofNullable(managers)
//                        .orElse("")
//                        .replaceAll("[\\s\\r\\n]+", "") + ",";
//
//                        boolean isManager = managersEmail.contains(","+userPrincipal.getEmail()+",");
//                data.put("manager", isManager);
//            }
//        }
//
//        data.put("groups", groupMap);
//
//        return data;
//    }


    @GetMapping("/user/me")
    @JsonResponse(mixins = {
            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
    })
//    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> getCurrentUser(@CurrentUser UserPrincipal userPrincipal,
                                              @RequestParam(value="appId",required = false) Long appId) throws AccountNotFoundException {
        Map<String, Object> data;

        if (userPrincipal==null){
            throw new AccountNotFoundException(); // IS THIS NECESSARY?
        }

        Long userPrincipalId = userPrincipal.getId(); // since here assume userPrincipal is not null, no need to check later
        Map<Long, UserGroup> groupMap = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userPrincipalId);

        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = MAPPER.convertValue(user, Map.class);

            appUserRepository.findByUserIdAndStatus(userPrincipalId, "approved")
                .forEach(au -> groupMap.put(
                        au.getGroup().getId(),
                        au.getGroup()
                ));

            Long principalAppId = userPrincipal.getAppId();
            if (principalAppId != null && principalAppId > 0) {

                Optional<App> app = appRepository.findById(principalAppId);

                if (app.isPresent() && app.get().getX()!=null) {
                    if (app.get().getX().at("/userFromApp").isNumber()) {
                        Long userFromApp = app.get().getX().at("/userFromApp").asLong();
                        List<AppUser> groups2 = appUserRepository.findByAppIdAndEmailAndStatus(userFromApp, userPrincipal.getEmail(), "approved");
                        Map<Long, UserGroup> groupMap2 = groups2.stream().collect(
                                Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
                        groupMap.putAll(groupMap2);
                    }
                }
            } else {
                Optional<String> managersOpt = keyValueRepository.getValue("platform", "managers");
                if (managersOpt.isPresent()) {
                    String managers = managersOpt.get();
                    // Remove spaces, tabs, and newlines (\n, \r)
                    String managersEmail = "," + Optional.ofNullable(managers)
                            .orElse("")
                            .replaceAll("[\\s\\r\\n]+", "").toLowerCase() + ",";

                    boolean isManager = managersEmail.contains(","+userPrincipal.getEmail().toLowerCase().trim()+",");
                    data.put("manager", isManager);
                }
            }

            data.put("groups", groupMap);

        }else{
            if (userPrincipal.getId()==0){
                User user = User.anonymous();
                user.setAppId(appId);
                data = MAPPER.convertValue(user, Map.class);
                data.put("groups", groupMap);

            }else{
                throw new ResourceNotFoundException("User", "id", userPrincipal.getId());
            }
        }
        return data;
    }


    @GetMapping("/user/debug-me")
    @JsonResponse(mixins = {
            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
    })
    //@PreAuthorize("hasRole('USER')")
    public Map<String, Object> getDebugUser(@RequestParam("email") String email,
                                            @RequestParam("appId") Long appId) {
        Map<String, Object> data;
        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);//.findById(userPrincipal.getId());
        Optional<App> app = appRepository.findById(appId);

        Map<Long, UserGroup> groupMap;

        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = MAPPER.convertValue(user, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(user.getId(),"approved");
            groupMap = groups.stream().collect(
                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));

        }else{
            groupMap = userGroupRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE))
                            .stream()
                            .collect(Collectors.toMap(x -> x.getId(), x -> x));
            String name = Helper.capitalize(email.split("@")[0]);

            data = new HashMap<>();

            data.put("id",-1l);
            data.put("email",email);
            data.put("name",name);
            data.put("imageUrl","assets/img/avatar-big.png");
            data.put("appId",appId);
            data.put("firstLogin",Helper.getCalendarDayStart().getTime());
            data.put("lastLogin",new Date());
            data.put("debug",true);
            data.put("provider","local");
            data.put("providerId","0");
            data.put("once",true);
            data.put("attributes",
                Map.of(
                    "email", email,
                    "email_verified", true,
                    "name", name,
                    "picture", "assets/img/avatar-big.png",
                    "sub", "0"
                )
            );

        }

        if (app.isPresent() && app.get().getX()!=null){
            if (app.get().getX().at("/userFromApp").isNumber()){
                Long userFromApp = app.get().getX().at("/userFromApp").asLong();
                List<AppUser> groups2 = appUserRepository.findByAppIdAndEmailAndStatus(userFromApp,email,"approved");
                Map<Long, UserGroup> groupMap2 = groups2.stream().collect(
                        Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
                groupMap.putAll(groupMap2);
            }
        }

        data.put("groups", groupMap);
        return data;
    }

}
