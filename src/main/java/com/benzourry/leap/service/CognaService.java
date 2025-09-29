package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.service.TokenStream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class CognaService {


    private final CognaRepository cognaRepository;

//    CognaActionRepository cognaActionRepository;

    private final AppRepository appRepository;
    private final EntryService entryService;
    private final LookupService lookupService;
    private final MailService mailService;
    private final EndpointService endpointService;
    private final AccessTokenService accessTokenService;
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;
    private final SqlService sqlService;
    private final BucketRepository bucketRepository;
    private final EntryAttachmentRepository entryAttachmentRepository;
    private final CognaSourceRepository cognaSourceRepository;
    private final CognaToolRepository cognaToolRepository;
    private final CognaMcpRepository cognaMcpRepository;
    private final CognaPromptHistoryRepository cognaPromptHistoryRepository;
    private final ChatService chatService;

    public record PromptObj(String prompt, List<String> fileList, String email, boolean fromCogna){}
    public record ExtractObj(String text, List<String> docList, String email, boolean fromCogna){}



    public CognaService(CognaRepository cognaRepository, AppRepository appRepository, EntryService entryService,
                        MailService mailService, EndpointService endpointService, AccessTokenService accessTokenService,
                        LookupService lookupService, UserRepository userRepository, AppUserRepository appUserRepository,
                        EntryAttachmentRepository entryAttachmentRepository,
                        SqlService sqlService, BucketRepository bucketRepository,
                        CognaSourceRepository cognaSourceRepository,
                        CognaToolRepository cognaToolRepository,
                        CognaMcpRepository cognaMcpRepository,
                        CognaPromptHistoryRepository cognaPromptHistoryRepository,
                        ChatService chatService) {
        this.appRepository = appRepository;
        this.cognaRepository = cognaRepository;
        this.entryService = entryService;
        this.lookupService = lookupService;
        this.mailService = mailService;
        this.endpointService = endpointService;
        this.accessTokenService = accessTokenService;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.sqlService = sqlService;
        this.bucketRepository = bucketRepository;
        this.chatService = chatService;
        this.cognaSourceRepository = cognaSourceRepository;
        this.cognaToolRepository = cognaToolRepository;
        this.cognaMcpRepository = cognaMcpRepository;
        this.cognaPromptHistoryRepository = cognaPromptHistoryRepository;

    }


    public Page<CognaPromptHistory> getHistory(Long cognaId, String type, String searchText, String email, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return cognaPromptHistoryRepository.findByCognaId(cognaId, type, searchText, email, pageable);
    }


    public Cogna saveCogna(long appId, Cogna cogna, String email) {
        App app = appRepository.getReferenceById(appId);
        cogna.setApp(app);
        if (cogna.getId() == null) {
            cogna.setEmail(email);
        } else {
            // when save, reinit the whole instance (models, store, assistant)
            chatService.reinitCogna(cogna.getId());
        }
        return cognaRepository.save(cogna);
    }

    public void removeCogna(Long id) {
        cognaRepository.deleteById(id);
    }

    public Page<Cogna> findByAppId(long appId, Pageable pageable) {
        return cognaRepository.findByAppId(appId, pageable);
    }

    public Cogna getCogna(long id) {
        return cognaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", id));
    }

    //    @Async("asyncExec")
    public Map<Long, Map> ingest(Long id, OutputStream out, UserPrincipal userPrincipal) {
        return _ingest(id, out, userPrincipal);
    }
    public Map<String, Object> ingestSrc(Long id, OutputStream out, UserPrincipal userPrincipal) {
        CognaSource cognaSource = cognaSourceRepository.findById(id).orElseThrow();
        return chatService.ingestSource(cognaSource);
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> clearById(Long id) {
        return CompletableFuture.completedFuture(chatService.clearMemoryById(id));
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> reinitCogna(Long id) {
        return CompletableFuture.completedFuture(chatService.reinitCognaAndChatHistory(id));
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> clearByIdAndEmail(Long id, String email) {
        return CompletableFuture.completedFuture(chatService.clearMemoryByIdAndEmail(id, email));
    }

    public Map<Long, Map> _ingest(Long id, OutputStream out, UserPrincipal userPrincipal) {
        return chatService.ingest(id);
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> prompt(Long id, PromptObj promptObj, ResponseBodyEmitter emitter, String email) throws Exception {
        return CompletableFuture.completedFuture(_prompt(id, promptObj, emitter, email));
    }



    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> promptByCode(String code, PromptObj promptObj, ResponseBodyEmitter out, String email) throws Exception {
        Cogna cogna = cognaRepository.findFirstByCode(code).orElseThrow();
        return CompletableFuture.completedFuture(_prompt(cogna.getId(), promptObj, out, email));
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> classify(Long id, ExtractObj extractObj, Long lookupId, String what, Double minScore, boolean multiple) {
        return CompletableFuture.completedFuture(chatService.classify(id, extractObj.text, lookupId, what, minScore, multiple));
    }
    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> classifyField(Long id, ExtractObj extractObj) {
        return CompletableFuture.completedFuture(chatService.classifyField(id, extractObj.text));
    }
    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> txtgenField(Long id, ExtractObj extractObj, String action) {
        return CompletableFuture.completedFuture(chatService.txtgenField(id, extractObj.text, action));
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> imggenField(Long id, ExtractObj extractObj) {

        return CompletableFuture.completedFuture(chatService.generateImageField(id, extractObj.text));
    }    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> imggen(Long id, ExtractObj extractObj) {

        return CompletableFuture.completedFuture(Map.of(
                "url",chatService.generateImage(id, extractObj.text),
                "text", extractObj.text
        ));
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> classifyByCode(String code, ExtractObj extractObj, Long lookupId, String what, Double minScore, boolean multiple) throws Exception {
        Cogna cogna = cognaRepository.findFirstByCode(code).orElseThrow();
        return CompletableFuture.completedFuture(chatService.classify(cogna.getId(), extractObj.text, lookupId, what, minScore, multiple));
//        return CompletableFuture.completedFuture(_prompt(cogna.getId(), promptObj, out, email));
    }
    @Async("asyncExec")
    public CompletableFuture<List<JsonNode>> extract(Long id, ExtractObj extractObj) {
        return CompletableFuture.completedFuture(chatService.extract(id, extractObj));
    }

    @Async("asyncExec")
    public CompletableFuture<Map> imgcls(Long id, ExtractObj extractObj) {
        return CompletableFuture.completedFuture(chatService.imgcls(id, extractObj));
    }

    @Async("asyncExec")
    public CompletableFuture<List<JsonNode>> extractByCode(String code, ExtractObj extractObj) throws Exception {
        Cogna cogna = cognaRepository.findFirstByCode(code).orElseThrow();
        return CompletableFuture.completedFuture(chatService.extract(cogna.getId(), extractObj));
//        return CompletableFuture.completedFuture(_prompt(cogna.getId(), promptObj, out, email));
    }

    @Async("asyncExec")
//    public CompletableFuture<List<EmbeddingMatch<TextSegment>>> findSimilarity(Long cognaId, String search, int maxResult, Double minScore){
    public CompletableFuture<List<Map<String, Object>>> findSimilarity(Long cognaId, String search, int maxResult, Double minScore){
        return CompletableFuture.completedFuture(chatService.findSimilarity(cognaId, search,maxResult,minScore));
    }

    public Map<String, Object> _prompt(Long id, PromptObj promptObj, ResponseBodyEmitter emitter, String email) throws Exception {
        Map<String, Object> result = new HashMap<>();
        List<Content> sources = new ArrayList<>();
        Cogna cogna = cognaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cogna", "id", id));
        boolean jsonOutput = cogna.getData().at("/jsonOutput").asBoolean();
        System.out.println("###########JSON Output: "+jsonOutput);

        try {

            if (emitter == null) {
                String response = chatService.prompt(email, id, promptObj);
                if (jsonOutput){
                    ObjectMapper mapper = new ObjectMapper();
                    result.put("result", mapper.readTree(response));
                }else{
                    result.put("result", response);
                }

                result.put("success", true);
                recordPrompt(id, promptObj.prompt, response, email, CognaPromptHistory.BY_LLM);
            } else {
                TokenStream responseStream = chatService.promptStream(email, id, promptObj);
                responseStream
                .onRetrieved(s-> {
                    result.put("sources", s);
                })
                .onPartialResponse(token -> {
                    try {
                        emitter.send(token);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).onCompleteResponse(msg -> {
                    System.out.println("token count:"+ msg.tokenUsage());
                    emitter.complete();
                    result.put("success", true);
                    result.put("result", msg.aiMessage());
                    recordPrompt(id, promptObj.prompt, msg.aiMessage().text(), email, CognaPromptHistory.BY_LLM);
                }).onError(e -> {
                    emitter.completeWithError(e);
                    result.put("success", false);
                    result.put("message", e.getMessage());
                }).start();
            }
        } catch (Exception e) {
            if (emitter!=null) {
                emitter.completeWithError(e);
            }
            result.put("success", false);
            result.put("message", e.getMessage());
            throw new RuntimeException(e);
        }

        return result;
    }

    //    @Async UPDATED ON 14-MARCH-2024
    @Async("asyncExec")
    public void recordPrompt(Long cognaId, String prompt, String response, String email, String by){
        CognaPromptHistory cph = new CognaPromptHistory(
                cognaId, prompt, response, email, by
        );
        System.out.println("Store cogna record: "+cph);
        cognaPromptHistoryRepository.save(cph);
    }

    public boolean checkByCode(String code) {
        return cognaRepository.checkByCode(code) > 0;
    }

    public CompletableFuture<Map<String, Object>> clearDb(Long id) {
        return CompletableFuture.completedFuture(chatService.clearDb(id));
    }

    public CognaSource addCognaSrc(long id, CognaSource cognaSrc) {
        Cogna cogna = cognaRepository.getReferenceById(id);
        cognaSrc.setCogna(cogna);
        return cognaSourceRepository.save(cognaSrc);
    }

    public CognaTool addCognaTool(long id, CognaTool cognaTool) {
        Cogna cogna = cognaRepository.getReferenceById(id);
        cognaTool.setCogna(cogna);
        CognaTool ct = cognaToolRepository.save(cognaTool);
        reinitCogna(cogna.getId());
        return ct;
    }

    public CognaMcp addCognaMcp(long id, CognaMcp cognaMcp) {
        Cogna cogna = cognaRepository.getReferenceById(id);
        cognaMcp.setCogna(cogna);
        CognaMcp ct = cognaMcpRepository.save(cognaMcp);
        reinitCogna(cogna.getId());
        return ct;
    }

    public Map<String, Object> removeCognaSrc(long id) {
        Map<String, Object> data = new HashMap<>();
        this.cognaSourceRepository.deleteById(id);
        data.put("success", true);
        return data;
    }

    public Map<String, Object> removeCognaTool(long id) {
        Map<String, Object> data = new HashMap<>();

        CognaTool cognaTool = this.cognaToolRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("CognaTool","id",id));
        reinitCogna(cognaTool.getCogna().getId());
        this.cognaToolRepository.deleteById(id);
        data.put("success", true);
        return data;
    }

    public Map<String, Object> removeCognaMcp(long id) {
        Map<String, Object> data = new HashMap<>();

        CognaMcp cognaMcp = this.cognaMcpRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("CognaMcp","id",id));
        reinitCogna(cognaMcp.getCogna().getId());
        this.cognaMcpRepository.deleteById(id);
        data.put("success", true);
        return data;
    }


    public ResponseEntity<StreamingResponseBody> getSrcIngestedFile(Long id,
                                                                    Long srcId) throws IOException {
        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/cogna-" + id + "/";

        CognaSource cognaSrc = cognaSourceRepository.findById(srcId).orElseThrow();

        String fileName = "";

        if ("dataset".equals(cognaSrc.getType())) {
            fileName = "dataset-" + cognaSrc.getSrcId() + ".txt";
        }
        if ("url".equals(cognaSrc.getType())) {
            fileName = "web-" + cognaSrc.getId() + ".txt";
        }
        if ("bucket".equals(cognaSrc.getType())) {
            fileName = "bucket-" + cognaSrc.getSrcId() + ".txt";
        }

//        if (!Helper.isNullOrEmpty(path)) {
//
        ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                .filename(fileName)
                .build();

        File file = new File(destStr + fileName);

        if (file.isFile()) {

//                String mimeType = Files.probeContentType(file.toPath());

            ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
//                        .cacheControl(CacheControl.maxAge(14, TimeUnit.DAYS))
                    .contentType(MediaType.TEXT_PLAIN)
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString());

            return builder.body(outputStream -> Files.copy(file.toPath(), outputStream));
        } else {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }


    }


    public Map<String, String> getFormatter(Long formId, boolean asSchema){
        return chatService.getJsonFormatter(formId, asSchema);
    }

//    public void startMcpPrompt(String cognaCode){
//        Cogna cogna = cognaRepository.findFirstByCode(cognaCode).orElseThrow(()->new ResourceNotFoundException("Cogna", "code", cognaCode));
//        chatService.startMcpPrompt(cogna.getId());
//    }
}
