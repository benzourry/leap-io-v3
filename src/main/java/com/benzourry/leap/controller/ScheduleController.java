package com.benzourry.leap.controller;

import com.benzourry.leap.model.Schedule;
import com.benzourry.leap.service.ScheduleService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/sched"})
//@CrossOrigin(allowCredentials="true")
public class ScheduleController {

    public final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService){
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public Schedule saveSschedule(@RequestParam("appId") long appId,
                                  @RequestBody Schedule schedule){
        return scheduleService.save(appId, schedule);
    }

    @PostMapping("/{id}/delete")
    public Map<String, Object> removeSchedule(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        scheduleService.delete(id);
        return data;
    }

    @GetMapping
    public List<Schedule> getScheduleList(@RequestParam("appId") long appId){
        return scheduleService.findByAppId(appId);
    }

    @GetMapping("/{id}")
    public Schedule getSchedule(@PathVariable("id") long id){
        return scheduleService.getSchedule(id);
    }



}
