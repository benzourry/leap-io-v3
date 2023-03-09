package com.benzourry.leap.service;

import com.benzourry.leap.exception.OAuth2AuthenticationProcessingException;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.config.Constant;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.script.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

//    LambdaActionRepository lambdaActionRepository;

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


    public LambdaService(LambdaRepository lambdaRepository, AppRepository appRepository, EntryService entryService,
                         MailService mailService, EndpointService endpointService, AccessTokenService accessTokenService,
                         LookupService lookupService, UserRepository userRepository, AppUserRepository appUserRepository,
                         EntryAttachmentRepository entryAttachmentRepository,
                         SqlService sqlService,BucketRepository bucketRepository) {
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
//        this.lambdaActionRepository = lambdaActionRepository;

    }

    public Lambda saveLambda(long appId, Lambda lambda, String email) {
        App app = appRepository.getReferenceById(appId);
        lambda.setApp(app);
        lambda.setEmail(email);
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

    @Async("asyncExec")
    public CompletableFuture<Map<String, Object>> run(Long id, HttpServletRequest req, HttpServletResponse res, UserPrincipal userPrincipal) throws ScriptException {
        return CompletableFuture.completedFuture(execLambda(id, req,res, userPrincipal));
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
            return CompletableFuture.completedFuture(execLambda(id, req,res, userPrincipal).get("out"));
        }

    }


    public Map<String, Object> execLambda(Long id, HttpServletRequest req, HttpServletResponse res, UserPrincipal userPrincipal) throws ScriptException {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> _out = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();

        Lambda lambda = lambdaRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","id",id));

        System.out.println("lambda name:"+lambda.getName());

        StringWriter writer = new StringWriter();
        ScriptContext context = new SimpleScriptContext();
        context.setWriter(writer);


        try {

//            if ("python".equals(lambda.getLang())) {
//                throw new ScriptException("Support for Python is currently disabled");
//            }

            ScriptEngine engine = new ScriptEngineManager().getEngineByName(lambda.getLang());
            String script = lambda.getData().get("f").asText("");

            if ("js".equals(lambda.getLang())) {
                System.out.println("lambda JS");
                HostAccess access = HostAccess.newBuilder(HostAccess.ALL)
                        .targetTypeMapping(Value.class, Object.class, Value::hasArrayElements, v -> new LinkedList<>(v.as(List.class)))
//                        .targetTypeMapping(Value.class, Object.class, Value::hasHashEntries, v -> new HashMap<String, Object>(v.as(Map.class)))
                        .build();
                engine = GraalJSScriptEngine.create(null,
                        Context.newBuilder("js")
                                .allowHostAccess(access)
                                .allowHostClassLookup(s -> true)
                );
            }
            if ("groovy".equals(lambda.getLang())){
                script = script.replaceAll("System\\.out\\.","");
            }


            CompiledScript compiled = ((Compilable) engine).compile(script);


            engine.setContext(context);

            Function<String, String> $param$ = req::getParameter;
//            Function<String, Entry> $save$ = (obj)->{
//                Entry entry = mapper.convertValue(obj,Entry.class);
//                try {
//                    entryService.save(entry, lambda);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                return entry;
//            };

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

            Function<Long,List> _filesFromBucket = (bucketId)-> bucketRepository.findPathByBucketId(bucketId);

            Function<String,Path> _path = (filename)->{
                EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(filename);
                String pathStr = destStr;
                if (entryAttachment != null && entryAttachment.getBucketId() != null) {
                    pathStr = destStr + "bucket-" + entryAttachment.getBucketId() + "/";
                }
                return Paths.get(pathStr+filename);
            };


            Function<List<String>,String> _zip = (fileList)->{
                File dir = new File(Constant.UPLOAD_ROOT_DIR + "/attachment/tmp/");
                dir.mkdirs();
                String filename = System.currentTimeMillis()+".zip";
                String zipFile = Constant.UPLOAD_ROOT_DIR + "/attachment/tmp/"+filename;

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

            bindings.put("_param",$param$);
            bindings.put("_out", _out);
            bindings.put("_request", req);
            bindings.put("_response", res);
            bindings.put("_this", lambda);

            lambda.getBinds().forEach(b -> {
                if ("dataset".equals(b.getType())) {
                    Page<Entry> b1 = entryService.findListByDataset(b.getSrcId(), "%", lambda.getEmail(), new HashMap(),null,null,PageRequest.of(0, Integer.MAX_VALUE), req);
//                    System.out.println(b1.getTotalElements());
//                    Map<String, Object> map = mapper.convertValue(b1, HashMap.class);
                    bindings.put(b.getType() + "_" + b.getSrcId(), b1);
                }
                if ("dashboard".equals(b.getType())) {
                    bindings.put(b.getType() + "_" + b.getSrcId(), entryService.getDashboardMapDataNativeNew(b.getSrcId(), new HashMap<>(), lambda.getEmail(), req));
                }
                if ("lookup".equals(b.getType())) {
                    Map<String, Object> b1 = null;
                    try {
                        b1 = lookupService.findAllEntry(b.getSrcId(), "", req, true, PageRequest.of(0, Integer.MAX_VALUE));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    bindings.put(b.getType() + "_" + b.getSrcId(), b1);
                }
                if ("_entry".equals(b.getType())) {
                    bindings.put("_entry", entryService);
                }
                if ("_mail".equals(b.getType())) {
                    bindings.put("_mail", mailService);
                }
                if ("_endpoint".equals(b.getType())) {
                    bindings.put("_endpoint", endpointService);
                }
                if ("_mapper".equals(b.getType())) {
                    bindings.put("_mapper", mapper);
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
                                    Collectors.toMap(x -> x.getGroup().getId(), x -> x.getGroup()));
                            _user.put("groups", groupMap);
                        }
                    }
                    bindings.put("_user", _user);
                }
                if ("_sql".equals(b.getType())){
                    bindings.put("_sql", sqlService);
                }
                if ("_io".equals(b.getType())){
//                    bindings.put("_path",_path);
//                    bindings.put("_write",_write);
//                    bindings.put("_read",_read);
                    bindings.put("_io",Map.of("write",_write,
                            "read",_read,
                            "path",_path,
                            "zip",_zip,
                            "pathFromBucket",_filesFromBucket));
                }
                bindings.put("_env",Map.of("IO_BASE_API_URL", Constant.IO_BASE_DOMAIN+"/api",
                        "IO_BASE_URL", Constant.IO_BASE_DOMAIN,
                        "UI_BASE_URL", "https://" + lambda.getApp().getAppPath() + "." + Constant.UI_BASE_DOMAIN ,
                        "UPLOAD_DIR", Constant.UPLOAD_ROOT_DIR + "/attachment/",
                        "TMP_DIR", Constant.UPLOAD_ROOT_DIR + "/tmp/"));

            });


            Object val = compiled.eval(bindings);

            result.put("success", true);
            result.put("print", writer.toString().trim());
            result.put("out", bindings.get("_out"));
            System.out.println("######123Lambda");
        } catch (ScriptException exp) {
            System.out.println("!!######123LambdaError");

            result.put("success", false);
            result.put("message", exp.getMessage());
            result.put("line", exp.getLineNumber());
            result.put("col", exp.getColumnNumber());
        }

        return result;
    }


    @Scheduled(cron = "0 0/10 * * * ?") //0 */1 * * * *
    public Map<String, Object> runSchedule() {

        Calendar now = Calendar.getInstance();

        String clock = String.format("%02d%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE));
        int day = now.get(Calendar.DAY_OF_WEEK); // sun=1, mon=2, tues=3,wed=4,thur=5,fri=6,sat=7
        int date = now.get(Calendar.DAY_OF_MONTH);
        int month = now.get(Calendar.MONTH); // 0-based month, ie: Jan=0, Feb=1, March=2

        // lambdaRepository.findByClockAndDayAndDateAndMonth(clock,day,date,month){
        // //select from lambda }

        lambdaRepository.findScheduledByClock(clock).forEach(s -> {
            if ("daily".equals(s.getFreq()) ||
                    ("weekly".equals(s.getFreq()) && s.getDayOfWeek() == day) ||
                    ("monthly".equals(s.getFreq()) && s.getDayOfMonth() == date) ||
                    ("yearly".equals(s.getFreq()) && s.getMonthOfYear() == month && s.getDayOfMonth() == date)
            ) {
                User user = userRepository.findFirstByEmailAndAppId(s.getEmail(), s.getApp().getId()).orElse(null);
                try {
                    long start = System.currentTimeMillis();
                    run(s.getId(), null, null, UserPrincipal.create(user));
                    long end = System.currentTimeMillis();
                    System.out.println("Duration:"+(end-start));
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
            return CompletableFuture.completedFuture(execLambda(id, req, res, userPrincipal).get(action));
        }

    }

    public CompletableFuture<Object> actionCode(String code, HttpServletRequest req, HttpServletResponse res, UserPrincipal userPrincipal, String action) throws ScriptException {
        Lambda l = lambdaRepository.findFirstByCode(code)
                .orElseThrow(()->new ResourceNotFoundException("Lambda","code",code));

        String name = userPrincipal == null ? null : userPrincipal.getName();
        boolean isPublic = l.isPublicAccess();
//        System.out.println("fromPrivate:"+anonymous);
        if (name==null && !isPublic) {
            // access to private lambda from public endpoint is not allowed
            throw new OAuth2AuthenticationProcessingException("Private Lambda: Access to private lambda without authentication is not allowed");
        } else {
            long start = System.currentTimeMillis();
            CompletableFuture<Object> cf = CompletableFuture.completedFuture(execLambda(l.getId(), req, res, userPrincipal).get(action));
            long end = System.currentTimeMillis();
            System.out.println("Duration:"+(end-start));
            return cf;
        }


    }

    public boolean checkByCode(String code) {
        return lambdaRepository.checkByCode(code)>0;
    }
}
