package com.benzourry.leap.controller;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.UserMixin;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.TokenProvider;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

    private final TokenProvider tokenProvider;

    public UserController(UserRepository userRepository,
                          UserGroupRepository userGroupRepository,
                          AppRepository appRepository,
                          KeyValueRepository keyValueRepository,
                          AppService appService,
                          AppUserRepository appUserRepository, ObjectMapper MAPPER,
                          TokenProvider tokenProvider){
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.keyValueRepository = keyValueRepository;
        this.appRepository = appRepository;
        this.appService = appService;
        this.appUserRepository = appUserRepository;
        this.MAPPER = MAPPER;
        this.tokenProvider = tokenProvider;
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
//    public Map<String, Object> getCurrentUser(@CurrentUser UserPrincipal userPrincipal,
//                                              @RequestParam(value="appId",required = false) Long appId) throws AccountNotFoundException {
//        Map<String, Object> data;
//
//        System.out.println("userPrincipal:"+userPrincipal.getId());
//
//        if (userPrincipal==null){
//            throw new AccountNotFoundException(); // IS THIS NECESSARY?
//        }
//
//        Long userPrincipalId = userPrincipal.getId(); // since here assume userPrincipal is not null, no need to check later
//        Map<Long, UserGroup> groupMap = new HashMap<>();
//
//        Optional<User> userOpt = userRepository.findById(userPrincipalId);
//
//        if (userOpt.isPresent()){
//            User user = userOpt.get();
//            data = MAPPER.convertValue(user, Map.class);
//
//            System.out.println("User:"+ user.getId() +", app:"+ user.getAppId()+ ", app_param:"+appId);
//
////            appUserRepository.findIdsByAppIdAndEmailAndStatus()
//
//            appUserRepository.findByUserIdAndStatus(userPrincipalId, "approved")
//                .forEach(au -> groupMap.put(
//                        au.getGroup().getId(),
//                        au.getGroup()
//                ));
//
//            Long principalAppId = userPrincipal.getAppId();
//            if (principalAppId != null && principalAppId > 0) {
//
//                Optional<App> app = appRepository.findById(principalAppId);
//
//                if (app.isPresent() && app.get().getX()!=null) {
//                    if (app.get().getX().at("/userFromApp").isNumber()) {
//                        Long userFromApp = app.get().getX().at("/userFromApp").asLong();
//                        List<AppUser> groups2 = appUserRepository.findByAppIdAndEmailAndStatus(userFromApp, userPrincipal.getEmail(), "approved");
//                        Map<Long, UserGroup> groupMap2 = groups2.stream().collect(
//                                Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
//                        groupMap.putAll(groupMap2);
//                    }
//                }
//            } else {
//                Optional<String> managersOpt = keyValueRepository.getValue("platform", "managers");
//                if (managersOpt.isPresent()) {
//                    String managers = managersOpt.get();
//                    // Remove spaces, tabs, and newlines (\n, \r)
//                    String managersEmail = "," + Optional.ofNullable(managers)
//                            .orElse("")
//                            .replaceAll("[\\s\\r\\n]+", "").toLowerCase() + ",";
//
//                    boolean isManager = managersEmail.contains(","+userPrincipal.getEmail().toLowerCase().trim()+",");
//                    data.put("manager", isManager);
//                }
//            }
//
//            data.put("groups", groupMap);
//
//        }else{
//            if (userPrincipal.getId()==0){
//                User user = User.anonymous();
//                user.setAppId(appId);
//                data = MAPPER.convertValue(user, Map.class);
//                data.put("groups", groupMap);
//
//            }else{
//                throw new ResourceNotFoundException("User", "id", userPrincipal.getId());
//            }
//        }
//        return data;
//    }

    @GetMapping("/user/me")
    @JsonResponse(mixins = {
            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
    })
    public Map<String, Object> getCurrentUser(@CurrentUser UserPrincipal userPrincipal,
                                              @RequestParam(value="appId",required = false) Long appId) throws AccountNotFoundException {
        Map<String, Object> data;

        System.out.println("userPrincipal:"+userPrincipal.getId());

        if (userPrincipal==null){
            throw new AccountNotFoundException();
        }

        Long userPrincipalId = userPrincipal.getId();
        Map<Long, UserGroup> groupMap = new HashMap<>();

        Optional<User> userOpt = userRepository.findById(userPrincipalId);

        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = MAPPER.convertValue(user, Map.class);

            System.out.println("User:"+ user.getId() +", app:"+ user.getAppId()+ ", app_param:"+appId);

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
                    String managersEmail = "," + Optional.ofNullable(managers)
                            .orElse("")
                            .replaceAll("[\\s\\r\\n]+", "").toLowerCase() + ",";

                    boolean isManager = managersEmail.contains(","+userPrincipal.getEmail().toLowerCase().trim()+",");
                    data.put("manager", isManager);
                }
            }

            data.put("groups", groupMap);

        } else {
            // --- NEW: Handle the Impersonated Ghost User (-1L) ---
            if (userPrincipalId == -1L) {
                data = new HashMap<>();
                data.put("id", -1L);
                data.put("email", userPrincipal.getEmail());

                // Extract name from attributes if set by the filter
                String name = userPrincipal.getAttributes() != null && userPrincipal.getAttributes().containsKey("name")
                        ? userPrincipal.getAttributes().get("name").toString()
                        : Helper.capitalize(userPrincipal.getEmail().split("@")[0]);

                data.put("name", name);
                data.put("imageUrl", "assets/img/avatar-big.png");
                data.put("appId", appId);
                data.put("firstLogin", Helper.getCalendarDayStart().getTime());
                data.put("lastLogin", new Date());
                data.put("debug", true);
                data.put("provider", "local");
                data.put("providerId", "0");
                data.put("once", true);
                data.put("attributes", userPrincipal.getAttributes());

                // Fetch ALL groups for this app to give the ghost user full access for debugging
                if (appId != null) {
                    Map<Long, UserGroup> dummyGroupMap = userGroupRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE))
                            .stream()
                            .collect(Collectors.toMap(UserGroup::getId, x -> x));
                    data.put("groups", dummyGroupMap);
                } else {
                    data.put("groups", new HashMap<>());
                }

            }
            // Handle Anonymous User (0)
            else if (userPrincipalId == 0) {
                User user = User.anonymous();
                user.setAppId(appId);
                data = MAPPER.convertValue(user, Map.class);
                data.put("groups", groupMap);
            }
            // Fallback for missing real users
            else {
                throw new ResourceNotFoundException("User", "id", userPrincipal.getId());
            }
        }
        return data;
    }

