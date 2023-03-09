package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.EntryMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.repository.ItemRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.EntryService;
import com.benzourry.leap.config.Constant;
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
import java.util.concurrent.ExecutionException;
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

    @Autowired
    public EntryController(EntryService entryService, EntryAttachmentRepository entryAttachmentRepository, ItemRepository itemRepository) {
        this.entryService = entryService;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.itemRepository = itemRepository;
    }

//    @GetMapping("{id}")
//    public Entry findById(@PathVariable("id") long id){
//        return entryService.findById(id);
//    }

    /*
    * @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),

    })*/

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)
    })
    public Entry findById(@PathVariable("id") long id, @RequestParam("formId") long formId, Principal principal) {
        String name = principal == null ? null : principal.getName();
        return entryService.findById(id, formId, name == null);
    }

    @GetMapping("{id}/trails")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryApprovalTrail> findTrailById(@PathVariable("id") long id, @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
        return entryService.findTrailById(id, pageable);
    }

    @GetMapping("{id}/files")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryAttachment> findFilesById(@PathVariable("id") long id, @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
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
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
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
    public Entry save(@RequestParam long formId,
                      @RequestParam(required = false) Long prevId,
                      @RequestBody Entry entry,
                      @RequestParam String email) {
//        System.out.println("principal:"+principal);
        return entryService.save(formId, entry,prevId, email);
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
    public Entry saveField(@RequestParam long entryId,
                           @RequestBody JsonNode value, @RequestParam(required = false) String root, @RequestParam(required = false) Long appId) {
        return entryService.updateField(entryId, value, root, appId);
    }


    @PostMapping("{id}/undelete")
    public Map<String, Object> undelete(@PathVariable long id,
                                        @RequestParam("trailId") long trailId,
                                        Authentication authentication) {
        return entryService.undelete(id, trailId, authentication.getName());
    }


    @GetMapping("update-entry")
    @JsonResponse(mixins = {
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public void updateEntry(@RequestParam long formId) {
//        System.out.println("principal:"+principal);
        entryService.updateEntry(formId);
    }

//    @GetMapping
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public Page<Entry> findAll(@RequestParam("formId") Long formId,
//                               @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                               @RequestParam(value = "status", defaultValue = "1") List<String> status,
//                               @RequestParam("email") String email,
//                               Pageable pageable){
//        return entryService.findByEmail(formId, searchText,email, status, pageable);
//    }

//    @GetMapping("list/{type}")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public Page<Entry> findAll(@RequestParam("formId") Long formId,
//                               @PathVariable("type") String type,
//                               @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                               @RequestParam(value = "status", defaultValue = "drafted") List<String> status,
//                               @RequestParam("email") String email,
//                               @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                               Pageable pageable,
//                               HttpServletRequest request){
////        System.out.println(status);
//        ObjectMapper mapper = new ObjectMapper();
//        Map p = new HashMap();
//        try {
//            p = mapper.readValue(URLDecoder.decode(filters, "UTF-8"), Map.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return entryService.findList(type, formId, searchText,email, status, p, pageable, request);
//    }

    //    @GetMapping("list-all")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public List<JsonNode> findAll(@RequestParam("formId") Long formId,
//                                  @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                                  @RequestParam(value = "status", defaultValue = "drafted") List<String> status,
//                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                  Pageable pageable,
//                                  HttpServletRequest request){
////        System.out.println(status);
//        ObjectMapper mapper = new ObjectMapper();
//        Map p = new HashMap();
//        try {
//            p = mapper.readValue(URLDecoder.decode(filters, "UTF-8"), Map.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return entryService.findListData(formId, searchText, status, p, pageable, request);
//    }
    @GetMapping("list-data")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
    })
    public List<JsonNode> findUnboxed(@RequestParam("datasetId") Long datasetId,
                                      @RequestParam(value = "searchText", required = false) String searchText,
                                      @RequestParam(value = "email", required = false) String email,
                                      @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                      @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                      Pageable pageable,
                                      HttpServletRequest request) {
//        System.out.println(status);
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
//        Map s = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
//            s = mapper.readValue(URLDecoder.decode(status, "UTF-8"), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.findListByDatasetData(datasetId, searchText, email, p, pageable, request);
    }

    @GetMapping("list")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)

    })
    public Page<Entry> findAllByDatasetIdCheck(@RequestParam("datasetId") Long datasetId,
                                               @RequestParam(value = "searchText", required = false) String searchText,
                                               @RequestParam(value = "email", required = false) String email,
                                               @RequestParam(value = "sorts", required = false) List<String> sorts,
                                               @RequestParam(value = "ids", required = false) List<Long> ids,
                                               @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                               Pageable pageable,
                                               HttpServletRequest request, Principal principal) {
        ObjectMapper mapper = new ObjectMapper();
        String name = principal == null ? null : principal.getName();
        Map p = new HashMap();
//        Map s = new HashMap();
        try {
            p = mapper.readValue(filters, Map.class);
//            s = mapper.readValue(URLDecoder.decode(status, "UTF-8"), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.findListByDatasetCheck(datasetId, searchText, email, p, sorts, ids,name == null, pageable, request);
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
                                                       Pageable pageable,
                                                       HttpServletRequest request, Principal principal) {
        ObjectMapper mapper = new ObjectMapper();
        String name = principal == null ? null : principal.getName();
        Map p = new HashMap();
//        Map s = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
//            s = mapper.readValue(URLDecoder.decode(status, "UTF-8"), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.streamListByDatasetCheck(datasetId, searchText, email, p, sorts, ids,name == null, pageable, request);
    }

    @GetMapping("$list-private")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),
            @JsonMixin(target = User.class, mixin = EntryMixin.EntryListApprovalApprover.class)

    })
    public Page<Entry> findAllByDatasetId(@RequestParam("datasetId") Long datasetId,
                                          @RequestParam(value = "searchText", required = false) String searchText,
                                          @RequestParam(value = "email", required = false) String email,
                                          @RequestParam(value = "sorts", required = false) List<String> sorts,
                                          @RequestParam(value = "ids", required = false) List<Long> ids,
                                          @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                          @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                          Pageable pageable,
                                          HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
//        Map s = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
//            s = mapper.readValue(URLDecoder.decode(status, "UTF-8"), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.findListByDataset(datasetId, searchText, email, p, sorts,ids, pageable, request);
    }

    @GetMapping("count")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class),
            @JsonMixin(target = EntryApproval.class, mixin = EntryMixin.EntryListApproval.class),
            @JsonMixin(target = Section.class, mixin = EntryMixin.EntryListApprovalTierSection.class),

    })
    public Map<String, Object> countByDatasetId(@RequestParam("datasetId") Long datasetId,
                                                @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
                                                @RequestParam(value = "email", required = false) String email,
                                                @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                          @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                Pageable pageable,
                                                HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, Object> data = new HashMap<>();
        data.put("count", entryService.countByDataset(datasetId, searchText, email, p, request));
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
//                                                     @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                     @RequestBody EmailTemplate emailTemplate,
                                                     HttpServletRequest request) {
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
//        Map s = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
//            s = mapper.readValue(URLDecoder.decode(filters, "UTF-8"), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.blastEmailByDataset(datasetId, searchText, email, p, emailTemplate, ids,request);
    }


    @GetMapping("list/{type}/ngx-chart")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
    })
    public Page<Entry> findAllNgxChart(@RequestParam("formId") Long formId,
                                       @PathVariable("type") String type,
                                       @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
                                       @RequestParam(value = "status", defaultValue = "drafted") List<String> status,
                                       @RequestParam("email") String email,
                                       @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                       Pageable pageable,
                                       HttpServletRequest request) {
//        System.out.println(status);
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.findListNormalizedNgxChart(type, formId, searchText, email, status, p, pageable, request);
    }

    @GetMapping("list/{type}/ngx-chart-series")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
    })
    public Page<Entry> findAllNgxChartSeries(@RequestParam("formId") Long formId,
                                             @PathVariable("type") String type,
                                             @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
                                             @RequestParam(value = "status", defaultValue = "drafted") List<String> status,
                                             @RequestParam("email") String email,
                                             @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                             Pageable pageable,
                                             HttpServletRequest request) {
//        System.out.println(status);
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.findListNormalizedNgxChartSeries(type, formId, searchText, email, status, p, "month", pageable, request);
    }

