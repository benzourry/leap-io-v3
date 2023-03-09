package com.benzourry.leap.controller;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.UserMixin;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
public class UserController {

//    @Autowired
    private final UserRepository userRepository;
//    @Autowired
    private final AppUserRepository appUserRepository;

    public UserController(UserRepository userRepository,
                          AppUserRepository appUserRepository){
        this.userRepository = userRepository;
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
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        Map<String, Object> data;
        ObjectMapper mapper = new ObjectMapper();
        Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = mapper.convertValue(user, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userPrincipal.getId(),"approved");
            Map<Long, UserGroup> groupMap = groups.stream().collect(
                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
            data.put("groups", groupMap);
        }else{
            throw new ResourceNotFoundException("User", "id", userPrincipal.getId());
        }
        return data;
    }

    @GetMapping("/user/debug-me")
    @JsonResponse(mixins = {
            @JsonMixin(target = Map.class, mixin = UserMixin.Attributes.class),
            @JsonMixin(target = UserGroup.class, mixin = UserMixin.GroupNameOnly.class)
    })
    //@PreAuthorize("hasRole('USER')")
    public Map<String, Object> getDebugUser(@RequestParam String email, @RequestParam Long appId) {
        Map<String, Object> data;
        ObjectMapper mapper = new ObjectMapper();
        Optional<User> userOpt = userRepository.findFirstByEmailAndAppId(email, appId);//.findById(userPrincipal.getId());
        if (userOpt.isPresent()){
            User user = userOpt.get();
            data = mapper.convertValue(user, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(user.getId(),"approved");
            Map<Long, UserGroup> groupMap = groups.stream().collect(
                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
            data.put("groups", groupMap);
        }else{
            throw new ResourceNotFoundException("User", "email", email);
        }
        return data;
    }
}
