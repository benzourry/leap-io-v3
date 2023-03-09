package com.benzourry.leap.service;

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

//import com.fasterxml.jackson.databind.JsonNode;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//import org.springframework.web.reactive.function.BodyInserters;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;

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
        endpoint.setEmail(email);
        return endpointRepository.save(endpoint);
    }

    public Page<Endpoint> findByAppId(Long appId, Pageable pageable){
        return this.endpointRepository.findByAppId(appId, pageable);
    }

    public Page<Endpoint> findShared(Pageable pageable){
        return this.endpointRepository.findShared(pageable);
    }

    public Endpoint findById(Long id) {
        return endpointRepository.getReferenceById(id);
    }

    public void delete(Long id) {
        endpointRepository.deleteById(id);
    }


    @Retryable(value = RuntimeException.class)
    public Object runEndpoint(Long restId, HttpServletRequest req) throws IOException, InterruptedException {

        Endpoint endpoint = endpointRepository.getReferenceById(restId);
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

//            HttpHeaders headers = new HttpHeaders();
            if (endpoint.getHeaders()!=null && !endpoint.getHeaders().isEmpty()){
                String [] h1 = endpoint.getHeaders().split(Pattern.quote("|"));
                Arrays.stream(h1).forEach(h->{
                    String [] h2 = h.split(Pattern.quote("->"));
//                    headers.set(h2[0],h2.length>1?h2[1]:null);
                    requestBuilder.setHeader(h2[0],h2.length>1?h2[1]:null);
                });
            }

            if (endpoint.isAuth()) {
                String accessToken = null;

                if ("authorization".equals(endpoint.getAuthFlow())){
//                    System.out.println("AUTHFLOW====AUTHORIZATION");
                    UserPrincipal userP = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (userP!=null){
                        User user = userRepository.getReferenceById(userP.getId());
                        accessToken = user.getProviderToken();
                    }
                }else{
//                    System.out.println("AUTHFLOW!!!==AUTHORIZATION");
                    accessToken = accessTokenService.getAccessToken(endpoint.getTokenEndpoint(),endpoint.getClientId(), endpoint.getClientSecret());
                }

//                System.out.println(accessToken);

                // Should have the toggle for token in url vs in header
//                String dm = fullUrl.contains("?") ? "&" : "?";
//                fullUrl = fullUrl + dm + "access_token=" + accessToken;

                requestBuilder.setHeader("Authorization","Bearer "+ accessToken);
            }

            if ("GET".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .GET()
                        .uri(URI.create(fullUrl))
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } else if ("POST".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(req.getParameterMap())))
                        .uri(URI.create(fullUrl))
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }

            if (endpoint.isAuth() && response.statusCode()!=200){
//                System.out.println("dlm response Status Not 200");
                clearTokens(endpoint.getClientId()+":"+endpoint.getClientSecret());
//                System.out.println("throw exception");
                throw new RuntimeException("Http request error from [ "+ endpoint.getUrl() + "]:" + response.body());
            }


            returnVal = mapper.readTree(response.body());
        }

        return returnVal;
    }

//    @Retryable(maxAttempts=5, value = RuntimeException.class,
//            backoff = @Backoff(delay = 15000, multiplier = 2))

    @Retryable(value = RuntimeException.class)
    public Object runEndpointByCode(String code, Long appId, HttpServletRequest req, Object body) throws IOException, InterruptedException {
        Map<String,String> map = new HashMap<>();
        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
            map.put(entry.getKey(), req.getParameter(entry.getKey()));
//            fullUrl = fullUrl.replace("{" + entry.getKey() + "}", req.getParameter(entry.getKey()));
        }
        return run(code,appId,map, body);
    }

    /**
     * FOR LAMBDA
     **/
    public Object run(String code, Map<String, String> map, Object body, Lambda lambda) throws Exception {
        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,lambda.getApp().getId());
