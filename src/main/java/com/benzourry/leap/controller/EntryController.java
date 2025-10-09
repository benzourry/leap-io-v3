package com.benzourry.leap.controller;

//import co.elastic.thumbnails4j.core.Dimensions;
//import co.elastic.thumbnails4j.core.Thumbnailer;
//import co.elastic.thumbnails4j.doc.DOCThumbnailer;
//import co.elastic.thumbnails4j.docx.DOCXThumbnailer;
//import co.elastic.thumbnails4j.image.ImageThumbnailer;
//import co.elastic.thumbnails4j.pdf.PDFThumbnailer;
//import co.elastic.thumbnails4j.pptx.PPTXThumbnailer;
//import co.elastic.thumbnails4j.xls.XLSThumbnailer;
//import co.elastic.thumbnails4j.xlsx.XLSXThumbnailer;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.EntryMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.BucketRepository;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.repository.ItemRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.EntryService;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.utility.ClamAVServiceUtil;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//import org.springframework.security.oauth2.provider.OAuth2Authentication;

@RestController
@RequestMapping({"api/entry", "api/public/entry"})
//@CrossOrigin(allowCredentials = "true")
public class EntryController {

    final EntryService entryService;

    final EntryAttachmentRepository entryAttachmentRepository;

    final ItemRepository itemRepository;

    final BucketRepository bucketRepository;

    final ClamAVServiceUtil clamavService;

    @Autowired
    public EntryController(EntryService entryService,
                           EntryAttachmentRepository entryAttachmentRepository,
                           ClamAVServiceUtil clamavService,
                           ItemRepository itemRepository,
                           BucketRepository bucketRepository) {
        this.entryService = entryService;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.clamavService = clamavService;
        this.itemRepository = itemRepository;
        this.bucketRepository = bucketRepository;
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry findById(@PathVariable("id") long id,
                          HttpServletRequest request, Principal principal) {
        String name = principal == null ? null : principal.getName();
        return entryService.findById(id, name == null, request);
    }

    @GetMapping("{id}/approval-trails")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryApprovalTrail> findTrailById(@PathVariable("id") long id,
                                                  @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
        return entryService.findApprovalTrailById(id, pageable);
    }

    @GetMapping("{id}/files")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryAttachment> findFilesById(@PathVariable("id") long id,
                                               @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
        return entryService.findFilesById(id, pageable);
    }

    @GetMapping({"first-by-params", "by-params"})
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry findFirstByParam(@RequestParam("formId") Long formId,
                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                  HttpServletRequest request, Authentication auth) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(filters, Map.class);
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (formId:" + formId + "):" + e.getMessage());
        }
        String name = auth == null ? null : auth.getName();
        return entryService.findFirstByParam(formId, p, request, name == null);
    }


    @PostMapping
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
//    })
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry save(@RequestParam("formId") long formId,
                      @RequestParam(value = "prevId", required = false) Long prevId,
                      @RequestBody Entry entry,
                      @RequestParam("email") String email) throws Exception {
//        System.out.println("principal:"+principal);
        return entryService.save(formId, entry, prevId, email, true);
    }

    @PostMapping("field")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
//    })
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry saveField(@RequestParam("entryId") long entryId,
                           @RequestBody JsonNode value,
                           @RequestParam(value = "root", required = false) String root,
                           @RequestParam(value = "appId", required = false) Long appId) throws Exception {
        return entryService.updateField(entryId, value, root, appId);
    }


    @PostMapping("{id}/undelete")
    public Map<String, Object> undelete(@PathVariable("id") long id,
                                        @RequestParam("trailId") long trailId,
                                        Authentication authentication) {
        return entryService.undelete(id, trailId, authentication.getName());
    }

    @PostMapping("{id}/undo")
    public Map<String, Object> undo(@PathVariable("id") long id,
                                    @RequestParam("trailId") long trailId,
                                    Authentication authentication) {
        return entryService.undo(id, trailId, authentication.getName());
    }

    @GetMapping("list-data")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
    })
    public List<JsonNode> findUnboxed(@RequestParam("datasetId") Long datasetId,
                                      @RequestParam(value = "searchText", required = false) String searchText,
                                      @RequestParam(value = "email", required = false) String email,
                                      @RequestParam(value = "sorts", required = false) List<String> sorts,
                                      @RequestParam(value = "ids", required = false) List<Long> ids,
                                      @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                      @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                      Pageable pageable,
                                      HttpServletRequest request, Principal principal) {
        ObjectMapper mapper = new ObjectMapper();
        String name = principal == null ? null : principal.getName();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (datasetId:" + datasetId + "):" + e.getMessage());
        }
        return entryService.findListByDatasetData(datasetId, searchText, email, p, cond, sorts, ids, name == null, pageable, request);
    }

    @GetMapping("list")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class),
