package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.EndpointRepository;
import com.benzourry.leap.repository.UserRepository;
import com.benzourry.leap.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class EndpointService {
    private final EndpointRepository endpointRepository;
    private final AppRepository appRepository;
    private final AccessTokenService accessTokenService;
    private final UserRepository userRepository;

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
    public Object runEndpoint(Long restId, HttpServletRequest req) throws IOException, InterruptedException {

        Endpoint endpoint = endpointRepository.findById(restId)
                .orElseThrow(()->new ResourceNotFoundException("Endpoint","id",restId));
        ObjectMapper mapper = new ObjectMapper();
        Object returnVal = null;

        if (restId!=null && endpoint != null) {

            String fullUrl = endpoint.getUrl(); //+dm+param;

            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                fullUrl = fullUrl.replace("{" + entry.getKey() + "}", req.getParameter(entry.getKey()));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            HttpResponse<String> response = null;
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

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

                response = httpClient.send(request, bodyHandler);
            } else if ("POST".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(req.getParameterMap())))
                        .uri(URI.create(fullUrl))
                        .build();

                response = httpClient.send(request, bodyHandler);
            }

            if (endpoint.isAuth() && response.statusCode()!=200){
                clearTokens(endpoint.getClientId()+":"+endpoint.getClientSecret());
                throw new RuntimeException("Http request error from [ "+ endpoint.getUrl() + "]:" + response.body());
            }

            if (endpoint.getResponseType()!=null) {
                if ("byte".equals(endpoint.getResponseType())||"text".equals(endpoint.getResponseType())){
                    returnVal = response.body();
                }else if ("json".equals(endpoint.getResponseType())){
                    returnVal = mapper.readTree(response.body());
                }
            }else{
                returnVal = response.body();
            }
        }

        return returnVal;
    }

//    @Retryable(maxAttempts=5, value = RuntimeException.class,
//            backoff = @Backoff(delay = 15000, multiplier = 2))

    @Retryable(retryFor = RuntimeException.class)
    public Object runEndpointByCode(String code, Long appId, HttpServletRequest req, Object body, UserPrincipal userPrincipal) throws IOException, InterruptedException {
        Map<String,Object> map = new HashMap<>();
        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            map.put(entry.getKey(), req.getParameter(entry.getKey()));
        }
//        System.out.println(map);
        return run(code,appId,map, body, userPrincipal);
    }

    /**
     * FOR LAMBDA
     **/
    public Object run(String code, Map<String, Object> map, Object body, UserPrincipal userPrincipal, Lambda lambda) throws Exception {
        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,lambda.getApp().getId());
//        System.out.println("run ep in lambda");
        if (endpoint != null) {
            return run(code,lambda.getApp().getId(),map, body, userPrincipal);
        } else {
            throw new Exception("Endpoint ["+code+"] doesn't exist in App");
        }
    }


    public Object run(String code, Long appId, Map<String, Object> map, Object body, UserPrincipal userPrincipal) throws IOException, InterruptedException, RuntimeException {
        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,appId);
        ObjectMapper mapper = new ObjectMapper();
        Object returnVal = null;

        if (code!=null && endpoint != null) {

            String fullUrl = endpoint.getUrl(); //+dm+param;

            try {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    fullUrl = fullUrl.replace("{" + entry.getKey() + "}", Optional.ofNullable(entry.getValue()).orElse("").toString());
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            HttpResponse<String> response = null;
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

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

                response = httpClient.send(request, bodyHandler);
            } else if ("POST".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                        .uri(URI.create(fullUrl))
                        .build();

                response = httpClient.send(request, bodyHandler);
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
                    returnVal = mapper.readTree(response.body());
                }
            }else{
                returnVal = response.body();
            }
        }
        return returnVal;
    }

    public void clearTokens(String pair){
        accessTokenService.clearAccessToken(pair);
    }


}
