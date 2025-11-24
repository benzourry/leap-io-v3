package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.EndpointRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class EndpointService {
    private final EndpointRepository endpointRepository;
    private final AppRepository appRepository;
    private final AccessTokenService accessTokenService;
    private final UserRepository userRepository;


    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public EndpointService(EndpointRepository endpointRepository,
                           AppRepository appRepository,
                           AccessTokenService accessTokenService,
                           UserRepository userRepository){
        this.endpointRepository = endpointRepository;
        this.appRepository = appRepository;
        this.accessTokenService = accessTokenService;
        this.userRepository = userRepository;
    }

    public Endpoint save(Endpoint endpoint, Long appId, String email){
        App app = appRepository.getReferenceById(appId);
        endpoint.setApp(app);
        if (endpoint.getId()==null) {
            endpoint.setEmail(email);
        }
        return endpointRepository.save(endpoint);
    }

    public Page<Endpoint> findByAppId(Long appId, Pageable pageable){
        return this.endpointRepository.findByAppId(appId, pageable);
    }

    public Page<Endpoint> findShared(Pageable pageable){
        return this.endpointRepository.findShared(pageable);
    }

    public Endpoint findById(Long id) {
        return endpointRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Endpoint","id",id));
    }

    public void delete(Long id) {
        endpointRepository.deleteById(id);
    }


//    @Retryable(retryFor = RuntimeException.class) // it is said retryable is dangerous for streaming
    public HttpResponse<InputStream> runEndpointById(Long restId,
                                  HttpServletRequest req,
                                 UserPrincipal userPrincipal) throws IOException, InterruptedException {

        Endpoint endpoint = endpointRepository.findById(restId)
                .orElseThrow(()->new ResourceNotFoundException("Endpoint","id",restId));

        Map<String,Object> params = new HashMap<>();
        req.getParameterMap().forEach((key, val) -> params.put(key, val[0]));

        return runStream(endpoint,params,req.getParameterMap(),userPrincipal);

    }

//    @Retryable(maxAttempts=5, value = RuntimeException.class,
//            backoff = @Backoff(delay = 15000, multiplier = 2))

//    @Retryable(retryFor = RuntimeException.class)
//    public Object runEndpointByCode(String code, Long appId, HttpServletRequest req, Object body, UserPrincipal userPrincipal) throws IOException, InterruptedException {
//        Map<String,Object> map = new HashMap<>();
//        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
//            map.put(entry.getKey(), req.getParameter(entry.getKey()));
//        }
////        System.out.println(map);
//        return run(code,appId,map, body, userPrincipal);
//    }

//    @Retryable(retryFor = RuntimeException.class)  // it is said retryable is dangerous for streaming
    public HttpResponse<InputStream> runEndpointByCode(
            String code,
            Long appId,
            HttpServletRequest req,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        if (code == null) return null;

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
                .orElseThrow(() -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App"));

        Map<String,Object> params = new HashMap<>();
        req.getParameterMap().forEach((key, val) -> params.put(key, val[0]));

        // NEW: call the ultra-streaming run()
        return runStream(endpoint, params, body, userPrincipal);
    }

    /**
     * FOR LAMBDA
     **/
    public Object run(String code, Map<String, Object> map, Object body, UserPrincipal userPrincipal, Lambda lambda) throws Exception {
        return run(code,lambda.getApp().getId(),map, body, userPrincipal);
    }

    public Object run(
            String code,
            Long appId,
            Map<String, Object> pathParams,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
                .orElseThrow(() -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App"));


        HttpResponse<InputStream> res = runStream(endpoint, pathParams, body, userPrincipal);

        int status = res.statusCode();
        if (status != 200) {
            throw new RuntimeException("Error from upstream: " + status);
        }

        String type = endpoint.getResponseType();

        // Wrap in buffered stream for performance (32KB buffer)
        try (InputStream rawIn = res.body();
             BufferedInputStream in = new BufferedInputStream(rawIn, 32 * 1024)) {

            if ("byte".equals(type)) {
                return in.readAllBytes(); // binary
            }

            if ("text".equals(type)) {
                // Read text fully for small text
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            if ("json".equals(type)) {
                // Streaming parse directly from InputStream
                return MAPPER.readTree(in);
            }

            // fallback: return bytes
            return in.readAllBytes();
        }
    }

    public HttpResponse<InputStream> runStream(
            Endpoint endpoint,
            Map<String, Object> pathParams,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        String url = endpoint.getUrl();
        if (pathParams != null && !pathParams.isEmpty()) {
            StringBuilder sb = new StringBuilder(url);
            for (Map.Entry<String, Object> e : pathParams.entrySet()) {
                String placeholder = "{" + e.getKey() + "}";
                String encoded = URLEncoder.encode(
                        e.getValue() == null ? "" : e.getValue().toString(),
                        StandardCharsets.UTF_8
                );
                int idx;
                while ((idx = sb.indexOf(placeholder)) != -1) {
                    sb.replace(idx, idx + placeholder.length(), encoded);
                }
            }
            url = sb.toString();
        }

        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder();

        String headerString = endpoint.getHeaders();
        if (headerString != null && !headerString.isEmpty()) {
            for (String h : headerString.split("\\|")) {
                int arrow = h.indexOf("->");
                if (arrow > 0) {
                    String key = h.substring(0, arrow).trim();
                    String val = h.substring(arrow + 2).trim();
                    reqBuilder.setHeader(key, val);
                }
            }
        }

        if (endpoint.isAuth()) {
            String token = null;

            if ("authorization".equals(endpoint.getAuthFlow())) {
                if (userPrincipal != null) {
                    User user = userRepository.findById(userPrincipal.getId()).orElse(null);
                    if (user != null) token = user.getProviderToken();
                }
            } else {
                token = accessTokenService.getAccessToken(
                        endpoint.getTokenEndpoint(),
                        endpoint.getClientId(),
                        endpoint.getClientSecret()
                );
            }

            if ("url".equals(endpoint.getTokenTo())) {
                url += (url.contains("?") ? "&" : "?") + "access_token=" + token;
            } else {
                reqBuilder.setHeader("Authorization", "Bearer " + token);
            }
        }

        if ("POST".equals(endpoint.getMethod())) {

            // Body: if body is already string, use it without converting
            HttpRequest.BodyPublisher publisher =
                    (body instanceof String)
                            ? HttpRequest.BodyPublishers.ofString((String) body)
                            : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));

            reqBuilder.POST(publisher);

        } else {
            reqBuilder.GET();
        }

        HttpRequest request = reqBuilder.uri(URI.create(url)).build();


        // ----------------------------
        // Execute (STREAMING response)
        // ----------------------------
        HttpResponse<InputStream> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());


        // ----------------------------
        // Auth failure handling
        // ----------------------------
        if (endpoint.isAuth() && response.statusCode() != 200) {
            clearTokens(endpoint.getClientId() + ":" + endpoint.getClientSecret());
            if ("authorization".equals(endpoint.getAuthFlow())) {
                SecurityContextHolder.clearContext();
            }
            // DO NOT read body → body is still streaming exactly from upstream
            throw new RuntimeException("HTTP [" + url + "] returned " + response.statusCode());
        }

        // ----------------------------
        // RETURN RAW RESPONSE EXACTLY
        // ----------------------------
        return response;
    }

    public void clearTokens(String pair){
        accessTokenService.clearAccessToken(pair);
    }


}
