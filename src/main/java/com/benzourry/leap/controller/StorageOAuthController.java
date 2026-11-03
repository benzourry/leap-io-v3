package com.benzourry.leap.controller;

import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.repository.BucketRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/bucket/oauth")
public class StorageOAuthController {

    private final BucketRepository bucketRepository;
    private final ObjectMapper MAPPER;

    public StorageOAuthController(BucketRepository bucketRepository, ObjectMapper MAPPER) {
        this.bucketRepository = bucketRepository;
        this.MAPPER = MAPPER;
    }

    @GetMapping("/gdrive-callback")
    public void gdriveCallback(@RequestParam("code") String code,
                               @RequestParam("state") String state,
                               HttpServletRequest request,
                               HttpServletResponse response) throws Exception {

        // 1. Get the bucket ID passed through the 'state' parameter
        Long bucketId = Long.parseLong(state);
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new IllegalArgumentException("Bucket not found"));

        JsonNode xNode = bucket.getX();
        String clientId = xNode.has("clientId") ? xNode.get("clientId").asText() : "";
        String clientSecret = xNode.has("clientSecret") ? xNode.get("clientSecret").asText() : "";

        // 2. The Redirect URI must perfectly match what was requested
        String redirectUri = request.getRequestURL().toString();

        // 3. Exchange the temporary code for a permanent Refresh Token
        GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientId,
                clientSecret,
                code,
                redirectUri)
                .execute();

        // 4. Update the bucket with the new Refresh Token
        if (tokenResponse.getRefreshToken() != null) {
            ObjectNode modifiableNode = xNode.deepCopy();
            modifiableNode.put("refreshToken", tokenResponse.getRefreshToken());
            bucket.setX(modifiableNode);
            bucketRepository.save(bucket);
        }

        // 5. Send an HTML response to tell the Angular parent window it succeeded, then close the popup
        response.setContentType("text/html");
        response.getWriter().write("""
            <html>
            <body>
                <script>
                    if (window.opener) {
                        window.opener.postMessage('gdrive_connected', '*');
                    }
                    window.close();
                </script>
                <p>Authentication successful! You can close this window.</p>
            </body>
            </html>
        """);
    }

    @GetMapping("/onedrive-callback")
    public void onedriveCallback(@RequestParam(value = "code", required = false) String code,
                                 @RequestParam("state") String state,
                                 HttpServletRequest request,
                                 HttpServletResponse response) throws Exception {

        Long bucketId = Long.parseLong(state);
        Bucket bucket = bucketRepository.findById(bucketId)
                .orElseThrow(() -> new IllegalArgumentException("Bucket not found"));

        if (code != null && !code.isBlank()) {
            JsonNode xNode = bucket.getX();
            String clientId = xNode.has("clientId") ? xNode.get("clientId").asText() : "";
            String clientSecret = xNode.has("clientSecret") ? xNode.get("clientSecret").asText() : "";

            String redirectUri = request.getRequestURL().toString();
            String tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
            String urlParameters = "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                    "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") +
                    "&code=" + URLEncoder.encode(code, "UTF-8") +
                    "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                    "&grant_type=authorization_code";

            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            HttpURLConnection conn = (HttpURLConnection) new URL(tokenEndpoint).openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            try (java.io.DataOutputStream wr = new java.io.DataOutputStream(conn.getOutputStream())) {
                wr.write(postData);
            }

            if (conn.getResponseCode() == 200) {
                try (InputStream is = conn.getInputStream()) {
                    JsonNode json = MAPPER.readTree(is);
                    if (json.has("refresh_token")) {
                        com.fasterxml.jackson.databind.node.ObjectNode modifiableNode = xNode.deepCopy();
                        modifiableNode.put("refreshToken", json.get("refresh_token").asText());
                        bucket.setX(modifiableNode);
                        bucketRepository.save(bucket);
                    }
                }
            }
        }

        response.setContentType("text/html");
        response.getWriter().write("""
            <html>
            <body>
                <script>
                    if (window.opener) {
                        window.opener.postMessage('onedrive_connected', '*');
                    }
                    window.close();
                </script>
                <p>Authentication successful! You can close this window.</p>
            </body>
            </html>
        """);
    }
}