//    @GetMapping("list/{type}/ngx-chart-series-flip")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public Page<Entry> findAllNgxChartSeriesFlip(@RequestParam("formId") Long formId,
//                               @PathVariable("type") String type,
//                               @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                               @RequestParam(value = "status", defaultValue = "drafted") List<String> status,
//                               @RequestParam("email") String email,
//                               @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                               Pageable pageable,
//                               HttpServletRequest request){
////        System.out.println(status);
//        ObjectMapper mapper = new ObjectMapper();
//        Map p = new HashMap();
//        try {
//            p = mapper.readValue(URLDecoder.decode(filters, "UTF-8"), Map.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return entryService.findListNormalizedNgxChartSeriesFlip(type, formId, searchText,email, status, p, "month", pageable, request);
//    }

    @GetMapping("list/{type}/ngx-echart-series")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
    })
    public Map findAllNgxEchartSeries(@RequestParam("datasetId") Long datasetId,
                                      @PathVariable("type") String type,
                                      @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                               @RequestParam(value = "status", defaultValue = "drafted") List<String> status,
                                      @RequestParam(value = "email", required = false) String email,
//                               @RequestParam("chartType") String chartType,
                                      @RequestParam("seriesCol") String seriesCol,
                                      @RequestParam(value = "exclude", required = false) List<String> exclude,
                                      @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                      Pageable pageable,
                                      HttpServletRequest request) {
//        System.out.println(status);
        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.findListNormalizedNgxEchartSeries(type, datasetId, searchText, email, p, seriesCol, exclude, pageable, request);
    }


