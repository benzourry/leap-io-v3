package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.GroupMixin;
import com.benzourry.leap.model.AppUser;
import com.benzourry.leap.model.UserGroup;
import com.benzourry.leap.service.AppUserService;
import com.benzourry.leap.service.UserGroupService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/group")
public class UserGroupController {

    private final UserGroupService userGroupService;

    private final AppUserService appUserService;

    public UserGroupController(UserGroupService userGroupService,
                               AppUserService appUserService) {
        this.userGroupService = userGroupService;
        this.appUserService = appUserService;
    }

    @PostMapping
    public UserGroup save(@RequestBody UserGroup userGroup,
                          @RequestParam("appId") Long appId){
        return userGroupService.save(userGroup, appId);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = UserGroup.class, mixin = GroupMixin.GroupBasicList.class)
    })
    public Page<UserGroup> findByAppId(@RequestParam("appId") Long appId, Pageable pageable){
        return userGroupService.findByAppId(appId, pageable);
    }

    @GetMapping("{id}")
    public UserGroup findById(@PathVariable("id") Long id){
        return userGroupService.findById(id);
    }

    @GetMapping("{id}/user")
    public Page<AppUser> userByGroupId(@PathVariable("id") Long id,
                                     @RequestParam(value="searchText",defaultValue = "") String searchText,
                                     @RequestParam(value="status",required = false) List<String> status,
                                     Pageable pageable){
        return appUserService.userByGroupId(id,searchText,status,pageable);
    }


    @PostMapping("{id}/delete")
    public Map<String,Object> delete(@PathVariable("id") Long id) {
        Map<String, Object> data = new HashMap<>();
        userGroupService.delete(id);
        data.put("success", "true");
        return data;
    }

    @GetMapping("reg-list")
    public List<UserGroup> getRegList(@RequestParam("appId") Long appId){
        return userGroupService.findRegListByAppId(appId);
    }

    @GetMapping("all-list")
    public List<UserGroup> getAllList(@RequestParam("appId") Long appId){
        return userGroupService.findAllListByAppId(appId);
    }

}
