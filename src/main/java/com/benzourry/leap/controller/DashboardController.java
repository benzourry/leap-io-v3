package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.DashboardMixin;
import com.benzourry.leap.model.Chart;
import com.benzourry.leap.model.Dashboard;
import com.benzourry.leap.service.DashboardService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("api/dashboard")
//@CrossOrigin(allowCredentials="true")
public class DashboardController {

    public final DashboardService dashboardService;

    public DashboardController(DashboardService datasetService){
        this.dashboardService = datasetService;
    }


    @PostMapping
    public Dashboard saveDashboard(@RequestParam("appId") long appId,
                                   @RequestBody Dashboard dashboard){
        return dashboardService.saveDashboard(appId, dashboard);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Dashboard.class, mixin = DashboardMixin.DashboardOne.class)
    })
    public Dashboard getDashboard(@PathVariable("id") long id){
        return dashboardService.getDashboard(id);
    }

    @GetMapping("{id}/basic")
    @JsonResponse(mixins = {
            @JsonMixin(target = Dashboard.class, mixin = DashboardMixin.BasicDashboard.class),
            @JsonMixin(target = Chart.class, mixin = DashboardMixin.BasicChart.class)
    })
    public Dashboard getDashboardBasic(@PathVariable("id") long id){
        return dashboardService.getDashboard(id);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Dashboard.class, mixin = DashboardMixin.DashboardBasicList.class)
    })
    public List<Dashboard> getDashboardList(@RequestParam("appId") long appId,
                                            @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable){
        return dashboardService.findByAppId(appId, pageable);
    }

    @PostMapping("{dashboardId}/chart")
    public Chart editChart(@PathVariable("dashboardId") Long dashboardId,
                           @RequestBody Chart chart){
        return dashboardService.saveChart(dashboardId, chart);
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> removeDashboard(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        dashboardService.removeDashboard(id);
        return data;
    }

    @PostMapping("chart/{id}/delete")
    public Map<String, Object> removeChart(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        dashboardService.removeChart(id);
        return data;
    }

    @PostMapping("save-chart-order")
    public List<Map<String, Long>> saveChartOrder(@RequestBody List<Map<String, Long>> formChartList){
        return dashboardService.saveChartOrder(formChartList);
    }


    @PostMapping("save-dashboard-order")
    public List<Map<String, Long>> saveDatasetOrder(@RequestBody List<Map<String, Long>> dashboardList){
        return dashboardService.saveDashboardOrder(dashboardList);
    }

}
