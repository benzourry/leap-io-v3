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
    public RestorePoint create(@RequestBody RestorePoint restorePoint,
                               @RequestParam("appId") Long appId,
                               @RequestParam("email") String email){
        return this.restorePointService.create(restorePoint, appId, email);
    }

    @PostMapping("{id}/restore")
    public Map restore(@PathVariable("id") Long id,
                       @RequestParam(value="clear",required = false) boolean clear){
        return this.restorePointService.restore(id, clear);
    }

    @PostMapping("{id}/delete")
    public Map delete(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        this.restorePointService.delete(id);
        data.put("success", true);
        return data;
    }

    @GetMapping
    public Page<RestorePoint> findByAppId(@RequestParam("appId") Long appId, Pageable pageable){
        return this.restorePointService.findByAppId(appId, pageable);
    }
}

