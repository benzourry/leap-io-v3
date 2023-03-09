package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.DatasetMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.Form;
import com.benzourry.leap.model.Lambda;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.LambdaService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;

import javax.script.ScriptException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    public Lambda saveLambda(@RequestParam long appId, @RequestBody Lambda lambda,
                             @RequestParam("email") String email,Principal principal){
        if (lambda.getId() != null && !lambda.getEmail().contains(principal.getName())){
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
//            @JsonMixin(target = Lambda.class, mixin = LambdaMixin.LambdaBasicList.class),
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
    })
    public Page<Lambda> getLambdaList(@RequestParam long appId, Pageable pageable){
        return lambdaService.findByAppId(appId, pageable);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
//            @JsonMixin(target = Lambda.class, mixin = LambdaMixin.LambdaOne.class),
//            @JsonMixin(target = Dataset.class, mixin = LambdaMixin.LambdaOneDataset.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class)
    })
    public Lambda getLambda(@PathVariable long id){
        return lambdaService.getLambda(id);
    }


    /*
    * Spring Boot 3 ada issue utk authenticate Async/CompletableFuture response.
    * Terpaksa tambah .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll() dalam SecurityFilterConfig
    * */
    @GetMapping("{id}/run")
    public CompletableFuture<Map<String, Object>> runLambda(@PathVariable(value = "id") Long id, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        return lambdaService.run(id, req, res, userPrincipal);
    }

    @RequestMapping(value = "{id}/out", method = {RequestMethod.GET,RequestMethod.POST})
    public CompletableFuture<Object> outLambda(@PathVariable(value = "id") Long id, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        return lambdaService.out(id, req, res, userPrincipal);
    }

    @RequestMapping(value = "{id}/{action}", method = {RequestMethod.GET,RequestMethod.POST})
    public CompletableFuture<Object> actionLambda(@PathVariable(value = "id") Long id,@PathVariable(value = "action") String action, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
        return lambdaService.action(action,id, req,res,userPrincipal);
    }

    @GetMapping("check-by-code")
    public boolean check(@RequestParam(value = "code") String code) {
//        System.out.println("CHECK BY KEY CTRL:"+appPath);
//        Map<String, Object> data = new HashMap<>();
//        data.put("exist",this.appService.checkByKey(appPath));
        return this.lambdaService.checkByCode(code);
    }



    @RestController
    @RequestMapping({"~"})
//@CrossOrigin(allowCredentials="true")
    public class LambdaControllerPublic {
        @RequestMapping(value = "{code}/{action}", method = {RequestMethod.GET,RequestMethod.POST})
        public CompletableFuture<Object> printLambdaCode(@PathVariable(value = "code") String code,@PathVariable(value = "action") String action, HttpServletRequest req, HttpServletResponse res, @CurrentUser UserPrincipal userPrincipal) throws ScriptException {
            return lambdaService.actionCode(code, req, res, userPrincipal,action);
        }

    }


}

