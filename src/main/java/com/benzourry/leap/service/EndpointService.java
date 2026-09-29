package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.exception.UpstreamServerErrorException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.Endpoint;
import com.benzourry.leap.model.Lambda;
import com.benzourry.leap.model.User;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.EndpointRepository;
import com.benzourry.leap.repository.SecretRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLSession;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class EndpointService {
    private final EndpointRepository endpointRepository;
    private final AppRepository appRepository;
    private final AccessTokenService accessTokenService;
    private final UserRepository userRepository;
    private final ObjectMapper MAPPER;
    private final HttpClient HTTP_CLIENT;

    private final SecretRepository secretRepository;

    private final EndpointService self; // for calling retryable methods internally

    public EndpointService(EndpointRepository endpointRepository,
                           AppRepository appRepository,
                           AccessTokenService accessTokenService,
                           UserRepository userRepository, ObjectMapper MAPPER,
                           SecretRepository secretRepository,
                           HttpClient HTTP_CLIENT,
                           @Lazy EndpointService self) {
        this.endpointRepository = endpointRepository;
        this.appRepository = appRepository;
        this.accessTokenService = accessTokenService;
        this.userRepository = userRepository;
        this.secretRepository = secretRepository;
        this.MAPPER = MAPPER;
        this.HTTP_CLIENT = HTTP_CLIENT;
        this.self = self;
    }

    public Endpoint save(Endpoint endpoint, Long appId, String email){
        App app = appRepository.getReferenceById(appId);
        endpoint.setApp(app);
        if (endpoint.getId()==null) {
            endpoint.setEmail(email);
        }
        return endpointRepository.save(endpoint);
    }

    public Page<Endpoint> findByAppId(Long appId, String searchText, Pageable pageable){
        searchText = "%" + searchText.toUpperCase() + "%";
        return this.endpointRepository.findByAppId(appId, searchText, pageable);
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
                                 UserPrincipal userPrincipal) throws IOException {

        Endpoint endpoint = endpointRepository.findById(restId)
                .orElseThrow(()->new ResourceNotFoundException("Endpoint","id",restId));

        Map<String,Object> params = new HashMap<>();
        req.getParameterMap().forEach((key, val) -> params.put(key, val[0]));

        Map<String, String> headers = extractHeaders(req);

        return self.runStream(endpoint,params,headers, req.getParameterMap(),userPrincipal);

    }


//    @Retryable(retryFor = RuntimeException.class)  // it is said retryable is dangerous for streaming
    public HttpResponse<InputStream> runEndpointByCode(
            String code,
            Long appId,
            HttpServletRequest req,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException {

        if (code == null) return null;

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
                .orElseThrow(() -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App"));

        Map<String,Object> params = new HashMap<>();
        req.getParameterMap().forEach((key, val) -> params.put(key, val[0]));

        Map<String, String> headers = extractHeaders(req);

        // NEW: call the ultra-streaming run()
        return self.runStream(endpoint, params, headers, body, userPrincipal);
    }

    /**
     * FOR LAMBDA
     **/
    public Object run(String code, Map<String, Object> map, Map<String, String> headers, Object body, UserPrincipal userPrincipal, Lambda lambda) throws Exception {
        return run(code,lambda.getApp().getId(),map, headers, body, userPrincipal);
    }

    // Add the new overloaded method that accepts headers
    public Object run(
            String code,
            Long appId,
            Map<String, Object> pathParams,
            Map<String, String> incomingHeaders, // NEW: Accept forwarded headers
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException {

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
                .orElseThrow(() -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App"));

        // Pass the incomingHeaders to runStream
        HttpResponse<InputStream> res = self.runStream(endpoint, pathParams, incomingHeaders, body, userPrincipal);

        int status = res.statusCode();
        if (status != 200) {
            TenantLogger.error(appId, "endpoint", endpoint.getId(), "Upstream returned non-200 status: " + status);
            throw new RuntimeException("Error from upstream: " + status);
        }

        String type = endpoint.getResponseType();

        try (InputStream rawIn = res.body();
             BufferedInputStream in = new BufferedInputStream(rawIn, 32 * 1024)) {

            return switch (type) {
                case "byte" -> in.readAllBytes();
                case "text" -> new String(in.readAllBytes(), StandardCharsets.UTF_8);
                case "json" -> MAPPER.readTree(in);
                default -> in.readAllBytes();
            };
        }
    }

//    public Object run(
//            String code,
//            Long appId,
//            Map<String, Object> pathParams,
//            Object body,
//            UserPrincipal userPrincipal
//    ) throws IOException {
//
//        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
//                .orElseThrow(() -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App"));
//
//
//        HttpResponse<InputStream> res = self.runStream(endpoint, pathParams, Collections.emptyMap(), body, userPrincipal);
//
//        int status = res.statusCode();
//        if (status != 200) {
//            TenantLogger.error(appId, "endpoint", endpoint.getId(), "Upstream returned non-200 status: " + status);
//            throw new RuntimeException("Error from upstream: " + status);
//        }
//
//        String type = endpoint.getResponseType();
//
//        // Wrap in buffered stream for performance (32KB buffer)
//        try (InputStream rawIn = res.body();
//             BufferedInputStream in = new BufferedInputStream(rawIn, 32 * 1024)) {
//
//
//            return switch (type) {
//                case "byte" -> in.readAllBytes();
//                case "text" -> new String(in.readAllBytes(), StandardCharsets.UTF_8);
//                case "json" -> MAPPER.readTree(in);
//                default ->
//                    // fallback for unknown type
//                        in.readAllBytes();
//            };
//        }
//    }

    @Retryable(
            retryFor = { IOException.class, UpstreamServerErrorException.class, IllegalStateException.class, ConnectException.class },
            noRetryFor = {
                    ResourceNotFoundException.class // DO NOT retry if the secret/app is missing
            },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public HttpResponse<InputStream> runStream(
            Endpoint endpoint,
            Map<String, Object> pathParams,
            Map<String, String> incomingHeaders, // Passed headers
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException {

        int attempt = Optional.ofNullable(RetrySynchronizationManager.getContext())
                .map(c -> c.getRetryCount() + 1)
                .orElse(1);

        Long appId = endpoint.getAppId();
        Long endpointId = endpoint.getId();
        String url = endpoint.getUrl();

        // 1. RESOLVE SECRETS
        if (url.contains("_secret")) {
            Map<String, Set<String>> secrets = Helper.extractVariables(Set.of("_secret"), url);
            for (String s : secrets.getOrDefault("_secret", Collections.emptySet())) {
                String value = secretRepository.getValue(appId, s)
                        .orElseThrow(() -> {
                            TenantLogger.error(appId, "endpoint", endpointId, "Secret [" + s + "] not found for endpoint URL");
                            return new ResourceNotFoundException("Secret", "key+appId", s + "+" + appId);
                        });
                url = url.replace("{_secret." + s + "}", value);
            }
        }

        // 1.5 RESOLVE TEMPLATE VARIABLES (User)
        if (url.contains("$user$") && userPrincipal != null) {
            Map<String, Object> dataMap = new HashMap<>();

            User user = userRepository.findById(userPrincipal.getId())
                    .orElseGet(() -> {
                        User newUser = new User();
                        newUser.setEmail(userPrincipal.getEmail());
                        return newUser;
                    });

            // Convert the full user object to a Map just like in updateApprover
            Map<String, Object> userMap = MAPPER.convertValue(user, Map.class);
            dataMap.put("user", userMap);

            // Compile the URL string
            url = Helper.compileTpl(url, dataMap);
        }

        // 2. RESOLVE PATH PARAMS
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

        // Parse the URI early to get the target host
        URI targetUri;
        try {
            targetUri = URI.create(url);
            reqBuilder.uri(targetUri);
        } catch (IllegalArgumentException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Invalid URI format";
            TenantLogger.error(appId, "endpoint", endpointId, "Failed to build request URI: " + errorMsg);
            throw new RuntimeException("Invalid endpoint URL: " + url, e);
        }

        // 3. RESOLVE HEADERS

//        System.out.println(">>>>>>>>>>>>>>>>>>>>" + incomingHeaders);

        // Conditionally forward headers ONLY if target host matches IO_BASE_DOMAIN
        if (incomingHeaders != null && !incomingHeaders.isEmpty()) {
//            String targetHost = targetUri.getHost();

//            System.out.println(">>>>>>>>>>>>>>>ada header");
//            System.out.println(">>>>>>>>>>>>>>>taergethost:"+ url);
//            System.out.println(">>>>>>>>>>>>>>>IO_BASE:"+ Constant.IO_BASE_DOMAIN);

            // Use endsWith to catch both "domain.com" and "api.domain.com"
            if (url.contains(Constant.IO_BASE_DOMAIN)) {
//                System.out.println(">>>>>>>>>>>>>APPLIED HEADERS");
                Set<String> restricted = Set.of("host", "connection", "content-length", "expect", "upgrade", "accept-encoding");
                incomingHeaders.forEach((key, val) -> {
                    if (!restricted.contains(key.toLowerCase())) {
                        reqBuilder.setHeader(key, val);
                    }
                });
            }
        }

        // 3. RESOLVE HEADERS
        String headerString = endpoint.getHeaders();
        if (headerString != null && !headerString.isEmpty()) {
            for (String h : headerString.split("\\|")) {
                int arrow = h.indexOf("->");
                if (arrow > 0) {
                    reqBuilder.setHeader(h.substring(0, arrow).trim(), h.substring(arrow + 2).trim());
                }
            }
        }

        // 4. RESOLVE AUTHENTICATION
        if (endpoint.isAuth()) {
            String token = null;

            if ("authorization".equals(endpoint.getAuthFlow())) {
                if (userPrincipal != null) {
                    token = userRepository.findById(userPrincipal.getId())
                            .map(User::getProviderToken)
                            .orElse(null);
                }
            } else {
                String clientSecret = endpoint.getClientSecret();

                if (clientSecret != null && clientSecret.contains("{{_secret.")) {
                    String key = Helper.extractTemplateKey(clientSecret, "{{_secret.", "}}").orElseThrow(() -> {
                        TenantLogger.error(appId, "endpoint", endpointId, "Cannot extract secret key from client secret template");
                        return new RuntimeException("Cannot extract secret key from template");
                    });

                    clientSecret = secretRepository.findByKeyAndAppId(key, appId)
                            .orElseThrow(() -> {
                                TenantLogger.error(appId, "endpoint", endpointId, "Secret [" + key + "] not found for client secret");
                                return new ResourceNotFoundException("Secret", "key+appId", key + "+" + appId);
                            })
                            .getValue();
                }

                token = accessTokenService.getAccessToken(endpoint.getTokenEndpoint(), endpoint.getClientId(), clientSecret);
            }

            // Apply token
            if (token != null) {
                if ("url".equals(endpoint.getTokenTo())) {
                    url += (url.contains("?") ? "&" : "?") + "access_token=" + token;
                } else {
                    reqBuilder.setHeader("Authorization", "Bearer " + token);
                }
            }
        }

        // 5. BUILD URI & HTTP METHOD
//        try {
//            reqBuilder.uri(URI.create(url));
//        } catch (IllegalArgumentException e) {
//            String errorMsg = e.getMessage() != null ? e.getMessage() : "Invalid URI format";
//            TenantLogger.error(appId, "endpoint", endpointId, "Failed to build request URI: " + errorMsg);
//            // CRITICAL FIX: Throw exception to prevent NullPointerException downstream!
//            throw new RuntimeException("Invalid endpoint URL: " + url, e);
//        }

        if ("POST".equalsIgnoreCase(endpoint.getMethod())) {
            HttpRequest.BodyPublisher publisher = (body instanceof String)
                    ? HttpRequest.BodyPublishers.ofString((String) body)
                    : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));
            reqBuilder.POST(publisher);
        } else {
            reqBuilder.GET();
        }

        HttpRequest request = reqBuilder.build();

        // 6. EXECUTE REQUEST
        HttpResponse<InputStream> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            TenantLogger.error(appId, "endpoint", endpointId, "Attempt #" + attempt + ": Endpoint request interrupted.");
            throw new IllegalStateException("Request interrupted", e);
        } catch (IOException e) {
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            TenantLogger.error(appId, "endpoint", endpointId, "Attempt #" + attempt + ": Network error: " + errorMsg);
            throw e;
        }

        // 7. HANDLE ERRORS
        if (response.statusCode() >= 400) {

            // 7a. Clean up auth if it failed
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                if (endpoint.isAuth()) {
                    accessTokenService.clearAccessToken(endpoint.getClientId() + ":" + endpoint.getClientSecret());
                    if ("authorization".equals(endpoint.getAuthFlow())) {
                        SecurityContextHolder.clearContext();
                    }
                    TenantLogger.error(appId, "endpoint", endpointId, "Attempt #" + attempt + ": Authentication failed with status " + response.statusCode() + ". Tokens cleared.");
                }
                TenantLogger.error(appId, "endpoint", endpoint.getId(), "Attempt #" + attempt + ": Endpoint require authentication, response status: " + response.statusCode());
                throw new UpstreamServerErrorException("Attempt #" + attempt + ": Endpoint Auth Failed: " + response.statusCode());
            }

            // 7b. Safely extract error body using Try-With-Resources to prevent stream leaks
            try (InputStream errorStream = response.body()) {
                byte[] errorBytes = errorStream != null ? errorStream.readAllBytes() : new byte[0];
                String errorBody = new String(errorBytes, StandardCharsets.UTF_8);

                TenantLogger.error(appId, "endpoint", endpointId,
                        "Attempt #" + attempt + ": Upstream returned non-200 status: " + response.statusCode() + ". Response: " + errorBody);

                response = cloneResponseWithNewBody(response, errorBytes);
            } catch (IOException e) {
                TenantLogger.error(appId, "endpoint", endpointId, "Failed to read error response body safely");
            }


            throw new UpstreamServerErrorException("Attempt #" + attempt + ": HTTP [" + url + "] returned " + response.statusCode());

        }

        return response;
    }

    // NEW HELPER METHOD: safely extract headers
    private Map<String, String> extractHeaders(HttpServletRequest req) {
        Map<String, String> headers = new HashMap<>();
        if (req != null && req.getHeaderNames() != null) {
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String key = headerNames.nextElement();
                headers.put(key, req.getHeader(key));
            }
        }
        return headers;
    }

    public void clearTokens(String pair){
        accessTokenService.clearAccessToken(pair);
    }


    private HttpResponse<InputStream> cloneResponseWithNewBody(HttpResponse<InputStream> originalResponse, byte[] newBodyBytes) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return originalResponse.statusCode(); }
            @Override public HttpRequest request() { return originalResponse.request(); }
            @Override public Optional<HttpResponse<InputStream>> previousResponse() { return originalResponse.previousResponse(); }
            @Override public HttpHeaders headers() { return originalResponse.headers(); }
            @Override public InputStream body() { return new ByteArrayInputStream(newBodyBytes); }
            @Override public Optional<SSLSession> sslSession() { return originalResponse.sslSession(); }
            @Override public URI uri() { return originalResponse.uri(); }
            @Override public HttpClient.Version version() { return originalResponse.version(); }
        };
    }


}