//    @GetMapping("admin")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public Page<Entry> findAdmin(@RequestParam("formId") Long formId,
//                                 @RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                               @RequestParam(value = "status", defaultValue = "1") List<String> status,
//                               @RequestParam("email") String email,
//                               Pageable pageable){
//        return entryService.findAdminByEmail(formId, searchText,email, status, pageable);
//    }

    @PostMapping("{id}/delete")
    public Map<String, Object> deleteEntry(@PathVariable("id") Long id, Authentication authentication) {
        Map<String, Object> data = new HashMap<>();
        String name = authentication.getName();
        entryService.deleteEntry(id, name);
        return data;
    }

    @PostMapping("bulk/delete")
    public Map<String, Object> deleteEntries(@RequestParam("ids") List<Long> ids, Authentication authentication) {
        Map<String, Object> data = new HashMap<>();
        String name = authentication.getName();
        entryService.deleteEntries(ids, name);
        return data;
    }

    @PostMapping("{id}/submit")
    public Entry submit(@PathVariable Long id) {
        return entryService.submit(id);
    }

    @PostMapping("{id}/reset")
    public Entry reset(@PathVariable Long id) {
        return entryService.reset(id);
    }

    @PostMapping("{id}/update-approval")
    public Entry updateApproval(@PathVariable Long id,
                                @RequestParam("tierId") Long tierId,
                                @RequestBody EntryApproval entryApproval) {
        return entryService.updateApproval(tierId, id, entryApproval);
    }

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
    public Entry removeApproval(@PathVariable Long id,
                                @RequestParam("tierId") Long tierId) {
        return entryService.removeApproval(tierId, id);
    }

    @PostMapping("{id}/resubmit")
    public Entry resubmit(@PathVariable Long id) {
        return entryService.resubmit(id);
    }

    @PostMapping("{id}/retract")
    public Entry retract(@PathVariable Long id, @RequestParam("email") String email) {
        return entryService.retractApp(id, email);
    }

    @PostMapping("{id}/assign")
    public Entry assign(@PathVariable Long id, @RequestParam("tierId") Long tierId, @RequestParam("email") String email) throws Exception {
        return entryService.assignApprover(id, tierId, email);
    }

//    @PostMapping("{id}/action")
//    public Entry actionApp(@PathVariable Long id,
//                           @RequestBody EntryApproval gas,
//                           @RequestParam("email") String email) throws Exception {
////        Map<String, Object> data = new HashMap<>();
//        Entry grant = entryService.actionApp(id, gas, email);
//
////        data.put("message", "success");
////        data.put("grantApplication", grant);
//
//        return grant;
//    }

    @PostMapping("{id}/action")
    public Entry actionApp(@PathVariable Long id,
                           @RequestBody EntryApproval gas,
                           @RequestParam("email") String email) {
//        Map<String, Object> data = new HashMap<>();

        return entryService.actionApp(id, gas, email);
    }

    @PostMapping("bulk/action")
    public Map<String, Object> actionApp(@RequestParam List<Long> ids,
                           @RequestBody EntryApproval gas,
                           @RequestParam("email") String email) {
//        Map<String, Object> data = new HashMap<>();

        return entryService.actionApps(ids, gas, email);
    }

    @PostMapping("{id}/save-approval")
    public Entry saveApproval(@PathVariable Long id,
                              @RequestBody EntryApproval gas,
                              @RequestParam("email") String email) {
//        Map<String, Object> data = new HashMap<>();
        return entryService.saveApproval(id, gas, email);

//        data.put("message", "success");
//        data.put("grantApplication", grant);

//        return grant;
    }