//        ObjectMapper mapper = new ObjectMapper();
//        Entry e = mapper.convertValue(entry, Entry.class);
//        Form form = formRepository.getReferenceById(formId);
        if (endpoint != null) {
//            Map<String,String> map = new HashMap<>();
//            for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
//                map.put(entry.getKey(), req.getParameter(entry.getKey()));
//            }
            return run(code,lambda.getApp().getId(),map, body);
        } else {
            throw new Exception("Endpoint ["+code+"] doesn't exist in App");
        }
    }


    public Object run(String code, Long appId, Map<String, String> map, Object body) throws IOException, InterruptedException, RuntimeException {
        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,appId);
        ObjectMapper mapper = new ObjectMapper();
        Object returnVal = null;

        if (code!=null && endpoint != null) {

            String fullUrl = endpoint.getUrl(); //+dm+param;

            for (Map.Entry<String, String> entry : map.entrySet()) {
                fullUrl = fullUrl.replace("{" + entry.getKey() + "}", entry.getValue());
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            HttpResponse<String> response = null;
            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

//            HttpHeaders headers = new HttpHeaders();
            if (endpoint.getHeaders()!=null && !endpoint.getHeaders().isEmpty()){
                String [] h1 = endpoint.getHeaders().split(Pattern.quote("|"));
                Arrays.stream(h1).forEach(h->{
                    String [] h2 = h.split(Pattern.quote("->"));
//                    headers.set(h2[0],h2.length>1?h2[1]:null);
                    requestBuilder.setHeader(h2[0],h2.length>1?h2[1]:null);
                });
            }

            if (endpoint.isAuth()) {
                String accessToken = null; // accessTokenService.getAccessToken(endpoint.getTokenEndpoint(),endpoint.getClientId(), endpoint.getClientSecret());

                if ("authorization".equals(endpoint.getAuthFlow())){
//                    System.out.println("AUTHFLOW====AUTHORIZATION");
                    UserPrincipal userP = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (userP!=null){
                        User user = userRepository.getReferenceById(userP.getId());
                        accessToken = user.getProviderToken();
                    }
                }else{
//                    System.out.println("AUTHFLOW!!!==AUTHORIZATION");
                    accessToken = accessTokenService.getAccessToken(endpoint.getTokenEndpoint(),endpoint.getClientId(), endpoint.getClientSecret());
                }

//                System.out.println(accessToken);

                if ("url".equals(endpoint.getTokenTo())){
                    // Should have the toggle for token in url vs in header
                    String dm = fullUrl.contains("?") ? "&" : "?";
                    fullUrl = fullUrl + dm + "access_token=" + accessToken;
                }else{
                    requestBuilder.setHeader("Authorization","Bearer "+ accessToken);
                }
            }

            if ("GET".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .GET()
                        .uri(URI.create(fullUrl))
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } else if ("POST".equals(endpoint.getMethod())) {
                HttpRequest request = requestBuilder
                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                        .uri(URI.create(fullUrl))
                        .build();

                response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            }
//            System.out.println("STATUS######:::"+response.statusCode());
            if (endpoint.isAuth() && response.statusCode()!=200){
//                System.out.println("dlm response Status Not 200");
                clearTokens(endpoint.getClientId()+":"+endpoint.getClientSecret());
//                System.out.println("throw exception");
                throw new RuntimeException("Http request error from [ "+ endpoint.getUrl() + "]:" + response.body());
            }

            returnVal = mapper.readTree(response.body());
        }

        return returnVal;
    }

//    Map<String,AccessToken> accessToken = new HashMap<>();

//    @Cacheable(value = "access_token")

    public void clearTokens(String pair){
        accessTokenService.clearAccessToken(pair);
    }

    /**
     * THE PROBLEM IF MULTIPLE ACCESS_TOKEN CRETAED AT THE SAME TIME WILL BE DUPLICATE TOKEN ERROR FROM OAUTH SERVER
     */
