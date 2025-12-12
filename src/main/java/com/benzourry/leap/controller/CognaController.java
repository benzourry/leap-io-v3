package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.mixin.CognaMixin;
import com.benzourry.leap.mixin.DatasetMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.repository.ItemRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.CognaService;
import com.benzourry.leap.service.EntryService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.supercsv.cellprocessor.FmtDate;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.benzourry.leap.utility.Helper.writeWithCsvBeanWriter;

@RestController
@RequestMapping({"api/cogna"})
//@CrossOrigin(allowCredentials="true")
public class CognaController {

    private static final Logger logger = LoggerFactory.getLogger(CognaController.class);
    public final CognaService cognaService;

    public final EntryAttachmentRepository entryAttachmentRepository;

    public final ItemRepository itemRepository;

    public CognaController(CognaService cognaService,
                           EntryAttachmentRepository entryAttachmentRepository,
                           ItemRepository itemRepository){
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.cognaService = cognaService;
        this.itemRepository = itemRepository;
    }


    /** ## SCREEN **/
    @PostMapping
    public Cogna saveCogna(@RequestParam("appId") long appId,
                           @RequestBody Cogna cogna,
                           @RequestParam("email") String email,Principal principal){
        if (cogna.getId() != null && !cogna.getEmail().contains(principal.getName())){
            throw new AuthorizationServiceException("Unauthorized modification by non-creator :" + principal.getName());
        }
        return cognaService.saveCogna(appId, cogna, email);
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> removeCogna(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        cognaService.removeCogna(id);
        return data;
    }

    @GetMapping
    @JsonResponse(mixins = {
//            @JsonMixin(target = Cogna.class, mixin = CognaMixin.CognaBasicList.class),
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class),
            @JsonMixin(target = Cogna.class, mixin = CognaMixin.CognaBasicList.class)
    })
    public Page<Cogna> getCognaList(@RequestParam("appId") long appId, Pageable pageable){
        return cognaService.findByAppId(appId, pageable);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
//            @JsonMixin(target = Cogna.class, mixin = CognaMixin.CognaOne.class),
//            @JsonMixin(target = Dataset.class, mixin = CognaMixin.CognaOneDataset.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class)
    })
    public Cogna getCogna(@PathVariable("id") long id){
        return cognaService.getCogna(id);
    }


    @PostMapping("{id}/src")
    public CognaSource addCognaSrc(@PathVariable("id") long id, @RequestBody CognaSource cognaSrc){
        return cognaService.addCognaSrc(id, cognaSrc);
    }

    @PostMapping("{id}/tool")
    public CognaTool addCognaSrc(@PathVariable("id") long id, @RequestBody CognaTool cognaTool){
        return cognaService.addCognaTool(id, cognaTool);
    }

    @PostMapping("{id}/mcp")
    public CognaMcp addCognaMcp(@PathVariable("id") long id, @RequestBody CognaMcp cognaMcp){
        return cognaService.addCognaMcp(id, cognaMcp);
    }

    @PostMapping("{id}/sub")
    public CognaSub addCognaSub(@PathVariable("id") long id, @RequestBody CognaSub cognaSub){
        return cognaService.addCognaSub(id, cognaSub);
    }

    @PostMapping("delete-src/{id}")
    public Map<String, Object> removeCognaSrc(@PathVariable("id") long id){
        return cognaService.removeCognaSrc(id);
    }

    @PostMapping("delete-tool/{id}")
    public Map<String, Object> removeCognaTool(@PathVariable("id") long id){
        return cognaService.removeCognaTool(id);
    }


    @PostMapping("delete-mcp/{id}")
    public Map<String, Object> removeCognaMcp(@PathVariable("id") long id){
        return cognaService.removeCognaMcp(id);
    }

    @PostMapping("delete-sub/{id}")
    public Map<String, Object> removeCognaSub(@PathVariable("id") long id){
        return cognaService.removeCognaSub(id);
    }


    /*
    * Spring Boot 3 ada issue utk authenticate Async/CompletableFuture response.
    * Terpaksa tambah .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll() dalam SecurityFilterConfig
    * */
    @PostMapping("{id}/ingest")
    public Map<Long, Map> runIngest(@PathVariable("id") Long id,
                                    HttpServletRequest req,
                                    HttpServletResponse res,
                                    @CurrentUser UserPrincipal userPrincipal){
        return cognaService.ingest(id, null,userPrincipal);
    }

    /*
    * Spring Boot 3 ada issue utk authenticate Async/CompletableFuture response.
    * Terpaksa tambah .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll() dalam SecurityFilterConfig
    * */
    @PostMapping("ingest-src/{id}")
    public Map<String, Object> runIngestSrc(@PathVariable("id") Long id,
                                            HttpServletRequest req,
                                            HttpServletResponse res,
                                            @CurrentUser UserPrincipal userPrincipal){
        return cognaService.ingestSrc(id, null,userPrincipal);
    }

    @PostMapping("{id}/clear-db")
    public CompletableFuture<Map<String, Object>> clearDb(@PathVariable("id") Long id,
                                                          @CurrentUser UserPrincipal userPrincipal) {
        return cognaService.clearDb(id);
    }
    @PostMapping("clear-db-src/{id}")
    public CompletableFuture<Map<String, Object>> clearDbBySrc(@PathVariable("id") Long id,
                                                          @CurrentUser UserPrincipal userPrincipal) {
        return cognaService.clearDbBySource(id);
    }

    @PostMapping("{id}/clear")
    public CompletableFuture<Map<String, Object>> clearById(@PathVariable("id") Long id,
                                                            @CurrentUser UserPrincipal userPrincipal) {
        return cognaService.clearById(id);
    }

    @PostMapping("{id}/reinit")
    public CompletableFuture<Map<String, Object>> reinitById(@PathVariable("id") Long id,
                                                            @CurrentUser UserPrincipal userPrincipal) {
        return cognaService.reinitCogna(id);
    }


    @PostMapping("{id}/clear-by-email")
    public CompletableFuture<Map<String, Object>> clearByIdAndEmail(@PathVariable("id") Long id,
                                                            @RequestParam("email") String email,
                                                            @CurrentUser UserPrincipal userPrincipal) {
        return cognaService.clearByIdAndEmail(id, email);
    }

    @GetMapping("{id}/search-db")
    public CompletableFuture<List<Map<String, Object>>> searchDb(@PathVariable("id") Long id,
                                                                  @RequestParam("search") String search,
                                                                  @RequestParam("maxResult") Integer maxResult,
                                                                  @RequestParam(value = "minScore", required = false) Double minScore,
                                                                  @CurrentUser UserPrincipal userPrincipal) {
        return cognaService.findSimilarity(id,search,maxResult,minScore);
    }

    @PostMapping("{id}/prompt")
    public CompletableFuture<Map<String, Object>> runPrompt(@PathVariable("id") Long id,
                                                            @RequestBody CognaService.PromptObj promptObj,
                                                            @CurrentUser UserPrincipal userPrincipal) throws Exception {
        return cognaService.prompt(id, promptObj,null, promptObj.email());
    }

    @PostMapping("{id}/prompt-stream")
    public CompletableFuture<ResponseEntity<ResponseBodyEmitter>> streamPrompt(@PathVariable("id") Long id,
                                                                                 @RequestBody CognaService.PromptObj promptObj,
                                                                                 @CurrentUser UserPrincipal userPrincipal) throws Exception {
        // Terpaksa pake sseemitter, StreamingResponseBody not working here. More study and inspection is needed
        ResponseBodyEmitter emitter = new ResponseBodyEmitter ();
        cognaService.prompt(id, promptObj,emitter,promptObj.email());
        return CompletableFuture.completedFuture(new ResponseEntity(emitter, HttpStatus.OK));
    }