//    @GetMapping("action-list")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public Page<Entry> actionList(@RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                                  @RequestParam("formId") Long formId,
//                                     @RequestParam("email") String email,
////                                     @RequestParam("group") List<String> group,
//                                     @RequestParam(value = "status", required = false, defaultValue = "resubmitted,submitted") List<String> status,
//                                     Pageable pageable) throws Exception {
//
//        return entryService.findActionByEmail(formId, searchText,email, status,pageable);
//    }


//    @GetMapping("action-archive-list")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Entry.class, mixin = EntryMixin.EntryList.class)
//    })
//    public Page<Entry> actionArchiveList(@RequestParam(value = "searchText", required = false, defaultValue = "") String searchText,
//                                  @RequestParam("formId") Long formId,
//                                     @RequestParam("email") String email,
//                                     @RequestParam(value = "status", required = false, defaultValue = "resubmitted,submitted") List<String> status,
//                                     Pageable pageable) throws Exception {
//
//        return entryService.getActionArchiveList(formId, searchText,email, status,pageable);
//    }


    @GetMapping("{appId}/start")
    public Map<String, Long> getStart(@PathVariable("appId") Long appId, @RequestParam("email") String email) {
        return this.entryService.getStart(appId, email);
    }

    @GetMapping("ef-exec")
    public CompletableFuture<Map<String, Object>> efExec(@RequestParam("formId") Long formId,
                                                         @RequestParam("field") String field,
                                                         @RequestParam(value = "force", defaultValue = "false") boolean force) {
//        Map<String, Object> data = new HashMap<>();
        return this.entryService.execVal(formId, field, force);
//        return this.entryService.execVal(formId, field, force);
//        return data;
    }


    @GetMapping("ef-exec2")
    public Object efExec2(@RequestParam("formId") Long formId,
                                                         @RequestParam("field") String field,
                                                         @RequestParam(value = "force", defaultValue = "false") boolean force) {

        return this.entryService.getStart(122L,"blmrazif@unimas.my");
    }

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
//        Map<String, String> details = (Map<String, String>) auth.getUserAuthentication().getDetails();
//        String username = details.get("email");

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]",""), StandardCharsets.UTF_8);


//        String random = Long.toString(UUID.randomUUID().getLessSignificantBits(), Character.MAX_RADIX);
        String filePath = userId + "_" + Instant.now().getEpochSecond() + "_" + originalFilename;
//        String filePath = Instant.now().getEpochSecond() + "_" + originalFilename;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        EntryAttachment attachment = new EntryAttachment();
        attachment.setFileName(originalFilename);
        attachment.setFileSize(fileSize);
        attachment.setFileType(contentType);
        attachment.setFileUrl(filePath);
        attachment.setEmail(principal.getEmail());
        attachment.setTimestamp(new Date());
        attachment.setMessage("success");
        attachment.setItemId(itemId);
        attachment.setAppId(appId);
//        attachment.setBucketId(bucketId);

        Item item = null;
        if (itemId != null || bucketId != null) {
            if (bucketId != null) {
                attachment.setBucketId(bucketId);
                destStr += "bucket-" + bucketId + "/";
            } else {
                item = itemRepository.getReferenceById(itemId);
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
                item = itemRepository.getReferenceById(itemId);
                attachment.setItemLabel(item.getLabel());
            }

        }
        attachment.setSuccess(true);

        File dir = new File(destStr);
        dir.mkdirs();


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
            System.out.println("no zip");
            File dest = new File(destStr + filePath);

            try {
                file.transferTo(dest);
            } catch (IllegalStateException e) {
                attachment.setMessage("failed");
                attachment.setSuccess(false);
            }
        }

