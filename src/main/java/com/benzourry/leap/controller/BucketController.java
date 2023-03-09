package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.mixin.GroupMixin;
import com.benzourry.leap.model.Bucket;
import com.benzourry.leap.model.EntryAttachment;
import com.benzourry.leap.model.Item;
import com.benzourry.leap.service.BucketService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.export.CsvView;
import com.benzourry.leap.utility.export.ExcelViewStream;
import com.benzourry.leap.utility.export.PdfView;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api/bucket")
public class BucketController {

//    @Autowired
    private final BucketService bucketService;

    public BucketController(BucketService bucketService){
        this.bucketService = bucketService;
    }

    @PostMapping
    public Bucket save(@RequestBody Bucket bucket,
                          @RequestParam("appId") Long appId){
        return bucketService.save(bucket, appId);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Bucket.class, mixin = GroupMixin.GroupBasicList.class)
    })
    public Page<Bucket> findByAppId(@RequestParam Long appId, Pageable pageable){
        return bucketService.findByAppId(appId, pageable);
    }

    @GetMapping("{id}")
    public Bucket findById(@PathVariable Long id){
        return bucketService.findById(id);
    }


    @GetMapping("{id}/stat")
    public CompletableFuture<Map<String, Object>> statById(@PathVariable Long id){
        return bucketService.getStat(id);
//                .thenApply(model-> model);
    }

    @GetMapping("{id}/files")
    public Page<EntryAttachment> findFilesByBucketCode(@PathVariable("id") Long id,
                                                       @RequestParam("searchText") String searchText,
                                                       Pageable pageable){
        return bucketService.findFilesByBucketId(id, searchText, pageable);
    }

    @GetMapping("{id}/zip")
    public CompletableFuture<Map<String, Object>> getZipInfo(@PathVariable("id") Long bucketId, HttpServletResponse response, Principal principal) throws IOException {

        return bucketService.getZipBucket(bucketId);
//            .thenApply(model->{
////                File file = new File(model.get("filepath")+"");
////                response.setHeader("Content-disposition", "attachment; filename=" + model.get("filename"));
////                byte[] obj = null;
////
////                try {
////                    obj =  Files.readAllBytes(file.toPath());
////                } catch (IOException e) {
////                    System.out.println(e.getMessage());
////                }
//                return model;
//            });

    }

    @GetMapping("zip-download/{path}")
    public StreamingResponseBody getFileEntity(@PathVariable("path") String path, HttpServletResponse response, Principal principal) throws IOException {

//        FileInfo fileInfo = fileService.findFileInfo(fileId);
//        response.setContentType(ContentType.);
        response.setHeader(
                HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=\"" + path + "\"");

        File file = new File(Constant.UPLOAD_ROOT_DIR+"/tmp/"+path);

        return outputStream -> {
            Files.copy(file.toPath(), outputStream);
        };

    }

    @PostMapping("{id}/reorganize")
    public CompletableFuture<Map<String,Object>> reorganize(@PathVariable("id") Long id){
//        Map<String, Object> data = new HashMap<>();
        return bucketService.reorganize(id);
//        return data;
    }

    @PostMapping("{id}/delete")
    public Map<String,Object> delete(@PathVariable("id") Long id) {
        Map<String, Object> data = new HashMap<>();
        bucketService.delete(id);
        data.put("success", "true");
        return data;
    }

    @PostMapping("delete-file/{id}")
    public Map<String,Object> deleteFile(@PathVariable("id") Long id) {
//        Map<String, Object> data = new HashMap<>();
        return bucketService.deleteFile(id);
//        data.put("success", "true");
//        return data;
    }

//    @GetMapping("reg-list")
//    public List<Bucket> getRegList(@RequestParam("appId") Long appId){
//        return bucketService.findRegListByAppId(appId);
//    }

}