//    @Async
    @GetMapping("{id}/prompt-stream")
    public CompletableFuture<ResponseEntity<ResponseBodyEmitter>> streamPrompt(@PathVariable("id") Long id,
                                                                               @RequestParam("email") String email,
                                                                               @RequestParam("prompt") String promptStr,
                                                                               @RequestParam(value = "base64Image", required = false) String base64Image,
                                                                               @RequestParam(value = "mimeType", required = false) String mimeType,
                                                                               @CurrentUser UserPrincipal userPrincipal) throws Exception{
        //        Terpaksa pake sseemitter, StreamingResponseBody not working here. More study and inspection is needed
        ResponseBodyEmitter emitter = new ResponseBodyEmitter ();
        cognaService.prompt(id, new CognaService.PromptObj(promptStr,null,email,Map.of(),true),emitter,email);
        return CompletableFuture.completedFuture(new ResponseEntity(emitter, HttpStatus.OK));
    }

    @GetMapping("get-formatter/{formId}")
    public Map<String, String> extract(@PathVariable("formId") Long formId,
                                       @RequestParam(defaultValue = "true") boolean asSchema) throws Exception{

        return cognaService.getFormatter(formId,asSchema);
    }

    @PostMapping("{id}/extract")
    public CompletableFuture<List<JsonNode>> extract(@PathVariable("id") Long id,
                                  @RequestBody CognaService.ExtractObj extractObj,
                                  @CurrentUser UserPrincipal userPrincipal) throws Exception{

        return cognaService.extract(id, extractObj);
    }

    @PostMapping("{id}/imgcls")
    public CompletableFuture<Map> imgcls(@PathVariable("id") Long id,
                                                                    @RequestBody CognaService.ExtractObj extractObj,
                                                                    @CurrentUser UserPrincipal userPrincipal) throws Exception{

        return cognaService.imgcls(id, extractObj);
    }

    @PostMapping("{id}/classify")
    public CompletableFuture<Map<String, Object>> classify(@PathVariable("id") Long id,
                                  @RequestBody CognaService.ExtractObj extractObj,
                                  @RequestParam(value = "lookupId", required = false) Long lookupId,
                                  @RequestParam(value = "what", required = false) String what,
                                  @RequestParam(value = "minScore", required = false) Double minScore,
                                  @RequestParam(value = "multiple", defaultValue = "false") boolean multiple,
                                  @CurrentUser UserPrincipal userPrincipal) throws Exception{

        return cognaService.classify(id, extractObj, lookupId, what, minScore, multiple);
    }

    @PostMapping("classify-field")
    public CompletableFuture<Map<String, Object>> classifyField(@RequestParam("itemId") Long id,
                                  @RequestBody CognaService.ExtractObj extractObj,
                                  @CurrentUser UserPrincipal userPrincipal) throws Exception{
        return cognaService.classifyField(id, extractObj);
    }

    @PostMapping("txtgen-field/{action}")
    public CompletableFuture<Map<String, Object>> txtgen(@RequestParam("itemId") Long id,
                                @PathVariable("action") String action,
                                  @RequestBody CognaService.ExtractObj extractObj,
                                  @CurrentUser UserPrincipal userPrincipal) throws Exception{
        return cognaService.txtgenField(id, extractObj, action);
    }

    @PostMapping("imggen-field")
    public CompletableFuture<Map<String, Object>> imggenField(@RequestParam("itemId") Long itemId,
                                  @RequestBody CognaService.ExtractObj extractObj,
                                  @CurrentUser UserPrincipal userPrincipal) throws Exception{

        return cognaService.imggenField(itemId, extractObj);
    }
    @PostMapping("{id}/imggen")
    public CompletableFuture<Map<String, Object>> imggen(@PathVariable("id") Long id,
                                  @RequestBody CognaService.ExtractObj extractObj,
                                  @CurrentUser UserPrincipal userPrincipal) throws Exception{

        return cognaService.imggen(id, extractObj);
    }


    /*
    * Spring Boot 3 ada issue utk authenticate Async/CompletableFuture response.
    * Terpaksa tambah .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll() dalam SecurityFilterConfig
    * */

    @GetMapping("check-by-code")
    public boolean check(@RequestParam(value = "code") String code) {
//        Map<String, Object> data = new HashMap<>();
//        data.put("exist",this.appService.checkByKey(appPath));
        return this.cognaService.checkByCode(code);
    }


    @RequestMapping(value = "{id}/ingested-file/{srcId}")
    public ResponseEntity<StreamingResponseBody> getIngestedFile(@PathVariable("id") Long id,
                                                                 @PathVariable("srcId") Long srcId,
                                                                 HttpServletResponse response,
                                                                 Principal principal) throws IOException {
       return cognaService.getSrcIngestedFile(id, srcId);

    }

    @PostMapping("{id}/upload-file")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @PathVariable("id") Long cognaId) throws IOException {

        // Fast filename sanitization (no regex)
        String originalName = file.getOriginalFilename();
        String sanitized = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", ""), StandardCharsets.UTF_8);

        String random = UUID.randomUUID().toString();
        String filePath = random + "_" + sanitized;

        Path destDir = Paths.get(Constant.UPLOAD_ROOT_DIR, "attachment", "cogna-" + cognaId);
        Files.createDirectories(destDir);

        Path destFile = destDir.resolve(filePath).normalize();

        // Security: prevent path traversal
        if (!destFile.startsWith(destDir)) {
            return ResponseEntity.badRequest().body(
                    Map.of("success", false, "message", "Invalid file path")
            );
        }

        // Stream copy (more memory-efficient than transferTo)
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destFile, StandardCopyOption.REPLACE_EXISTING);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("fileName", sanitized);
        data.put("fileSize", file.getSize());
        data.put("fileType", file.getContentType());
        data.put("fileUrl", Constant.IO_BASE_DOMAIN + "/api/cogna/" + cognaId + "/file/" + filePath);
        data.put("filePath", filePath);
        data.put("timestamp", System.currentTimeMillis());
        data.put("success", true);
        data.put("message", "success");

        return ResponseEntity.ok(data);
    }

    @PostMapping(value = "{id}/ocr")
    public Map<String, Object> ocr(@RequestParam("file") MultipartFile file,
                                   @PathVariable("id") Long cognaId,
                                   @RequestParam(value = "lang", defaultValue = "eng") String lang,
                                   HttpServletRequest request) throws Exception {

        // Date dateNow = new Date();
        Map<String, Object> data = new HashMap<>();

//        String username = principal.getName();
//        Long userId = principal.getId();
//        Map<String, String> details = (Map<String, String>) auth.getUserAuthentication().getDetails();
//        String username = details.get("email");

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", ""), StandardCharsets.UTF_8);


        String random = UUID.randomUUID().toString();
        String filePath =random + "_" + originalFilename;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/";


        // only to make folder
        File dir = new File(destStr);
        dir.mkdirs();


        File dest = new File(destStr + filePath);

        Helper.ocr(destStr + filePath,"eng");

        data.put("fileName", originalFilename);
        data.put("fileSize", fileSize);
        data.put("fileType", contentType);
        data.put("fileUrl", Constant.IO_BASE_DOMAIN+"/api/cogna/"+cognaId+"/file/"+filePath);
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

        return data;
    }



    @RequestMapping(value = "ocr/{path}")
    public ResponseEntity<String> getFileEntity(@PathVariable("path") String path,
                                                               @RequestParam(value = "lang", defaultValue = "eng") String lang,
                                                               HttpServletResponse response, Principal principal) throws IOException {
        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        if (path.startsWith("lookup")){
            path = path.replaceAll("~","/");
        }

        if (!Helper.isNullOrEmpty(path)) {
            EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(path);
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
            }

            if (entryAttachment != null && entryAttachment.getItemId() != null) {
                Item item = itemRepository.findById(entryAttachment.getItemId()).orElse(null);
                if (item != null && item.getX() != null && item.getX().get("secure") != null && item.getX().get("secure").asBoolean() && principal==null) {
                    // is private
                    // ERROR 401
                    throw new OAuth2AuthenticationException(new OAuth2Error("401"),"Full authentication is required to access this resource");
                }
            }

            File file = new File(destStr + path);

            if (file.isFile()) {
                String text = Helper.ocr(destStr + path,"eng");
                return new ResponseEntity<>(text, HttpStatus.OK);
            } else {
                return new ResponseEntity(HttpStatus.NOT_FOUND);
            }


        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }


    }
    @GetMapping(value = "{id}/ocr")
    public Map<String, Object> ocr(@RequestParam("file") MultipartFile file,
                                   @PathVariable("id") Long cognaId,
                                   HttpServletRequest request) throws Exception {

        Map<String, Object> data = new HashMap<>();

//        String username = principal.getName();
//        Long userId = principal.getId();
//        Map<String, String> details = (Map<String, String>) auth.getUserAuthentication().getDetails();
//        String username = details.get("email");

        long fileSize = file.getSize();
        String contentType = file.getContentType();
        String originalFilename = URLEncoder.encode(file.getOriginalFilename().replaceAll("[^a-zA-Z0-9.]", ""), StandardCharsets.UTF_8);


        String random = UUID.randomUUID().toString();
        String filePath =random + "_" + originalFilename;

        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/";


        // only to make folder
        File dir = new File(destStr);
        dir.mkdirs();


        File dest = new File(destStr + filePath);

        String text = Helper.ocr(destStr + filePath,"eng");

        data.put("fileName", originalFilename);
        data.put("fileSize", fileSize);
        data.put("fileType", contentType);
        data.put("fileUrl", Constant.IO_BASE_DOMAIN+"/api/cogna/"+cognaId+"/file/"+filePath);
        data.put("filePath", filePath);
        data.put("text", text);
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

    @RequestMapping(value = "{id}/file/{path}")
    public ResponseEntity<StreamingResponseBody> getFileEntity(@PathVariable("id") Long cognaId,
                                                               @PathVariable("path") String path,
                                                               HttpServletResponse response, Principal principal) throws IOException {
        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + cognaId + "/";

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

    @GetMapping("{id}/history")
    public Page<CognaPromptHistory> getLog(@PathVariable("id") Long cognaId,
                                           @RequestParam(value = "email", required = false) String email,
                                           @RequestParam(value = "type", required = false) String type,
                                           @RequestParam(value = "searchText", defaultValue = "") String searchText,
                                           Pageable pageable){
        return cognaService.getHistory(cognaId, type, searchText, email, pageable);
    }

    @GetMapping("{id}/export-log-csv")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> getLogCsv(@PathVariable("id") Long cognaId,HttpServletResponse response){

        StreamingResponseBody stream = out -> {
            Page<CognaPromptHistory> pageHistory = cognaService.getHistory(cognaId, null, "", null, Pageable.unpaged());
            writeWithCsvBeanWriter(new OutputStreamWriter(out),pageHistory.toList(),
            new CellProcessor[] {
                    new NotNull(), // id
                    new NotNull(), // email
                    new NotNull(), // text
                    new NotNull(), // response
                    new NotNull(), // type
                    new FmtDate("hh:mm dd/MM/yyyy"), // timestamp
                    new NotNull() // cognaId
            },new String[] { "id", "email",
                            "text", "response","type","timestamp","cognaId"
            });
        };

        response.setContentType("text/csv");
        response.setHeader("Content-disposition", "attachment; filename=cogna-"+cognaId+"-log-" + Instant.now().getEpochSecond() + ".csv");

        return CompletableFuture.completedFuture(new ResponseEntity(stream, HttpStatus.OK));
    }


    @GetMapping("export-log-csv")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> getLogCsv(HttpServletResponse response){

        StreamingResponseBody stream = out -> {
            Page<CognaPromptHistory> pageHistory = cognaService.getHistory(null, null, "", null, Pageable.unpaged());
            writeWithCsvBeanWriter(new OutputStreamWriter(out),pageHistory.toList(),
            new CellProcessor[] {
                    new NotNull(), // id
                    new NotNull(), // email
                    new NotNull(), // text
                    new NotNull(), // response
                    new NotNull(), // type
                    new FmtDate("hh:mm dd/MM/yyyy"), // timestamp
                    new NotNull() // cognaId
            },new String[] { "id", "email", "text", "response","type","timestamp","cognaId"
            });
        };

        response.setContentType("text/csv");
        response.setHeader("Content-disposition", "attachment; filename=cogna-log-" + Instant.now().getEpochSecond() + ".csv");

        return CompletableFuture.completedFuture(new ResponseEntity(stream, HttpStatus.OK));
    }



    @RestController
    @RequestMapping({"~cogna"})
//@CrossOrigin(allowCredentials="true")
    public class CognaControllerPublic {

        public final CognaService cognaService;

        public CognaControllerPublic(CognaService cognaService){
            this.cognaService = cognaService;
        }

//        @RequestMapping(value = "{code}/pdf", method = {RequestMethod.GET,RequestMethod.POST})
//        public ResponseEntity<byte[]> pdfCogna(@PathVariable("code") String code, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException, IOException {
////            cognaService.pdf(null,code, req, res, userPrincipal);
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().toString())
//                    .contentType(MediaType.APPLICATION_PDF)
//                    .body(cognaService.pdf(null,code, req, res, userPrincipal));
//        }
//
//
//        @RequestMapping(value = "{code}/{action}", method = {RequestMethod.GET,RequestMethod.POST})
//        public CompletableFuture<Object> printCognaCode(@PathVariable("code") String code,@PathVariable("action") String action, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
//            return cognaService.actionCode(code, req, res, null, userPrincipal,action);
//        }
//
        @PostMapping("{code}/extract")
        public CompletableFuture<List<JsonNode>> extract(@PathVariable("code") String code,
                                        @RequestBody CognaService.ExtractObj extractObj,
                                        HttpServletRequest req,
                                        HttpServletResponse res,
                                        @CurrentUser UserPrincipal userPrincipal) throws Exception {

            return cognaService.extractByCode(code, extractObj);
        }

        @PostMapping("{code}/classify")
        public CompletableFuture<Map<String, Object>> classify(@PathVariable("code") String code,
                                        @RequestBody CognaService.ExtractObj extractObj,
                                        @RequestParam(value = "lookupId", required = false) Long lookupId,
                                        @RequestParam(value = "what", required = false) String what,
                                        @RequestParam(value = "minScore", required = false) Double minScore,
                                        @RequestParam(value = "multiple", defaultValue = "false") boolean multiple,
                                        HttpServletRequest req,
                                        HttpServletResponse res,
                                        @CurrentUser UserPrincipal userPrincipal) throws Exception {

            return cognaService.classifyByCode(code, extractObj, lookupId, what, minScore, multiple);
        }
//
        @PostMapping("{code}/prompt")
        public CompletableFuture<Map<String, Object>> prompt(@PathVariable("code") String code,
                                                             @RequestBody CognaService.PromptObj promptObj,
                                                             HttpServletRequest req,
                                                             HttpServletResponse res,
                                                             @CurrentUser UserPrincipal userPrincipal) throws Exception {

            return cognaService.promptByCode(code, promptObj, null, promptObj.email());
        }

//        @PostMapping("{code}/mcp")
//        public CompletableFuture<Map<String, Object>> mcp(@PathVariable("code") String code,
//                                                          @CurrentUser UserPrincipal userPrincipal) throws Exception {
//
//            cognaService.startMcpPrompt(code);
//            return CompletableFuture.completedFuture(Map.of("success", true));
//        }
//
        @PostMapping("{code}/stream-prompt")
        public CompletableFuture<ResponseEntity<StreamingResponseBody>> streamPrompt(@PathVariable("code") String code,
                                                                                    @RequestBody CognaService.PromptObj promptObj,
                                                                                     HttpServletRequest req,
                                                                                     HttpServletResponse res,
                                                                                     @CurrentUser UserPrincipal userPrincipal) throws Exception{
            ResponseBodyEmitter emitter = new ResponseBodyEmitter ();
            cognaService.promptByCode(code, promptObj, emitter, promptObj.email());

//            StreamingResponseBody stream = out -> {
//                try {
//                    cognaService.promptByCode(code, promptObj.prompt, req, res, out, userPrincipal);
//                } catch (ScriptException e) {
//                    out.write(e.getMessage().getBytes());
//                }
//            };
            return CompletableFuture.completedFuture(new ResponseEntity(emitter, HttpStatus.OK));
        }
//
//        @RequestMapping(value = "{code}/{action}/cache", method = {RequestMethod.GET,RequestMethod.POST})
//        @Cacheable(value = "cognas", key = "{#code,#action}")
//        public CompletableFuture<Object> cachedCognaCode(@PathVariable("code") String code,@PathVariable("action") String action, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
//            return cognaService.actionCode(code, req, res, null,userPrincipal,action);
//        }

    }


}

