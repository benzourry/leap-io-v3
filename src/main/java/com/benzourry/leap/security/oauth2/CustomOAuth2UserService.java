package com.benzourry.leap.security.oauth2;

import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AuthProvider;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.KeyValueRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.security.oauth2.user.OAuth2UserInfo;
import com.benzourry.leap.security.oauth2.user.OAuth2UserInfoFactory;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.service.EntryService;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    private final UserRepository userRepository;
    private final AppRepository appRepository;
    private final KeyValueRepository keyValueRepository;
    private final AppService appService;
    private final ObjectMapper MAPPER;

    public CustomOAuth2UserService(UserRepository userRepository,
                                   AppRepository appRepository,
                                   KeyValueRepository keyValueRepository,
                                   AppService appService, ObjectMapper MAPPER) {
        this.userRepository = userRepository;
        this.appRepository = appRepository;
        this.appService = appService;
        this.keyValueRepository = keyValueRepository;
        this.MAPPER = MAPPER;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest oAuth2UserRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(oAuth2UserRequest);
        try {
            return processOAuth2User(oAuth2UserRequest, oAuth2User);
        } catch (AuthenticationException ex) {
            throw ex;
        } catch (Exception ex) {
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex.getCause());
        }
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest oAuth2UserRequest, OAuth2User oAuth2User) {
        String accessToken = oAuth2UserRequest.getAccessToken().getTokenValue();
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(oAuth2UserRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes(), accessToken);
        String email = oAuth2UserInfo.getEmail();
        //        if(StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
        if(!StringUtils.hasLength(email)) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

        /**
         *
         if (oAuth2UserRequest.getAdditionalParameters().get("appId")!=null){
         Object key = oAuth2UserRequest.getAdditionalParameters().get("appId");
         Long appId = ((Number) key).longValue();
         user.setAppId(appId);
         }
         *
         * **/
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession(false);

        Long appId = null;

        if (!Helper.isNullOrEmpty(request.getParameter("appId"))){
            // IF appId is passed in the request, use it
            String key = request.getParameter("appId");
            appId = Long.parseLong(key);
        }else if (session!=null && session.getAttribute("appId")!= null && !Helper.isNullOrEmpty(session.getAttribute("appId")+"")) {
            // IF xda dlm parameter, check dlm session
            String key = session.getAttribute("appId").toString();
            appId = Long.parseLong(key);
            session.removeAttribute("appId");
           // session.invalidate();
        }else{
            // IF xda dlm session, try check appPath dlm request. If ada load app n dptkan id
            if (request.getParameter("appPath")!=null){
                App app = appService.findByKey(request.getParameter("appPath"));
                appId = app.getId();
            }
        }

        if (Long.valueOf(-1).equals(appId)){

            Optional<String> allowedCreatorEmail = keyValueRepository.getValue("platform", "allowed-creator-email");
            AntPathMatcher am = new AntPathMatcher();

            if (allowedCreatorEmail.isPresent()){
                String[] patterns = allowedCreatorEmail.get().split(",");
                boolean match = false;
                for (String pattern: patterns){
                    if (am.match(pattern.trim(), email)){
                        match = true;
                        break;
                    }
                }
                if (!match){
                    throw new AuthenticationServiceException("User not allowed to login as Creator : " + email);
                }
            }
        }

        Map<String,String> providers = new HashMap<>();
        providers.put("local","Email/Password");
        providers.put("unimas","UNIMAS Identity");
        providers.put("unimasid","UNIMAS ID");
        providers.put("icatsid","i-CATS Identity");
        providers.put("ssone","ssOne");
        providers.put("facebook","Facebook");
        providers.put("google","Google");
        providers.put("github","Github");
        providers.put("linkedin","LinkedIn");
        providers.put("twitter","Twitter");
        providers.put("azuread","Microsoft");
        providers.put("sarawakid","SarawakID");
        providers.put("mydigitalid","MyDigitalID");

        Optional<User> userOptional = userRepository.findFirstByEmailAndAppId(email,appId);
        User user;
        if(userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider().equals(AuthProvider.undetermine)){
                user = updateNewUser(user,oAuth2UserRequest, oAuth2UserInfo,appId, accessToken);

            }else if(!user.getProvider().equals(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))) {
                logger.info("Email:"+ user.getEmail()+",signed:"+user.getProvider().name()+",attempt:"+oAuth2UserRequest.getClientRegistration().getRegistrationId()+",appId:"+appId);
                throw new OAuth2AuthenticationProcessingException("Looks like you're signed up with " +
                        providers.get(user.getProvider().name()) + " account. Please use your " + providers.get(user.getProvider().name()) +
                        " account to login.|"+user.getProvider()+"|"+appId);
            }else{
                user = updateExistingUser(user, oAuth2UserInfo, accessToken);
            }

        } else {
            user = registerNewUser(oAuth2UserRequest, oAuth2UserInfo,appId);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    private User registerNewUser(OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, Long appId) {
        User user = new User();

        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setImageUrl(oAuth2UserInfo.getImageUrl());
        user.setFirstLogin(new Date());
        user.setLastLogin(new Date());

        if (appId!=null){
            user.setAppId(appId);
        }

        user.setAttributes(MAPPER.valueToTree(oAuth2UserInfo.getAttributes()));

        return userRepository.save(user);
    }

    private User updateNewUser(User user,OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, Long appId, String token) {
//        User user = new User();

        user.setProvider(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()));
        user.setProviderId(oAuth2UserInfo.getId());
        user.setName(oAuth2UserInfo.getName());
        user.setEmail(oAuth2UserInfo.getEmail());
        user.setImageUrl(oAuth2UserInfo.getImageUrl());
        user.setFirstLogin(new Date());
        user.setLastLogin(new Date());
        user.setProviderToken(token);

        if (appId!=null){
            user.setAppId(appId);
            Optional<App> appOpt = appRepository.findById(appId);
            if (appOpt.isPresent()){
                App app = appOpt.get();
//                app.getX().at("/userInfoUri");
            }
        }

        user.setAttributes(MAPPER.valueToTree(oAuth2UserInfo.getAttributes()));

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo, String token) {
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setImageUrl(oAuth2UserInfo.getImageUrl());
        existingUser.setLastLogin(new Date());
        existingUser.setProviderToken(token);
        existingUser.setProviderId(oAuth2UserInfo.getId());
        existingUser.setAttributes(MAPPER.valueToTree(oAuth2UserInfo.getAttributes()));

        return userRepository.save(existingUser);
    }

}
