package com.benzourry.leap.service.storage;

import com.benzourry.leap.model.Bucket;
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
        } catch (IllegalStateException | IOException e) {
            throw e;
        }
        return filePath;
    }

    @Override
    public InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception {
        return new FileInputStream(new File(destStr + fileUrl));
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
        File dest = new File(destStr + fileUrl);
        if (dest.exists()) {
            return dest.delete();
        }
        return true; // Return true if it's already gone
    }
}