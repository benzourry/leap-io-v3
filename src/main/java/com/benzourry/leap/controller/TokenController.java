package com.benzourry.leap.controller;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.payload.AuthResponse;
import com.benzourry.leap.repository.AppUserRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.TokenProvider;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.security.oauth2.CustomOAuth2UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Controller
@RequestMapping("token")
public class TokenController {

    private final UserRepository userRepository;

    private final AppUserRepository appUserRepository;

    private final TokenProvider tokenProvider;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final ObjectMapper MAPPER;

    public TokenController(UserRepository userRepository, AppUserRepository appUserRepository, TokenProvider tokenProvider, CustomOAuth2UserService customOAuth2UserService, ClientRegistrationRepository clientRegistrationRepository, ObjectMapper MAPPER) {
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
        this.tokenProvider = tokenProvider;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.MAPPER = MAPPER;
    }

    @GetMapping("get")
    public ResponseEntity<?> authenticateUsingAccessToken(@RequestParam("access_token") String access_token,
                                                          @RequestParam("provider") String provider) {

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, access_token,null,null);

        OAuth2User user = customOAuth2UserService.loadUser(new OAuth2UserRequest(clientRegistrationRepository.findByRegistrationId(provider), accessToken ));

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(user,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")),provider);

        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        String token = tokenProvider.createToken(authenticationToken);

        Map<String, Object> rval = new HashMap<>();

        Long userId = Long.parseLong(user.getName());

        rval.put("auth", new AuthResponse(token));

        Map<String, Object> data;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()){
            User userObj = userOpt.get();
            data = MAPPER.convertValue(userObj, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userId,"approved");
            Map<Long, UserGroup> groupMap = new HashMap<>();
            for (AppUser x : groups) {
                groupMap.put(x.getGroup().getId(), x.getGroup());
            }
            data.put("groups", groupMap);
            rval.put("user",data);
        }else{
            throw new ResourceNotFoundException("User", "id", userId);
        }

        return ResponseEntity.ok(rval);
    }

    @GetMapping("by-apikey")
    public ResponseEntity<?> authenticateUsingApiKey(@RequestParam("api_key") String api_key,
                                                     Authentication authentication,
                                                     @CurrentUser UserPrincipal userPrincipal) {

        Map<String, Object> rval = new HashMap<>();

        String token = tokenProvider.createToken(authentication);

        rval.put("auth", new AuthResponse(token));

        Map<String, Object> data;

        User user = User.anonymous();

        data = MAPPER.convertValue(user, Map.class);
        data.put("groups", Collections.emptyMap());

        rval.put("user",data);

        return ResponseEntity.ok(rval);
    }




}
