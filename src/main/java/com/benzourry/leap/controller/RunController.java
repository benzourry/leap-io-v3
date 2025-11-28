package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.*;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.*;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/run")
//@CrossOrigin(allowCredentials="true")
public class RunController {

    final FormService formService;

    final DashboardService dashboardService;

    final DatasetService datasetService;

    final ScreenService screenService;

    final AppService appService;

    public RunController(FormService formService,
                         DashboardService dashboardService,
                         DatasetService datasetService,
                         ScreenService screenService,
                         AppService appService){
        this.formService = formService;
        this.dashboardService = dashboardService;
        this.datasetService = datasetService;
        this.screenService = screenService;
        this.appService = appService;
    }


    @GetMapping("app/{appId}")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneRun.class),
    })
    public App findById(@PathVariable("appId") Long appId) {
        return this.appService.findById(appId);
    }

    @GetMapping("app/path/{key:.+}")
    @JsonResponse(mixins = {
            @JsonMixin(target = App.class, mixin = AppMixin.AppOneRun.class),
    })
    public App findByKey(@PathVariable("key") String key) {
        return this.appService.findByKey(key);
    }



    /** FORM **/

    @GetMapping("form/{formId}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormOneRun.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOneRun.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOneRun.class),
            @JsonMixin(target = Section.class, mixin = FormMixin.FormSectionOneRun.class),
            @JsonMixin(target = Tab.class, mixin = FormMixin.FormTabOneRun.class),
            @JsonMixin(target = Tier.class, mixin = FormMixin.FormEntityHidePrePostOneRun.class),
            @JsonMixin(target = TierAction.class, mixin = FormMixin.FormEntityHidePrePostOneRun.class),
    })
    public Form findByIdRun(@PathVariable("formId") Long formId){
        return formService.findFormById(formId);
    }

    /** DASHBOARD **/
    @GetMapping("dashboard/{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Dashboard.class, mixin = DashboardMixin.BasicDashboard.class),
            @JsonMixin(target = Chart.class, mixin = DashboardMixin.BasicChart.class)
    })
    public Dashboard getDashboard(@PathVariable("id") long id){
        return dashboardService.getDashboard(id);
    }

    @GetMapping("dataset/{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetOneRun.class),
            @JsonMixin(target = DatasetItem.class, mixin = DatasetMixin.DatasetItemOneRun.class),
            @JsonMixin(target = DatasetAction.class, mixin = DatasetMixin.DatasetActionOneRun.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneFormRun.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOneRun.class),
            @JsonMixin(target = Section.class, mixin = FormMixin.FormSectionOneRun.class),
            @JsonMixin(target = Tab.class, mixin = FormMixin.FormTabOneRun.class),
            @JsonMixin(target = Tier.class, mixin = FormMixin.FormEntityHidePrePostOneRun.class),
            @JsonMixin(target = TierAction.class, mixin = FormMixin.FormEntityHidePrePostOneRun.class),
    })
    public Dataset getDataset(@PathVariable("id") long id){
        return datasetService.getDataset(id);
    }

    @GetMapping("screen/{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Screen.class, mixin = ScreenMixin.ScreenOneRun.class),
            @JsonMixin(target = Action.class, mixin = ScreenMixin.ScreenActionOneRun.class),
            @JsonMixin(target = Dataset.class, mixin = ScreenMixin.ScreenOneDataset.class),
            @JsonMixin(target = DatasetItem.class, mixin = DatasetMixin.DatasetItemOneRun.class),
            @JsonMixin(target = DatasetAction.class, mixin = DatasetMixin.DatasetActionOneRun.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneFormRun.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOneRun.class),
            @JsonMixin(target = Section.class, mixin = FormMixin.FormSectionOneRun.class),
            @JsonMixin(target = Tab.class, mixin = FormMixin.FormTabOneRun.class),
            @JsonMixin(target = Tier.class, mixin = FormMixin.FormEntityHidePrePostOneRun.class),
            @JsonMixin(target = TierAction.class, mixin = FormMixin.FormEntityHidePrePostOneRun.class),
            @JsonMixin(target = Cogna.class, mixin = CognaMixin.CognaHideSensitive.class)
    })
    public Screen getScreen(@PathVariable("id") long id){
        return screenService.getScreen(id);
    }

}
