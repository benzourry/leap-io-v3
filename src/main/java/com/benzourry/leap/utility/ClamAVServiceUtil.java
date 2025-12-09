package com.benzourry.leap.utility;

import com.benzourry.leap.config.ClamAVClientFactory;
import fi.solita.clamav.ClamAVClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
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
        ClamAVClient a = clamAVClientFactory.newClient();
        logger.info("Clamd responding? :: " + a.ping());
        return a.ping();
    }

    /**
     * @return Clamd scan result
     */

    public boolean handleFileUpload( String name, MultipartFile file) throws IOException{
        if (!clamAVClientFactory.isEnable()){
            return true;
        }

        if (!file.isEmpty()) {
            ClamAVClient client = clamAVClientFactory.newClient();
            byte[] byteFile = client.scan(file.getInputStream());
            boolean isOk = ClamAVClient.isCleanReply(byteFile);
            logger.info("Everything ok ? :: {}",isOk );
            return isOk ;
        } else throw new IllegalArgumentException("Empty file");
    }

    /**
     * @return Clamd scan result
     */

    public boolean handleFile( String name, String filePath) throws IOException{
        if (!clamAVClientFactory.isEnable()){
            return true;
        }

        if (Files.exists(Paths.get(filePath))) {
            ClamAVClient client = clamAVClientFactory.newClient();
            byte[] byteFile = client.scan(new FileInputStream(filePath));
            boolean isOk = ClamAVClient.isCleanReply(byteFile);
            logger.info("Everything ok ? :: {}",isOk );
            return isOk ;
        } else throw new IllegalArgumentException("Empty file");
    }

    /**
     * @return Clamd scan reply
     */
    public String handleFileUploadReply(String name, MultipartFile file) throws IOException{
        if (!file.isEmpty()) {
            ClamAVClient client = clamAVClientFactory.newClient();
            String reply= new String( client.scan(file.getInputStream()));
            logger.info(reply);
            return reply;
        } else throw new IllegalArgumentException("Empty file");
    }

    /**
     * @return Clamd scan result helper method.
     * @throws Exception
     */
    public Boolean scanFileAttachment(String name, MultipartFile file) throws IOException, Exception{
        if (!clamAVClientFactory.isEnable()){
            return true;
        }

        boolean isFileSafe;
        if (ping()) {
            isFileSafe= handleFileUpload(name,file);
//            logger.info(handleFileUploadReply(name,file));
        }
        else
            throw new Exception("Error connecting to Clam AV.. Please try again");

        return isFileSafe;
    }

   public Boolean scanFile(String name, String filePath) throws IOException, Exception{
       if (!clamAVClientFactory.isEnable()){
           return true;
       }

       boolean isFileSafe;
        if (ping()) {
            isFileSafe= handleFile(name,filePath);
//            logger.info(handleFileUploadReply(name,file));
        }
        else
            throw new Exception("Error connecting to Clam AV.. Please try again");

        return isFileSafe;
    }

    public boolean isEnabled(){
        return clamAVClientFactory.isEnable();
    }

}