package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.*;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.ScreenService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/screen")
//@CrossOrigin(allowCredentials="true")
public class ScreenController {

    public final ScreenService screenService;

    public ScreenController(ScreenService screenService){
        this.screenService = screenService;
    }

    @PostMapping
    public Screen saveScreen(@RequestParam("appId") long appId,
                             @RequestBody Screen screen){
        return screenService.saveScreen(appId, screen);
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> removeScreen(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        screenService.removeScreen(id);
        return data;
    }

    @GetMapping("{id}/action-comps")
    @JsonResponse(mixins = {
            @JsonMixin(target = Screen.class, mixin = ScreenMixin.ScreenBasicList.class),
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dashboard.class, mixin = DashboardMixin.DashboardBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
    })
    public Map<String, Object> getActionComponents(@PathVariable("id") Long id){
        return screenService.getActionComps(id);
    }

    @PostMapping("{screenId}/actions")
    public Action saveAction(@PathVariable("screenId") Long screenId,
                             @RequestBody Action action){
        return screenService.saveAction(screenId, action);
    }

    @PostMapping("actions/{id}/delete")
    public Map<String, Object> removeAction(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        screenService.removeAction(id);
        return data;
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Screen.class, mixin = ScreenMixin.ScreenBasicList.class),
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class),
            @JsonMixin(target = Cogna.class, mixin = CognaMixin.CognaBasicList.class),

    })
    public List<Screen> getScreenList(@RequestParam("appId") long appId,
                                      @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable){
        return screenService.findByAppId(appId, pageable);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Screen.class, mixin = ScreenMixin.ScreenOne.class),
            @JsonMixin(target = Dataset.class, mixin = ScreenMixin.ScreenOneDataset.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class),
            @JsonMixin(target = Cogna.class, mixin = CognaMixin.CognaHideSensitive.class)
    })
    public Screen getScreen(@PathVariable("id") long id){
        return screenService.getScreen(id);
    }


    @PostMapping("clone")
    public Screen clone(@RequestParam("screenId") Long screenId,
                        @RequestParam("appId") Long appId){
        return screenService.cloneScreen(screenId, appId);
    }


    @PostMapping("save-screen-order")
    public List<Map<String, Long>> saveScreenOrder(@RequestBody List<Map<String, Long>> screenList){
        return screenService.saveScreenOrder(screenList);
    }



}