//            @JsonMixin(target = JsonNode.class, mixin = EntryMixin.JsonNodeF.class)

    })
    public Page<Entry> findAllByDatasetIdCheck(@RequestParam("datasetId") Long datasetId,
                                               @RequestParam(value = "searchText", required = false) String searchText,
                                               @RequestParam(value = "email", required = false) String email,
                                               @RequestParam(value = "sorts", required = false) List<String> sorts,
                                               @RequestParam(value = "ids", required = false) List<Long> ids,
                                               @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                               @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                               Pageable pageable,
                                               HttpServletRequest request, Principal principal) {
        ObjectMapper mapper = new ObjectMapper();
        String name = principal == null ? null : principal.getName();

//        System.out.println(URLDecoder.decode(filters, StandardCharsets.UTF_8));
        Map p = new HashMap();

        try {
            // Masalah double decoding.
            p = mapper.readValue(filters, Map.class);
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (datasetId:" + datasetId + "):" + e.getMessage());
        }
        return entryService.findListByDatasetCheck(datasetId, searchText, email, p, cond, sorts, ids, name == null, pageable, request);
    }

    @Transactional
    @GetMapping("stream")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)

    })
    public Stream<Entry> findAllByDatasetIdCheckStream(@RequestParam("datasetId") Long datasetId,
                                                       @RequestParam(value = "searchText", required = false) String searchText,
                                                       @RequestParam(value = "email", required = false) String email,
                                                       @RequestParam(value = "sorts", required = false) List<String> sorts,
                                                       @RequestParam(value = "ids", required = false) List<Long> ids,
                                                       @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                                       @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                                       Pageable pageable,
                                                       HttpServletRequest request, Principal principal) {
        ObjectMapper mapper = new ObjectMapper();
        String name = principal == null ? null : principal.getName();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (datasetId:" + datasetId + "):" + e.getMessage());
        }
        return entryService.streamListByDatasetCheck(datasetId, searchText, email, p, cond, sorts, ids, name == null, pageable, request);
    }

    @GetMapping("count")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),

    })
    public Map<String, Object> countByDatasetId(@RequestParam("datasetId") Long datasetId,
                                                @RequestParam(value = "searchText", required = false) String searchText,
                                                @RequestParam(value = "email", required = false) String email,
                                                @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                                @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
//                                          @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                Pageable pageable,
                                                HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (datasetId:" + datasetId + "):" + e.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", entryService.countByDataset(datasetId, searchText, email, p, cond, request));
        return data;
    }

    @PostMapping("list-blast")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Map<String, Object> blastEmailByDatasetId(@RequestParam("datasetId") Long datasetId,
                                                     @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
                                                     @RequestParam("email") String email,
                                                     @RequestParam(value = "ids", required = false) List<Long> ids,
                                                     @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                                     @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                                     @RequestBody EmailTemplate emailTemplate,
                                                     @CurrentUser UserPrincipal principal,
                                                     HttpServletRequest request) throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (datasetId:" + datasetId + "):" + e.getMessage());
        }
        if (emailTemplate!=null){
            emailTemplate.setEnabled(1);
        }
        return entryService.blastEmailByDataset(datasetId, searchText, email, p, cond, emailTemplate, ids, request, principal.getEmail(), principal);
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> deleteEntry(@PathVariable("id") Long id,
                                           Authentication authentication) {
        Map<String, Object> data = new HashMap<>();
        String name = authentication.getName();
        entryService.deleteEntry(id, name);
        return data;
    }

    @PostMapping("bulk/delete")
    public Map<String, Object> deleteEntries(@RequestParam("ids") List<Long> ids,
                                             Authentication authentication) {
        Map<String, Object> data = new HashMap<>();
        String name = authentication.getName();
        entryService.deleteEntries(ids, name);
        return data;
    }

    @PostMapping("{id}/submit")
    public Entry submit(@PathVariable("id") Long id,
                        @CurrentUser UserPrincipal principal) throws Exception {
        return entryService.submit(id, principal.getEmail());
    }

    @PostMapping("{id}/resubmit")
    public Entry resubmit(@PathVariable("id") Long id,
                          @CurrentUser UserPrincipal principal) {
        return entryService.resubmit(id, principal.getEmail());
    }

    @PostMapping("{id}/reset")
    public Entry reset(@PathVariable("id") Long id) {
        return entryService.reset(id);
    }

