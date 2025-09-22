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
import com.itextpdf.html2pdf.HtmlConverter;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.jsoup.Jsoup;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final BucketRepository bucketRepository;
    private final EntryAttachmentRepository entryAttachmentRepository;

    private final ChatService chatService;


    public LambdaService(LambdaRepository lambdaRepository, AppRepository appRepository, EntryService entryService,
                         MailService mailService, EndpointService endpointService, AccessTokenService accessTokenService,
                         LookupService lookupService, UserRepository userRepository, AppUserRepository appUserRepository,
                         EntryAttachmentRepository entryAttachmentRepository,
                         SqlService sqlService,BucketRepository bucketRepository,
                         @Lazy ChatService chatService) {
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
        this.bucketRepository = bucketRepository;
        this.chatService = chatService;

    }

    public Lambda saveLambda(long appId, Lambda lambda, String email) {
        App app = appRepository.getReferenceById(appId);
        lambda.setApp(app);
        if (lambda.getId()==null) {
            lambda.setEmail(email);
        }
        return lambdaRepository.save(lambda);
    }

    public void removeLambda(Long id) {
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
//        System.out.println("fromPrivate:"+anonymous);
        if (name==null && !isPublic) {
            // access to private lambda from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Lambda: Access to private lambda without authentication is not allowed");
        } else {
            return CompletableFuture.completedFuture(execLambda(id, null, req,res, null,userPrincipal).get("out"));
        }

    }

    Helper helper = new Helper();

    @Transactional
    public Map<String, Object> execLambda(Long id, Map<String,Object> param, HttpServletRequest req, HttpServletResponse res, OutputStream out, UserPrincipal userPrincipal) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> _out = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();

        Lambda lambda = lambdaRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));

        Writer writer = out!=null? new OutputStreamWriter(out): new StringWriter();

//        OutputStreamWriter writer2 = new OutputStreamWriter(System.out);
//        StringWriter writer = new StringWriter();
        ScriptContext context = new SimpleScriptContext();
        context.setWriter(writer);

        String email = userPrincipal!=null?userPrincipal.getEmail():lambda.getEmail();


        try {

//            if ("python".equals(lambda.getLang())) {
//                throw new ScriptException("Support for Python is currently disabled");
//            }

            ScriptEngine engine = new ScriptEngineManager().getEngineByName(lambda.getLang());
            String script = lambda.getData().get("f").asText("");

            if ("js".equals(lambda.getLang())) {
                HostAccess access = HostAccess.newBuilder(HostAccess.ALL)
                        .targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class)))
