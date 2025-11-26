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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.stream.Collectors;

@Controller
@RequestMapping("token")
public class TokenController {

//    @Autowired
//    private AuthenticationManager authenticationManager;
//
    private final UserRepository userRepository;

    private final AppUserRepository appUserRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;

    private final TokenProvider tokenProvider;

    private final CustomOAuth2UserService customOAuth2UserService;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    public TokenController(UserRepository userRepository, AppUserRepository appUserRepository, TokenProvider tokenProvider, CustomOAuth2UserService customOAuth2UserService, ClientRegistrationRepository clientRegistrationRepository) {
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
        this.tokenProvider = tokenProvider;
        this.customOAuth2UserService = customOAuth2UserService;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

//    @GetMapping("/get-old")
//    public ResponseEntity<?> authenticateUsingAccessToken(@RequestParam String access_token, @RequestParam String provider) {
//
//        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, access_token,null,null);
//
//        OAuth2User user = customOAuth2UserService.loadUser(new OAuth2UserRequest(clientRegistrationRepository.findByRegistrationId("unimas"), accessToken ));
//        OAuth2AuthenticationToken c = new OAuth2AuthenticationToken(user, Collections.
//                singletonList(new SimpleGrantedAuthority("ROLE_USER")),provider);
//        SecurityContextHolder.getContext().setAuthentication(c);
//
//        String token = tokenProvider.createToken(c);
//        return ResponseEntity.ok(new AuthResponse(token));
//    }


    @GetMapping("get")
    public ResponseEntity<?> authenticateUsingAccessToken(@RequestParam("access_token") String access_token,
                                                          @RequestParam("provider") String provider) {

        System.out.println("DLM GET TOKEN");

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, access_token,null,null);

        OAuth2User user = customOAuth2UserService.loadUser(new OAuth2UserRequest(clientRegistrationRepository.findByRegistrationId(provider), accessToken ));
        System.out.println("USER:"+user.getName());
        OAuth2AuthenticationToken c = new OAuth2AuthenticationToken(user, Collections.
                singletonList(new SimpleGrantedAuthority("ROLE_USER")),provider);
        SecurityContextHolder.getContext().setAuthentication(c);

//        System.out.println("name:"+user.getName());
        String token = tokenProvider.createToken(c);
        Map<String, Object> rval = new HashMap<>();
        Long userId = Long.parseLong(user.getName());
        rval.put("auth", new AuthResponse(token));

        Map<String, Object> data;
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()){
            User userObj = userOpt.get();
            data = MAPPER.convertValue(userObj, Map.class);
            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userId,"approved");
            Map<Long, UserGroup> groupMap = groups.stream().collect(
                    Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
            data.put("groups", groupMap);
            rval.put("user",data);
        }else{
            throw new ResourceNotFoundException("User", "id", userId);
        }

//        rval.put("user",userRepository.findById(userId)
//                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId)));
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
        data.put("groups", new HashMap());
        rval.put("user",data);

        System.out.println(rval);
        return ResponseEntity.ok(rval);
    }




}