//    @PostMapping("{id}/update-approval")
//    public Entry updateApproval(@PathVariable Long id,
//                                @RequestParam("tierId") Long tierId,
//                                @RequestBody EntryApproval entryApproval) {
//        return entryService.updateApproval(tierId, id, entryApproval);
//    }

    @PostMapping("update-approver")
    public CompletableFuture<Map<String, Object>> updateApproval(@RequestParam("formId") Long formId,
                                                                 @RequestParam("tierId") Long tierId,
                                                                 @RequestParam(value = "all", defaultValue = "false") boolean updateApproved) {
        return entryService.updateApproverBulk(formId, tierId, updateApproved);
    }

    @PostMapping("update-approver-alltier")
    public CompletableFuture<Map<String, Object>> updateApprovalAllTier(@RequestParam("formId") Long formId) {
        return entryService.updateApproverAllTier(formId);
    }

    @PostMapping("{id}/remove-approval")
    public Entry removeApproval(@PathVariable("id") Long id,
                                @RequestParam("tierId") Long tierId,
                                @CurrentUser UserPrincipal principal) {
        return entryService.removeApproval(tierId, id, principal.getEmail());
    }

    @PostMapping("{id}/retract")
    public Entry retract(@PathVariable("id") Long id,
                         @RequestParam("email") String email) {
        return entryService.retractApp(id, email);
    }

    @PostMapping("{id}/assign")
    public Entry assign(@PathVariable("id") Long id,
                        @RequestParam("tierId") Long tierId,
                        @RequestParam("email") String email) throws Exception {
        return entryService.assignApprover(id, tierId, email);
    }

    @PostMapping("{id}/action")
    public Entry actionApp(@PathVariable("id") Long id,
                           @RequestBody EntryApproval gas,
                           @RequestParam("email") String email,
                           @RequestParam(value = "silent", required = false) boolean silent) {
//        Map<String, Object> data = new HashMap<>();

        return entryService.actionApp(id, gas, silent, email);
    }

    @PostMapping("bulk/action")
    public Map<String, Object> actionApp(@RequestParam("ids") List<Long> ids,
                                         @RequestBody EntryApproval gas,
                                         @RequestParam("email") String email) {
//        Map<String, Object> data = new HashMap<>();

        return entryService.actionApps(ids, gas, email);
    }

    @PostMapping("{id}/save-approval")
    public Entry saveApproval(@PathVariable("id") Long id,
                              @RequestBody EntryApproval gas,
                              @RequestParam("email") String email) {
        return entryService.saveApproval(id, gas, email);
    }

    @GetMapping("{appId}/start")
    public Map<String, Long> getStart(@PathVariable("appId") Long appId, @RequestParam("email") String email) {
        return this.entryService.getStart(appId, email);
    }

    @GetMapping("ef-exec")
    public CompletableFuture<Map<String, Object>> efExec(@RequestParam("formId") Long formId,
                                                         @RequestParam("field") String field,
                                                         @RequestParam(value = "section", required = false) String sectionCode,
                                                         @RequestParam(value = "force", defaultValue = "false") boolean force) {
//        Map<String, Object> data = new HashMap<>();
        return this.entryService.execVal(formId, field, sectionCode, force);
//        return this.entryService.execVal(formId, field, force);
//        return data;
    }


//    @GetMapping("ef-exec2")
//    public Object efExec2(@RequestParam("formId") Long formId,
//                                                         @RequestParam("field") String field,
//                                                         @RequestParam(value = "force", defaultValue = "false") boolean force) {
//
//        return this.entryService.getStart(122L,"blmrazif@unimas.my");
//    }

    @Transactional
    @PostMapping(value = "upload-file")
    public EntryAttachment uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "itemId", required = false) Long itemId,
                                      @RequestParam(value = "bucketId", required = false) Long bucketId,
                                      @RequestParam(value = "appId", required = false) Long appId,
                                      @CurrentUser UserPrincipal principal,
//                                          OAuth2Authentication auth,
                                      HttpServletRequest request) throws Exception {

        // Date dateNow = new Date();
        Map<String, Object> data = new HashMap<>();

        String username = principal.getName();
        Long userId = principal.getId();

        System.out.println("username:" + username + ",userId:" + userId);
//        Map<String, String> details = (Map<String, String>) auth.getUserAuthentication().getDetails();
//        String username = details.get("email");

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9_\\.\\-]", "_"), StandardCharsets.UTF_8);