//                        .targetTypeMapping(Value.class, Object.class, Value::hasHashEntries, v -> new HashMap<String, Object>(v.as(Map.class)))
                        .build();
                engine = GraalJSScriptEngine.create(Engine.newBuilder()
                                .option("engine.WarnInterpreterOnly", "false")
                                .build(),
                        Context.newBuilder("js")
                                .allowHostAccess(access)
                                .allowHostClassLookup(s -> true)
                                .allowAllAccess(true)

                );
                try {
                    Resource resource = new ClassPathResource("dayjs.min.js");
                    FileReader fr = new FileReader(resource.getFile());
                    engine.eval(fr);
                } catch (IOException e) {
                    System.out.println("WARNING: Error loading dayjs.min.js with errors: " + e.getMessage());
//                    throw new RuntimeException(e);
                }
            }
            if ("groovy".equals(lambda.getLang())){
                script = script.replaceAll("System\\.out\\.","");
            }


            CompiledScript compiled = ((Compilable) engine).compile(script);

            engine.setContext(context);

            String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
            BiFunction<String, String, Path> _write = (content, filename)->{
                Path path = null;
                try {
//                    String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
                    path = Files.writeString(Paths.get(destStr+filename), content);
                }catch (IOException ex) {
                    System.out.print("Invalid Path");
                }
                return path;
            };

            Function<String,String> _read = (filename)->{
                String bytes = null;
                try {
                    bytes = Files.readString(Paths.get(destStr+filename));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return bytes;
            };

            Function<Integer,List> _filesFromBucket = (bucketId)-> bucketRepository.findPathByBucketId(bucketId.longValue());

            Function<String,Path> _path = (filename)->{
                EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(filename);
                String pathStr = destStr;
                if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                    pathStr = destStr + "bucket-" + entryAttachment.getBucketId() + "/";
                }
                return Paths.get(pathStr+filename);
            };

            Function<String,XSSFWorkbook> _readExcel = (filePath) ->{
                XSSFWorkbook workbook = null;
                try {
                    workbook = new XSSFWorkbook(_path.apply(filePath).toFile());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InvalidFormatException e) {
                    throw new RuntimeException(e);
                }
                return workbook;
            };

            Function<List<String>,String> _zip = (fileList)->{
                File dir = new File(Constant.UPLOAD_ROOT_DIR + "/tmp/");
                dir.mkdirs();
                String filename = System.currentTimeMillis()+".zip";
                String zipFile = Constant.UPLOAD_ROOT_DIR + "/tmp/"+filename;

                try {
                    byte[] buffer = new byte[1024];
                    FileOutputStream fos = new FileOutputStream(zipFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);

                    for(String file: fileList){
                        try {

//                            File srcFile = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/"+file);
                            File srcFile = _path.apply(file).toFile();
                            FileInputStream fis = new FileInputStream(srcFile);

                            // begin writing a new ZIP entry, positions the stream to the start of the entry data
                            zos.putNextEntry(new ZipEntry(srcFile.getName()));
                            int length;
                            while ((length = fis.read(buffer)) > 0) {
                                zos.write(buffer, 0, length);
                            }
                            zos.closeEntry();
                            // close the InputStream
                            fis.close();
                        }catch (Exception fnfe){
                            System.out.println("Zip error: "+ fnfe.getMessage());
                        }
                    }
                    zos.close();

                }catch (IOException ioe) {
                    System.out.println("Error creating zip file: " + ioe);
                }
                return filename;
            };

            Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
//            bindings.put("_user", _user);
//            bindings.put("_mapper", mapper);
//            bindings.put("_save",$save$);

//            bindings.put("_param",$param$);
            bindings.put("_out", _out);
            if (req != null) {
                bindings.put("_request", req);
            }
            if (res != null) {
                bindings.put("_response", res);
            }
            bindings.put("_this", lambda);



