package com.benzourry.leap.controller;

import com.benzourry.leap.service.LiveService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("live")
public class LiveController {


    private LiveService liveService;
    public LiveController(LiveService liveService){
        this.liveService = liveService;
    }
    @RequestMapping(value = "pass/{channel:.+}", method = RequestMethod.POST)
    public LiveService.LiveMessage passFromHttp(@PathVariable("channel") String channel,
                                                @RequestBody LiveService.LiveMessage message){
        return liveService.pass(channel, message);
    }


}
