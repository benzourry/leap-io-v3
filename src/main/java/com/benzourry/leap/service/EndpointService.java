package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.EndpointRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.UserPrincipal;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

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
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

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


    @Retryable(retryFor = RuntimeException.class)
    public Object runEndpointById(Long restId, HttpServletRequest req) throws IOException, InterruptedException {

        Endpoint endpoint = endpointRepository.findById(restId)
                .orElseThrow(()->new ResourceNotFoundException("Endpoint","id",restId));
//        ObjectMapper mapper = new ObjectMapper();
        Object returnVal = null;

        if (restId!=null && endpoint != null) {

            String fullUrl = endpoint.getUrl(); //+dm+param;

            if (req !=null) {
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    fullUrl = fullUrl.replace("{" + entry.getKey() + "}", URLEncoder.encode(req.getParameter(entry.getKey()), StandardCharsets.UTF_8));
                }
                fullUrl = fullUrl.replaceAll("\\{.*?\\}", "");
            }

//            if (parameter != null) {
//                for (Map.Entry<String, String> entry : parameter.entrySet()) {
//                    fullUrl = fullUrl.replace("{" + entry.getKey() + "}", URLEncoder.encode(parameter.get(entry.getKey()), StandardCharsets.UTF_8));
//                }
//                //replace remaining with blank
//                fullUrl = fullUrl.replaceAll("\\{.*?\\}", "");
//            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            HttpResponse<String> response = null;
//            HttpClient httpClient = HttpClient.newBuilder()
//                    .version(HttpClient.Version.HTTP_1_1)
//                    .connectTimeout(Duration.ofSeconds(30))
//                    .build();

            if (endpoint.getHeaders()!=null && !endpoint.getHeaders().isEmpty()){
                String [] h1 = endpoint.getHeaders().split(Pattern.quote("|"));
                Arrays.stream(h1).forEach(h->{
                    String [] h2 = h.split(Pattern.quote("->"));
                    requestBuilder.setHeader(h2[0],h2.length>1?h2[1]:null);
                });
            }

            if (endpoint.isAuth()) {
                String accessToken = null;

                if ("authorization".equals(endpoint.getAuthFlow())){
                    UserPrincipal userP = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (userP!=null){
                        User user = userRepository.findById(userP.getId()).orElse(null);
                        if (user!=null){
                            accessToken = user.getProviderToken();
                        }
                    }
                }else{
                    accessToken = accessTokenService.getAccessToken(endpoint.getTokenEndpoint(),endpoint.getClientId(), endpoint.getClientSecret());
                }

                requestBuilder.setHeader("Authorization","Bearer "+ accessToken);
            }

            HttpResponse.BodyHandler bodyHandler;
            if (endpoint.getResponseType()!=null && "byte".equals(endpoint.getResponseType())){
                bodyHandler = HttpResponse.BodyHandlers.ofByteArray();
            }else{
                bodyHandler = HttpResponse.BodyHandlers.ofString();
            }

            if ("GET".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .GET()
                        .uri(URI.create(fullUrl))
                        .build();

                response = HTTP_CLIENT.send(request, bodyHandler);
            } else if ("POST".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(req.getParameterMap())))
                        .uri(URI.create(fullUrl))
                        .build();

                response = HTTP_CLIENT.send(request, bodyHandler);
            }

            if (endpoint.isAuth() && response.statusCode()!=200){
                clearTokens(endpoint.getClientId()+":"+endpoint.getClientSecret());
                throw new RuntimeException("Http request error from [ "+ endpoint.getUrl() + "]:" + response.body());
            }

            if (endpoint.getResponseType()!=null) {
                if ("byte".equals(endpoint.getResponseType())||"text".equals(endpoint.getResponseType())){
                    returnVal = response.body();
                }else if ("json".equals(endpoint.getResponseType())){
                    returnVal = MAPPER.readTree(response.body());
                }
            }else{
                returnVal = response.body();
            }
        }

        return returnVal;
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

    @Retryable(retryFor = RuntimeException.class)
    public HttpResponse<InputStream> runEndpointByCode(
            String code,
            Long appId,
            HttpServletRequest req,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        Map<String,Object> params = new HashMap<>();
        req.getParameterMap().forEach((key, val) -> params.put(key, val[0]));

        // NEW: call the ultra-streaming run()
        return runStream(code, appId, params, body, userPrincipal);
    }

    /**
     * FOR LAMBDA
     **/
    public Object run(String code, Map<String, Object> map, Object body, UserPrincipal userPrincipal, Lambda lambda) throws Exception {
//        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,lambda.getApp().getId()).orElseThrow(()->new Exception("Endpoint ["+code+"] doesn't exist in App"));
////        System.out.println("run ep in lambda");
//        if (endpoint != null) {
            return run(code,lambda.getApp().getId(),map, body, userPrincipal);
//        } else {
//            throw new Exception("Endpoint ["+code+"] doesn't exist in App");
//        }
    }

