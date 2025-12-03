package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.KryptaWallet;
import com.benzourry.leap.model.Signa;
import com.benzourry.leap.service.SignaService;
import com.benzourry.leap.utility.Helper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("api/signa")
public class SignaController {

    private final SignaService signaService;

    public SignaController(SignaService signaService) {
        this.signaService = signaService;
    }

    @GetMapping("{id}")
    public Signa getWalletInfo(@PathVariable Long id) {
        return signaService.get(id);
    }

    @GetMapping
    public Page<Signa> getWalletInfos(@RequestParam Long appId, Pageable pageable) {
        return signaService.getSignaList(appId, pageable);
    }

    @PostMapping
    public Signa saveWalletInfo(@RequestBody Signa walletInfo,
                                       @RequestParam("appId") Long appId,
                                       @RequestParam("email") String email) {
        return signaService.save(appId,walletInfo, email);
    }

    @PostMapping("{id}/delete")
    public ResponseEntity<?> deleteWalletInfo(@PathVariable Long id) {
        signaService.delete(id);
        return ResponseEntity.ok().build();
    }


    @PostMapping(value = "{id}/upload-{type}")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file,
                                          @PathVariable("id") Long signaId,
                                          @PathVariable("type") String type,
                                          HttpServletRequest request) throws Exception {

        if (!"key".equals(type) && !"img".equals(type)) {
            throw new IllegalArgumentException("Invalid upload type: " + type);
        }

        Map<String, Object> data = new HashMap<>();

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", ""), StandardCharsets.UTF_8);


        String random = UUID.randomUUID().toString();
        String filePath = type + "_" + random + "_" + originalFilename;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/signa-" + signaId + "/";


        // only to make folder
        File dir = new File(destStr);
        dir.mkdirs();


        File dest = new File(destStr + filePath);

        data.put("fileName", originalFilename);
        data.put("fileSize", fileSize);
        data.put("fileType", contentType);
        data.put("fileUrl", Constant.IO_BASE_DOMAIN+"/api/signa/"+signaId+"/file/"+filePath);
        data.put("filePath", filePath);
        data.put("timestamp", new Date());
        data.put("message", "success");
        data.put("success", true);

        try {
            file.transferTo(dest);
        } catch (IllegalStateException e) {
            data.put("message", "failed");
            data.put("success", false);
        }

        Signa signa = signaService.get(signaId);
        if (signa != null) {
            if ("key".equals(type)) {
                signa.setKeyPath(filePath);
            } else if ("img".equals(type)) {
                signa.setImagePath(filePath);
            }
            signaService.save(signa.getAppId(), signa, signa.getEmail());
        }

        return data;
    }


    @RequestMapping(value = "{id}/file/{path}")
    public ResponseEntity<StreamingResponseBody> getFileEntity(@PathVariable("id") Long signaId,
                                                               @PathVariable("path") String path,
                                                               HttpServletResponse response, Principal principal) throws IOException {

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/signa-" + signaId + "/";

        if (!Helper.isNullOrEmpty(path)) {

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .filename(path)
                    .build();

            File file = new File(destStr + path);

            if (file.isFile()) {

                String mimeType = Files.probeContentType(file.toPath());

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

                if (!Helper.isNullOrEmpty(mimeType)) {
                    builder.contentType(MediaType.parseMediaType(mimeType));
                }
                return builder.body(outputStream -> Files.copy(file.toPath(), outputStream));
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }




}
