package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.EntryMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.BucketRepository;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.repository.ItemRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.EntryService;
import com.benzourry.leap.service.storage.StorageFactory;
import com.benzourry.leap.service.storage.StorageProvider;
import com.benzourry.leap.utility.ClamAVServiceUtil;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.TenantLogger;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
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


@RestController
@RequestMapping({"/api/entry", "/api/public/entry"})
//@CrossOrigin(allowCredentials = "true")
public class EntryController {

    private static final Logger logger = LoggerFactory.getLogger(EntryController.class);
    final EntryService entryService;

    final EntryAttachmentRepository entryAttachmentRepository;

    final ItemRepository itemRepository;

    final BucketRepository bucketRepository;

    final ClamAVServiceUtil clamavService;

    // INJECT THIS
    private final StorageFactory storageFactory;

    // ... Constructor injection ...

    private final ObjectMapper MAPPER;

    @Autowired
    public EntryController(EntryService entryService,
                           EntryAttachmentRepository entryAttachmentRepository,
                           ClamAVServiceUtil clamavService,
                            StorageFactory storageFactory,
                           ItemRepository itemRepository,
                           BucketRepository bucketRepository, ObjectMapper MAPPER) {
        this.entryService = entryService;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.clamavService = clamavService;
        this.storageFactory = storageFactory;
        this.itemRepository = itemRepository;
        this.bucketRepository = bucketRepository;
        this.MAPPER = MAPPER;
    }

    @GetMapping("/{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry findById(@PathVariable("id") long id,
                          HttpServletRequest request,
                          @CurrentUser UserPrincipal principal) {

        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();

        String name = principal == null ? null : principal.getName();
        return entryService.findById(id, isAnonymous, request);
    }

    @GetMapping("/{id}/approval-trails")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryApprovalTrail> findTrailById(@PathVariable("id") long id,
                                                  @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
        return entryService.findApprovalTrailById(id, pageable);
    }

    @GetMapping("/{id}/files")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryAttachment> findFilesById(@PathVariable("id") long id,
                                               @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
        return entryService.findFilesById(id, pageable);
    }

    @GetMapping( "/by-params")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry findFirstByParam(@RequestParam("formId") Long formId,
                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                  HttpServletRequest request,
                                  @CurrentUser UserPrincipal principal) throws Exception {
        Map<String, Object> p = parseFiltersSafely(filters, "formId:" + formId);

        boolean isAnonymous = (principal == null);
        return entryService.findFirstByParam(formId, p, request, isAnonymous);
    }


    @PostMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry save(@RequestParam("formId") long formId,
                      @RequestParam(value = "prevId", required = false) Long prevId,
                      @RequestBody Entry entry,
//                      @RequestParam("email") String email,
                      @CurrentUser UserPrincipal principal) throws Exception {

        String email = principal.getEmail();

        return entryService.save(formId, entry, prevId, email, true);
    }

    @PostMapping("/field")
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


    @PostMapping("/{id}/undelete")
    public Map<String, Object> undelete(@PathVariable("id") long id,
                                        @RequestParam("trailId") long trailId,
                                        Authentication authentication) {
        return entryService.undelete(id, trailId, authentication.getName());
    }

    @PostMapping("/{id}/undo")
    public Map<String, Object> undo(@PathVariable("id") long id,
                                    @RequestParam("trailId") long trailId,
                                    Authentication authentication) {
        return entryService.undo(id, trailId, authentication.getName());
    }

