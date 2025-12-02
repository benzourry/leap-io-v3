package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.QuadFunction;
import com.benzourry.leap.utility.TriFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.itextpdf.html2pdf.HtmlConverter;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.script.*;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class LambdaService {


    private final LambdaRepository lambdaRepository;
    private final AppRepository appRepository;
    private final EntryService entryService;
    private final LookupService lookupService;
    private final MailService mailService;
    private final EndpointService endpointService;
    private final AccessTokenService accessTokenService;
    private final UserRepository userRepository;
    private final AppUserRepository appUserRepository;
    private final SqlService sqlService;
    private final KryptaService kryptaService;
    private final BucketRepository bucketRepository;
    private final EntryAttachmentRepository entryAttachmentRepository;
    private final ChatService chatService;
    private final LambdaService self;

//    instance.scheduler.enabled
    @org.springframework.beans.factory.annotation.Value("${instance.scheduler.enabled:true}")
    boolean schedulerEnabled;

    public LambdaService(LambdaRepository lambdaRepository, AppRepository appRepository, EntryService entryService,
                         MailService mailService, EndpointService endpointService, AccessTokenService accessTokenService,
                         LookupService lookupService, UserRepository userRepository, AppUserRepository appUserRepository,
                         EntryAttachmentRepository entryAttachmentRepository,
                         SqlService sqlService, KryptaService kryptaService,
                         BucketRepository bucketRepository,
                         @Lazy ChatService chatService,
                         @Lazy LambdaService lambdaService, ObjectMapper MAPPER) {
        this.appRepository = appRepository;
        this.lambdaRepository = lambdaRepository;
        this.entryService = entryService;
        this.lookupService = lookupService;
        this.mailService = mailService;
        this.endpointService = endpointService;
        this.accessTokenService = accessTokenService;
        this.userRepository = userRepository;
        this.appUserRepository = appUserRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.sqlService = sqlService;
        this.kryptaService = kryptaService;
        this.bucketRepository = bucketRepository;
        this.chatService = chatService;
        this.self = lambdaService;
        this.MAPPER = MAPPER;

        this.globalHttpBindings = initHttpBindings();
        this.globalIoBindings = initIoBindings();
        this.globalUtilBindings = initUtilBindings();
        this.globalPdfBindings = initPdfBindings();
    }

    private Map<String, Object> initHttpBindings(){
        Function<String, HttpResponse> _get = (url) -> {
            try {
                var httpGet = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .GET()
                        .build();
                return HTTP_CLIENT.send(httpGet, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                return null;
            }
        };

        BiFunction<String, Map<String, Map<String, Object>>, HttpResponse> _getNew = (url, payload) -> {
            try {
                var httpGet = HttpRequest.newBuilder().uri(new URI(url)).GET();
                Map<String, Object> headers = payload.get("headers");
                if (headers != null)
                    headers.forEach((k, v) -> httpGet.header(k, v.toString()));
                return HTTP_CLIENT.send(httpGet.build(), HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                return null;
            }
        };

        TriFunction<String, Map<String, Object>, String, HttpResponse> _post = (url, payload, type) -> {
            try {
                String body = "json".equals(type)
                        ? MAPPER.writeValueAsString(payload)
                        : getFormData(payload);
                String contentType = "json".equals(type)
                        ? "application/json; charset=UTF-8"
                        : "application/x-www-form-urlencoded; charset=UTF-8";
                var httpPost = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .headers("Content-Type", contentType)
                        .build();
                return HTTP_CLIENT.send(httpPost, HttpResponse.BodyHandlers.ofString());
            } catch (Exception e) {
                return null;
            }
        };

        TriFunction<String, Map<String, Map<String, Object>>, String, HttpResponse> _postNew = (url, payload, type) -> {
            HttpResponse val = null;
            try {
                Map<String, Object> bodyObj = payload.get("body");
                Map<String, Object> headerObj = payload.get("headers");
                String body = "json".equals(type) ? MAPPER.writeValueAsString(bodyObj) : getFormData(bodyObj);


                String contentType = "json".equals(type) ? "application/json; charset=UTF-8" : "application/x-www-form-urlencoded; charset=UTF-8";
                var httpPost = HttpRequest.newBuilder()
                        .uri(new URI(url))
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .headers("Content-Type", contentType);

                if (headerObj!=null){
                    headerObj.keySet().forEach(k->{
                        httpPost.header(k, headerObj.get(k).toString());
                    });
                }

                var response = HTTP_CLIENT
                        .send(httpPost.build(), HttpResponse.BodyHandlers.ofString());
                val = response;
            } catch (Exception e) {
            }
            return val;
        };
        return Map.of("GETo", _get,
                "GET", _getNew,
                "POSTo", _post,
                "POST", _postNew);
    }

    // Optimized IO bindings initialization
    private Map<String, Object> initIoBindings() {
        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

        Function<String, Path> _path = (filename) -> {
            EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(filename);
            String pathStr = destStr;
            if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                pathStr = destStr + "bucket-" + entryAttachment.getBucketId() + "/";
            }
            return Paths.get(pathStr + filename);
        };

        BiFunction<String, String, Path> _write = (content, filename) -> {
            try {
                Path path = Paths.get(destStr + filename);
                Files.createDirectories(path.getParent());
                return Files.writeString(path, content, StandardCharsets.UTF_8);
            } catch (IOException ex) {
                System.err.println("File write error: " + ex.getMessage());
                return null;
            }
        };

        Function<String, String> _read = (filename) -> {
            try {
                return Files.readString(Paths.get(destStr + filename), StandardCharsets.UTF_8);
            } catch (IOException e) {
                System.err.println("File read error: " + e.getMessage());
                return null;
            }
        };

        Function<Integer, List> _filesFromBucket = (bucketId) ->
                bucketRepository.findPathByBucketId(bucketId.longValue());

        Function<List<String>, String> _zip = (fileList) -> {
            File dir = new File(Constant.UPLOAD_ROOT_DIR + "/tmp/");
            dir.mkdirs();
            String filename = System.currentTimeMillis() + ".zip";
            String zipFile = Constant.UPLOAD_ROOT_DIR + "/tmp/" + filename;

            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                byte[] buffer = new byte[8192]; // Increased buffer size
                for (String file : fileList) {
                    Path filePath = _path.apply(file);
                    if (filePath == null || !Files.exists(filePath)) {
                        continue;
                    }

                    try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
                        zos.putNextEntry(new ZipEntry(filePath.getFileName().toString()));
                        int length;
                        while ((length = fis.read(buffer)) > 0) {
                            zos.write(buffer, 0, length);
                        }
                        zos.closeEntry();
                    } catch (Exception e) {
                        System.err.println("Zip entry error: " + e.getMessage());
                    }
                }
            } catch (IOException ioe) {
                System.err.println("Zip creation error: " + ioe.getMessage());
                return null;
            }
            return filename;
        };

        return Map.of(
                "write", _write,
                "read", _read,
                "path", _path,
                "zip", _zip,
                "pathFromBucket", _filesFromBucket
        );
    }

    // Optimized Util bindings initialization
    private Map<String, Object> initUtilBindings() {
        BiFunction<String, String, String> ocr = Helper::ocr;
        BiFunction<String, Map<String, String>, String> replaceMulti = Helper::replaceMulti;

        Function<String, String> btoa = (input) ->
                Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));

        Function<String, String> atob = (input) ->
                new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);

        BiFunction<String, String, String> encode = (input, type) ->
                new DigestUtils(type).digestAsHex(input);

        Function<String, XSSFWorkbook> _readExcel = (filePath) -> {
            try {
                Path path = globalIoBindings.containsKey("path")
                        ? ((Function<String, Path>) globalIoBindings.get("path")).apply(filePath)
                        : Paths.get(Constant.UPLOAD_ROOT_DIR + "/attachment/" + filePath);
                return new XSSFWorkbook(path.toFile());
            } catch (IOException | InvalidFormatException e) {
                System.err.println("Excel read error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        };

        return Map.of(
                "ocr", ocr,
                "replaceMulti", replaceMulti,
                "readExcel", _readExcel,
                "btoa", btoa,
                "atob", atob,
                "hash", encode
        );
    }

    // PDF bindings initialization
    private Map<String, Object> initPdfBindings() {
        Function<String, byte[]> _htmltoPdf = (html) -> {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                HtmlConverter.convertToPdf(html, baos);
                return baos.toByteArray();
            } catch (IOException e) {
                System.err.println("PDF conversion error: " + e.getMessage());
                return null;
            }
        };

        return Map.of("fromHtml", _htmltoPdf);
    }

    public Lambda saveLambda(long appId, Lambda lambda, String email) {
        App app = appRepository.getReferenceById(appId);
        lambda.setApp(app);
        if (lambda.getId()==null) {
            lambda.setEmail(email);
        }else{
            scriptCache.invalidate(lambda.getId());
        }

        return lambdaRepository.save(lambda);
    }

    public void removeLambda(Long id) {
        scriptCache.invalidate(id);
        lambdaRepository.deleteById(id);
    }

    public Page<Lambda> findByAppId(long appId, Pageable pageable) {
        return lambdaRepository.findByAppId(appId, pageable);
    }

    public Lambda getLambda(long id) {
        return lambdaRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));
    }

    public Lambda getLambdaByCode(String code) {
        return lambdaRepository.findFirstByCode(code)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","code",code));
    }

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> run(Long id, HttpServletRequest req, HttpServletResponse res, OutputStream out, UserPrincipal userPrincipal) throws ScriptException {
        return CompletableFuture.completedFuture(execLambda(id, null, req,res, out,userPrincipal));
    }

