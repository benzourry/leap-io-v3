package com.benzourry.leap.service.storage;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.repository.SecretRepository;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

@Service
public class S3StorageProvider implements StorageProvider {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageProvider.class);

    private final SecretRepository secretRepository;

    public S3StorageProvider(SecretRepository secretRepository) {
        this.secretRepository = secretRepository;
    }

    @Override
    public String getType() {
        return "s3";
    }

    private S3Client getS3Client(Bucket bucket) {
        JsonNode config = bucket.getX();
        String accessKey = config != null && config.has("accessKey") ? config.get("accessKey").asText() : "";
        String secretKey = config != null && config.has("secretKey") ? config.get("secretKey").asText() : "";
        String regionStr = config != null && config.has("region") ? config.get("region").asText("us-east-1") : "us-east-1";
        String endpoint = config != null && config.has("endpoint") ? config.get("endpoint").asText() : null;

        // --- RESOLVE SECRET ---
        if (secretKey != null && secretKey.contains("{{_secret.")) {
            String key = Helper.extractTemplateKey(secretKey, "{{_secret.", "}}").orElseThrow(() -> {
                TenantLogger.error(bucket.getAppId(), "bucket", bucket.getId(), "Cannot extract secret key from S3 secret key");
                return new RuntimeException("Cannot extract secret key from template");
            });

            secretKey = secretRepository.findByKeyAndAppId(key, bucket.getAppId())
                    .orElseThrow(() -> {
                        TenantLogger.error(bucket.getAppId(), "bucket", bucket.getId(), "Secret [" + key + "] not found");
                        return new ResourceNotFoundException("Secret", "key+appId", key + "+" + bucket.getAppId());
                    })
                    .getValue();
        }
        // ----------------------

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(regionStr))
                .credentialsProvider(StaticCredentialsProvider.create(credentials));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }

    private String getBucketName(Bucket bucket) {
        JsonNode config = bucket.getX();
        return config != null && config.has("bucketName") ? config.get("bucketName").asText() : "";
    }

    private String resolveKey(Bucket bucket, String destStr, String filePathOrUrl) {
        if (filePathOrUrl == null || filePathOrUrl.isBlank()) {
            return "";
        }

        JsonNode config = bucket.getX();
        String prefix = config != null && config.has("prefix") ? config.get("prefix").asText("") : "";

        String cleanPath = filePathOrUrl.startsWith("/") ? filePathOrUrl.substring(1) : filePathOrUrl;

        // If a prefix is configured and the path doesn't already start with it, prepend it
        if (!prefix.isBlank()) {
            String cleanPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
            if (cleanPrefix.startsWith("/")) {
                cleanPrefix = cleanPrefix.substring(1);
            }
            if (cleanPath.startsWith(cleanPrefix)) {
                return cleanPath;
            }

            StringBuilder sb = new StringBuilder(cleanPrefix);
            if (destStr != null && !destStr.isBlank()) {
                String cleanDest = destStr.startsWith("/") ? destStr.substring(1) : destStr;
                if (!cleanDest.endsWith("/")) cleanDest += "/";
                if (!cleanPath.startsWith(cleanDest)) {
                    sb.append(cleanDest);
                }
            }
            sb.append(cleanPath);
            return sb.toString();
        } else {
            if (destStr != null && !destStr.isBlank()) {
                String cleanDest = destStr.startsWith("/") ? destStr.substring(1) : destStr;
                if (!cleanDest.endsWith("/")) cleanDest += "/";
                if (!cleanPath.startsWith(cleanDest)) {
                    return cleanDest + cleanPath;
                }
            }
            return cleanPath;
        }
    }

    @Override
    public String upload(MultipartFile file, File localZipFallback, String destStr, String filePath, Bucket bucket) throws Exception {
        try (S3Client s3 = getS3Client(bucket)) {
            String targetFileName = (localZipFallback != null && localZipFallback.exists()) ? filePath + ".zip" : filePath;
            String s3Key = resolveKey(bucket, destStr, targetFileName);
            String bucketName = getBucketName(bucket);

            if (localZipFallback != null && localZipFallback.exists()) {
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType("application/zip")
                        .build();

                s3.putObject(putReq, RequestBody.fromFile(localZipFallback));
            } else {
                PutObjectRequest putReq = PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Key)
                        .contentType(file.getContentType())
                        .build();

                s3.putObject(putReq, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }

            return s3Key;
        }
    }

    @Override
    public InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception {
        S3Client s3 = getS3Client(bucket);
        String s3Key = resolveKey(bucket, destStr, fileUrl);

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(getBucketName(bucket))
                .key(s3Key)
                .build();

        ResponseInputStream<GetObjectResponse> s3Stream = s3.getObject(getReq);

        // Closes both the S3 response stream and the S3Client upon completion
        return new FilterInputStream(s3Stream) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    s3.close();
                }
            }
        };
    }

    @Override
    public boolean exists(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try (S3Client s3 = getS3Client(bucket)) {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(getBucketName(bucket))
                    .key(resolveKey(bucket, destStr, fileUrl))
                    .build();
            s3.headObject(req);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    @Override
    public long getLastModified(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try (S3Client s3 = getS3Client(bucket)) {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(getBucketName(bucket))
                    .key(resolveKey(bucket, destStr, fileUrl))
                    .build();
            HeadObjectResponse res = s3.headObject(req);
            if (res.lastModified() != null) {
                return res.lastModified().toEpochMilli();
            }
        } catch (Exception e) {
            // Ignored, returns 0L fallback
        }
        return 0L;
    }

    @Override
    public long getFileSize(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try (S3Client s3 = getS3Client(bucket)) {
            HeadObjectRequest req = HeadObjectRequest.builder()
                    .bucket(getBucketName(bucket))
                    .key(resolveKey(bucket, destStr, fileUrl))
                    .build();
            HeadObjectResponse res = s3.headObject(req);
            if (res.contentLength() != null) {
                return res.contentLength();
            }
        } catch (Exception e) {
            // Ignored, returns 0L fallback
        }
        return 0L;
    }

    @Override
    public boolean delete(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try (S3Client s3 = getS3Client(bucket)) {
            DeleteObjectRequest delReq = DeleteObjectRequest.builder()
                    .bucket(getBucketName(bucket))
                    .key(resolveKey(bucket, destStr, fileUrl))
                    .build();
            s3.deleteObject(delReq);
            return true;
        } catch (NoSuchKeyException e) {
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                return true;
            }
            logger.error("Amazon S3 delete failed", e);
            return false;
        }
    }
}