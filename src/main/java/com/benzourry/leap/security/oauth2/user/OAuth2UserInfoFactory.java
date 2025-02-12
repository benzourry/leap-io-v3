package com.benzourry.leap.security.oauth2.user;

import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.model.AuthProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class OAuth2UserInfoFactory {

    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes, String accessToken) {
//        System.out.println(attributes);
        if(registrationId.equalsIgnoreCase(AuthProvider.unimas.toString())) {
            return new UnimasOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.azuread.toString())) {
            return new AzureadOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.google.toString())) {
            return new GoogleOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.icatsid.toString())) {
            return new IcatsIdOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.ssone.toString())) {
            return new SsoneOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.sarawakid.toString())) {
            return new SarawakidOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.unimasid.toString())) {
            return new UnimasIdOAuth2UserInfo(attributes);
        }else if(registrationId.equalsIgnoreCase(AuthProvider.linkedin.toString())) {
            RestTemplate rt = new RestTemplate();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("Authorization", "Bearer "+ accessToken);
            ResponseEntity<JsonNode> h = rt.exchange("https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))", HttpMethod.GET, new HttpEntity<>(params), JsonNode.class);
//            System.out.println(h.getBody().toString());
            String email = h.getBody().at("/elements/0/handle~").at("/emailAddress").asText();
//            System.out.println("email:"+email);
            return new LinkedInOAuth2UserInfo(attributes, email);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.facebook.toString())) {
            return new FacebookOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.instagram.toString())) {
            return new InstagramOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.github.toString())) {
            RestTemplate rt = new RestTemplate();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("Authorization", "Bearer "+ accessToken);
            ResponseEntity<JsonNode> h = rt.exchange("https://api.github.com/user/emails", HttpMethod.GET, new HttpEntity<>(params), JsonNode.class);
//            System.out.println(h.getBody().toString());
            String email = h.getBody().at("/0/email").asText();
            return new GithubOAuth2UserInfo(attributes, email);
        } else if (registrationId.equalsIgnoreCase(AuthProvider.twitter.toString())) {
            return new TwitterOAuth2UserInfo(attributes);
        } else {
            throw new OAuth2AuthenticationProcessingException("Sorry! Login with " + registrationId + " is not supported yet.");
        }
    }
}
