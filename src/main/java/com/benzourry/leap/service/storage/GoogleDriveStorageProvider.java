package com.benzourry.leap.service.storage;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.repository.SecretRepository;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;

@Service
public class GoogleDriveStorageProvider implements StorageProvider {

    private static final String APPLICATION_NAME = "Leap-App-Drive-Integration";

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(GoogleDriveStorageProvider.class);

    private final SecretRepository secretRepository;

    public GoogleDriveStorageProvider(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    @Override
    public String getType() {
        return "gdrive";
    }

    private Drive getDriveService(Bucket bucket) throws Exception {
        JsonNode config = bucket.getX();
        String clientId = config.has("clientId") ? config.get("clientId").asText() : null;
        String clientSecret = config.has("clientSecret") ? config.get("clientSecret").asText() : null;
        String refreshToken = config.has("refreshToken") ? config.get("refreshToken").asText() : null;

        if (clientId == null || clientSecret == null || refreshToken == null) {
            TenantLogger.error(bucket != null ? bucket.getAppId() : null, "bucket", bucket != null ? bucket.getId() : null, "Google Drive OAuth configuration is missing for this bucket.");
            throw new IllegalArgumentException("Google Drive OAuth configuration is missing.");
        }

        // --- RESOLVE SECRET ---
        if (clientSecret.contains("{{_secret.")) {
            String key = Helper.extractTemplateKey(clientSecret, "{{_secret.", "}}").orElseThrow(() -> {
                TenantLogger.error(bucket.getAppId(), "bucket", bucket.getId(), "Cannot extract secret key from GDrive client secret");
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

        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(refreshToken)
                    .build();

            return new Drive.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            TenantLogger.error(bucket != null ? bucket.getAppId() : null, "bucket", bucket != null ? bucket.getId() : null, "Failed to authenticate with Google Drive: " + e.getMessage());
            throw e;
        }
    }

    @Override
    public String upload(MultipartFile file, java.io.File localZipFallback, String destStr, String filePath, Bucket bucket) throws Exception {
        JsonNode config = bucket.getX();
        String folderId = config.has("folderId") ? config.get("folderId").asText() : null;

        File fileMetadata = new File();
        String targetFileName = (localZipFallback != null && localZipFallback.exists()) ? filePath + ".zip" : filePath;
        fileMetadata.setName(targetFileName);

        if (folderId != null && !folderId.isBlank()) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }

        InputStreamContent mediaContent;
        if (localZipFallback != null && localZipFallback.exists()) {
            mediaContent = new InputStreamContent("application/zip", new FileInputStream(localZipFallback));
            mediaContent.setLength(localZipFallback.length());
        } else {
            mediaContent = new InputStreamContent(file.getContentType(), file.getInputStream());
            mediaContent.setLength(file.getSize());
        }

        try {
            File uploadedFile = getDriveService(bucket).files().create(fileMetadata, mediaContent)
                    .setFields("id, webViewLink")
                    .execute();

            return uploadedFile.getId();
        } catch (Exception e) {
            Long safeAppId = bucket != null ? bucket.getAppId() : null;
            Long safeBucketId = bucket != null ? bucket.getId() : null;
            TenantLogger.error(safeAppId, "bucket", safeBucketId, "Google Drive upload failed for file [" + targetFileName + "]: " + e.getMessage());

            throw e;
        }
    }

    @Override
    public InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            return getDriveService(bucket).files().get(fileUrl).executeMediaAsInputStream();
        } catch (Exception e) {
            Long safeAppId = bucket != null ? bucket.getAppId() : null;
            Long safeBucketId = bucket != null ? bucket.getId() : null;
            TenantLogger.error(safeAppId, "bucket", safeBucketId, "Google Drive download failed for file ID [" + fileUrl + "]: " + e.getMessage());

            throw e;
        }
    }

    @Override
    public boolean exists(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            getDriveService(bucket).files().get(fileUrl).setFields("id").execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getLastModified(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            File file = getDriveService(bucket).files().get(fileUrl).setFields("modifiedTime").execute();
            if (file.getModifiedTime() != null) {
                return file.getModifiedTime().getValue();
            }
        } catch (Exception e) {}
        return 0L;
    }

    @Override
    public long getFileSize(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            File file = getDriveService(bucket).files().get(fileUrl).setFields("size").execute();
            if (file.getSize() != null) {
                return file.getSize();
            }
        } catch (Exception e) {}
        return 0L;
    }

    @Override
    public boolean delete(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            getDriveService(bucket).files().delete(fileUrl).execute();
            return true;
        } catch (Exception e) {
            // If it returns a 404, the file is already deleted, which is a successful end state
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return true;
            }
            logger.error("Google Drive delete failed", e);

            Long safeAppId = bucket != null ? bucket.getAppId() : null;
            Long safeBucketId = bucket != null ? bucket.getId() : null;
            TenantLogger.error(safeAppId, "bucket", safeBucketId, "Google Drive delete failed for file ID [" + fileUrl + "]: " + e.getMessage());

            return false;
        }
    }
}