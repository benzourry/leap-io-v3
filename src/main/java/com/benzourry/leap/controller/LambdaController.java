package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.DatasetMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.mixin.LambdaMixin;
import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.Form;
import com.benzourry.leap.model.Lambda;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.LambdaService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.script.ScriptException;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping({"api/lambda"})
//@CrossOrigin(allowCredentials="true")
public class LambdaController {

    public final LambdaService lambdaService;

    public LambdaController(LambdaService lambdaService){
        this.lambdaService = lambdaService;
    }


    /** ## SCREEN **/
    @PostMapping
    public Lambda saveLambda(@RequestParam("appId") long appId,
                             @RequestBody Lambda lambda,
                             @RequestParam("email") String email,
                             Principal principal){
        if (lambda.getId() != null && !lambda.getEmail().contains(principal.getName())){
            System.out.println("Lambda saved by: "+principal.getName());
            throw new AuthorizationServiceException("Unauthorized modification by non-creator :" + principal.getName());
        }
        return lambdaService.saveLambda(appId, lambda, email);
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> removeLambda(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        lambdaService.removeLambda(id);
        return data;
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Lambda.class, mixin = LambdaMixin.LambdaBasicList.class),
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
    })
    public Page<Lambda> getLambdaList(@RequestParam("appId") long appId,
                                      Pageable pageable){
        return lambdaService.findByAppId(appId, pageable);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
//            @JsonMixin(target = Lambda.class, mixin = LambdaMixin.LambdaOne.class),
//            @JsonMixin(target = Dataset.class, mixin = LambdaMixin.LambdaOneDataset.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class)
    })
    public Lambda getLambda(@PathVariable("id") long id){
        return lambdaService.getLambda(id);
    }


    /*
    * Spring Boot 3 ada issue utk authenticate Async/CompletableFuture response.
    * Terpaksa tambah .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll() dalam SecurityFilterConfig
    * */
    @GetMapping("{id}/run")
    public CompletableFuture<Map<String, Object>> runLambda(@PathVariable("id") Long id,
                                                            HttpServletRequest req,
                                                            HttpServletResponse res,
                                                            @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        long startTime = System.currentTimeMillis();
        CompletableFuture<Map<String, Object>> result = lambdaService.run(id, req, res, null,userPrincipal);
        long endTime = System.currentTimeMillis();
        System.out.println("Duration:"+(endTime-startTime));
        return result;
    }

//    @GetMapping("{id}/stream")
//    public Stream<String> runLambda(@PathVariable("id") Long id, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
//        return lambdaService.run(id, req, res, userPrincipal);
//    }

    @GetMapping("{id}/stream")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> streamLambda(@PathVariable("id") Long id,
                                                           HttpServletRequest req,
                                                           HttpServletResponse res,
                                                           @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        StreamingResponseBody stream = out -> {
            try {
                lambdaService.stream(id, req, res, out, userPrincipal);
            } catch (ScriptException e) {
                out.write(e.getMessage().getBytes());
            }
        };
        return CompletableFuture.completedFuture(new ResponseEntity(stream, HttpStatus.OK));
    }

    @RequestMapping(value = "{id}/out", method = {RequestMethod.GET,RequestMethod.POST})
    public CompletableFuture<Object> outLambda(@PathVariable("id") Long id,
                                               HttpServletRequest req,
                                               HttpServletResponse res,
                                               @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        return lambdaService.out(id, req, res, userPrincipal);
    }

    @RequestMapping(value = "{id}/pdf", method = {RequestMethod.GET,RequestMethod.POST})
    public ResponseEntity<byte[]> pdfLambda(@PathVariable("id") Long id,
                                            HttpServletRequest req,
                                            HttpServletResponse res,
                                            @CurrentUser UserPrincipal userPrincipal) throws ScriptException, IOException {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(lambdaService.pdf(id,null, req, res, userPrincipal));
    }