//        String random = Long.toString(UUID.randomUUID().getLessSignificantBits(), Character.MAX_RADIX);
        String filePath = userId + "_" + Instant.now().getEpochSecond() + "_" + originalFilename;
//        String filePath = Instant.now().getEpochSecond() + "_" + originalFilename;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        EntryAttachment attachment = new EntryAttachment();

        Item item = null;
        if (itemId != null || bucketId != null) {
            if (bucketId != null) {
                attachment.setBucketId(bucketId);
                destStr += "bucket-" + bucketId + "/";
            } else {
                item = itemRepository.findById(itemId).orElse(null);
                if (item != null) {
                    try {
                        Long bucketIdItem = item.getX().get("bucket").asLong();
                        if (bucketIdItem != null) {
                            destStr += "bucket-" + bucketIdItem + "/";
                            attachment.setBucketId(bucketIdItem);
                        }
                    } catch (Exception e) {
//                        System.out.println("Error retrieving bucket id.");
                    }
                }
            }
            if (itemId != null) {
                item = itemRepository.findById(itemId).orElse(null);
                if (item != null) {
                    attachment.setItemLabel(item.getLabel());
                    if (item.getX().at("/filenameTpl")!=null) {
                        String filenameTpl = item.getX().at("/filenameTpl").asText("");
//                    System.out.println(">>item wujud: "+ item.getX().get("filenameTpl").isEmpty()+","+ item.getX().get("filenameTpl").asText());
                        if (!filenameTpl.isBlank()) {
                            if (filenameTpl.contains("$unique$")) {
                                filePath = originalFilename;
                            } else {
                                filePath = Instant.now().getEpochSecond() + "-" + originalFilename;
                            }
//                        System.out.println("filePath:"+filePath);
                        }
                    }
                }
            }
        }

        attachment.setFileName(originalFilename);
        attachment.setFileSize(fileSize);
        attachment.setFileType(contentType);
        attachment.setFileUrl(filePath);
        attachment.setEmail(principal.getEmail());
        attachment.setTimestamp(new Date());
        attachment.setMessage("success");
        attachment.setItemId(itemId);
        attachment.setAppId(appId);
        attachment.setSuccess(true);

        File dir = new File(destStr);

        dir.mkdirs();

        Boolean isFileSafe = true;
        if (bucketId != null) {
            Bucket bucket = bucketRepository.findById(bucketId).orElseThrow(() -> new ResourceNotFoundException("Bucket", "id", bucketId));
            if (bucket.getX() != null && bucket.getX().at("/scanUpload").asBoolean(false)) {
                try {
                    long startScan = System.currentTimeMillis();
                    isFileSafe = clamavService.scanFileAttachment(filePath, file);
                    long endScan = System.currentTimeMillis();
                } catch (Exception ex) {
                    isFileSafe = true; // if error scanning, just return true;
                }
            }
        } else {
            // if not in bucket, FORCE file scanning
            try {
                long startScan = System.currentTimeMillis();
                isFileSafe = clamavService.scanFileAttachment(filePath, file);
                long endScan = System.currentTimeMillis();
            } catch (Exception ex) {
                isFileSafe = true; // if error scanning, just return true;
            }

        }