//    @Async("asyncExec")
    public Map<String, Object> stream(Long id, HttpServletRequest req, HttpServletResponse res, OutputStream out, UserPrincipal userPrincipal) throws ScriptException {
        return execLambda(id, null,req,res, out,userPrincipal);
    }

    @Async("asyncExec")
    public CompletableFuture<Object> out(Long id, HttpServletRequest req, HttpServletResponse res, UserPrincipal userPrincipal) throws ScriptException {
        Lambda l = lambdaRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));
        String name = userPrincipal == null ? null : userPrincipal.getName();
        boolean isPublic = l.isPublicAccess();
        if (name==null && !isPublic) {
            // access to private lambda from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Lambda: Access to private lambda without authentication is not allowed");
        } else {
            return CompletableFuture.completedFuture(execLambda(id, null, req,res, null,userPrincipal).get("out"));
        }
    }

    Helper helper = new Helper();
//    private final Map<Long, CompiledScript> scriptCache = new ConcurrentHashMap<>();

    private final Cache<Long, CompiledScript> scriptCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(Duration.ofHours(12))
            .build();

    private CompiledScript getOrCompileScript(Lambda lambda, ScriptEngine engine) throws ScriptException {
        return scriptCache.get(lambda.getId(), id -> {
            try {
                String script = lambda.getData().get("f").asText("");
                if ("groovy".equals(lambda.getLang())) {
                    script = script.replaceAll("System\\.out\\.", "");
                }

                if (!(engine instanceof Compilable)) {
                    throw new RuntimeException("Engine does not support compilation: " + lambda.getLang());
                }

                return ((Compilable) engine).compile(script);
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String dayjsSource;
    @PostConstruct
    public void init() {
        try (InputStream is = new ClassPathResource("dayjs.min.js").getInputStream()) {
            dayjsSource = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load dayjs.min.js from classpath", e);
        }
    }


    @PreDestroy
    public void cleanup() {
        System.out.println("Cleaning up LambdaService resources...");

        // Clear caches
        scriptCache.invalidateAll();

        // Close GraalVM engine
        if (SHARED_GRAAL_ENGINE != null) {
            SHARED_GRAAL_ENGINE.close();
        }

        System.out.println("LambdaService cleanup completed");
    }


    private static final Engine SHARED_GRAAL_ENGINE = Engine.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    private static final HostAccess HOST_ACCESS = HostAccess.newBuilder(HostAccess.ALL)
            .targetTypeMapping(Value.class, Object.class,
                    Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class)))
            .build();

    private ScriptEngine createJsEngine() {
        try {
            // Restrict class loading to specific packages only
            ScriptEngine engine = GraalJSScriptEngine.create(
                    SHARED_GRAAL_ENGINE,
                    Context.newBuilder("js")
                            .allowHostAccess(HOST_ACCESS)
                            .allowHostClassLookup(name -> name != null && (
//                                    name.startsWith("java.net.") ||
//                                    name.startsWith("java.io.") ||
//                                    name.startsWith("java.nio.") ||
//                                    name.startsWith("java.lang.") ||
//                                    name.startsWith("java.util.") ||
//                                    name.startsWith("java.time.") ||
//                                    name.startsWith("java.text.") ||
                                    name.startsWith("java.") ||
                                    name.startsWith("com.benzourry.leap.")
                            ))
                            .allowAllAccess(true)
            );

            return engine;
        } catch (Exception e) {
            System.err.println("Failed to create JS engine: " + e.getMessage());
            throw new RuntimeException("Failed to initialize JavaScript engine", e);
        }
    }

    private ScriptEngine getEngine(String lang) {
        if ("js".equals(lang)) {
            return createJsEngine();
        } else {
            return new ScriptEngineManager().getEngineByName(lang);
        }
    }

    private final ObjectMapper MAPPER;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    private final Map<String, Object> globalHttpBindings;
    private final Map<String, Object> globalIoBindings;
    private final Map<String, Object> globalUtilBindings;
    private final Map<String, Object> globalPdfBindings;


    @Transactional
    public Map<String, Object> execLambda(Long id, Map<String,Object> param, HttpServletRequest req, HttpServletResponse res, OutputStream out, UserPrincipal userPrincipal) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> _out = new HashMap<>();

        Lambda lambda = lambdaRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));

        try (Writer writer = out != null ?
                new OutputStreamWriter(out) : new StringWriter()) {

            ScriptContext context = new SimpleScriptContext();
            context.setWriter(writer);

            String email = userPrincipal != null ? userPrincipal.getEmail() : lambda.getEmail();

            try {

                ScriptEngine engine = getEngine(lambda.getLang());

                CompiledScript compiled = getOrCompileScript(lambda, engine);

                engine.setContext(context);

                Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);

                engine.eval(dayjsSource);

                bindings.put("_out", _out);
                if (req != null) {
                    bindings.put("_request", req);
                }
                if (res != null) {
                    bindings.put("_response", res);
                }
                bindings.put("_this", lambda);

                if (param == null) param = new HashMap<>();

                if (req != null) {
                    for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                        param.put(entry.getKey(), req.getParameter(entry.getKey()));
                    }

                    if ("POST".equalsIgnoreCase(req.getMethod())) {
                        try {
                            String body = IOUtils.toString(req.getReader());
                            param.put("_body", body);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                if (!param.isEmpty()) {
                    bindings.put("_param", param);
                }

                lambda.getBinds().forEach(b -> {

                    switch (b.getType()) {

                        case "dataset" -> {
                            Page<EntryDto> datasetEntries = entryService.findListByDataset(
                                    b.getSrcId(), "%", email, new HashMap<>(), "AND", null, null,
                                    PageRequest.of(0, Integer.MAX_VALUE), req
                            );
                            bindings.put(b.getType() + "_" + b.getSrcId(), datasetEntries);
                        }

                        case "dashboard" -> {
                            bindings.put(b.getType() + "_" + b.getSrcId(),
                                    entryService.getDashboardMapDataNativeNew(b.getSrcId(), new HashMap<>(), email, req));
                        }

                        case "lookup" -> {
                            try {
                                Map<String, Object> lookupMap = lookupService.findAllEntry(
                                        b.getSrcId(), null, req, true, PageRequest.of(0, Integer.MAX_VALUE)
                                );
                                bindings.put(b.getType() + "_" + b.getSrcId(), lookupMap);
                            } catch (IOException | InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        case "_entry" -> bindings.put("_entry", entryService);

                        case "_lookup" -> bindings.put("_lookup", lookupService);

                        case "_mail" -> {
                            TriFunction<Integer, Entry, Lambda, String> sendWithTemplate = (templateId, entry, l) -> {
                                mailService.sendWithTemplate(templateId, entry, l, email);
                                return "";
                            };
                            BiFunction<Map<String, String>, Lambda, String> send = (params, l) -> {
                                mailService.send(params, l, email);
                                return "";
                            };
                            bindings.put("_mail", Map.of(
                                    "sendWithTemplate", sendWithTemplate,
                                    "send", send
                            ));
                        }

                        case "_endpoint" -> {
                            QuadFunction<String, Map<String, Object>, Object, Lambda, Object> _run = (code, p, remove, body) -> {
                                try {
                                    return endpointService.run(code, p, body, userPrincipal, lambda);
                                } catch (Exception e) {
                                    return null;
                                }
                            };
                            bindings.put("_endpoint", Map.of("run", _run));
                        }

                        case "_mapper" -> {
                            Function<Object, Map> _toMap = (o) -> MAPPER.convertValue(o, Map.class);
                            Function<Object, Entry> _toEntry = (o) -> MAPPER.convertValue(o, Entry.class);
                            bindings.put("_mapper", MAPPER);
                            bindings.put("_toMap", _toMap);
                            bindings.put("_toEntry", _toEntry);
                        }

                        case "_token" -> bindings.put("_token", accessTokenService);

                        case "_user" -> {
                            Map<String, Object> _user = new HashMap<>();
                            if (userPrincipal != null) {
                                userRepository.findById(userPrincipal.getId()).ifPresent(user -> {
                                    _user.putAll(MAPPER.convertValue(user, Map.class));
                                    List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userPrincipal.getId(), "approved");
                                    Map<Long, UserGroup> groupMap = groups.stream()
                                            .collect(Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
                                    _user.put("groups", groupMap);
                                });
                            }
                            bindings.put("_user", _user);
                        }

                        case "_sql" -> bindings.put("_sql", sqlService);

                        case "_io" -> bindings.put("_io", globalIoBindings);

                        case "_pdf" -> bindings.put("_pdf", globalPdfBindings);

                        case "_cogna" -> bindings.put("_cogna", chatService);

                        case "_krypta" -> bindings.put("_krypta", kryptaService);

                        case "_util" -> bindings.put("_util", globalUtilBindings);

                        case "_helper" -> bindings.put("_helper", helper);

                        case "_http" -> bindings.put("_http", globalHttpBindings);

                        case "_jsoup" -> {
                            Function<String, org.jsoup.nodes.Document> parse = Jsoup::parse;
                            Function<String, org.jsoup.nodes.Document> parseBodyFragment = Jsoup::parseBodyFragment;
                            Function<String, org.jsoup.Connection> connect = Jsoup::connect;
                            bindings.put("_jsoup", Map.of("parse", parse, "parseBodyFragment", parseBodyFragment, "connect", connect));
                        }

                        case "_live" -> {
                            BiFunction<List<String>, String, Map<String, HttpResponse>> _pingPublish = (channels, msg) -> {
                                Map<String, HttpResponse> responses = new HashMap<>();
                                channels.forEach(c -> {
                                    try {
                                        String channelName = "app-" + lambda.getApp().getId() + "-" + c;
                                        var httpPost = HttpRequest.newBuilder()
                                                .uri(new URI(Constant.BROKER_BASE_HTTP + "/pass/" + channelName))
                                                .POST(HttpRequest.BodyPublishers.ofString(
                                                        MAPPER.writeValueAsString(Map.of("content", msg, "channel", c))
                                                ))
                                                .headers("Content-Type", "application/json; charset=UTF-8");
                                        var response = HTTP_CLIENT.send(httpPost.build(), HttpResponse.BodyHandlers.ofString());
                                        responses.put(c, response);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                                return responses;
                            };
                            bindings.put("_live", Map.of("publish", _pingPublish));
                        }

                        default -> {
                            // Optionally handle unknown types
                            System.out.println("Unknown binding type: " + b.getType());
                        }
                    }

                });

                String dev = lambda.getApp().isLive() ? "" : "--dev";
                bindings.put("_env", Map.of(
                        "IO_BASE_API_URL", Constant.IO_BASE_DOMAIN + "/api",
                        "IO_BASE_URL", Constant.IO_BASE_DOMAIN,
                        "UI_BASE_URL", "https://" + lambda.getApp().getAppPath() + dev + "." + Constant.UI_BASE_DOMAIN + "/#",
                        "UPLOAD_DIR", Constant.UPLOAD_ROOT_DIR + "/attachment/",
                        "TMP_DIR", Constant.UPLOAD_ROOT_DIR + "/tmp/"));

                Object val = compiled.eval(bindings);

                result.put("success", true);
                result.put("print", writer.toString().trim());
                result.put("out", bindings.get("_out"));

            } catch (ScriptException exp) {
                try {
                    if (out != null) {
                        out.write(("❌ !err -> " + exp.getMessage()).getBytes());
                    }
                } catch (IOException e) {
                    System.err.println("Error writing error message: " + e.getMessage());
                }
                result.put("success", false);
                result.put("message", exp.getMessage());
                result.put("line", exp.getLineNumber());
                result.put("col", exp.getColumnNumber());
            } finally {
                try {
                    writer.close();
                } catch (IOException e) {
                    System.err.println("Error closing writer: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private String getFormData(Map<String, Object> m) {
        StringBuilder sb = new StringBuilder(m.size() * 32);
        boolean first = true;
        for (Map.Entry<String, Object> entry : m.entrySet()) {
            if (!first) sb.append('&');
            sb.append(entry.getKey())
                    .append('=')
                    .append(URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
    }

    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule() {

        if (!schedulerEnabled){
            System.out.println("Scheduler disabled - skipping scheduled lambda execution");
            return null;
        }

        Calendar now = Calendar.getInstance();

        String clock = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        int day = now.get(Calendar.DAY_OF_WEEK); // sun=1, mon=2, tues=3,wed=4,thur=5,fri=6,sat=7
        int date = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH); // 0-based month, ie: Jan=0, Feb=1, March=2
        System.out.println("START Sched Lambda execution:"+clock);

        lambdaRepository.findScheduledByClock(clock).forEach(s -> {
            if ("daily".equals(s.getFreq()) ||
                    ("weekly".equals(s.getFreq()) && s.getDayOfWeek() == day) ||
                    ("monthly".equals(s.getFreq()) && s.getDayOfMonth() == date) ||
                    ("yearly".equals(s.getFreq()) && s.getMonthOfYear() == month && s.getDayOfMonth() == date)
            ) {
                System.out.println("Running Lambda: "+ s.getName());
                User user = userRepository.findFirstByEmailAndAppId(s.getEmail(), s.getApp().getId()).orElse(null);
                try {
                    long start = System.currentTimeMillis();
                    self.run(s.getId(), null, null, null, UserPrincipal.create(user));
                    long end = System.currentTimeMillis();
                    System.out.println("Sched Lambda Duration ("+s.getName()+"):"+(end-start));
                } catch (ScriptException e) {
                    System.out.println("ERROR executing Lambda:" + s.getName());
                }
            }
        });
        return null;
    }

    @Async("asyncExec")
    public CompletableFuture<Object> action(String action,Long id, HttpServletRequest req, HttpServletResponse res, UserPrincipal userPrincipal) throws ScriptException {
        Lambda l = lambdaRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));
        String name = userPrincipal == null ? null : userPrincipal.getName();
        boolean isPublic = l.isPublicAccess();
//        System.out.println("fromPrivate:"+anonymous);
        if (name==null && !isPublic) {
            // access to private lambda from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Lambda: Access to private lambda without authentication is not allowed");
        } else {
            return CompletableFuture.completedFuture(execLambda(id, null,req, res, null,userPrincipal).get(action));
        }

    }

    public CompletableFuture<Object> actionCode(String code, HttpServletRequest req, HttpServletResponse res, OutputStream out, UserPrincipal userPrincipal, String action) throws ScriptException {
        Lambda l = lambdaRepository.findFirstByCode(code)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","code",code));

        String name = userPrincipal == null ? null : userPrincipal.getName();
        boolean isPublic = l.isPublicAccess();
        if (name==null && !isPublic) {
            // access to private lambda from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Lambda: Access to private lambda without authentication is not allowed");
        } else {
            long start = System.currentTimeMillis();
            CompletableFuture<Object> cf = CompletableFuture.completedFuture(execLambda(l.getId(), null,req, res, out,userPrincipal).get(action));
            long end = System.currentTimeMillis();
            System.out.println("Duration:"+(end-start));
            return cf;
        }


    }

    public boolean checkByCode(String code) {
        return lambdaRepository.checkByCode(code)>0;
    }



//    @Async("asyncExec")
    public byte[] pdf(Long id, String code, HttpServletRequest req,
                      HttpServletResponse res, UserPrincipal userPrincipal) {

        // Resolve Lambda
        Lambda lambda = (id != null)
                ? lambdaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lambda", "id", id))
                : lambdaRepository.findFirstByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Lambda", "code", code));

        String username = (userPrincipal != null) ? userPrincipal.getName() : null;

        // Access control
        if (username == null && !lambda.isPublicAccess()) {
            throw new OAuth2AuthenticationProcessingException(
                    "Private Lambda: Access to private lambda without authentication is not allowed"
            );
        }

        // Execute Lambda
        Object printContent = execLambda(lambda.getId(), null, req, res, null, userPrincipal).get("print");
        String html = (printContent != null) ? printContent.toString() : "";

        // Convert HTML to PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            HtmlConverter.convertToPdf(html, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

}
