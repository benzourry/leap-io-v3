package com.benzourry.leap.controller;

import com.benzourry.leap.model.RestorePoint;
import com.benzourry.leap.service.RestorePointService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("api/restore-point")
public class RestorePointController {

    public final RestorePointService restorePointService;

    public RestorePointController(RestorePointService restorePointService){
        this.restorePointService = restorePointService;
    }

    @PostMapping("create")
    public RestorePoint create(@RequestBody RestorePoint restorePoint, @RequestParam Long appId, @RequestParam String email){
        return this.restorePointService.create(restorePoint, appId, email);
    }

    @PostMapping("{id}/restore")
    public Map restore(@PathVariable Long id,@RequestParam(required = false) boolean clear){
        return this.restorePointService.restore(id, clear);
    }

    @PostMapping("{id}/delete")
    public Map delete(@PathVariable Long id){
        Map<String, Object> data = new HashMap<>();
        this.restorePointService.delete(id);
        data.put("success", true);
        return data;
    }

    @GetMapping
    public Page<RestorePoint> findByAppId(@RequestParam Long appId, Pageable pageable){
        return this.restorePointService.findByAppId(appId, pageable);
    }
}

