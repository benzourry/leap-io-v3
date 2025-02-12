package com.benzourry.leap.config;//package com.benzourry.leap.config;//import javax.servlet.http.HttpServletRequest;
////import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.net.URI;
//import java.net.URLEncoder;
//import java.util.Map;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
//import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
//import org.springframework.security.web.RedirectStrategy;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.web.util.UriComponentsBuilder;
//
//public class PostRedirectStrategy implements RedirectStrategy {
//
//    private final RestTemplate restTemplate = new RestTemplate();
//
//    @Override
//    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
//        // Extract the authorization request from the session or elsewhere
//        OAuth2AuthorizationRequest authorizationRequest =
//                (OAuth2AuthorizationRequest) request.getSession()
//                        .getAttribute(OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_ATTR_NAME);
//
//        if (authorizationRequest != null) {
//            // Build the parameters for the POST request
//            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
//            for (Map.Entry<String, Object> entry : authorizationRequest.getAdditionalParameters().entrySet()) {
//                formData.add(entry.getKey(), URLEncoder.encode(entry.getValue().toString(), "UTF-8"));
//            }
//
//            // Make the POST request to the authorization server
//            URI authorizationUri = UriComponentsBuilder.fromUriString(authorizationRequest.getAuthorizationUri())
//                    .build().toUri();
//
//            restTemplate.postForEntity(authorizationUri, formData, String.class);
//        } else {
//            // Fallback to default GET redirect if authorization request is not present
//            response.sendRedirect(url);
//        }
//    }
//}