//        File dest = new File(destStr + filePath);
//
//        try {
//            file.transferTo(dest);
//        } catch (IllegalStateException e) {
//            attachment.setMessage("failed");
//            attachment.setSuccess(false);
//        }

        return entryAttachmentRepository.save(attachment);
    }


    @PostMapping(value = "{id}/link-files")
    @Transactional
    public Map<String, Object> linkFiles(@PathVariable("id") Long id,
                                         @RequestBody List<String> files,
                                         HttpServletRequest request) {

        // Date dateNow = new Date();
        Map<String, Object> data = new HashMap<>();

        List<EntryAttachment> eaList = files.stream().map(f -> {
//            System.out.println(f);
            EntryAttachment ea = entryAttachmentRepository.findByFileUrl(f);
            ea.setEntryId(id);
            return ea;
        }).collect(Collectors.toList());

        entryAttachmentRepository.saveAll(eaList);

//        if (!Helper.isNullOrEmpty(fileUrl)) {
//            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(fileUrl);
//
//            String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
//
//            if (entryAttachment.getBucketId()!=null){
//                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
//            }
//
//
//            File dir = new File(destStr);
//            dir.mkdirs();
//
//            File dest = new File( destStr + fileUrl);
//            data.put("success", dest.delete());
//
//
//            entryAttachmentRepository.delete(entryAttachment);
//        }else{
//            data.put("success", false);
//            data.put("message", "Empty fileUrl");
//        }
        return data;
    }

    @PostMapping(value = "delete-file")
    public Map<String, Object> deleteFile(@RequestParam("fileUrl") List<String> fileUrl,
                                          Principal principal,
                                          HttpServletRequest request) {

        // Date dateNow = new Date();
        Map<String, Object> data = new HashMap<>();

        fileUrl.forEach(file -> {
            Map<String, Object> mdata = new HashMap<>();
            if (!Helper.isNullOrEmpty(file)) {
                EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(file);

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
                mdata.put("message", "Empty fileUrl");
            }
            data.put(file, mdata);
        });


        return data;
    }

//    @RequestMapping(value="file/{path}")
//    public FileSystemResource getFlowChartFile(@PathVariable("path") String path, HttpServletResponse response){
////        GrantApp g = grantAppService.getGrantApp(id);
//ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
//          .filename(path)
//                .build();
//        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
//        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
//
//      //  response.setHeader("Content-Disposition", "attachment; filename=\""+path+"\"");
//        return new FileSystemResource(Constant.UPLOAD_ROOT_DIR +"/attachment/" +path);
//    }

    @RequestMapping(value = "file/{path}")
    public ResponseEntity<StreamingResponseBody> getFileEntity(@PathVariable("path") String path, HttpServletResponse response, Principal principal) throws IOException {
//        GrantApp g = grantAppService.getGrantApp(id);

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (!Helper.isNullOrEmpty(path)) {
            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(path);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            if (entryAttachment != null && entryAttachment.getItemId() != null) {
                Item item = itemRepository.getReferenceById(entryAttachment.getItemId());
                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal==null) {
                    // is private
                        // ERROR 401
                        throw new OAuth2AuthenticationException(new OAuth2Error("401"),"Full authentication is required to access this resource");
                }
            }

            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(path)
                    .build();

            File file = new File(destStr + path);

            if (file.isFile()) {

                String mimeType = Files.probeContentType(file.toPath());

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

                if (!Helper.isNullOrEmpty(mimeType)) {
                    builder.contentType(MediaType.parseMediaType(mimeType));
                } else {
                    if (entryAttachment != null && Helper.isNullOrEmpty(entryAttachment.getFileType())) {
                        builder.contentType(MediaType.parseMediaType(entryAttachment.getFileType()));
                    }
                }
//                return builder.body(Files.readAllBytes(file.toPath()));
                return builder.body(outputStream -> Files.copy(file.toPath(), outputStream));
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }


        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }


    }