/*
    public Object runOld(String code, Long appId, Map<String, Object> map, Object body, UserPrincipal userPrincipal) throws IOException, InterruptedException, RuntimeException {
        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,appId).orElseThrow(()->new RuntimeException("Endpoint ["+code+"] doesn't exist in App"));
//        ObjectMapper mapper = new ObjectMapper();
        Object returnVal = null;

        if (code!=null && endpoint != null) {

            String fullUrl = endpoint.getUrl(); //+dm+param;

            try {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    fullUrl = fullUrl.replace("{" + entry.getKey() + "}", URLEncoder.encode(Optional.ofNullable(entry.getValue()).orElse("").toString(), StandardCharsets.UTF_8));
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            HttpResponse<String> response = null;
//            HttpClient httpClient = HttpClient.newBuilder()
//                    .version(HttpClient.Version.HTTP_1_1)
//                    .connectTimeout(Duration.ofSeconds(30))
//                    .build();

            if (endpoint.getHeaders()!=null && !endpoint.getHeaders().isEmpty()){
                String [] h1 = endpoint.getHeaders().split(Pattern.quote("|"));
                Arrays.stream(h1).forEach(h->{
                    String [] h2 = h.split(Pattern.quote("->"));
                    requestBuilder.setHeader(h2[0],h2.length>1?h2[1]:null);
                });
            }

            if (endpoint.isAuth()) {
                String accessToken = null; // accessTokenService.getAccessToken(endpoint.getTokenEndpoint(),endpoint.getClientId(), endpoint.getClientSecret());

                if ("authorization".equals(endpoint.getAuthFlow())){
                    if (userPrincipal!=null){
                        User user = userRepository.findById(userPrincipal.getId()).orElse(null);
                        if (user!=null){
                            accessToken = user.getProviderToken();
                        }
                    }
                }else{
                    accessToken = accessTokenService.getAccessToken(endpoint.getTokenEndpoint(),endpoint.getClientId(), endpoint.getClientSecret());
                }

                if ("url".equals(endpoint.getTokenTo())){
                    // Should have the toggle for token in url vs in header
                    String dm = fullUrl.contains("?") ? "&" : "?";
                    fullUrl = fullUrl + dm + "access_token=" + accessToken;
                }else{
                    requestBuilder.setHeader("Authorization","Bearer "+ accessToken);
                }
            }

            HttpResponse.BodyHandler bodyHandler;
            if (endpoint.getResponseType()!=null && "byte".equals(endpoint.getResponseType())){
                bodyHandler = HttpResponse.BodyHandlers.ofByteArray();
            }else{
                bodyHandler = HttpResponse.BodyHandlers.ofString();
            }

            if ("GET".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .GET()
                        .uri(URI.create(fullUrl))
                        .build();

                response = HTTP_CLIENT.send(request, bodyHandler);
            } else if ("POST".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                        .uri(URI.create(fullUrl))
                        .build();

                response = HTTP_CLIENT.send(request, bodyHandler);
            }

            if (endpoint.isAuth() && response.statusCode()!=200){
                clearTokens(endpoint.getClientId()+":"+endpoint.getClientSecret());
                if ("authorization".equals(endpoint.getAuthFlow())) {
                    SecurityContextHolder.clearContext();
                }
                throw new RuntimeException("Http request error from ["+ fullUrl + "]:" + response.body());
            }

            if (endpoint.getResponseType()!=null) {
                if ("byte".equals(endpoint.getResponseType())||"text".equals(endpoint.getResponseType())){
                    returnVal = response.body();
                }else if ("json".equals(endpoint.getResponseType())){
                    returnVal = MAPPER.readTree(response.body());
                }
            }else{
                returnVal = response.body();
            }
        }
        return returnVal;
    }
*/

    /*
    public Object runNormal(
            String code,
            Long appId,
            Map<String, Object> pathParams,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        if (code == null) return null;

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId).orElseThrow(
                () -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App")
        );

        if (endpoint == null) return null;

        // --- Build URL efficiently ---
        String urlTemplate = endpoint.getUrl();
        if (pathParams != null && !pathParams.isEmpty()) {
            StringBuilder sb = new StringBuilder(urlTemplate);
            for (Map.Entry<String, Object> e : pathParams.entrySet()) {
                String placeholder = "{" + e.getKey() + "}";
                int idx;
                while ((idx = sb.indexOf(placeholder)) != -1) {
                    String encoded = URLEncoder.encode(
                            e.getValue() == null ? "" : e.getValue().toString(),
                            StandardCharsets.UTF_8
                    );
                    sb.replace(idx, idx + placeholder.length(), encoded);
                }
            }
            urlTemplate = sb.toString();
        }

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();

        // --- Headers ---
        String headerString = endpoint.getHeaders();
        if (headerString != null && !headerString.isEmpty()) {
            // Faster split without regex
            for (String h : headerString.split("\\|")) {
                int arrow = h.indexOf("->");
                if (arrow > 0) {
                    String key = h.substring(0, arrow).trim();
                    String val = h.substring(arrow + 2).trim();
                    if (!key.isEmpty()) requestBuilder.setHeader(key, val);
                }
            }
        }

        // --- Auth handling ---
        if (endpoint.isAuth()) {
            String token = null;

            if ("authorization".equals(endpoint.getAuthFlow())) {
                if (userPrincipal != null) {
                    User user = userRepository.findById(userPrincipal.getId()).orElse(null);
                    if (user != null) {
                        token = user.getProviderToken();
                    }
                }
            } else {
                token = accessTokenService.getAccessToken(
                        endpoint.getTokenEndpoint(),
                        endpoint.getClientId(),
                        endpoint.getClientSecret()
                );
            }

            if ("url".equals(endpoint.getTokenTo())) {
                urlTemplate += (urlTemplate.contains("?") ? "&" : "?") + "access_token=" + token;
            } else {
                requestBuilder.setHeader("Authorization", "Bearer " + token);
            }
        }

        HttpResponse.BodyHandler<?> bodyHandler =
                "byte".equals(endpoint.getResponseType())
                        ? HttpResponse.BodyHandlers.ofByteArray()
                        : HttpResponse.BodyHandlers.ofString();

        // --- Build Request ---
        HttpRequest request;
        if ("POST".equals(endpoint.getMethod())) {
            String bodyJson = (body instanceof String)
                    ? (String) body
                    : MAPPER.writeValueAsString(body);

            request = requestBuilder
                    .uri(URI.create(urlTemplate))
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();
        } else {
            request = requestBuilder
                    .uri(URI.create(urlTemplate))
                    .GET()
                    .build();
        }

        // --- Execute ---
        HttpResponse<?> response = HTTP_CLIENT.send(request, bodyHandler);

        if (endpoint.isAuth() && response.statusCode() != 200) {
            clearTokens(endpoint.getClientId() + ":" + endpoint.getClientSecret());
            if ("authorization".equals(endpoint.getAuthFlow())) {
                SecurityContextHolder.clearContext();
            }
            throw new RuntimeException("Http request error from [" + urlTemplate + "]: " + response.body());
        }

        // --- Response Handling ---
        switch (endpoint.getResponseType()) {
            case "byte":
            case "text":
                return response.body();

            case "json":
                return MAPPER.readTree((String) response.body());

            default:
                return response.body();
        }
    }
    */