//    public String getAccessToken(String tokenEndpoint, String clientId, String clientSecret){
//        String pair = clientId+":"+clientSecret;
//        AccessToken t = this.accessToken.get(pair);
//        // if expiry in 30 sec, request for new token
//        if (t!=null && ((System.currentTimeMillis()/1000)+30)<t.getExpiry_time()){
////            AccessToken t = accessToken.get(pair);
//            System.out.println("AccessToken exist! expiry_time:"+t.getExpiry_time()+", current:"+System.currentTimeMillis()/1000 + ", expires_in:"+ t.getExpires_in());
//            System.out.println("< ACCESS TOKEN EXIST ==============");
//            System.out.println("current_time:"+(System.currentTimeMillis()/1000));
//            System.out.println("access_token:"+t.getAccess_token());
//            System.out.println("expiry_time:"+t.getExpiry_time());
//            System.out.println("expires_in:"+t.getExpires_in());
//            System.out.println("token_type:"+t.getToken_type());
//            System.out.println("scope:"+t.getScope());
//            System.out.println("=====================>");
//            return t.getAccess_token();
//        }else{
////            System.out.println("tokenEndpoint:"+tokenEndpoint+",clientId="+clientId+",clientSecret="+clientSecret);
//            RestTemplate tokenRt = new RestTemplate();
//            HttpHeaders headers = new HttpHeaders();
//            String basic = Base64Utils.encodeToString(pair.getBytes());
//            headers.set("Authorization", "Basic " + basic);
//            HttpEntity<String> entity = new HttpEntity<>(headers);
//
//            ResponseEntity<AccessToken> re = tokenRt.exchange(tokenEndpoint + "?grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret,
//                    HttpMethod.POST, entity, AccessToken.class);
//
//            AccessToken at = re.getBody();
//            System.out.println("< NEW ACCESS TOKEN REQUESTED =================");
//            System.out.println("access_token:"+at.getAccess_token());
//            System.out.println("expiry_time:"+at.getExpiry_time());
//            System.out.println("expires_in:"+at.getExpires_in());
//            System.out.println("token_type:"+at.getToken_type());
//            System.out.println("scope:"+at.getScope());
//            System.out.println("=====================>");
//            at.setExpiry_time((System.currentTimeMillis()/1000)+at.getExpires_in());
//            this.accessToken.put(pair, at);
//
//            return  at.getAccess_token();
//        }
//
//    }
//    public Page<Endpoint> findRegListByAppId(Long appId, Pageable pageable) {
//        return this.restRepository.findRegListByAppId(appId, pageable);
//    }

////    @Autowired
//    WebClient client;
//
//    public Mono<Object> getSecureEndpoint(String code, Long appId, MultiValueMap<String, String> queryMap, HttpServletRequest req) {
//        Endpoint endpoint = endpointRepository.findFirstByCodeAndApp_Id(code,appId);
//
//        String fullUrl = endpoint.getUrl(); //+dm+param;
//
//        for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
//            fullUrl = fullUrl.replace("{" + entry.getKey() + "}", req.getParameter(entry.getKey()));
//        }
//
//        final String finalUrl = fullUrl;
//
//
//        String encodedClientData = Base64Utils.encodeToString((endpoint.getClientId() + ":" + endpoint.getClientSecret()).getBytes());
//
//        WebClient client = WebClient.builder().build();
//
//        if (endpoint.isAuth()){
//
//            Mono<Object> resource = client.post()
//                    .uri(endpoint.getTokenEndpoint())
//                    .header("Authorization", "Basic " + encodedClientData)
//                    .body(BodyInserters.fromFormData("grant_type", "client_credentials"))
//                    .retrieve()
//                    .bodyToMono(JsonNode.class)
//                    .flatMap(tokenResponse -> {
//                        String accessTokenValue = tokenResponse.get("access_token")
//                                .textValue();
//                        if ("GET".equals(endpoint.getMethod())){
//                            return client.get()
//                                    .uri(finalUrl)
//                                    .headers(h -> h.setBearerAuth(accessTokenValue))
//                                    .retrieve()
//                                    .bodyToMono(Object.class);
//                        }else{
//                            return client.post()
//                                    .uri(finalUrl)
//                                    .body(BodyInserters.fromFormData(queryMap))
//                                    .headers(h -> h.setBearerAuth(accessTokenValue))
//                                    .retrieve()
//                                    .bodyToMono(Object.class);
//                        }
//
//                    });
//            return resource.map(res -> res);
//
//        }else{
//            if ("GET".equals(endpoint.getMethod())){
//                return client.get()
//                        .uri(finalUrl)
//                        .retrieve()
//                        .bodyToMono(Object.class);
//            }else{
//                return client.post()
//                        .uri(finalUrl)
//                        .body(BodyInserters.fromFormData(queryMap))
//                        .retrieve()
//                        .bodyToMono(Object.class);
//            }
//        }
//    }
//
//


}