//            Function<String, String> $param$ = req::getParameter;

            if (param==null) param = new HashMap<>();

            if (req != null) {
                for (Map.Entry<String, String[]> entry : req.getParameterMap().entrySet()) {
                    param.put(entry.getKey(), req.getParameter(entry.getKey()));
                }
            }

            if (param.size()>0){
                bindings.put("_param", param);
            }

            lambda.getBinds().forEach(b -> {
                if ("dataset".equals(b.getType())) {
                    Page<Entry> b1 = entryService.findListByDataset(b.getSrcId(), "%", email, new HashMap(),"AND",null,null,PageRequest.of(0, Integer.MAX_VALUE), req);
                    bindings.put(b.getType() + "_" + b.getSrcId(), b1);
                }
                if ("dashboard".equals(b.getType())) {
                    bindings.put(b.getType() + "_" + b.getSrcId(), entryService.getDashboardMapDataNativeNew(b.getSrcId(), new HashMap<>(), email, req));
                }
                if ("lookup".equals(b.getType())) {
                    Map<String, Object> b1 = null;
                    try {
                        b1 = lookupService.findAllEntry(b.getSrcId(), null, req, true, PageRequest.of(0, Integer.MAX_VALUE));
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    bindings.put(b.getType() + "_" + b.getSrcId(), b1);
                }
                if ("_entry".equals(b.getType())) {
                    bindings.put("_entry", entryService);
                }
                if ("_lookup".equals(b.getType())) {
                    bindings.put("_lookup", lookupService);
                }
                if ("_mail".equals(b.getType())) {
                    TriFunction<Integer, Entry, Lambda, String> sendWithTemplate = (templateId, entry, l)->{
//                        String email = userPrincipal!=null?userPrincipal.getEmail():l.getEmail();
                        mailService.sendWithTemplate(templateId, entry, l, email);
                        return "";
                    };
                    //Map<String, String> params, Lambda lambda, String initBy
                    BiFunction<Map<String, String>, Lambda, String> send = (params, l)->{
//                        String email = userPrincipal!=null?userPrincipal.getEmail():l.getEmail();
                        mailService.send(params, l, email);
                        return "";
                    };

                    bindings.put("_mail", Map.of(
                            "sendWithTemplate", sendWithTemplate,
                            "send", send
                    ));

//                    bindings.put("_mail", mailService);
                }
                if ("_endpoint".equals(b.getType())) {
                    QuadFunction<String, Map<String, Object>, Object, Lambda, Object> _run = (code, p, remove, body) -> {
                        Object val = null;
                        try {
                            var httpResponse = endpointService.run(code, p,body, userPrincipal, lambda);
//                            System.out.println("testas");
                            val = httpResponse;
                        } catch (Exception e) {
                        }
                        return val;
                    };
                    bindings.put("_endpoint", Map.of("run", _run));
                }
                if ("_mapper".equals(b.getType())) {
                    Function<Object, Map> _toMap = (o) -> mapper.convertValue(o,Map.class);
                    Function<Object, Entry> _toEntry = (o) -> mapper.convertValue(o,Entry.class);
                    bindings.put("_mapper", mapper);
                    bindings.put("_toMap",_toMap);
                    bindings.put("_toEntry",_toEntry);
                }
                if ("_token".equals(b.getType())) {
                    bindings.put("_token", accessTokenService);
                }
                if ("_user".equals(b.getType())) {
                    Map<String, Object> _user = new HashMap<>();
                    if (userPrincipal != null) {
                        Optional<User> userOpt = userRepository.findById(userPrincipal.getId());
                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            _user = mapper.convertValue(user, Map.class);
                            List<AppUser> groups = appUserRepository.findByUserIdAndStatus(userPrincipal.getId(), "approved");
                            Map<Long, UserGroup> groupMap = groups.stream().collect(
                                    Collectors.toMap(x -> x.getGroup().getId(), AppUser::getGroup));
                            _user.put("groups", groupMap);
                        }
                    }
                    bindings.put("_user", _user);
                }
                if ("_sql".equals(b.getType())){
                    bindings.put("_sql", sqlService);
                }
                if ("_io".equals(b.getType())){
                    bindings.put("_io",Map.of(
                            "write",_write,
                            "read",_read,
                            "path",_path,
                            "zip",_zip,
                            "pathFromBucket",_filesFromBucket));
                }
                if ("_pdf".equals(b.getType())){
                    Function<String, byte[]> _htmltoPdf = (html) -> {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        HtmlConverter.convertToPdf(out.toString(), baos);
                        return baos.toByteArray();
                    };
                    bindings.put("_pdf", Map.of("fromHtml", _htmltoPdf));
                }

                if("_cogna".equals(b.getType())){
                    bindings.put("_cogna", chatService);
                }

                if("_util".equals(b.getType())){
                    BiFunction<String, String, String> ocr = Helper::ocr;
                    BiFunction<String, Map<String, String>, String> replaceMulti = Helper::replaceMulti;
                    Function<Path, XSSFWorkbook> readExcel = Helper::readExcel;
                    Function<String, String> btoa = (input)->Base64.getEncoder().encodeToString(input.getBytes());
                    Function<String, String> atob = (input)->new String(Base64.getDecoder().decode(input));

                    BiFunction<String, String, String> encode = (input, type) ->{
                        String sha3Hex = new DigestUtils(type).digestAsHex(input);
                        return sha3Hex;
                    };

                    bindings.put("_util", Map.of("ocr", ocr,
                            "replaceMulti", replaceMulti,
                            "readExcel", _readExcel,
                            "btoa", btoa,
                            "atob", atob,
                            "hash", encode));
                }
                if("_helper".equals(b.getType())){
                    bindings.put("_helper", helper);
                }

                if ("_http".equals(b.getType())) {
                    Function<String, HttpResponse> _get = (url) -> {
                        HttpResponse val = null;
                        try {
                            var httpGet = HttpRequest.newBuilder()
                                    .uri(new URI(url))
                                    .GET()
                                    .build();
                            var response = HttpClient.newHttpClient()
                                    .send(httpGet, HttpResponse.BodyHandlers.ofString());
                            val = response;
                        } catch (Exception e) {
                        }
                        return val;
                    };

                    BiFunction<String, Map<String, Map<String, Object>>, HttpResponse> _getNew = (url, payload) -> {
                        HttpResponse val = null;
                        try {
                            Map<String, Object> headerObj = payload.get("headers");
                            Map<String, Object> paramObj = payload.get("params");
                            var httpGet = HttpRequest.newBuilder()
                                    .uri(new URI(url))
                                    .GET();

                            if (headerObj!=null){
                                headerObj.keySet().forEach(k->{
                                    httpGet.header(k, headerObj.get(k).toString());
                                });
                            }

                            var response = HttpClient.newHttpClient()
                                    .send(httpGet.build(), HttpResponse.BodyHandlers.ofString());
                            val = response;
                        } catch (Exception e) {
                        }
                        return val;
                    };

                    TriFunction<String, Map<String, Object>, String, HttpResponse> _post = (url, payload, type) -> {
                        HttpResponse val = null;
                        try {
                            String body = "json".equals(type) ? mapper.writeValueAsString(payload) : getFormData(payload);
                            String contentType = "json".equals(type) ? "application/json; charset=UTF-8" : "application/x-www-form-urlencoded; charset=UTF-8";
                            var httpPost = HttpRequest.newBuilder()
                                    .uri(new URI(url))
                                    .POST(HttpRequest.BodyPublishers.ofString(body))
                                    .headers("Content-Type", contentType)
                                    .build();
                            var response = HttpClient.newHttpClient()
                                    .send(httpPost, HttpResponse.BodyHandlers.ofString());
                            val = response;
                        } catch (Exception e) {
                        }
                        return val;
                    };
                    TriFunction<String, Map<String, Map<String, Object>>, String, HttpResponse> _postNew = (url, payload, type) -> {
                        HttpResponse val = null;
                        try {
                            Map<String, Object> bodyObj = payload.get("body");
                            Map<String, Object> headerObj = payload.get("headers");
                            String body = "json".equals(type) ? mapper.writeValueAsString(bodyObj) : getFormData(bodyObj);


                            String contentType = "json".equals(type) ? "application/json; charset=UTF-8" : "application/x-www-form-urlencoded; charset=UTF-8";
                            var httpPost = HttpRequest.newBuilder()
                                    .uri(new URI(url))
                                    .POST(HttpRequest.BodyPublishers.ofString(body))
                                    .headers("Content-Type", contentType);

                            if (headerObj!=null){
                                headerObj.keySet().forEach(k->{
                                    httpPost.header(k, headerObj.get(k).toString());
                                    System.out.println("header:"+k+headerObj.get(k).toString());
                                });
                            }

                            var response = HttpClient.newHttpClient()
                                    .send(httpPost.build(), HttpResponse.BodyHandlers.ofString());
                            val = response;
                        } catch (Exception e) {
                        }
                        return val;
                    };
                    // _http.get x working sebab _http ialah Map n .get would invoke Map's get()
                    bindings.put("_http", Map.of("GETo", _get, "GET", _getNew, "POSTo", _post,"POST", _postNew));
                }
//                if ("_jsoup".equals(b.getType())){
//                    bindings.put("_jsoup", Jsoup.class); // <--- inject Jsoup class
//                }
                if ("_jsoup".equals(b.getType())){
                    Function<String, org.jsoup.nodes.Document> parse = Jsoup::parse;
                    Function<String, org.jsoup.nodes.Document> parseBodyFragment = Jsoup::parseBodyFragment;
                    Function<String, org.jsoup.Connection> connect = Jsoup::connect;
                    bindings.put("_jsoup", Map.of("parse", parse,
                            "parseBodyFragment", parseBodyFragment,
                            "connect", connect));
                }

                if ("_live".equals(b.getType())){
                    BiFunction<List<String>, String, Map<String,HttpResponse>> _pingPublish = (channels,msg) -> {
                        Map<String,HttpResponse> responses = new HashMap<>();
                        channels.forEach(c-> {
                            String channelName = "app-"+lambda.getApp().getId()+"-"+ c;
                            HttpResponse val = null;
                            try {

                                var httpPost = HttpRequest.newBuilder()
                                        .uri(new URI(Constant.BROKER_BASE_HTTP+"/pass/"+channelName))
                                        .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(Map.of("content",msg, "channel", c))))
                                        .headers("Content-Type", "application/json; charset=UTF-8");

                                var response = HttpClient.newHttpClient()
                                        .send(httpPost.build(), HttpResponse.BodyHandlers.ofString());
                                val = response;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            responses.put(c,val);
                        });
                        return responses;

                    };

                    bindings.put("_live", Map.of("publish", _pingPublish) );
                }
            });


            String dev = lambda.getApp().isLive() ? "" : "--dev";
            bindings.put("_env",Map.of(
                    "IO_BASE_API_URL", Constant.IO_BASE_DOMAIN+"/api",
                    "IO_BASE_URL", Constant.IO_BASE_DOMAIN,
                    "UI_BASE_URL", "https://" + lambda.getApp().getAppPath() + dev +  "." + Constant.UI_BASE_DOMAIN + "/#" ,
                    "UPLOAD_DIR", Constant.UPLOAD_ROOT_DIR + "/attachment/",
                    "TMP_DIR", Constant.UPLOAD_ROOT_DIR + "/tmp/"));


            //// ###### End HTTP Helper lambda ##########

            Object val = compiled.eval(bindings);

