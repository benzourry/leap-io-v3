package com.benzourry.leap.utility;

import com.benzourry.leap.config.ClamAVClientFactory;
import fi.solita.clamav.ClamAVClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class ClamAVServiceUtil {

    private static final Logger logger = LoggerFactory.getLogger(ClamAVServiceUtil.class);

    private final ClamAVClientFactory clamAVClientFactory;

    public ClamAVServiceUtil(ClamAVClientFactory clamAVClientFactory) {
        this.clamAVClientFactory = clamAVClientFactory;
    }

    /**
     * @return Clamd status.
     */
    public boolean ping() throws IOException {
        ClamAVClient client = clamAVClientFactory.newClient();
        boolean isResponding = client.ping();
        logger.info("Clamd responding? :: {}", isResponding);
        return isResponding;
    }

    /**
     * @return Clamd scan result
     */
    public boolean handleFileUpload(String name, MultipartFile file) throws IOException {
        if (!clamAVClientFactory.isEnable()) {
            return true;
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file provided for: " + name);
        }

        // try-with-resources ensures the InputStream is properly closed
        try (InputStream is = file.getInputStream()) {
            return doScan(name, is);
        }
    }

    /**
     * @return Clamd scan result
     */
    public boolean handleFile(String name, String filePath) throws IOException {
        if (!clamAVClientFactory.isEnable()) {
            return true;
        }

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("File does not exist: " + filePath);
        }

        // Files.newInputStream wrapped in try-with-resources fixes the memory/handle leak
        try (InputStream is = Files.newInputStream(path)) {
            return doScan(name, is);
        }
    }

    /**
     * @return Clamd scan reply
     */
    public String handleFileUploadReply(String name, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty file provided for: " + name);
        }

        try (InputStream is = file.getInputStream()) {
            ClamAVClient client = clamAVClientFactory.newClient();
            byte[] byteFile = client.scan(is);

            // Explicitly defining charset is safer than relying on JVM default
            String reply = new String(byteFile, StandardCharsets.UTF_8);
            logger.info("Scan reply for [{}] :: {}", name, reply);
            return reply;
        }
    }

    /**
     * @return Clamd scan result helper method.
     * @throws Exception
     */
    public Boolean scanFileAttachment(String name, MultipartFile file) throws Exception {
        if (!clamAVClientFactory.isEnable()) {
            return true;
        }

        if (ping()) {
            return handleFileUpload(name, file);
        } else {
            // Throwing an IOException is more standard here than a generic Exception
            throw new IOException("Error connecting to Clam AV.. Please try again");
        }
    }

    public Boolean scanFile(String name, String filePath) throws Exception {
        if (!clamAVClientFactory.isEnable()) {
            return true;
        }

        if (ping()) {
            return handleFile(name, filePath);
        } else {
            throw new IOException("Error connecting to Clam AV.. Please try again");
        }
    }

    public boolean isEnabled() {
        return clamAVClientFactory.isEnable();
    }

    // --- Private Helper Methods ---

    /**
     * DRY helper method to handle the actual client generation and scanning logic
     */
    private boolean doScan(String name, InputStream inputStream) throws IOException {
        ClamAVClient client = clamAVClientFactory.newClient();
        byte[] byteFile = client.scan(inputStream);
        boolean isOk = ClamAVClient.isCleanReply(byteFile);

        logger.info("Scan result for file [{}] - is clean? :: {}", name, isOk);
        return isOk;
    }
}