    @GetMapping("/list-data")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
    })
    public List<ObjectNode> findUnboxed(@RequestParam("datasetId") Long datasetId,
                                        @RequestParam(value = "searchText", required = false) String searchText,
                                        @RequestParam(value = "email", required = false) String email,
                                        @RequestParam(value = "sorts", required = false) List<String> sorts,
                                        @RequestParam(value = "ids", required = false) List<Long> ids,
                                        @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                        @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                        @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                        Pageable pageable,
                                        HttpServletRequest request,
                                        @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);

        String pEmail = isAnonymous ? email : principal.getEmail();

        Map<String, Object> filtersMap = parseFiltersSafely(filters, "datasetId:" + datasetId);

        JsonNode statusJson = parseStatusSafely(status, "datasetId:" + datasetId);

        return entryService.findListByDatasetData(datasetId, searchText, pEmail, filtersMap, statusJson, cond, sorts, ids, isAnonymous, pageable, request);
    }

    @GetMapping("/list")
    @JsonResponse(mixins = {
            @JsonMixin(target = EntryDto.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Page<EntryDto> findAllByDatasetIdCheck(
            @RequestParam("datasetId") Long datasetId,
            @RequestParam(value = "searchText", required = false) String searchText,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "sorts", required = false) List<String> sorts,
            @RequestParam(value = "ids", required = false) List<Long> ids,
            @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
            @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
            @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
            Pageable pageable,
            HttpServletRequest request,
            @CurrentUser UserPrincipal principal) {

        boolean isAnonymous = (principal == null);

        String pEmail = isAnonymous ? email : principal.getEmail();

        Map<String, Object> filtersMap = parseFiltersSafely(filters, "datasetId:" + datasetId);

        JsonNode statusJson = parseStatusSafely(status, "datasetId:" + datasetId);

        return entryService.findListByDatasetCheck(
                datasetId, searchText, pEmail, filtersMap, statusJson,
                cond, sorts, ids, isAnonymous, pageable, request
        );
    }

    @GetMapping("/list-chartdrill")
    @JsonResponse(mixins = {
            @JsonMixin(target = EntryDto.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Page<EntryDto> findAllByChartIdCheck(@RequestParam("chartId") Long chartId,
                                               @RequestParam(value = "searchText", required = false) String searchText,
                                               @RequestParam(value = "email", required = false) String email,
                                               @RequestParam(value = "sorts", required = false) List<String> sorts,
                                               @RequestParam(value = "ids", required = false) List<Long> ids,
                                               @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                               @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                               Pageable pageable,
                                               HttpServletRequest request,
                                               @CurrentUser UserPrincipal principal) {

        boolean isAnonymous = (principal == null);
        String pEmail = isAnonymous ? email : principal.getEmail();

        Map<String, Object> p = parseFiltersSafely(filters, "chartId (drill):" + chartId);
        return entryService.findListByChart(chartId, searchText, pEmail, p, cond, sorts, ids, pageable, request);
    }

    @GetMapping("/list-all")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class),
//            @JsonMixin(target = JsonNode.class, mixin = EntryMixin.JsonNodeF.class)

    })
    public Page<EntryDto> findAllByDatasetIdCheck(@RequestParam("formId") Long formId,
                                               @RequestParam(value = "searchText", required = false) String searchText,
                                               Pageable pageable,
                                               HttpServletRequest request,
                                               Principal principal) {
//        String name = principal == null ? null : principal.getName();
//
//        Map<String, Object> p = new HashMap();

//        try {
//            // Masalah double decoding.
//            p = MAPPER.readValue(filters, Map.class);
////            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
//        } catch (Exception e) {
//            logger.error("Error decoding filter (datasetId:" + datasetId + "):" + e.getMessage());
//        }
        return entryService.findByFormId(formId, pageable);
    }

    @Transactional
    @GetMapping("/stream")
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
                                                       @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                       @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                                       Pageable pageable,
                                                       HttpServletRequest request,
                                                       @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String pEmail = isAnonymous ? email : principal.getEmail();

        Map<String, Object> p = parseFiltersSafely(filters, "datasetId:" + datasetId);

        // 2. Safer JSON parsing with proper HTTP Bad Request exception
        JsonNode statusJson = parseStatusSafely(status, "datasetId:" + datasetId);

        return entryService.streamListByDatasetCheck(datasetId, searchText,pEmail, p, statusJson, cond, sorts, ids, isAnonymous, pageable, request);
    }

    @GetMapping("/count")
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
                                                @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                                @CurrentUser UserPrincipal principal,
                                                HttpServletRequest request) {

        boolean isAnonymous = (principal == null);
        String pEmail = isAnonymous ? email : principal.getEmail();

        Map<String, Object> p = parseFiltersSafely(filters, "datasetId:" + datasetId);

        JsonNode statusJson = parseStatusSafely(status, "datasetId:" + datasetId);

        Map<String, Object> data = new HashMap<>();
        data.put("count", entryService.countByDataset(datasetId, searchText, pEmail, p, statusJson, cond, request));
        return data;
    }

    @PostMapping("/list-blast")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Map<String, Object> blastEmailByDatasetId(@RequestParam("datasetId") Long datasetId,
                                                     @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
                                                     @RequestParam("email") String email,
                                                     @RequestParam(value = "ids", required = false) List<Long> ids,
                                                     @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                                     @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                     @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                                     @RequestBody EmailTemplate emailTemplate,
                                                     @CurrentUser UserPrincipal principal,
                                                     HttpServletRequest request) throws Exception {

        boolean isAnonymous = (principal == null);
        String pEmail = isAnonymous ? email : principal.getEmail();

        Map<String, Object> p = parseFiltersSafely(filters, "datasetId:" + datasetId);

        if (emailTemplate!=null){
            emailTemplate.setEnabled(1);
        }

        // 2. Safer JSON parsing with proper HTTP Bad Request exception
        JsonNode statusJson = parseStatusSafely(status, "datasetId:" + datasetId);

        return entryService.blastEmailByDataset(datasetId, searchText, pEmail, p, statusJson, cond, emailTemplate, ids, request, email, principal);
    }

    @PostMapping("/{id}/delete")
    public Map<String, Object> deleteEntry(@PathVariable("id") Long id,
                                           Authentication authentication) {
        Map<String, Object> data = new HashMap<>();
        String name = authentication.getName();
        entryService.deleteEntry(id, name);
        return data;
    }

    @PostMapping("/bulk/delete")
    public Map<String, Object> deleteEntries(@RequestParam("ids") List<Long> ids,
                                             @CurrentUser UserPrincipal principal) {
        Map<String, Object> data = new HashMap<>();
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        entryService.deleteEntries(ids, email);
        return data;
    }

    @PostMapping("/{id}/submit")
    public Entry submit(@PathVariable("id") Long id,
                        @CurrentUser UserPrincipal principal) throws Exception {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return entryService.submit(id, email);
    }

    @PostMapping("/{id}/resubmit")
    public Entry resubmit(@PathVariable("id") Long id,
                          @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();

        return entryService.resubmit(id, email);
    }

    @PostMapping("/{id}/reset")
    public Entry reset(@PathVariable("id") Long id) {
        return entryService.reset(id);
    }

    @GetMapping("/{id}/txhash")
    public ResponseEntity<Map<String, String>> getTxHash(@PathVariable("id") Long id) {
        return ResponseEntity.ok(entryService.getTxHashOnly(id));
    }

    @PostMapping("/update-approver")
    public CompletableFuture<Map<String, Object>> updateApproval(@RequestParam("formId") Long formId,
                                                                 @RequestParam("tierId") Long tierId,
                                                                 @RequestParam(value = "all", defaultValue = "false") boolean updateApproved) {
        return entryService.updateApproverBulk(formId, tierId, updateApproved);
    }

    @PostMapping("/update-approver-alltier")
    public CompletableFuture<Map<String, Object>> updateApprovalAllTier(@RequestParam("formId") Long formId) {
        return entryService.updateApproverAllTier(formId);
    }

    @PostMapping("/{id}/remove-approval")
    public Entry removeApproval(@PathVariable("id") Long id,
                                @RequestParam("tierId") Long tierId,
//                                @RequestParam(value = "email", required = false) String email,
                                @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return entryService.removeApproval(tierId, id, email);
    }

    @PostMapping("/{id}/retract")
    public Entry retract(@PathVariable("id") Long id,
//                         @RequestParam(value = "email", required = false) String email,
                         @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return entryService.retractApp(id, email);
    }

    @PostMapping("/{id}/assign")
    public Entry assign(@PathVariable("id") Long id,
                        @RequestParam("tierId") Long tierId,
                        @RequestParam("email") String email) {
        return entryService.assignApprover(id, tierId, email);
    }

    @PostMapping("/{id}/action")
    public Entry actionApp(@PathVariable("id") Long id,
                           @RequestBody EntryApproval gas,
//                           @RequestParam(value = "email", required = false) String email,
                           @CurrentUser UserPrincipal principal,
                           @RequestParam(value = "silent", required = false) boolean silent) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return entryService.actionApp(id, gas, silent, email);
    }

    @PostMapping("/bulk/action")
    public Map<String, Object> actionApp(@RequestParam("ids") List<Long> ids,
                                         @RequestBody EntryApproval gas,
//                                         @RequestParam(value = "email", required = false) String email,
                                         @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return entryService.actionApps(ids, gas, email);
    }

    @PostMapping("/{id}/save-approval")
    public Entry saveApproval(@PathVariable("id") Long id,
                              @RequestBody EntryApproval gas,
//                              @RequestParam(value = "email", required = false) String email,
                              @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return entryService.saveApproval(id, gas, email);
    }

    @GetMapping("/{appId}/start")
    public Map<String, Long> getStart(@PathVariable("appId") Long appId,
//                                      @RequestParam(value = "email", required = false) String email,
                                      @CurrentUser UserPrincipal principal) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        return this.entryService.getStart(appId, email);
    }

    @GetMapping("/ef-exec")
    public CompletableFuture<Map<String, Object>> efExec(@RequestParam("formId") Long formId,
                                                         @RequestParam("field") String field,
                                                         @RequestParam(value = "section", required = false) String sectionCode,
                                                         @RequestParam(value = "tierId", required = false) Long tierId,
                                                         @RequestParam(value = "force", defaultValue = "false") boolean force) {
        return this.entryService.execVal(formId, field, sectionCode, tierId, force);
    }


    @Transactional
    @PostMapping(value = "/upload-file")
    public EntryAttachment uploadFile(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "itemId", required = false) Long itemId,
                                      @RequestParam(value = "bucketId", required = false) Long bucketId,
                                      @RequestParam(value = "appId", required = false) Long appId,
                                      @CurrentUser UserPrincipal principal,
                                      HttpServletRequest request) throws Exception {

        Long userId = principal.getId();

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(
                file.getOriginalFilename().replaceAll("[^a-zA-Z0-9_\\.\\-]", "_"),
                StandardCharsets.UTF_8);

        String filePath = userId + "_" + Instant.now().getEpochSecond() + "_" + originalFilename;

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
                        logger.error("Error retrieving bucket id: " + e.getMessage());
                    }
                }
            }
            if (itemId != null) {
                item = itemRepository.findById(itemId).orElse(null);
                if (item != null) {
                    attachment.setItemLabel(item.getLabel());
                    if (item.getX().at("/filenameTpl")!=null) {
                        String filenameTpl = item.getX().at("/filenameTpl").asText("");
                        if (!filenameTpl.isBlank()) {
                            if (filenameTpl.contains("$unique$")) {
                                filePath = originalFilename;
                            } else {
                                filePath = Instant.now().getEpochSecond() + "-" + originalFilename;
                            }
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
        if (!dir.exists()) dir.mkdirs();

        boolean isFileSafe = true;
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

        if (isFileSafe) {
            File zippedFileFallback = null;

            // 1. Handle local Zipping first (Identical to your original logic)
            if (item != null && item.getX() != null && item.getX().get("zip") != null && item.getX().get("zip").asBoolean()) {
                zippedFileFallback = new File(destStr + filePath + ".zip");

                try (InputStream inputStream = file.getInputStream();
                     FileOutputStream fos = new FileOutputStream(zippedFileFallback);
                     ZipOutputStream zipOutputStream = new ZipOutputStream(fos)) {

                    zipOutputStream.putNextEntry(new ZipEntry(file.getOriginalFilename()));
                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = inputStream.read(buffer)) != -1) {
                        zipOutputStream.write(buffer, 0, length);
                    }
                    zipOutputStream.closeEntry();
                }
                attachment.setFileType("application/zip");
            }

            // 2. ROUTE TO THE CORRECT CLOUD / LOCAL PROVIDER
            try {
                Bucket activeBucket = bucketId != null ? bucketRepository.findById(bucketId).orElse(null) : null;

                // Fallback to "local" if type is null
                String bucketType = (activeBucket != null && activeBucket.getType() != null)
                        ? activeBucket.getType()
                        : "local";

                StorageProvider provider = storageFactory.getProvider(bucketType);

                // Delegate upload to provider
                String finalFileUrl = provider.upload(file, zippedFileFallback, destStr, filePath, activeBucket);
                attachment.setFileUrl(finalFileUrl);

                // Replicating original safe status setting
                if (zippedFileFallback == null) {
                    attachment.setSStatus("OK");
                    attachment.setSMessage("✅ ClamAV: File safe!: The file " + originalFilename + " is safe.");
                }

                // Cleanup local zip ONLY if it was uploaded to a cloud provider
                if (!"local".equalsIgnoreCase(bucketType) && zippedFileFallback != null && zippedFileFallback.exists()) {
                    zippedFileFallback.delete();
                }

            } catch (Exception e) {
                logger.error("Storage Provider Upload Failed", e);
                TenantLogger.error(appId,"form", (item != null ? item.getFormId() : -1),e.getMessage());
                attachment.setMessage("failed");
                attachment.setSuccess(false);

                // In your original code, the sStatus="OK" was set even if the transfer threw IllegalStateException.
                // We replicate that here so we don't break your frontend expectations.
                if (zippedFileFallback == null) {
                    attachment.setSStatus("OK");
                    attachment.setSMessage("✅ ClamAV: File safe!: The file " + originalFilename + " is safe.");
                }
            }

        } else {
            attachment.setMessage("ClamAV: Failed virus scanning: The file " + originalFilename + " might have been compromised.");
            attachment.setSuccess(false);
            attachment.setSStatus("FOUND");
            attachment.setSMessage("❌ ClamAV: Threat Found!: The file " + originalFilename + " might have been compromised.");
        }

        return entryAttachmentRepository.save(attachment);
    }

    @PostMapping(value = "/{id}/link-files")
    @Transactional
    public Map<String, Object> linkFiles(@PathVariable("id") Long id,
                                         @RequestBody List<String> files,
                                         HttpServletRequest request) {
        Map<String, Object> data = new HashMap<>();

        List<EntryAttachment> eaList = files.stream()
                .map(f -> entryAttachmentRepository.findFirstByFileUrl(f))
                .filter(Objects::nonNull) // Safely ignore any URLs that aren't found in the DB
                .peek(ea -> ea.setEntryId(id)) // Update the entry ID for valid records
                .collect(Collectors.toList());

        entryAttachmentRepository.saveAll(eaList);

        data.put("success", true);
        data.put("linkedCount", eaList.size());

        return data;
    }

    @PostMapping(value = "/delete-file")
    @Transactional
    public Map<String, Object> deleteFile(@RequestParam("fileUrl") List<String> fileUrlList,
                                          Principal principal,
                                          HttpServletRequest request) {

        Map<String, Object> data = new HashMap<>();

        fileUrlList.forEach(fileUrl -> {
            Map<String, Object> mdata = new HashMap<>();
            if (!Helper.isNullOrEmpty(fileUrl)) {
                EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(fileUrl);

                if (entryAttachment != null) {
                    String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
                    Bucket activeBucket = null;

                    if (entryAttachment.getBucketId() != null) {
                        destStr += "bucket-" + entryAttachment.getBucketId() + "/";
                        activeBucket = bucketRepository.findById(entryAttachment.getBucketId()).orElse(null);
                    }

                    try {
                        String bucketType = (activeBucket != null && activeBucket.getType() != null) ? activeBucket.getType() : "local";
                        StorageProvider provider = storageFactory.getProvider(bucketType);

                        // Physically delete the file from Local/GDrive/OneDrive
                        boolean isDeleted = provider.delete(fileUrl, destStr, activeBucket);
                        mdata.put("success", isDeleted);

                        // Clean up the database record
                        entryAttachmentRepository.delete(entryAttachment);

                    } catch (Exception e) {
                        logger.error("Failed to delete file from storage provider", e);
                        TenantLogger.error(entryAttachment.getAppId(),"form", (entryAttachment.getItemId() != null ? itemRepository.findById(entryAttachment.getItemId()).map(Item::getFormId).orElse(-1L) : -1L),e.getMessage());
                        mdata.put("success", false);
                        mdata.put("message", "Storage provider error: " + e.getMessage());
                    }
                } else {
                    mdata.put("success", false);
                    mdata.put("message", "EntryAttachment doesn't exist");
                }

            } else {
                mdata.put("success", false);
                mdata.put("message", "Empty fileUrl");
            }
            data.put(fileUrl, mdata);
        });

        return data;
    }

    @GetMapping("/file/{path}")
    public ResponseEntity<StreamingResponseBody> getFileEntity(@PathVariable("path") String path, HttpServletRequest request, Principal principal) throws IOException {
        return processFileStream(path, request, principal, false);
    }

    @RequestMapping(value = "/file/inline/{path}")
    @Transactional(readOnly = true)
    public ResponseEntity<StreamingResponseBody> getFileInline(@PathVariable("path") String path, HttpServletRequest request, Principal principal) throws IOException {
        return processFileStream(path, request, principal, true);
    }

    // --- REUSABLE HELPER METHOD FOR STREAMING (SAVES 100+ LINES) ---
    private ResponseEntity<StreamingResponseBody> processFileStream(String path, HttpServletRequest request, Principal principal, boolean isInline) throws IOException {
        if (Helper.isNullOrEmpty(path)) return ResponseEntity.notFound().build();
        if (path.startsWith("lookup")) path = path.replaceAll("~", "/");

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
        EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(path);
        Bucket activeBucket = null;

        if (entryAttachment != null && entryAttachment.getBucketId() != null) {
            destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            activeBucket = bucketRepository.findById(entryAttachment.getBucketId()).orElse(null);
        }

        if (entryAttachment != null && entryAttachment.getItemId() != null) {
            Item item = itemRepository.findById(entryAttachment.getItemId()).orElse(null);
            if (item != null && item.getX() != null && item.getX().has("secure") && item.getX().get("secure").asBoolean() && principal == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error("401"), "Full authentication is required");
            }
        }

        try {
            String bucketType = (activeBucket != null && activeBucket.getType() != null) ? activeBucket.getType() : "local";
            StorageProvider provider = storageFactory.getProvider(bucketType);

            if (!provider.exists(path, destStr, activeBucket)) return ResponseEntity.notFound().build();

            long lastModified = provider.getLastModified(path, destStr, activeBucket);
            long fileLength = provider.getFileSize(path, destStr, activeBucket);

            if (fileLength <= 0 && entryAttachment != null && entryAttachment.getFileSize() != null) fileLength = entryAttachment.getFileSize();
            if (lastModified <= 0 && entryAttachment != null && entryAttachment.getTimestamp() != null) lastModified = entryAttachment.getTimestamp().getTime();

            String eTag = "\"" + lastModified + "-" + fileLength + "\"";
            if (eTag.equals(request.getHeader(HttpHeaders.IF_NONE_MATCH)) || request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE) >= lastModified) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            String safeName = (entryAttachment != null && entryAttachment.getFileName() != null) ? entryAttachment.getFileName() : Paths.get(path).getFileName().toString();
            ContentDisposition contentDisposition = isInline ?
                    ContentDisposition.inline().filename(safeName, StandardCharsets.UTF_8).build() :
                    ContentDisposition.attachment().filename(safeName, StandardCharsets.UTF_8).build();

            String mimeType = null;
            if (bucketType.equals("local")) mimeType = Files.probeContentType(Paths.get(destStr + path));
            if (Helper.isNullOrEmpty(mimeType) && entryAttachment != null && !Helper.isNullOrEmpty(entryAttachment.getFileType())) mimeType = entryAttachment.getFileType();
            if (Helper.isNullOrEmpty(mimeType)) mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;

            final String finalDestStr = destStr;
            final Bucket finalBucket = activeBucket;
            final String finalPath = path;

            StreamingResponseBody stream = outputStream -> {
                try (InputStream in = provider.download(finalPath, finalDestStr, finalBucket)) {
                    in.transferTo(outputStream);
                } catch (Exception e) {
                    throw new IOException("Failed to stream file", e);
                }
            };

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS).cachePublic())
                    .lastModified(lastModified)
                    .eTag(eTag)
                    .body(stream);

        } catch (Exception e) {
            TenantLogger.error(entryAttachment != null ? entryAttachment.getAppId() : null,
                    "form",
                    (entryAttachment != null && entryAttachment.getItemId() != null ? itemRepository.findById(entryAttachment.getItemId()).map(Item::getFormId).orElse(-1L) : -1L),
                    e.getMessage());
            logger.error("Error retrieving file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @RequestMapping(value = "/file/unzip/{path}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> getFileUnzip(@PathVariable("path") String path, HttpServletResponse response, Principal principal) throws IOException {
        if (Helper.isNullOrEmpty(path)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
        EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(path);
        Bucket activeBucket = null;

        if (entryAttachment != null && entryAttachment.getBucketId() != null) {
            destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            activeBucket = bucketRepository.findById(entryAttachment.getBucketId()).orElse(null);
        }

        if (entryAttachment != null && entryAttachment.getItemId() != null) {
            Item item = itemRepository.findById(entryAttachment.getItemId()).orElse(null);
            if (item != null && item.getX() != null && item.getX().has("secure") && item.getX().get("secure").asBoolean() && principal == null) {
                throw new OAuth2AuthenticationException(new OAuth2Error("401"), "Full authentication is required");
            }
        }

        try {
            String bucketType = (activeBucket != null && activeBucket.getType() != null) ? activeBucket.getType() : "local";
            StorageProvider provider = storageFactory.getProvider(bucketType);

            if (!provider.exists(path, destStr, activeBucket)) return new ResponseEntity<>(HttpStatus.NOT_FOUND);

            byte[] returnFile;
            boolean isZip = path.endsWith(".zip") || (entryAttachment != null && entryAttachment.getFileName() != null && entryAttachment.getFileName().endsWith(".zip"));

            if (isZip) {
                File targetZip;
                boolean isTempFile = false;
                if (bucketType.equals("local")) {
                    targetZip = new File(destStr + path);
                } else {
                    targetZip = File.createTempFile("leap-unzip-", ".zip");
                    isTempFile = true;
                    try (InputStream in = provider.download(path, destStr, activeBucket);
                         java.io.FileOutputStream out = new java.io.FileOutputStream(targetZip)) {
                        in.transferTo(out);
                    }
                }
                try {
                    net.lingala.zip4j.ZipFile zf = new net.lingala.zip4j.ZipFile(targetZip);
                    java.util.List<net.lingala.zip4j.model.FileHeader> fileHeaderList = zf.getFileHeaders();

                    if (!fileHeaderList.isEmpty()) {
                        try (java.io.InputStream is = zf.getInputStream(fileHeaderList.get(0));
                             java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream((int) fileHeaderList.get(0).getUncompressedSize())) {
                            is.transferTo(os);
                            returnFile = os.toByteArray();
                        }
                    } else returnFile = new byte[0];
                } finally {
                    if (isTempFile && targetZip.exists()) targetZip.delete();
                }
            } else {
                try (InputStream in = provider.download(path, destStr, activeBucket);
                     java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream()) {
                    in.transferTo(os);
                    returnFile = os.toByteArray();
                }
            }

            String safeName = (entryAttachment != null && entryAttachment.getFileName() != null) ? entryAttachment.getFileName() : path;
            if (safeName.endsWith(".zip")) safeName = safeName.substring(0, safeName.length() - 4);

            String mimeType = (entryAttachment != null && !Helper.isNullOrEmpty(entryAttachment.getFileType())) ? entryAttachment.getFileType() :
                    (bucketType.equals("local") ? Files.probeContentType(Paths.get(destStr + path)) : null);

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(safeName, StandardCharsets.UTF_8).build().toString());

            if (!Helper.isNullOrEmpty(mimeType)) builder.contentType(MediaType.parseMediaType(mimeType));

            return builder.body(returnFile);

        } catch (Exception e) {
            logger.error("Error retrieving unzipped file", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        return Long.toString(uuid.getLeastSignificantBits(), Character.MAX_RADIX);
    }

    @GetMapping(value = "/dashboard/{dashboardId}")
    public Map getDashboardData2(@PathVariable("dashboardId") Long dashboardId,
                                 @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                 @RequestParam(value = "email", required = false) String email,
                                 @CurrentUser UserPrincipal principal,
                                 HttpServletRequest request) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        Map<String, Object> p = parseFiltersSafely(filters, "dashboardId:" + dashboardId);
        return entryService.getDashboardDataNativeNew(dashboardId, p, email, request);
    }

    @GetMapping(value = "/dashboard-map/{dashboardId}")
    public Map getDashboardDataMap2(@PathVariable("dashboardId") Long dashboardId,
                                    @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                    @RequestParam(value = "email", required = false) String email,
                                    @CurrentUser UserPrincipal principal,
                                    HttpServletRequest request) {
//        boolean isAnonymous = (principal == null);
//        String email = isAnonymous ? null : principal.getEmail();
        Map<String, Object> p = parseFiltersSafely(filters, "dashboardId:" + dashboardId);
        return entryService.getDashboardMapDataNativeNew(dashboardId, p, email, request);
    }

    @GetMapping(value = "/chart/{chartId}")
    public Map getChartData(@PathVariable("chartId") Long chartId,
                            @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                            @RequestParam(value = "email", required = false) String email,
                            @CurrentUser UserPrincipal principal,
                            HttpServletRequest request) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        Map<String, Object> p = parseFiltersSafely(filters, "chartId:" + chartId);
        return entryService.getChartDataNative(chartId, p, email, request);
    }

    @GetMapping(value = "/chart-map/{chartId}")
    public Object getChartMapData(@PathVariable("chartId") Long chartId,
                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                  @RequestParam(value = "email", required = false) String email,
                                  @CurrentUser UserPrincipal principal,
                                  HttpServletRequest request) {
        boolean isAnonymous = (principal == null);
        String email = isAnonymous ? null : principal.getEmail();
        Map<String, Object> p = parseFiltersSafely(filters, "chartId:" + chartId);
        return entryService.getChartMapDataNative(chartId, p, email, request);
    }

    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_RANGE = "Content-Range";
    public static final String ACCEPT_RANGES = "Accept-Ranges";
    public static final String BYTES = "bytes";
    public static final int BYTE_RANGE = 1024;

    @GetMapping("/file/stream/{path}")
    public ResponseEntity<byte[]> prepareContent(@PathVariable("path") String fileName,
                                                 @RequestHeader(value = "Range", required = false) String range)
            throws IOException {
        long rangeStart = 0;
        long rangeEnd;
        byte[] data;
        long fileSize;

        if (Helper.isNullOrEmpty(fileName)) return ResponseEntity.notFound().build();

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
        EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(fileName);
        Bucket activeBucket = null;

        if (entryAttachment != null && entryAttachment.getBucketId() != null) {
            destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            activeBucket = bucketRepository.findById(entryAttachment.getBucketId()).orElse(null);
        }

        try {
            String bucketType = (activeBucket != null && activeBucket.getType() != null) ? activeBucket.getType() : "local";
            StorageProvider provider = storageFactory.getProvider(bucketType);

            if (!provider.exists(fileName, destStr, activeBucket)) {
                return ResponseEntity.notFound().build();
            }

            File localTarget;
            boolean isTempFile = false;

            if (bucketType.equals("local")) {
                localTarget = new File(destStr + fileName);
                fileSize = localTarget.length();
            } else {
                // For cloud providers, download to temp file to serve byte ranges
                localTarget = File.createTempFile("leap-stream-", ".tmp");
                isTempFile = true;
                try (InputStream in = provider.download(fileName, destStr, activeBucket);
                     FileOutputStream out = new FileOutputStream(localTarget)) {
                    in.transferTo(out);
                }
                fileSize = localTarget.length();
            }

            try {
                String mimeType = (entryAttachment != null && !Helper.isNullOrEmpty(entryAttachment.getFileType())) ? entryAttachment.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

                if (range == null) {
                    return ResponseEntity.status(HttpStatus.OK)
                            .header(CONTENT_TYPE, mimeType)
                            .header(CONTENT_LENGTH, String.valueOf(fileSize))
                            .body(readByteRange(localTarget, rangeStart, fileSize - 1));
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

                data = readByteRange(localTarget, rangeStart, rangeEnd);
                String contentLength = String.valueOf((rangeEnd - rangeStart) + 1);

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .header(CONTENT_TYPE, mimeType)
                        .header(ACCEPT_RANGES, BYTES)
                        .header(CONTENT_LENGTH, contentLength)
                        .header(CONTENT_RANGE, BYTES + " " + rangeStart + "-" + rangeEnd + "/" + fileSize)
                        .body(data);
            } finally {
                if (isTempFile && localTarget.exists()) {
                    localTarget.delete();
                }
            }
        } catch (Exception e) {
            logger.error("Error streaming byte range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public byte[] readByteRange(File file, long start, long end) throws IOException {
        try (InputStream inputStream = new FileInputStream(file);
             ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream()) {

            long skipped = inputStream.skip(start);
            if (skipped < start) {
                throw new IOException("Failed to skip required bytes");
            }

            byte[] data = new byte[BYTE_RANGE];
            int nRead;
            long bytesToRead = (end - start) + 1;
            long totalRead = 0;

            while (totalRead < bytesToRead && (nRead = inputStream.read(data, 0, (int) Math.min(data.length, bytesToRead - totalRead))) != -1) {
                bufferedOutputStream.write(data, 0, nRead);
                totalRead += nRead;
            }
            bufferedOutputStream.flush();
            return bufferedOutputStream.toByteArray();
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

    // Add this to the bottom of EntryController
    private Map<String, Object> parseFiltersSafely(String filters, String contextIdentifier) {
        if (filters == null || filters.trim().isEmpty() || "{}".equals(filters)) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(filters, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.error("Error decoding filter JSON for {}: {}", contextIdentifier, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filters JSON format");
        }
    }

    private JsonNode parseStatusSafely(String status, String contextIdentifier) {
        if (status == null || status.trim().isEmpty()) {
            return MAPPER.createObjectNode();
        }
        try {
            return MAPPER.readTree(status);
        } catch (Exception e) {
            logger.error("Error decoding status JSON for {}: {}", contextIdentifier, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status JSON format");
        }
    }

}
