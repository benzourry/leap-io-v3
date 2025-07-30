package com.benzourry.leap.security.oauth2;

import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.AuthProvider;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.security.oauth2.user.OAuth2UserInfo;
import com.benzourry.leap.security.oauth2.user.OAuth2UserInfoFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final AppRepository appRepository;

    public CustomOAuth2UserService(UserRepository userRepository, AppRepository appRepository) {
        this.userRepository = userRepository;
        this.appRepository = appRepository;
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
//        if(StringUtils.isEmpty(oAuth2UserInfo.getEmail())) {
        if(!StringUtils.hasLength(oAuth2UserInfo.getEmail())) {
            throw new OAuth2AuthenticationProcessingException("Email not found from OAuth2 provider");
        }

//        System.out.println("EMail found");
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
//        System.out.println("session:"+session.getAttribute("appId"));
//        System.out.println(request.getQueryString());
//
//        System.out.println("<====additionalparameters=====");
//        System.out.println(oAuth2UserRequest.getAdditionalParameters());
//        System.out.println("====additionalparameters=====>");

        Long appId = null;
//        System.out.println("REQ###2a:"+request.getQueryString());
//        System.out.println("REQ###2b:"+request.getParameter("appId"));
//        System.out.println("SESSION:"+session.getAttribute("appId"));
        System.out.println("SESSION$$$$$$$$$$$$$$$$$:"+session.getAttribute("appId"));

        if (request.getParameter("appId")!=null){
            String key = request.getParameter("appId");
            appId = Long.parseLong(key);
        }else if (session.getAttribute("appId")!= null) {
            String key = session.getAttribute("appId").toString();
            appId = Long.parseLong(key);
            session.removeAttribute("appId");
           // session.invalidate();
        }

//        local,
//                unimas,
//                unimasid,
//                icatsid,
//                facebook,
//                google,
//                github,
//                linkedin,
//                twitter,
//                azuread
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

        Optional<User> userOptional = userRepository.findFirstByEmailAndAppId(oAuth2UserInfo.getEmail(),appId);
        User user;
        if(userOptional.isPresent()) {
            user = userOptional.get();
            if (user.getProvider().equals(AuthProvider.undetermine)){
                user = updateNewUser(user,oAuth2UserRequest, oAuth2UserInfo,appId, accessToken);

            }else if(!user.getProvider().equals(AuthProvider.valueOf(oAuth2UserRequest.getClientRegistration().getRegistrationId()))) {
                System.out.println("Email:"+ user.getEmail()+",signed:"+user.getProvider().name()+",attempt:"+oAuth2UserRequest.getClientRegistration().getRegistrationId()+",appId:"+appId);
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
        ObjectMapper mapper = new ObjectMapper();
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

        user.setAttributes(mapper.valueToTree(oAuth2UserInfo.getAttributes()));

        return userRepository.save(user);
    }

    private User updateNewUser(User user,OAuth2UserRequest oAuth2UserRequest, OAuth2UserInfo oAuth2UserInfo, Long appId, String token) {
        ObjectMapper mapper = new ObjectMapper();
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

        user.setAttributes(mapper.valueToTree(oAuth2UserInfo.getAttributes()));

        return userRepository.save(user);
    }

    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo, String token) {
        ObjectMapper mapper = new ObjectMapper();
        existingUser.setName(oAuth2UserInfo.getName());
        existingUser.setImageUrl(oAuth2UserInfo.getImageUrl());
        existingUser.setLastLogin(new Date());
        existingUser.setProviderToken(token);
        existingUser.setProviderId(oAuth2UserInfo.getId());
        existingUser.setAttributes(mapper.valueToTree(oAuth2UserInfo.getAttributes()));

        return userRepository.save(existingUser);
    }

}
