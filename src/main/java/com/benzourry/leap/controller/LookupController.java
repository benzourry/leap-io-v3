package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.mixin.LookupMixin;
import com.benzourry.leap.model.Lookup;
import com.benzourry.leap.model.LookupEntry;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.LookupService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"api/lookup", "api/public/lookup"})
//@CrossOrigin(allowCredentials="true")
public class LookupController {

    final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupOne.class)
    })
    public Lookup getLookup(@PathVariable("id") long id) {
        return lookupService.getLookup(id);
    }

    @PostMapping
    public Lookup save(@RequestBody Lookup lookup,
                       @RequestParam("appId") Long appId,
                       @RequestParam("email") String email) {
        return lookupService.save(lookup, appId, email);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupBasicList.class)
    })
    public Page<Lookup> findByAppId(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                    @RequestParam(value = "appId", required = false) Long appId, Pageable pageable) {
        return lookupService.findByAppId(searchText, appId, pageable);
    }

    @GetMapping("all")
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupBasicList.class)
    })
    public Page<Lookup> findAllLookup(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                      @RequestParam(value = "email", required = false) String email,
                                      Pageable pageable) {
        return lookupService.findByQuery(searchText, email, pageable);
    }

    @GetMapping("full")
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupBasicList.class)
    })
    public Page<Lookup> findAllFullLookup(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                          @RequestParam(value = "appId", required = false) Long appId) {
        return lookupService.findByAppId(searchText, appId, PageRequest.of(0, Integer.MAX_VALUE));
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> delete(@PathVariable("id") long id) {
        Map<String, Object> data = new HashMap<>();
        lookupService.removeLookup(id);
        return data;
    }

    @PostMapping("{id}/clear-entries")
    public Map<String, Object> clearEntries(@PathVariable("id") long id) {
        Map<String, Object> data = new HashMap<>();
        lookupService.clearEntries(id);
        return data;
    }

    @PostMapping("{id}/entry")
    public LookupEntry save(@PathVariable("id") long id,
                            @RequestBody LookupEntry lookup) {
        return lookupService.save(id, lookup);
    }


    @PostMapping("entry/field")
    public LookupEntry updateEntry(@RequestParam("entryId") long id,
                                   @RequestBody JsonNode lookup) {
        return lookupService.updateLookupEntry(id, lookup);
    }

    @PostMapping("entry/{id}/delete")
    public Map<String, Object> deleteEntry(@PathVariable("id") long id) {
        Map<String, Object> data = new HashMap<>();
        lookupService.removeLookupEntry(id);
        return data;
    }

    @GetMapping("{id}/entry")
    @JsonResponse(mixins = {
            @JsonMixin(target = LookupEntry.class, mixin = LookupMixin.LookupEntryList.class)
    })
//    @Cacheable(value = "lookupEntry", key = "{#id,}")
    public ResponseEntity<Map<String, Object>> findAllEntry(@PathVariable("id") long id,
                                                            @RequestParam(value = "searchText", required = false) String searchText,
                                                            HttpServletRequest request, Pageable pageable) {
        try {
            return ResponseEntity
                    .ok(lookupService.findAllEntry(id, searchText, request, true, pageable));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(Map.of("error", "Problem loading lookup entry", "message", e.getMessage()));
        }
    }

    @GetMapping("{id}/entry-full")
    @JsonResponse(mixins = {
            @JsonMixin(target = LookupEntry.class, mixin = LookupMixin.LookupEntryListFull.class)
    })
    public ResponseEntity<Map<String, Object>> findAllEntryFull(@PathVariable("id") long id,
                                                                @RequestParam(value = "searchText", required = false) String searchText,
                                                                HttpServletRequest request, Pageable pageable) {
        try {
            return ResponseEntity
                    .ok(lookupService.findAllEntry(id, searchText, request, false, pageable));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(Map.of("error", "Problem loading lookup entry", "message", e.getMessage()));
        }
    }

    @PostMapping(value = "{id}/upload-file")
    public Map<String, Object> uploadFile(@RequestParam("file") MultipartFile file,
                                      @PathVariable("id") Long lookupId,
                                      @CurrentUser UserPrincipal principal,
                                      HttpServletRequest request) throws Exception {

        Map<String, Object> data = new HashMap<>();

        String username = principal.getName();
        Long userId = principal.getId();

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", ""), StandardCharsets.UTF_8);


//        String random = Long.toString(UUID.randomUUID().getLessSignificantBits(), Character.MAX_RADIX);
        String filePath = "lookup-" + lookupId + "/" +userId + "_" + originalFilename;
//        String filePath = Instant.now().getEpochSecond() + "_" + originalFilename;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";


        // only to make folder
        File dir = new File(destStr + "lookup-" + lookupId + "/");
        dir.mkdirs();


        File dest = new File(destStr + filePath);

        data.put("fileName", originalFilename);
        data.put("fileSize", fileSize);
        data.put("fileType", contentType);
        data.put("fileUrl", filePath.replaceAll("/","~"));
        data.put("email", principal.getEmail());
        data.put("timestamp", new Date());
        data.put("message", "success");
        data.put("success", true);

        try {
            file.transferTo(dest);
        } catch (IllegalStateException e) {
            data.put("message", "failed");
            data.put("success", false);
        }

        return data;
    }


    @GetMapping("in-form/{formId}")
    public List<Map> findLookupInForm(@PathVariable("formId") Long formId,
                                      @RequestParam(name = "sectionType", defaultValue = "section,list,approval") List<String> sectionType) {
        return lookupService.findIdByFormIdAndSectionType(formId, sectionType);
    }


    @PostMapping("save-order")
    public List<Map<String, Long>> saveOrder(@RequestBody List<Map<String, Long>> lookupOrderList) {
        return lookupService.saveOrder(lookupOrderList);
    }


    @GetMapping("update-data")
    public Map<String, Object> updateLookupData(@RequestParam("lookupId") Long lookupId,
                                 @RequestParam("refCol") String refCol) throws IOException, InterruptedException {

        this.lookupService.bulkResyncEntryData_lookup(lookupId, refCol);

        return Map.of("success", true);
    }
}
