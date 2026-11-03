package com.benzourry.leap.service.storage;

import com.benzourry.leap.model.Bucket;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;

public interface StorageProvider {
    String getType();

    String upload(MultipartFile file, File localZipFallback, String destStr, String filePath, Bucket bucket) throws Exception;

    // --- NEW RETRIEVAL METHODS ---
    InputStream download(String fileUrl, String destStr, Bucket bucket) throws Exception;
    boolean exists(String fileUrl, String destStr, Bucket bucket) throws Exception;
    long getLastModified(String fileUrl, String destStr, Bucket bucket) throws Exception;
    long getFileSize(String fileUrl, String destStr, Bucket bucket) throws Exception;

    boolean delete(String fileUrl, String destStr, Bucket bucket) throws Exception;
}