/*
    public Object runStream(
            String code,
            Long appId,
            Map<String, Object> pathParams,
            Object bodyObj,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        if (code == null) return null;

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId).orElseThrow(
                () -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App")
        );

        if (endpoint == null) return null;

        // ---------------------------------------------------------
        //            URL BUILDING (stream & low-GC)
        // ---------------------------------------------------------
        String url = endpoint.getUrl();
        if (pathParams != null && !pathParams.isEmpty()) {
            StringBuilder sb = new StringBuilder(url);
            for (Map.Entry<String, Object> e : pathParams.entrySet()) {
                String placeholder = "{" + e.getKey() + "}";
                int idx;
                String encoded = URLEncoder.encode(
                        e.getValue() == null ? "" : e.getValue().toString(),
                        StandardCharsets.UTF_8
                );

                while ((idx = sb.indexOf(placeholder)) != -1) {
                    sb.replace(idx, idx + placeholder.length(), encoded);
                }
            }
            url = sb.toString();
        }

        HttpRequest.Builder req = HttpRequest.newBuilder();

        // ---------------------------------------------------------
        //                   HEADERS (zero-regex)
        // ---------------------------------------------------------
        String hdr = endpoint.getHeaders();
        if (hdr != null && !hdr.isEmpty()) {
            for (String h : hdr.split("\\|")) {
                int arrow = h.indexOf("->");
                if (arrow > 0) {
                    String k = h.substring(0, arrow).trim();
                    String v = h.substring(arrow + 2).trim();
                    if (!k.isEmpty()) req.setHeader(k, v);
                }
            }
        }

        // ---------------------------------------------------------
        //                       AUTH
        // ---------------------------------------------------------
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
                req.setHeader("Authorization", "Bearer " + token);
            }
        }

        // ---------------------------------------------------------
        //       REQUEST BODY STREAMING (no large JSON strings)
        // ---------------------------------------------------------
        HttpRequest request;

        if ("POST".equals(endpoint.getMethod())) {

            // Produce JSON stream on demand
            HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofInputStream(() -> {
                try {
                    // Stream JSON directly into the POST body
                    byte[] json = MAPPER.writeValueAsBytes(bodyObj);
                    return new ByteArrayInputStream(json);
                } catch (Exception ex) {
                    throw new UncheckedIOException(new IOException("JSON streaming error", ex));
                }
            });

            request = req
                    .uri(URI.create(url))
                    .POST(publisher)
                    .build();

        } else {
            request = req
                    .uri(URI.create(url))
                    .GET()
                    .build();
        }

        // ---------------------------------------------------------
        //               STREAM RESPONSE (no strings)
        // ---------------------------------------------------------
        HttpResponse<InputStream> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());

        int status = response.statusCode();

        if (endpoint.isAuth() && status != 200) {
            clearTokens(endpoint.getClientId() + ":" + endpoint.getClientSecret());
            if ("authorization".equals(endpoint.getAuthFlow())) {
                SecurityContextHolder.clearContext();
            }
            try (InputStream is = response.body()) {
                String err = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                throw new RuntimeException("HTTP request failed [" + url + "]: " + err);
            }
        }

        // ---------------------------------------------------------
        //               RESPONSE HANDLING (streaming)
        // ---------------------------------------------------------
        String respType = endpoint.getResponseType();

        // 1. BINARY STREAM (download)
        if ("byte".equals(respType)) {
            try (InputStream is = response.body()) {
                return is.readAllBytes(); // caller gets the byte[]; could also stream to file
            }
        }

        // 2. JSON STREAM (no huge in-memory string)
        if ("json".equals(respType)) {
            try (InputStream is = response.body()) {
                return MAPPER.readTree(is); // streaming parser
            }
        }

        // 3. TEXT STREAM
        if ("text".equals(respType)) {
            try (InputStream is = response.body()) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        // 4. DEFAULT → treat as text
        try (InputStream is = response.body()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
*/

    public Object run(
            String code,
            Long appId,
            Map<String, Object> pathParams,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        HttpResponse<InputStream> res = runStream(code, appId, pathParams, body, userPrincipal);

        int status = res.statusCode();
        if (status != 200) {
            throw new RuntimeException("Error from upstream: " + status);
        }

        InputStream in = res.body();

        // Decide type based on endpoint config
        String type = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
                .orElseThrow()
                .getResponseType();

        if ("byte".equals(type)) {
            return in.readAllBytes(); // legacy behavior
        }

        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);

        if ("json".equals(type)) {
            return MAPPER.readTree(text);
        }

        // text or default
        return text;
    }

    public HttpResponse<InputStream> runStream(
            String code,
            Long appId,
            Map<String, Object> pathParams,
            Object body,
            UserPrincipal userPrincipal
    ) throws IOException, InterruptedException {

        if (code == null) return null;

        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code, appId)
                .orElseThrow(() -> new RuntimeException("Endpoint [" + code + "] doesn't exist in App"));


        // ----------------------------
        // Build URL (efficient)
        // ----------------------------
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
