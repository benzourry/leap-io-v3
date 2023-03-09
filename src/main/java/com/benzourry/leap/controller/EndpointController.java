package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.EndpointMixin;
import com.benzourry.leap.model.Endpoint;
import com.benzourry.leap.service.EndpointService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"api/endpoint","api/public/endpoint"})
public class EndpointController {

//    @Autowired
    private final EndpointService endpointService;

    public EndpointController(EndpointService endpointService){
        this.endpointService = endpointService;
    }

    @PostMapping
    public Endpoint save(@RequestBody Endpoint endpoint,
                         @RequestParam("appId") Long appId,
                         @RequestParam("email") String email){
        return endpointService.save(endpoint, appId, email);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Endpoint.class, mixin = EndpointMixin.EndpointBasicList.class)
    })
    public Page<Endpoint> findByAppId(@RequestParam Long appId, Pageable pageable){
        return endpointService.findByAppId(appId, pageable);
    }

    @GetMapping("shared")
    public Page<Endpoint> findShared(Pageable pageable){
        return endpointService.findShared(pageable);
    }

    @GetMapping("{id}")
    public Endpoint findById(@PathVariable Long id){
        return endpointService.findById(id);
    }

    @PostMapping("{id}/delete")
    public Map<String,Object> delete(@PathVariable("id") Long id) {
        Map<String, Object> data = new HashMap<>();
        endpointService.delete(id);
        data.put("success", "true");
        return data;
    }

    @GetMapping("/run/{restId}")
    public Object getRegList(@PathVariable("restId") Long restId,HttpServletRequest request) throws IOException, InterruptedException {
        return endpointService.runEndpoint(restId, request);
    }

    @GetMapping("/run")
    public Object getRegList(@RequestParam("code") String code, @RequestParam("appId") Long appId,
                             @RequestBody(required = false) Object body,  HttpServletRequest request) throws IOException, InterruptedException {
        return endpointService.runEndpointByCode(code, appId, request, body);
    }

    @GetMapping("/clear-token")
    public Map<String,Object> clearToken(@RequestParam("pair") String pair,  HttpServletRequest request) {
        endpointService.clearTokens(pair);
        return Map.of("success",true);
    }

//    @GetMapping("/all-token")
//    public List<AccessToken> allToken(@RequestParam("pair") String pair, HttpServletRequest request) throws IOException, InterruptedException {
//        endpointService
//        return Map.of("success",true);
//    }

//    @GetMapping("/run-wc")
//    public Object getRegList2(@RequestParam("code") String code, @RequestParam("appId") Long appId, @RequestParam MultiValueMap<String, String> queryMap, HttpServletRequest request){
//        return endpointService.getSecureEndpoint(code, appId,queryMap, request);
//    }

}
