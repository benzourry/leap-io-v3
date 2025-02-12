package com.benzourry.leap.controller;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.UserMixin;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserGroupRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
public class UserController {

//    @Autowired
    private final UserRepository userRepository;
    private final UserGroupRepository userGroupRepository;
    private final AppRepository appRepository;
//    @Autowired
    private final AppUserRepository appUserRepository;

    private final AppService appService;

    public UserController(UserRepository userRepository,
                          UserGroupRepository userGroupRepository,
                          AppRepository appRepository,
                          AppService appService,
                          AppUserRepository appUserRepository){
        this.userRepository = userRepository;
        this.userGroupRepository = userGroupRepository;
        this.appRepository = appRepository;
        this.appService = appService;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/user/me-old")
    @PreAuthorize("hasRole('USER')")
    public User getCurrentUserOld(@CurrentUser UserPrincipal userPrincipal) {
        return userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));
    }

    @GetMapping("/user/me")
    @JsonResponse(mixins = {
            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
    })
//    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> getCurrentUser(@CurrentUser UserPrincipal userPrincipal,
                                              @RequestParam(value="appId",required = false) Long appId) {
        Map<String, Object> data;
        ObjectMapper mapper = new ObjectMapper();
        Optional<User> userOpt = userRepository.findById(userPrincipal.getId());

        Map<Long, UserGroup> groupMap = new HashMap<>();

        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = mapper.convertValue(user, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userPrincipal.getId(),"approved");
            groupMap = groups.stream().collect(
                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));

            /// check if ada appId;
            if (userPrincipal!=null && userPrincipal.getAppId()!=null && userPrincipal.getAppId()>0){
                Optional<App> app = appRepository.findById(userPrincipal.getAppId());
                if (app.isPresent()){
                    if (app.get().getX().at("/userFromApp").isNumber()){
                        Long userFromApp = app.get().getX().at("/userFromApp").asLong();
                        List<AppUser> groups2 = appUserRepository.findByAppIdAndEmailAndStatus(userFromApp,userPrincipal.getEmail(),"approved");
                        Map<Long, UserGroup> groupMap2 = groups2.stream().collect(
                                Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
                        groupMap.putAll(groupMap2);
                    }
                }
            }

            data.put("groups", groupMap);
//
        }else{
            if (userPrincipal.getId()==0){

                User user = User.anonymous();
                user.setAppId(appId);
                data = mapper.convertValue(user, Map.class);
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
        ObjectMapper mapper = new ObjectMapper();
        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);//.findById(userPrincipal.getId());
        Optional<App> app = appRepository.findById(appId);

        Map<Long, UserGroup> groupMap;

        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = mapper.convertValue(user, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(user.getId(),"approved");
            groupMap = groups.stream().collect(
                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));

        }else{
            groupMap = userGroupRepository.findByAppId(appId, PageRequest.ofSize(Integer.MAX_VALUE))
                            .stream()
                            .collect(Collectors.toMap(x -> x.getId(), x -> x));
            String name = Helper.capitalize(email.split("@")[0]);

            /**
            data = Map.of(
                    "id",-1l,
                    "email",email,
                    "name",name,
                    "appId",appId,
                    "firstLogin", new Date(),
                    "lastLogin", new Date(),
                    "debug", true,
                    "provider", "local",
                    "providerId","0",
                    "once", true,
                    "groups", groupMap
            );
             **/

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
        if (app.isPresent()){
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