//        System.out.println("File safe:"+isFilesafe);
//        System.out.println("Duration:" + 125);// (endScan-startScan));
//        isFileSafe = false;


        if (isFileSafe) {
            if (item != null && item.getX() != null && item.getX().get("zip") != null && item.getX().get("zip").asBoolean()) {
                InputStream inputStream = file.getInputStream();
                FileOutputStream fos = new FileOutputStream(destStr + filePath + ".zip");
                ZipOutputStream zipOutputStream = new ZipOutputStream(fos);
                ZipEntry zipEntry = new ZipEntry(file.getOriginalFilename());
                zipOutputStream.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = inputStream.read(bytes)) >= 0) {
                    zipOutputStream.write(bytes, 0, length);
                }
                zipOutputStream.close();

                attachment.setFileType("application/zip");
                attachment.setFileUrl(filePath + ".zip");

            } else {
//            System.out.println("no zip");
                File dest = new File(destStr + filePath);

                try {
                    file.transferTo(dest);
                } catch (IllegalStateException e) {
                    attachment.setMessage("failed");
                    attachment.setSuccess(false);
                }

                attachment.setSStatus("OK");
                attachment.setSMessage("✅ ClamAV: File safe!: The file " + originalFilename + " is safe.");


                // create thumbnail
//            Thumbnailer thumbnailer = null;
//            File dirThumb = new File (destStr+"thumbs/");
//            dirThumb.mkdirs();
//
//            System.out.println(file.getOriginalFilename());
//            if (file.getOriginalFilename().endsWith(".doc")){
//                thumbnailer = new DOCThumbnailer();
//            }else if (file.getOriginalFilename().endsWith(".docx")){
//                System.out.println("!!!!dlm docx");
//                thumbnailer = new DOCXThumbnailer();
//            }else if (file.getOriginalFilename().contains(".pdf")){
//                thumbnailer = new PDFThumbnailer();
//            }else if (file.getOriginalFilename().contains(".pptx")){
//                thumbnailer = new PPTXThumbnailer();
//            }else if (file.getOriginalFilename().contains(".xls")){
//                thumbnailer = new XLSThumbnailer();
//            }else if (file.getOriginalFilename().contains(".xlsx")){
//                thumbnailer = new XLSXThumbnailer();
//            }else if (file.getContentType().contains("image/")){
//                thumbnailer = new ImageThumbnailer("png");
//            }
//            if (thumbnailer!=null){
//                List<Dimensions> outputDimensions = Collections.singletonList(new Dimensions(240, 240));
//                BufferedImage thumbImage = thumbnailer.getThumbnails(dest, outputDimensions).get(0);
//                ImageIO.write(thumbImage, "jpg", new File(destStr+"thumbs/"+filePath+".jpg"));
//            }
                // end thumbnail

            }
        } else {
            attachment.setMessage("ClamAV: Failed virus scanning: The file " + originalFilename + " might have been compromised.");
            attachment.setSuccess(false);
            attachment.setSStatus("FOUND");
            attachment.setSMessage("❌ ClamAV: Threat Found!: The file " + originalFilename + " might have been compromised.");

        }

        return entryAttachmentRepository.save(attachment);
    }


