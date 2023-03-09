package com.benzourry.leap.config;

import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private final OAuth2AuthorizationRequestResolver defaultAuthorizationRequestResolver;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {

        this.defaultAuthorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorize");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest =
                this.defaultAuthorizationRequestResolver.resolve(request);

        return authorizationRequest != null ?
                customAuthorizationRequest(authorizationRequest, request) :
                null;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(
            HttpServletRequest request, String clientRegistrationId) {

        OAuth2AuthorizationRequest authorizationRequest =
                this.defaultAuthorizationRequestResolver.resolve(
                        request, clientRegistrationId);

        return authorizationRequest != null ?
                customAuthorizationRequest(authorizationRequest, request) :
                null;
    }

    //    private OAuth2AuthorizationRequest customAuthorizationRequest(
//            OAuth2AuthorizationRequest authorizationRequest) {
//
//        Map<String, Object> additionalParameters =
//                new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
//        additionalParameters.put("prompt", "consent");
//
//        return OAuth2AuthorizationRequest.from(authorizationRequest)
//                .additionalParameters(additionalParameters)
//                .build();
//    }
//    private OAuth2AuthorizationRequest customAuthorizationRequest(
//            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {
//
//        System.out.println("REQ###:" + request.getQueryString());
//
//        String customAuthorizationRequestUri = UriComponentsBuilder
//                .fromUriString(authorizationRequest.getAuthorizationRequestUri())
//                .queryParam("appId", request.getParameter("appId"))
//                .build(true)
//                .toUriString();
//
//        return OAuth2AuthorizationRequest.from(authorizationRequest)
//                .authorizationRequestUri(customAuthorizationRequestUri)
//                .build();
//    }
    private OAuth2AuthorizationRequest customAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request) {

        Map<String, Object> additionalParameters = new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
//        additionalParameters.put("access_type", "offline");
//        additionalParameters.put("appId", request.getParameter("appId"));



        request.getSession().setAttribute("appId",request.getParameter("appId"));
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}