//    @RequestMapping(value = "{id}/signed-pdf", method = {RequestMethod.GET,RequestMethod.POST})
//    public ResponseEntity<byte[]> signedPdfLambda(@PathVariable("id") Long id,
//                                            HttpServletRequest req,
//                                            HttpServletResponse res,
//                                            @CurrentUser UserPrincipal userPrincipal) throws ScriptException, IOException {
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().toString())
//                .contentType(MediaType.APPLICATION_PDF)
//                .body(lambdaService.pdf(id,null, req, res, userPrincipal));
//    }

    @RequestMapping(value = "{id}/{action}", method = {RequestMethod.GET,RequestMethod.POST})
    public CompletableFuture<Object> actionLambda(@PathVariable("id") Long id,
                                                  @PathVariable("action") String action,
                                                  HttpServletRequest req,
                                                  HttpServletResponse res,
                                                  @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        return lambdaService.action(action,id, req,res,userPrincipal);
    }

    @GetMapping("check-by-code")
    public boolean check(@RequestParam(value = "code") String code) {
//        System.out.println("CHECK BY KEY CTRL:"+appPath);
//        Map<String, Object> data = new HashMap<>();
//        data.put("exist",this.appService.checkByKey(appPath));
        return this.lambdaService.checkByCode(code);
    }


    @GetMapping("cache-evict")
    @CacheEvict(value = "lambdas", key = "{#code,#action}")
    public Map<String, Object> evictLambdaCode(@RequestParam(value = "code") String code,
                                               @RequestParam(value = "action") String action,
                                               HttpServletRequest req,
                                               HttpServletResponse res,
                                               @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        return Map.of("success",true);
    }



    @RestController
    @RequestMapping({"~"})
//@CrossOrigin(allowCredentials="true")
    public class LambdaControllerPublic {

        public final LambdaService lambdaService;

        public LambdaControllerPublic(LambdaService lambdaService){
            this.lambdaService = lambdaService;
        }

        @RequestMapping(value = "{code}/pdf", method = {RequestMethod.GET,RequestMethod.POST})
        public ResponseEntity<byte[]> pdfLambda(@PathVariable("code") String code, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException, IOException {
//            lambdaService.pdf(null,code, req, res, userPrincipal);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(lambdaService.pdf(null,code, req, res, userPrincipal));
        }


//        @RequestMapping(value = "{code}/signed-pdf", method = {RequestMethod.GET,RequestMethod.POST})
//        public ResponseEntity<byte[]> signedPdfLambda(@PathVariable("code") String code, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException, IOException {
////            lambdaService.pdf(null,code, req, res, userPrincipal);
//            return ResponseEntity.ok()
//                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().toString())
//                    .contentType(MediaType.APPLICATION_PDF)
//                    .body(lambdaService.pdfWithSignature(null,code, req, res, userPrincipal));
//        }



        @RequestMapping(value = "{code}/info", method = {RequestMethod.GET,RequestMethod.POST})
        @JsonResponse(mixins = {
                @JsonMixin(target = Lambda.class, mixin = LambdaMixin.LambdaOneInfo.class)
        })
        public @ResponseBody Lambda lambdaInfo(@PathVariable("code") String code, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException, IOException {
//            lambdaService.pdf(null,code, req, res, userPrincipal);
            return lambdaService.getLambdaByCode(code);
        }


        @RequestMapping(value = "{code}/{action}", method = {RequestMethod.GET,RequestMethod.POST})
        public CompletableFuture<Object> printLambdaCode(@PathVariable("code") String code,@PathVariable("action") String action, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
            return lambdaService.actionCode(code, req, res, null, userPrincipal,action);
        }

        @GetMapping("{code}/stream")
        public CompletableFuture<ResponseEntity<StreamingResponseBody>> streamLambda(@PathVariable("code") String code,
                                                                                     HttpServletRequest req,
                                                                                     HttpServletResponse res,
                                                                                     @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
            StreamingResponseBody stream = out -> {
                try {
                    lambdaService.actionCode(code, req, res, out, userPrincipal, "stream");
                } catch (ScriptException e) {
                    out.write(e.getMessage().getBytes());
                }
            };
            return CompletableFuture.completedFuture(new ResponseEntity(stream, HttpStatus.OK));
        }

        @RequestMapping(value = "{code}/{action}/cache", method = {RequestMethod.GET,RequestMethod.POST})
        @Cacheable(value = "lambdas", key = "{#code,#action}")
        public CompletableFuture<Object> cachedLambdaCode(@PathVariable("code") String code,
                                                          @PathVariable("action") String action,
                                                          HttpServletRequest req,
                                                          HttpServletResponse res,
                                                          @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
            return lambdaService.actionCode(code, req, res, null,userPrincipal,action);
        }

    }


}

