package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.PushSubMixin;
import com.benzourry.leap.model.PushSub;
import com.benzourry.leap.service.PushService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/push")
public class PushController {


    final PushService pushService;

    public PushController(PushService pushService) {
        this.pushService = pushService;
    }


    @PostMapping("check")
    @JsonResponse(mixins = {
            @JsonMixin(target = PushSub.class, mixin = PushSubMixin.Basic.class)
    })
    public PushSub check(@RequestBody PushSub pushSub) {
//        Map<String, Object> data = new HashMap<>();
        return pushService.findByEndpoint(pushSub);
//        data.put("success", true);
//        return data;
    }

    @PostMapping("subscribe")
    @JsonResponse(mixins = {
            @JsonMixin(target = PushSub.class, mixin = PushSubMixin.Basic.class)
    })
    public PushSub subscribe(@RequestBody PushSub pushSub, @RequestParam Long userId) {
//        Map<String, Object> data = new HashMap<>();
        return pushService.subscribe(pushSub, userId);
//        data.put("success", true);
//        return data;
    }

    @PostMapping("unsubscribe")
    @JsonResponse(mixins = {
            @JsonMixin(target = PushSub.class, mixin = PushSubMixin.Basic.class)
    })
    public Map<String, Object> unsubscribe(@RequestBody PushSub pushSub) {
        Map<String, Object> data = new HashMap<>();

        pushService.unsubscribe(pushSub.getEndpoint());
        data.put("success", true);
        return data;
//        return pushService.unsubscribe(pushSub.getEndpoint());
    }

//    @PostMapping("resubscribe")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = PushSub.class, mixin = PushSubMixin.Basic.class)
//    })
//    public PushSub resubscribe(@RequestBody PushSub pushSub) {
////        Map<String, Object> data = new HashMap<>();
//
//        return pushService.resubscribe(pushSub.getEndpoint());
////        data.put("success", true);
////        return data;
//    }

    @RequestMapping("send")
    public Map<String, Object> send(@RequestParam("userId") Long userId,
                                    @RequestParam String title,
                                    @RequestParam String body,
                                    @RequestParam(required = false) String url) {
        return pushService.send(userId, title, body, url);
    }

    @RequestMapping("send-by-email")
    public Map<String, Object> sendByEmail(@RequestParam Long appId,
                                           @RequestParam("email") String email,
                                    @RequestParam String title,
                                    @RequestParam String body,
                                           @RequestParam(required = false) String url) {
        return pushService.sendByEmail(email,appId, title, body, url);
    }


    @GetMapping("send-all")
    public void sendAll(@RequestParam Long appId, @RequestParam String title, @RequestParam String body,@RequestParam(required = false) String url) {
        pushService.sendAll(appId, title, body, url);
    }

    @GetMapping("subscription")
    @JsonResponse(mixins = {
            @JsonMixin(target = PushSub.class, mixin = PushSubMixin.Basic.class)
    })
    public List<PushSub> getSubscriptionByUserId(@RequestParam("userId") Long userId){
        return pushService.getSubscriptions(userId);
    }
}
