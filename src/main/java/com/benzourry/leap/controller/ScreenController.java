package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.DashboardMixin;
import com.benzourry.leap.mixin.DatasetMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.mixin.ScreenMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.ScreenService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
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


    /** ## SCREEN **/
    @PostMapping
    public Screen saveScreen(@RequestParam long appId, @RequestBody Screen screen){
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
//        Map<String, Object> data = new HashMap<>();
        return screenService.getActionComps(id);
//        return data;
    }

    @PostMapping("{screenId}/actions")
    public Action saveAction(@PathVariable Long screenId,
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
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
    })
    public List<Screen> getScreenList(@RequestParam long appId){
        return screenService.findByAppId(appId);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Screen.class, mixin = ScreenMixin.ScreenOne.class),
            @JsonMixin(target = Dataset.class, mixin = ScreenMixin.ScreenOneDataset.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class)
    })
    public Screen getScreen(@PathVariable long id){
        return screenService.getScreen(id);
    }

}
