package com.benzourry.leap.controller;

import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.service.AppUserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.jboss.aerogear.security.otp.Totp;
//import org.jboss.aerogear.security.otp.api.Base32;
//import org.jboss.aerogear.security.otp.api.Clock;

@RestController
@RequestMapping("api/app-user")
//@CrossOrigin(allowCredentials="true")
public class AppUserController {

//    AppRepository appRepository;
//
    final AppUserService appUserService;

    public AppUserController(AppUserService appUserService){
//        this.appRepository = appRepository;
        this.appUserService = appUserService;
    }

    @GetMapping
    public List<AppUser> findOne(@RequestParam Long appId, @RequestParam String email){
        return appUserService.findByAppIdAndEmail(appId, email);
    }

    @GetMapping("by-userid/{userId}")
    public List<AppUser> findByUserId(@PathVariable Long userId){
        return appUserService.findByUserId(userId);
    }

    @PostMapping
    public AppUser save(@RequestBody AppUser appUser, @RequestParam("email") String email){
        return this.appUserService.save(appUser);
    }

    @PostMapping("{id}/delete")
    public Map<String,Object> delete(@PathVariable Long id, @RequestParam("email") String email){
        Map<String, Object> data = new HashMap<>();
        this.appUserService.deleteById(id);
        data.put("success", true);
        return data;
    }

    @PostMapping("{id}/approval")
    public AppUser approval(@PathVariable Long id, @RequestParam String status){

//        List<AppUser> appUserList = appUserService.findByUserId(appUser.getUser().getId());
        return this.appUserService.approval(id, status);
    }

    @GetMapping("by-group")
    public Page<AppUser> findByGroup(@RequestParam("groupId") Long groupId, Pageable pageable){
        return this.appUserService.findByGroupId(groupId, pageable);
    }
    @GetMapping("by-app")
    public Page<AppUser> findByAppId(@RequestParam("appId") Long appId, @RequestParam List<String> status, Pageable pageable){
        return this.appUserService.findByAppIdAndStatus(appId, "%", null, null, pageable);
    }

    @PostMapping("save-order")
    public List<Map<String, Long>> saveTierOrder(@RequestBody List<Map<String, Long>> userOrderList){
        return appUserService.saveOrder(userOrderList);
    }


}
