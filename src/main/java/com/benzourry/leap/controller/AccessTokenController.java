package com.benzourry.leap.controller;

import com.benzourry.leap.model.AccessToken;
import com.benzourry.leap.service.AccessTokenService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/at")
public class AccessTokenController {

    private final AccessTokenService accessTokenService;

    public AccessTokenController(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }


    @GetMapping("/clear-token")
    public Map<String,Object> clearToken(@RequestParam("pair") String pair, HttpServletRequest request){
        accessTokenService.clearAccessToken(pair);
        return Map.of("success",true);
    }

    @GetMapping("/all-token")
    public List<AccessToken> allToken(@RequestParam("pair") String pair, HttpServletRequest request){
        return accessTokenService.getAllAccessToken();
    }
}
