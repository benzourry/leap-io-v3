package com.benzourry.leap.service.storage;

import com.benzourry.leap.model.Bucket;
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
            throw new IllegalArgumentException("Google Drive OAuth configuration is missing.");
        }

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

        File uploadedFile = getDriveService(bucket).files().create(fileMetadata, mediaContent)
                .setFields("id, webViewLink")
                .execute();

        return uploadedFile.getId();
    }

    @Override
    public InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception {
        return getDriveService(bucket).files().get(fileUrl).executeMediaAsInputStream();
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
            return false;
        }
    }
}