//    @PostMapping(value = "upload-file-new")
//    public EntryAttachment uploadFileNew(@RequestParam("file") MultipartFile file,
//                                      @RequestParam(value = "itemId", required = false) Long itemId,
//                                      @RequestParam(value = "bucketId", required = false) Long bucketId,
//                                      @RequestParam(value = "appId", required = false) Long appId,
//                                      @CurrentUser UserPrincipal principal,
//                                      HttpServletRequest request) throws Exception {
//
//        // Date dateNow = new Date();
//        Map<String, Object> data = new HashMap<>();
//
//        String username = principal.getName();
//        Long userId = principal.getId();
//
//        long fileSize = file.getSize();
//        String contentType = file.getContentType();
//        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]",""), StandardCharsets.UTF_8);
//
//
//        String filePath = userId + "_" + Instant.now().getEpochSecond() + "_" + originalFilename;
//
//        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
//
//        EntryAttachment attachment = new EntryAttachment();
//        attachment.setFileName(originalFilename);
//        attachment.setFileSize(fileSize);
//        attachment.setFileType(contentType);
//        attachment.setFileUrl(filePath);
//        attachment.setEmail(principal.getEmail());
//        attachment.setTimestamp(new Date());
//        attachment.setMessage("success");
//        attachment.setItemId(itemId);
//        attachment.setAppId(appId);
//
//        Item item = null;
//        if (itemId != null || bucketId != null) {
//            if (bucketId != null) {
//                attachment.setBucketId(bucketId);
//                destStr += "bucket-" + bucketId + "/";
//            } else {
//                item = itemRepository.findById(itemId).orElse(null);
//                if (item != null) {
//                    try {
//                        Long bucketIdItem = item.getX().get("bucket").asLong();
//                        if (bucketIdItem != null) {
//                            destStr += "bucket-" + bucketIdItem + "/";
//                            attachment.setBucketId(bucketIdItem);
//                        }
//                    } catch (Exception e) {
////                        System.out.println("Error retrieving bucket id.");
//                    }
//                }
//            }
//            if (itemId != null) {
//                item = itemRepository.findById(itemId).orElse(null);
//                if (item!=null){
//                    attachment.setItemLabel(item.getLabel());
//                }
//            }
//        }
//        attachment.setSuccess(true);
//
//        File dir = new File(destStr);
//        File dirThumb = new File (destStr+"thumbs/");
//        dir.mkdirs();
//        dirThumb.mkdirs();
//
//        File dest = new File(destStr + filePath);
//        try {
//            file.transferTo(dest);
//        } catch (IllegalStateException e) {
//            attachment.setMessage("failed");
//            attachment.setSuccess(false);
//        }
//
//        // create thumbnail
//        Thumbnailer thumbnailer = new PDFThumbnailer();
//        List<Dimensions> outputDimensions = Collections.singletonList(new Dimensions(240, 240));
//        BufferedImage thumbImage = thumbnailer.getThumbnails(dest, outputDimensions).get(0);
//        ImageIO.write(thumbImage, "jpg", new File(destStr+"thumbs/"+filePath+".jpg"));
//        // end thumbnail
//
////        InputStream inputStream = file.getInputStream();
//        if (item != null && item.getX() != null && item.getX().get("zip") != null && item.getX().get("zip").asBoolean()) {
//
//
//            FileOutputStream fos = new FileOutputStream(destStr + filePath + ".zip");
//            ZipOutputStream zipOut = new ZipOutputStream(fos);
//
//            FileInputStream fis = new FileInputStream(dest);
//            ZipEntry zipEntry = new ZipEntry(file.getOriginalFilename());
//            zipOut.putNextEntry(zipEntry);
//
//            byte[] bytes = new byte[1024];
//            int length;
//            while((length = fis.read(bytes)) >= 0) {
//                zipOut.write(bytes, 0, length);
//            }
//
//            zipOut.close();
//            fis.close();
//            fos.close();
//
//            dest.deleteOnExit();
//
////            dest.delete();
//
//            attachment.setFileType("application/zip");
//            attachment.setFileUrl(filePath + ".zip");
//
//        }
//
//        return entryAttachmentRepository.save(attachment);
//    }


    @PostMapping(value = "{id}/link-files")
    @Transactional
    public Map<String, Object> linkFiles(@PathVariable("id") Long id,
                                         @RequestBody List<String> files,
                                         HttpServletRequest request) {

        // Date dateNow = new Date();
        Map<String, Object> data = new HashMap<>();

        List<EntryAttachment> eaList = files.stream().map(f -> {
//            System.out.println(f);
            EntryAttachment ea = entryAttachmentRepository.findFirstByFileUrl(f);
            ea.setEntryId(id);
            return ea;
        }).collect(Collectors.toList());

        entryAttachmentRepository.saveAll(eaList);

        return data;
    }

    @PostMapping(value = "delete-file")
    @Transactional
    public Map<String, Object> deleteFile(@RequestParam("fileUrl") List<String> fileUrl,
                                          Principal principal,
                                          HttpServletRequest request) {

        // Date dateNow = new Date();
        Map<String, Object> data = new HashMap<>();

        fileUrl.forEach(file -> {
            Map<String, Object> mdata = new HashMap<>();
            if (!Helper.isNullOrEmpty(file)) {
                EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(file);

                if (entryAttachment != null) {
                    String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

                    if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                        destStr += "bucket-" + entryAttachment.getBucketId() + "/";
                    }

                    File dir = new File(destStr);
                    dir.mkdirs();

                    File dest = new File(destStr + file);
                    mdata.put("success", dest.delete());

                    entryAttachmentRepository.delete(entryAttachment);
                } else {
                    mdata.put("success", false);
                    mdata.put("message", "EntryAttachment doesn't exist");
                }


            } else {
                mdata.put("success", false);
                mdata.put("message", "Empty fileUrl");
            }
            data.put(file, mdata);
        });


        return data;
    }

    @RequestMapping(value = "file/{path}")
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> getFileEntity(@PathVariable("path") String path,
                                                               HttpServletResponse response, Principal principal) throws IOException {

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (path.startsWith("lookup")) {
            path = path.replaceAll("~", "/");
        }

        if (Helper.isNullOrEmpty(path)) return new ResponseEntity(HttpStatus.NOT_FOUND);

        EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(path);
        if (entryAttachment != null && entryAttachment.getBucketId() != null) {
            destStr += "bucket-" + entryAttachment.getBucketId() + "/";
        }

        if (entryAttachment != null && entryAttachment.getItemId() != null) {
            Item item = itemRepository.findById(entryAttachment.getItemId()).orElse(null);
            if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal == null) {
                // is private
                // ERROR 401
                throw new OAuth2AuthenticationException(new OAuth2Error("401"), "Full authentication is required to access this resource");
            }
        }

        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(path)
                .build();

        File file = new File(destStr + path);

        if (!file.isFile()) return new ResponseEntity(HttpStatus.NOT_FOUND);

        String mimeType = Files.probeContentType(file.toPath());

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

        if (!Helper.isNullOrEmpty(mimeType)) {
            builder.contentType(MediaType.parseMediaType(mimeType));
        } else {
            if (entryAttachment != null && Helper.isNullOrEmpty(entryAttachment.getFileType())) {
                builder.contentType(MediaType.parseMediaType(entryAttachment.getFileType()));
            }
        }

        return builder.body(outputStream -> Files.copy(file.toPath(), outputStream));

    }

    @RequestMapping(value = "file/inline/{path}")
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> getFileInline(@PathVariable("path") String path,
                                                               HttpServletResponse response, Principal principal) throws IOException {

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (!Helper.isNullOrEmpty(path)) {
            EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(path);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            if (entryAttachment != null && entryAttachment.getItemId() != null) {
                Item item = itemRepository.findById(entryAttachment.getItemId()).orElse(null);
                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal == null) {
                    // is private
                    // ERROR 401
                    throw new OAuth2AuthenticationException(new OAuth2Error("401"), "Full authentication is required to access this resource");
                }
            }

            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .build();

            File file = new File(destStr + path);

            if (file.isFile()) {

                String mimeType = Files.probeContentType(file.toPath());

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

                if (!Helper.isNullOrEmpty(mimeType)) {
                    builder.contentType(MediaType.parseMediaType(mimeType));
                } else {
                    if (entryAttachment != null && Helper.isNullOrEmpty(entryAttachment.getFileType())) {
                        builder.contentType(MediaType.parseMediaType(entryAttachment.getFileType()));
                    }
                }

                return builder
                        .body(outputStream -> Files.copy(file.toPath(), outputStream));
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "file/unzip/{path}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getFileUnzip(@PathVariable("path") String path, HttpServletResponse response, Principal principal) throws IOException {

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        String mimeType;

        EntryAttachment entryAttachment = null;

        if (!Helper.isNullOrEmpty(path)) {
            entryAttachment = entryAttachmentRepository.findFirstByFileUrl(path);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            if (entryAttachment != null && entryAttachment.getItemId() != null) {
                Item item = itemRepository.findById(entryAttachment.getItemId()).orElse(null);
                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal == null) {
                    // is private
                    // ERROR 401
                    throw new OAuth2AuthenticationException(new OAuth2Error("401"), "Full authentication is required to access this resource");
                }
            }


            File file = new File(destStr + path);

            if (entryAttachment != null) {
                if (entryAttachment.getFileType() != null) {
                    mimeType = entryAttachment.getFileType();
                } else {
                    mimeType = Files.probeContentType(file.toPath());
                }
            } else {
                mimeType = Files.probeContentType(file.toPath());
            }

            if (file.isFile()) {

                byte[] returnFile;
                if (path.endsWith(".zip")) {
                    ZipFile zf = new ZipFile(file);
                    List<FileHeader> fileHeaderList = zf.getFileHeaders();
                    if (fileHeaderList.size() > 0) {
                        FileHeader fileHeader = fileHeaderList.get(0);
                        ZipInputStream is = zf.getInputStream(fileHeader);
                        int uncompressedSize = (int) fileHeader.getUncompressedSize();
                        OutputStream os = new ByteArrayOutputStream(uncompressedSize);
                        int bytesRead;
                        byte[] buffer = new byte[4096];
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        returnFile = ((ByteArrayOutputStream) os).toByteArray();
                        is.close();
                    } else {
                        returnFile = new byte[0];
                    }
                } else {
                    returnFile = Files.readAllBytes(file.toPath());
                }

//        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
//        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
                ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                        .filename(path.substring(0, path.length() - 4))
                        .build();

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

                if (!Helper.isNullOrEmpty(mimeType)) {
                    builder.contentType(MediaType.parseMediaType(mimeType));
                } else {
                    if (entryAttachment != null && Helper.isNullOrEmpty(entryAttachment.getFileType())) {
                        builder.contentType(MediaType.parseMediaType(entryAttachment.getFileType()));
                    }
                }

                return builder.body(returnFile);
            } else {
                return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
        }
    }

    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        return Long.toString(uuid.getLeastSignificantBits(), Character.MAX_RADIX);
    }

    @GetMapping(value = "dashboard/{dashboardId}")
    public Map getDashboardData2(@PathVariable("dashboardId") Long dashboardId,
                                 @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                 @RequestParam(value = "email", required = false) String email,
                                 HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (dashboardId:" + dashboardId + "):" + e.getMessage());
        }
        return entryService.getDashboardDataNativeNew(dashboardId, p, email, request);
    }

    @GetMapping(value = "dashboard-map/{dashboardId}")
    public Map getDashboardDataMap2(@PathVariable("dashboardId") Long dashboardId,
                                    @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                    @RequestParam(value = "email", required = false) String email,
                                    HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (dashboardId:" + dashboardId + "):" + e.getMessage());
        }
        return entryService.getDashboardMapDataNativeNew(dashboardId, p, email, request);
    }

    @GetMapping(value = "chart/{chartId}")
    public Map getChartData(@PathVariable("chartId") Long chartId,
                            @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                            @RequestParam(value = "email", required = false) String email,
                            HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (chartId:" + chartId + "):" + e.getMessage());
        }
        return entryService.getChartDataNative(chartId, p, email, request);
    }

    @GetMapping(value = "chart-map/{chartId}")
    public Object getChartMapData(@PathVariable("chartId") Long chartId,
                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                  @RequestParam(value = "email", required = false) String email,
                                  HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
//            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
            p = mapper.readValue(filters, Map.class);
        } catch (Exception e) {
            System.out.println("Filters:" + filters);
            System.out.println("Error decoding filter (chartId:" + chartId + "):" + e.getMessage());
        }
        return entryService.getChartMapDataNative(chartId, p, email, request);
    }

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_RANGE = "Content-Range";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String BYTES = "bytes";
    public static final int BYTE_RANGE = 1024;

    @GetMapping("file/stream/{path}")
    public ResponseEntity<byte[]> prepareContent(@PathVariable("path") String fileName, @RequestHeader(value = "Range", required = false) String range) throws IOException {
        long rangeStart = 0;
        long rangeEnd;
        byte[] data;
        Long fileSize;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (!Helper.isNullOrEmpty(fileName)) {
            EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(fileName);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }
        }

        String fullFileName = destStr + fileName;
        String mimeType = Files.probeContentType(Paths.get(fullFileName));
        try {
            fileSize = getFileSize(fullFileName);
            if (range == null) {
                return ResponseEntity.status(HttpStatus.OK)
                        .header(CONTENT_TYPE, mimeType)
                        .header(CONTENT_LENGTH, String.valueOf(fileSize))
                        .body(readByteRange(fullFileName, rangeStart, fileSize - 1)); // Read the object and convert it as bytes
            }
            String[] ranges = range.split("-");
            rangeStart = Long.parseLong(ranges[0].substring(6));
            if (ranges.length > 1) {
                rangeEnd = Long.parseLong(ranges[1]);
            } else {
                rangeEnd = fileSize - 1;
            }
            if (fileSize < rangeEnd) {
                rangeEnd = fileSize - 1;
            }
            data = readByteRange(fullFileName, rangeStart, rangeEnd);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(CONTENT_TYPE, mimeType)
                .header(ACCEPT_RANGES, BYTES)
                .header(CONTENT_LENGTH, contentLength)
                .header(CONTENT_RANGE, BYTES + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                .body(data);
    }


    public byte[] readByteRange(String filename, long start, long end) throws IOException {
        Path path = Paths.get(filename);
        try (InputStream inputStream = (Files.newInputStream(path));
             ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[BYTE_RANGE];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                bufferedOutputStream.write(data, 0, nRead);
            }
            bufferedOutputStream.flush();
            byte[] result = new byte[(int) (end - start) + 1];
            System.arraycopy(bufferedOutputStream.toByteArray(), (int) start, result, 0, result.length);
            return result;
        }
    }

    /**
     * Content length.
     *
     * @param fileName String.
     * @return Long.
     */
    public Long getFileSize(String fileName) {
        return Optional.ofNullable(fileName)
                .map(Paths::get)
                .map(this::sizeFromFile)
                .orElse(0L);
    }

    /**
     * Getting the size from the path.
     *
     * @param path Path.
     * @return Long.
     */
    private Long sizeFromFile(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ioException) {
//            logger.error("Error while getting the file size", ioException);
        }
        return 0L;
    }

    @GetMapping("resync")
    public void updateDatasetData(@RequestParam("datasetId") Long datasetId) {
        this.entryService.bulkResyncEntryData_ModelPicker(datasetId);
    }


}