//    @RequestMapping(value = "file/inline/{path}")
//    public ResponseEntity<byte[]> getFileInline(@PathVariable("path") String path, HttpServletResponse response,Principal principal) throws IOException {
//
//        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
//
//        if (!Helper.isNullOrEmpty(path)) {
//            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(path);
//            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
//                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
//            }
//
//            if (entryAttachment != null && entryAttachment.getItemId() != null) {
//                Item item = itemRepository.getReferenceById(entryAttachment.getItemId());
//                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal==null) {
//                    // is private
//                    // ERROR 401
//                    throw new OAuth2AuthenticationException(new OAuth2Error("401"),"Full authentication is required to access this resource");
//                }
//            }
//
//
//            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
//                    .build();
//
//            File file = new File(destStr + path);
//
//            if (file.isFile()) {
//
//                String mimeType = Files.probeContentType(file.toPath());
//
//                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
//                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
//
//                if (!Helper.isNullOrEmpty(mimeType)) {
//                    builder.contentType(MediaType.parseMediaType(mimeType));
//                } else {
//                    if (entryAttachment != null && Helper.isNullOrEmpty(entryAttachment.getFileType())) {
//                        builder.contentType(MediaType.parseMediaType(entryAttachment.getFileType()));
//                    }
//                }
//
//                return builder
//                        .body(Files.readAllBytes(file.toPath()));
//            } else {
//                return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
//            }
//        } else {
//            return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
//        }
//    }

    @RequestMapping(value = "file/inline/{path}")
    public ResponseEntity<StreamingResponseBody> getFileInline(@PathVariable("path") String path, HttpServletResponse response, Principal principal) throws IOException {

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (!Helper.isNullOrEmpty(path)) {
            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(path);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            if (entryAttachment != null && entryAttachment.getItemId() != null) {
                Item item = itemRepository.getReferenceById(entryAttachment.getItemId());
                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal==null) {
                    // is private
                    // ERROR 401
                    throw new OAuth2AuthenticationException(new OAuth2Error("401"),"Full authentication is required to access this resource");
                }
            }


            ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                    .build();

            File file = new File(destStr + path);

            if (file.isFile()) {

                String mimeType = Files.probeContentType(file.toPath());

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

                if (!Helper.isNullOrEmpty(mimeType)) {
                    builder.contentType(MediaType.parseMediaType(mimeType));
                } else {
                    if (entryAttachment != null && Helper.isNullOrEmpty(entryAttachment.getFileType())) {
                        builder.contentType(MediaType.parseMediaType(entryAttachment.getFileType()));
                    }
                }

                return builder
                        .body(outputStream-> Files.copy(file.toPath(), outputStream));
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }
    }

    @RequestMapping(value = "file/unzip/{path}")
    public ResponseEntity<byte[]> getFileUnzip(@PathVariable("path") String path, HttpServletResponse response, Principal principal) throws IOException {

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        String mimeType;

        EntryAttachment entryAttachment = null;

        if (!Helper.isNullOrEmpty(path)) {
            entryAttachment = entryAttachmentRepository.findByFileUrl(path);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            if (entryAttachment != null && entryAttachment.getItemId() != null) {
                Item item = itemRepository.getReferenceById(entryAttachment.getItemId());
                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal==null) {
                    // is private
                    // ERROR 401
                    throw new OAuth2AuthenticationException(new OAuth2Error("401"),"Full authentication is required to access this resource");
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
//                String tempFile = Constant.UPLOAD_ROOT_DIR + "/temp-unzip/"+ Instant.now().toEpochMilli();
//                File destDir = new File(tempFile);
//                destDir.mkdirs();
//
//                new ZipFile(file)
//                        .extractAll(tempFile);
//                File f = new File(tempFile);
//                File df = f.listFiles()[0];

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
//                for (int i = 0; i < fileHeaderList.size(); i++) {
//                    FileHeader fileHeader = fileHeaderList.get(i);
//                    ZipInputStream is = zf.getInputStream(fileHeader);
//                    int uncompressedSize = (int) fileHeader.getUncompressedSize();
//                    OutputStream os = new ByteArrayOutputStream(uncompressedSize);
//                    int bytesRead;
//                    byte[] buffer = new byte[4096];
//                    while ((bytesRead = is.read(buffer)) != -1) {
//                        os.write(buffer, 0, bytesRead);
//                    }
//                    byte[] uncompressedBytes = ((ByteArrayOutputStream) os).toByteArray();
//                    inMemoryFiles.put(fileHeader.getFileName(), new ByteArrayInputStream(uncompressedBytes));
//                    is.close();
//                }

//                returnFile = Files.readAllBytes(df.toPath());
                } else {
                    returnFile = Files.readAllBytes(file.toPath());
                }

//        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());
//        response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
                ContentDisposition contentDisposition = ContentDisposition.builder("inline")
                        .filename(path.substring(0, path.length() - 4))
                        .build();

                ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
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


//    public byte[] unzip(String fileZip) throws IOException {
////        String fileZip = "src/main/resources/unzipTest/compressed.zip";
//        String tempFile = Constant.UPLOAD_ROOT_DIR + "/temp-unzip/"+ Instant.now().toEpochMilli();
//        File destDir = new File(tempFile);
//        destDir.mkdirs();
//
//        byte[] buffer = new byte[1024];
//        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
//        ZipEntry zipEntry = zis.getNextEntry();
//        while (zipEntry != null) {
//            File newFile = newFile(destDir, zipEntry);
//            if (zipEntry.isDirectory()) {
//                if (!newFile.isDirectory() && !newFile.mkdirs()) {
//                    throw new IOException("Failed to create directory " + newFile);
//                }
//            } else {
//                // fix for Windows-created archives
//                File parent = newFile.getParentFile();
//                if (!parent.isDirectory() && !parent.mkdirs()) {
//                    throw new IOException("Failed to create directory " + parent);
//                }
//
//                // write file content
//                FileOutputStream fos = new FileOutputStream(newFile);
//                int len;
//                while ((len = zis.read(buffer)) > 0) {
//                    fos.write(buffer, 0, len);
//                }
//                fos.close();
//            }
//            zipEntry = zis.getNextEntry();
//        }
//        zis.closeEntry();
//        zis.close();
//        return buffer;
//    }
//
//    public static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
//        File destFile = new File(destinationDir, zipEntry.getName());
//
//        String destDirPath = destinationDir.getCanonicalPath();
//        String destFilePath = destFile.getCanonicalPath();
//
//        if (!destFilePath.startsWith(destDirPath + File.separator)) {
//            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
//        }
//
//        return destFile;
//    }

    public static String shortUUID() {
        UUID uuid = UUID.randomUUID();
        return Long.toString(uuid.getLeastSignificantBits(), Character.MAX_RADIX);
    }

//    @GetMapping(value = "dashboard2/{dashboardId}")
//    public Map getDashboardData(@PathVariable Long dashboardId) {
//        return entryService.getDashboardData(dashboardId);
//    }


    @GetMapping(value = "dashboard/{dashboardId}")
    public Map getDashboardData2(@PathVariable Long dashboardId,
                                 @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                 @RequestParam(value = "email", required = false) String email,
                                 HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.getDashboardDataNativeNew(dashboardId, p, email, request);
    }

    @GetMapping(value = "dashboard-map/{dashboardId}")
    public Map getDashboardDataMap2(@PathVariable Long dashboardId,
                                    @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                    @RequestParam(value = "email", required = false) String email,
                                    HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.getDashboardMapDataNativeNew(dashboardId, p, email, request);
    }

    @GetMapping(value = "chart/{chartId}")
    public Map getChartData(@PathVariable Long chartId,
                            @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                            @RequestParam(value = "email", required = false) String email,
                            HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.getChartDataNative(chartId, p, email, request);
    }

    @GetMapping(value = "chart-map/{chartId}")
    public Object getChartMapData(@PathVariable Long chartId,
                               @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                               @RequestParam(value = "email", required = false) String email,
                               HttpServletRequest request) {


        ObjectMapper mapper = new ObjectMapper();
        Map p = new HashMap();
        try {
            p = mapper.readValue(URLDecoder.decode(filters, StandardCharsets.UTF_8), Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return entryService.getChartMapDataNative(chartId, p, email, request);
    }


//    @GetMapping(value = "chart2/{chartId}")
//    public Map getChartData02(@PathVariable Long chartId,
//                              @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                              HttpServletRequest req) {
//
//
//        ObjectMapper mapper = new ObjectMapper();
//        Map p = new HashMap();
//        try {
//            p = mapper.readValue(URLDecoder.decode(filters, "UTF-8"), Map.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return entryService.getChartDataNative2(chartId, p, req);
//    }

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
            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(fileName);
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


//    public byte[] readByteRange(String filename, long start, long end) throws IOException {
//
//        FileInputStream inputStream = new FileInputStream(filename);
//        ByteArrayOutputStream bufferedOutputStream = new ByteArrayOutputStream();
//        byte[] data = new byte[1024];
//        int nRead;
//        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
//            bufferedOutputStream.write(data, 0, nRead);
//        }
//        bufferedOutputStream.flush();
//        byte[] result = new byte[(int) (end - start)];
//        System.arraycopy(bufferedOutputStream.toByteArray(), (int) start, result, 0, (int) (end - start));
//        return result;
//    }

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
}