//    @GetMapping("/user/debug-me")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
//            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
//    })
//    //@PreAuthorize("hasRole('USER')")
//    public Map<String, Object> getDebugUser(@RequestParam("email") String email,
//                                            @RequestParam("appId") Long appId) {
//        Map<String, Object> data;
//        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);//.findById(userPrincipal.getId());
//        Optional<App> app = appRepository.findById(appId);
//
//        Map<Long, UserGroup> groupMap;
//        UserPrincipal simulatedPrincipal;
//
//        if (userOpt.isPresent()){
//            User user = userOpt.get();
//            data = MAPPER.convertValue(user, Map.class);
//            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(user.getId(),"approved");
//            groupMap = groups.stream().collect(
//                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
//
//            // Create principal from existing user
//            simulatedPrincipal = UserPrincipal.create(user);
//
//        }else{
//            groupMap = userGroupRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE))
//                            .stream()
//                            .collect(Collectors.toMap(x -> x.getId(), x -> x));
//            String name = Helper.capitalize(email.split("@")[0]);
//
//            data = new HashMap<>();
//
//            data.put("id",-1l);
//            data.put("email",email);
//            data.put("name",name);
//            data.put("imageUrl","assets/img/avatar-big.png");
//            data.put("appId",appId);
//            data.put("firstLogin",Helper.getCalendarDayStart().getTime());
//            data.put("lastLogin",new Date());
//            data.put("debug",true);
//            data.put("provider","local");
//            data.put("providerId","0");
//            data.put("once",true);
//            Map<String, Object> attributes = Map.of(
//                    "email", email,
//                    "email_verified", true,
//                    "name", name,
//                    "picture", "assets/img/avatar-big.png",
//                    "sub", "0"
//            );
//            data.put("attributes", attributes);
//
//            // Create principal for simulated dummy user
//            simulatedPrincipal = new UserPrincipal(
//                    -1L, email, "", appId, null,
//                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
//            );
//            simulatedPrincipal.setAttributes(attributes);
//
//        }
//
//        if (app.isPresent() && app.get().getX()!=null){
//            if (app.get().getX().at("/userFromApp").isNumber()){
//                Long userFromApp = app.get().getX().at("/userFromApp").asLong();
//                List<AppUser> groups2 = appUserRepository.findByAppIdAndEmailAndStatus(userFromApp,email,"approved");
//                Map<Long, UserGroup> groupMap2 = groups2.stream().collect(
//                        Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
//                groupMap.putAll(groupMap2);
//            }
//        }
//
//        data.put("groups", groupMap);
//
//        // --- GENERATE TOKEN ---
//        // Most TokenProviders require an Authentication object.
//        // Adapt this to match how your tokenProvider creates tokens.
//        Authentication auth = new UsernamePasswordAuthenticationToken(
//                simulatedPrincipal, null, simulatedPrincipal.getAuthorities()
//        );
//        String token = tokenProvider.createToken(auth);
//
//        data.put("accessToken", token);
//
//        return data;
//    }

}
