package com.benzourry.leap.service.storage;

import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.utility.TenantLogger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class LocalStorageProvider implements StorageProvider {

    @Override
    public String getType() {
        return "local";
    }

    @Override
    public String upload(MultipartFile file, File localZipFallback, String destStr, String filePath, Bucket bucket) throws Exception {
        if (localZipFallback != null && localZipFallback.exists()) {
            return filePath + ".zip";
        }

        File dest = new File(destStr + filePath);
        try {
            file.transferTo(dest);
            return filePath;
        } catch (IllegalStateException | IOException e) {
            Long safeAppId = bucket != null ? bucket.getAppId() : null;
            Long safeBucketId = bucket != null ? bucket.getId() : null;
            TenantLogger.error(safeAppId, "bucket", safeBucketId, "Local storage upload failed for file [" + filePath + "]: " + e.getMessage());

            throw e;
        }
    }

    @Override
    public InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            return new FileInputStream(new File(destStr + fileUrl));
        } catch (Exception e) {
            Long safeAppId = bucket != null ? bucket.getAppId() : null;
            Long safeBucketId = bucket != null ? bucket.getId() : null;
            TenantLogger.error(safeAppId, "bucket", safeBucketId, "Local storage download failed for file [" + fileUrl + "]: " + e.getMessage());

            throw e;
        }
    }

    @Override
    public boolean exists(String fileUrl, String destStr, Bucket bucket) throws Exception {
        return new File(destStr + fileUrl).isFile();
    }

    @Override
    public long getLastModified(String fileUrl, String destStr, Bucket bucket) throws Exception {
        return new File(destStr + fileUrl).lastModified();
    }

    @Override
    public long getFileSize(String fileUrl, String destStr, Bucket bucket) throws Exception {
        return new File(destStr + fileUrl).length();
    }

    @Override
    public boolean delete(String fileUrl, String destStr, Bucket bucket) throws Exception {
        try {
            File dest = new File(destStr + fileUrl);
            if (dest.exists()) {
                boolean isDeleted = dest.delete();

                // File.delete() returns false if it fails (e.g., due to OS permissions or file locks)
                if (!isDeleted) {
                    Long safeAppId = bucket != null ? bucket.getAppId() : null;
                    Long safeBucketId = bucket != null ? bucket.getId() : null;
                    TenantLogger.error(safeAppId, "bucket", safeBucketId, "Local storage delete failed for file [" + fileUrl + "]. The OS denied the deletion (check folder permissions/file locks).");
                }

                return isDeleted;
            }
            return true; // Return true if it's already gone
        } catch (Exception e) {
            Long safeAppId = bucket != null ? bucket.getAppId() : null;
            Long safeBucketId = bucket != null ? bucket.getId() : null;
            TenantLogger.error(safeAppId, "bucket", safeBucketId, "Local storage delete encountered an error for file [" + fileUrl + "]: " + e.getMessage());

            return false;
        }
    }
}