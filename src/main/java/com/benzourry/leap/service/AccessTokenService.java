package com.benzourry.leap.service;

import com.benzourry.leap.model.AccessToken;
import com.benzourry.leap.repository.AccessTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class AccessTokenService {

    private static final Logger logger = LoggerFactory.getLogger(AccessTokenService.class);
    private final AccessTokenRepository accessTokenRepository;

    private final ObjectMapper MAPPER;

    private final HttpClient HTTP_CLIENT;

    public AccessTokenService(AccessTokenRepository accessTokenRepository,
                             HttpClient HTTP_CLIENT, ObjectMapper MAPPER) {
        this.MAPPER = MAPPER;
        this.HTTP_CLIENT = HTTP_CLIENT;
        this.accessTokenRepository = accessTokenRepository;
    }

    public List<AccessToken> getAllAccessToken(){
        return this.accessTokenRepository.findAll();
    }

    public void clearAccessToken(String pair){
        try {
            this.accessTokenRepository.deleteById(pair);
        }catch(Exception e){
            logger.error("Error clear access token:" + e.getMessage());
        }
    }

    /**
     * THE PROBLEM IF MULTIPLE ACCESS_TOKEN CREATED AT THE SAME TIME WILL BE DUPLICATE TOKEN ERROR FROM OAUTH SERVER
     */
    @Transactional
    public String getAccessToken(String tokenEndpoint, String clientId, String clientSecret){
        String pair = clientId+":"+clientSecret;
        Optional<AccessToken> t = this.accessTokenRepository.findById(pair);

        // if expiry in 3 sec, always request for new token ! No need to request before 30sec. Just request when expired
        if (t.isPresent() && ((System.currentTimeMillis()/1000)+3)<t.get().getExpiry_time()){
            return t.get().getAccess_token();
        }

        try {
            String encodedAuth = Base64.getEncoder()
                    .encodeToString(pair.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenEndpoint))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get token: HTTP " + response.statusCode());
            }

            AccessToken at = MAPPER.readValue(response.body(), AccessToken.class);
            at.setExpiry_time((System.currentTimeMillis() / 1000) + at.getExpires_in());
            at.setPair(pair);

            accessTokenRepository.save(at);

            return at.getAccess_token();

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Request interrupted", e);
            }
            throw new RuntimeException("Failed to get access token", e);
        }

    }

}