//            System.out.println(writer);

            result.put("success", true);
            result.put("print", writer.toString().trim());
            result.put("out", bindings.get("_out"));
        } catch (ScriptException exp) {
            try {
                if (out!=null){
                    out.write(("❌ !err -> "+exp.getMessage()).getBytes());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            result.put("success", false);
            result.put("message", exp.getMessage());
            result.put("line", exp.getLineNumber());
            result.put("col", exp.getColumnNumber());
        }

        return result;
    }

    public String getFormData(Map<String, Object> m){
        return m.keySet().stream().map(k-> {
            var h = "";
            try {
                h =  k+"="+ URLEncoder.encode(m.get(k)+"", "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return h;
        }).collect(Collectors.joining("&"));
    }

    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule() {

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
                    run(s.getId(), null, null, null, UserPrincipal.create(user));
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
    public byte[] pdf(Long id, String code, HttpServletRequest req, HttpServletResponse res, UserPrincipal userPrincipal) {
        Lambda l = null;
        if (id!=null){
            l = lambdaRepository.findById(id)
                    .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));
        }else{
            l = lambdaRepository.findFirstByCode(code)
                    .orElseThrow(()->new ResourceNotFoundException("Lambda","code",code));
        }

        String name = userPrincipal == null ? null : userPrincipal.getName();
        boolean isPublic = l.isPublicAccess();
        if (name==null && !isPublic) {
            // access to private lambda from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Lambda: Access to private lambda without authentication is not allowed");
        } else {
//            res.setContentType("application/pdf");
//            PrintWriter pw = new PrintWriter();
            Object out = execLambda(l.getId(), null,req,res, null,userPrincipal).get("print");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(String.valueOf(out), baos);
            return baos.toByteArray();
//            return CompletableFuture.completedFuture();
        }

    }

}
