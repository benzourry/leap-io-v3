package com.benzourry.leap.controller;


import com.benzourry.leap.model.AppGroup;
import com.benzourry.leap.model.KeyValue;
import com.benzourry.leap.repository.AppGroupRepository;
import com.benzourry.leap.security.CurrentUser;
import com.benzourry.leap.security.UserPrincipal;
import com.benzourry.leap.service.AppService;
import com.benzourry.leap.service.KeyValueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AuthorizationServiceException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Created by MohdRazif on 5/11/2016.
 */
@RestController
@RequestMapping("api/platform")
public class PlatformController {

    @Autowired
    KeyValueService keyValueService;

    @Autowired
    AppGroupRepository appGroupRepository;

    @Autowired
    AppService appService;


    @GetMapping
    public KeyValue getByKey(@RequestParam("key") String key){
        return keyValueService.getByKey(key);
    }

    @GetMapping("keyvalue/{group}")
    public List<KeyValue> getByGroup(@PathVariable("group") String group){
        return keyValueService.getByGroup(group);
    }
    @GetMapping("keyvalue/{group}/{key}")
    public KeyValue getByGroupAndKey(@PathVariable("group") String group,
                                                 @PathVariable("key") String key){
        return keyValueService.getByGroupAndKey(group, key);
    }

    @PostMapping("keyvalue/{group}/{key}")
    public KeyValue save(@PathVariable("group") String group,
                         @PathVariable("key") String key,
                         @RequestBody KeyValue keyvalue,
                         @CurrentUser UserPrincipal principal){
        if (!keyValueService.getValue("platform","managers").orElse("").contains(principal.getEmail())){
            throw new AuthorizationServiceException("Unauthorized modification :" + principal.getEmail());
        }
        return keyValueService.save(group, key, keyvalue);
    }

    @PostMapping("keyvalue/{group}/{key}/remove")
    public Map<String, Object> removePropByGroupAndKey(@PathVariable("group") String group,
                                                       @PathVariable("key") String key,
                                                       @CurrentUser UserPrincipal principal){
        if (!keyValueService.getValue("platform","managers").orElse("").contains(principal.getEmail())){
            throw new AuthorizationServiceException("Unauthorized modification :" + principal.getEmail());
        }
        return keyValueService.removePropByGroupAndKey(group, key);
    }
    @PostMapping("keyvalue/{id}/remove")
    public Map<String, Object> removeProp(@PathVariable("id") Long id,
                                          @CurrentUser UserPrincipal principal){
        if (!keyValueService.getValue("platform","managers").orElse("").contains(principal.getEmail())){
            throw new AuthorizationServiceException("Unauthorized modification :" + principal.getEmail());
        }
        return keyValueService.removeProp(id);
    }
    @GetMapping(value="keyvalue/all")
    public List<KeyValue> getAll(){
        return keyValueService.getAll();
    }

    @GetMapping(value="keyvalue/group-all")
    public Map<String,List<KeyValue>> getAllGroup(){
        return keyValueService.getAllGroup();
    }

    @GetMapping(value="keyvalue/map-all")
    public Map getAllGroupMap(){
        return keyValueService.getAllGroupMap();
    }


    @GetMapping("appgroup")
    public Page<AppGroup> listAppGroup(@RequestParam(value = "email", required = false) String email, Pageable pageable){
        return appGroupRepository.findByParams(email,Pageable.unpaged());
    }

    @PostMapping("appgroup")
    public AppGroup saveAppGroup(@RequestBody AppGroup appGroup){
        return appGroupRepository.save(appGroup);
    }

    @PostMapping("appgroup/{id}/remove")
    public Map<String, Object> removeAppGroup(@PathVariable("id") Long id){
        appGroupRepository.deleteById(id);
        return Map.of("success", true);
    }


    @GetMapping("summary")
    @Cacheable("platform.summary")
    public ResponseEntity<Map> getSummary() {

        var now = new Date();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");
        LocalDateTime localDate = LocalDateTime.now();

        // bilangan form,dataset, dashboard, screen, users
        Map<String, Object> summary = appService.getPlatformSummary();
        summary.put("updatedOn", dtf.format(localDate));


        keyValueService.saveValue("platform","summary-updated",dtf.format(localDate));

        return ResponseEntity.ok().cacheControl(CacheControl.maxAge(20, TimeUnit.SECONDS))
                .body(summary);
    }


    @CacheEvict(value = "platform.summary", allEntries = true)
    @Scheduled(fixedRate = 3600000)
    public void emptyHotelsCache() {
        System.out.println("emptying [platform.summary] cache");
    }



}
