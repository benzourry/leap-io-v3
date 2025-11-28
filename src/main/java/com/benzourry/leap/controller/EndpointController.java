package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.EndpointMixin;
import com.benzourry.leap.model.Endpoint;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.EndpointService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping({"api/endpoint","api/public/endpoint"})
public class EndpointController {

//    @Autowired
    private final EndpointService endpointService;

    @Value("${instance.endpoint.response-buffer-size:8192}")
    int ENDPOINT_RESPONSE_BUFFER;

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
    public Page<Endpoint> findByAppId(@RequestParam("appId") Long appId, Pageable pageable){
        return endpointService.findByAppId(appId, pageable);
    }

    @GetMapping("shared")
    public Page<Endpoint> findShared(Pageable pageable){
        return endpointService.findShared(pageable);
    }

    @GetMapping("{id}")
    public Endpoint findById(@PathVariable("id") Long id){
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
    public void runEndpoint(@PathVariable("restId") Long restId, @CurrentUser UserPrincipal userPrincipal, HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException {

        HttpResponse<InputStream> upstream =
                endpointService.runEndpointById(restId, request, userPrincipal);

        // 1) Set status code
        response.setStatus(upstream.statusCode());

        // 2) Copy headers (skip hop-by-hop ones)
//        upstream.headers().map().forEach((key, values) -> {
//            if (!"transfer-encoding".equalsIgnoreCase(key)) {
//                for (String v : values) response.addHeader(key, v);
//            }
//        });

        Set<String> allowedHeaders = Set.of("content-type", "content-disposition","cache-control",
                "expires","pragma","date");

        upstream.headers().map().forEach((key, values) -> {
            if (allowedHeaders.contains(key.toLowerCase())) {
                for (String v : values) response.addHeader(key, v);
            }
        });

        // 3) Stream body to client
        try (InputStream in = upstream.body();
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[ENDPOINT_RESPONSE_BUFFER];
            int len;

            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        }

    }

    @GetMapping("/run")
    public Object runEndpointByCode(@RequestParam("code") String code, @RequestParam("appId") Long appId,
                                    @RequestBody(required = false) Object body, @CurrentUser UserPrincipal userPrincipal, HttpServletRequest request) throws IOException, InterruptedException {
        return endpointService.runEndpointByCode(code, appId, request, body, userPrincipal);
    }


    @GetMapping("/run/{appId}/{code}")
    public void runEndpointByCodePath(
            @PathVariable("code") String code,
            @PathVariable("appId") Long appId,
            @RequestBody(required = false) Object body,
            HttpServletRequest request,
            HttpServletResponse response,
            @CurrentUser UserPrincipal userPrincipal
    ) throws Exception {

        HttpResponse<InputStream> upstream =
                endpointService.runEndpointByCode(code, appId, request, body, userPrincipal);

        // 1) Set status code
        response.setStatus(upstream.statusCode());

        // 2) Copy headers (skip hop-by-hop ones)
//        upstream.headers().map().forEach((key, values) -> {
//            if (!"transfer-encoding".equalsIgnoreCase(key)) {
//                for (String v : values) response.addHeader(key, v);
//            }
//        });

        Set<String> allowedHeaders = Set.of("content-type", "content-disposition","cache-control",
                "expires","pragma","date");

        upstream.headers().map().forEach((key, values) -> {
            if (allowedHeaders.contains(key.toLowerCase())) {
                for (String v : values) response.addHeader(key, v);
            }
        });


        // 3) Stream body to client
        try (InputStream in = upstream.body();
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[ENDPOINT_RESPONSE_BUFFER];
            int len;

            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
        }
    }

    @GetMapping("/clear-token")
    public Map<String,Object> clearToken(@RequestParam("pair") String pair,  HttpServletRequest request) {
        endpointService.clearTokens(pair);
        return Map.of("success",true);
    }

}
