package com.benzourry.leap.controller;

import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.User;
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
@RequestMapping("/api/app-user")
//@CrossOrigin(allowCredentials="true")
public class AppUserController {

    final AppUserService appUserService;

    public AppUserController(AppUserService appUserService){
//        this.appRepository = appRepository;
        this.appUserService = appUserService;
    }
    @GetMapping
    public List<AppUser> findOne(@RequestParam("appId") Long appId,
                                 @RequestParam("email") String email){
        return appUserService.findByAppIdAndEmail(appId, email);
    }
    @GetMapping("/by-userid/{userId}")
    public List<AppUser> findByUserId(@PathVariable("userId") Long userId){
        return appUserService.findByUserId(userId);
    }

    @PostMapping
    public AppUser save(@RequestBody AppUser appUser,
                        @RequestParam("email") String email){
        return this.appUserService.save(appUser);
    }

    @PostMapping("/{id}/delete")
    public Map<String,Object> delete(@PathVariable("id") Long id,
                                     @RequestParam("email") String email){
        Map<String, Object> data = new HashMap<>();
        this.appUserService.deleteById(id);
        data.put("success", true);
        return data;
    }

    @PostMapping("/delete")
    public Map<String,Object> deleteByAppIdAndEmail(@RequestParam("userId") Long userId,
                                                    @RequestParam(value="appUserId", required = false) Long appUserId){
        Map<String, Object> data = new HashMap<>();
        if (appUserId!=null){
            this.appUserService.deleteById(appUserId);
        }else if (userId!=null){
            this.appUserService.deleteUserById(userId);
        }

        data.put("success", true);
        return data;
    }


    @PostMapping("/{id}/approval")
    public AppUser approval(@PathVariable("id") Long id,
                            @RequestParam("status") String status,
                            @RequestBody AppController.AppUserPayload payload){

        return this.appUserService.approval(id, status, payload.tags());
    }

    @PostMapping("/user/{id}/approval")
    public User userApproval(@PathVariable("id") Long id,
                         @RequestParam("status") String status){
        return this.appUserService.userApproval(id, status);
    }

    @GetMapping("/by-group")
    public Page<AppUser> findByGroup(@RequestParam("groupId") Long groupId,
                                     Pageable pageable){
        return this.appUserService.findByGroupId(groupId, pageable);
    }
    @GetMapping("/by-app")
    public Page<AppUser> findByAppId(@RequestParam("appId") Long appId,
                                     @RequestParam("status") List<String> status,
                                     Pageable pageable){
        return this.appUserService.findByAppIdAndStatus(appId, "%", null, null, pageable);
    }

    @PostMapping("/save-order")
    public List<Map<String, Long>> saveTierOrder(@RequestBody List<Map<String, Long>> userOrderList){
        return appUserService.saveOrder(userOrderList);
    }


}
