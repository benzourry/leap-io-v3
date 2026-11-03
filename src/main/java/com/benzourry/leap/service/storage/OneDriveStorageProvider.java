package com.benzourry.leap.service.storage;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.repository.SecretRepository;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Service
public class OneDriveStorageProvider implements StorageProvider {

    private final ObjectMapper MAPPER = new ObjectMapper();
    private final SecretRepository secretRepository;

    public OneDriveStorageProvider(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    @Override
    public String getType() {
        return "onedrive";
    }

    private String getAccessToken(Bucket bucket) throws Exception {
        JsonNode config = bucket.getX();
        String clientId = config.has("clientId") ? config.get("clientId").asText() : "";
        String clientSecret = config.has("clientSecret") ? config.get("clientSecret").asText() : "";
        String refreshToken = config.has("refreshToken") ? config.get("refreshToken").asText() : "";

        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            TenantLogger.error(bucket.getAppId(), "bucket", bucket.getId(), "OneDrive OAuth configuration is missing.");
            throw new IllegalArgumentException("OneDrive OAuth configuration is missing.");
        }

        // --- RESOLVE SECRET ---
        if (clientSecret.contains("{{_secret.")) {
            String key = Helper.extractTemplateKey(clientSecret, "{{_secret.", "}}").orElseThrow(() -> {
                TenantLogger.error(bucket.getAppId(), "bucket", bucket.getId(), "Cannot extract secret key from OneDrive client secret");
                return new RuntimeException("Cannot extract secret key from template");
            });

            clientSecret = secretRepository.findByKeyAndAppId(key, bucket.getAppId())
                    .orElseThrow(() -> {
                        TenantLogger.error(bucket.getAppId(), "bucket", bucket.getId(), "Secret [" + key + "] not found");
                        return new ResourceNotFoundException("Secret", "key+appId", key + "+" + bucket.getAppId());
                    })
                    .getValue();
        }
        // ----------------------

        String tokenEndpoint = "https://login.microsoftonline.com/common/oauth2/v2.0/token";
        String urlParameters = "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") +
                "&refresh_token=" + URLEncoder.encode(refreshToken, "UTF-8") +
                "&grant_type=refresh_token";

        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(tokenEndpoint).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", Integer.toString(postData.length));

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.write(postData);
        }

        try (InputStream is = conn.getInputStream()) {
            JsonNode json = MAPPER.readTree(is);
            return json.get("access_token").asText();
        }
    }

    @Override
    public String upload(MultipartFile file, File localZipFallback, String destStr, String filePath, Bucket bucket) throws Exception {
        String accessToken = getAccessToken(bucket);
        JsonNode config = bucket.getX();
        String folderId = config.has("folderId") ? config.get("folderId").asText() : null;

        String targetFileName = (localZipFallback != null && localZipFallback.exists()) ? filePath + ".zip" : filePath;
        String encodedFileName = URLEncoder.encode(targetFileName, "UTF-8").replace("+", "%20");

        String uploadUrl = "https://graph.microsoft.com/v1.0/me/drive/";
        if (folderId != null && !folderId.isBlank()) {
            uploadUrl += "items/" + folderId + ":/" + encodedFileName + ":/content";
        } else {
            uploadUrl += "root:/" + encodedFileName + ":/content";
        }

        HttpURLConnection conn = (HttpURLConnection) new URL(uploadUrl).openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setRequestProperty("Content-Type", "application/octet-stream");

        try (OutputStream os = conn.getOutputStream();
             InputStream is = (localZipFallback != null && localZipFallback.exists()) ? new FileInputStream(localZipFallback) : file.getInputStream()) {
            is.transferTo(os);
        }

        try (InputStream is = conn.getInputStream()) {
            JsonNode json = MAPPER.readTree(is);
            return json.get("id").asText();
        }
    }

    @Override
    public InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception {
        String downloadUrl = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileUrl + "/content";
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken(bucket));
        conn.setInstanceFollowRedirects(true);
        return conn.getInputStream();
    }

    private JsonNode getMetadata(String fileUrl, Bucket bucket) throws Exception {
        String metadataUrl = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileUrl;
        HttpURLConnection conn = (HttpURLConnection) new URL(metadataUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken(bucket));

        if (conn.getResponseCode() == 200) {
            try (InputStream is = conn.getInputStream()) {
                return MAPPER.readTree(is);
            }
        }
        return null;
    }

    @Override
    public boolean exists(String fileUrl, String destStr, Bucket bucket) throws Exception {
        return getMetadata(fileUrl, bucket) != null;
    }

    @Override
    public long getLastModified(String fileUrl, String destStr, Bucket bucket) throws Exception {
        JsonNode meta = getMetadata(fileUrl, bucket);
        if (meta != null && meta.has("lastModifiedDateTime")) {
            return Instant.parse(meta.get("lastModifiedDateTime").asText()).toEpochMilli();
        }
        return 0L;
    }

    @Override
    public long getFileSize(String fileUrl, String destStr, Bucket bucket) throws Exception {
        JsonNode meta = getMetadata(fileUrl, bucket);
        if (meta != null && meta.has("size")) {
            return meta.get("size").asLong();
        }
        return 0L;
    }

    @Override
    public boolean delete(String fileUrl, String destStr, Bucket bucket) throws Exception {
        String deleteUrl = "https://graph.microsoft.com/v1.0/me/drive/items/" + fileUrl;
        HttpURLConnection conn = (HttpURLConnection) new URL(deleteUrl).openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken(bucket));

        int responseCode = conn.getResponseCode();
        // 204 No Content means successful deletion. 404 means it's already gone.
        return responseCode == 204 || responseCode == 404;
    }
}