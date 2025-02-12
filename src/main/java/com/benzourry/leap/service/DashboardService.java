package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.Chart;
import com.benzourry.leap.model.Dashboard;
import com.benzourry.leap.model.Form;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.ChartRepository;
import com.benzourry.leap.repository.DashboardRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    private final ChartRepository chartRepository;

    private final AppRepository appRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                          ChartRepository chartRepository,
                          AppRepository appRepository){
        this.dashboardRepository = dashboardRepository;
        this.chartRepository = chartRepository;
        this.appRepository = appRepository;
    }



    public Chart getChart(Long chartId) {
        return chartRepository.findById(chartId)
                .orElseThrow(()->new ResourceNotFoundException("Chart","id",chartId));
    }

    public Chart saveChart(Long dashboardId, Chart chart) {
        // getreferencebyid only getreference. no need to query from database to set the reference of related entity
        Dashboard d = dashboardRepository.getReferenceById(dashboardId);
        chart.setDashboard(d);
//        f.getTiers().add(tier);
        return chartRepository.save(chart);
    }

    public void removeChart(Long id) {
        chartRepository.deleteById(id);
    }

    public List<Map<String,Long>> saveChartOrder(List<Map<String, Long>> formChartList) {
        for(Map<String, Long> element: formChartList){
            Chart fi = chartRepository.getReferenceById(element.get("id"));
            fi.setSortOrder(element.get("sortOrder"));
            chartRepository.save(fi);
        }
        return formChartList;
    }

    public Dashboard getDashboard(Long dashboardId) {
        return dashboardRepository.findById(dashboardId)
                .orElseThrow(()->new ResourceNotFoundException("Dashboard","id",dashboardId));
    }

    public List<Dashboard> findByAppId(long formId, Pageable pageable) {
        return dashboardRepository.findByAppId(formId, pageable);
    }

    public Dashboard saveDashboard(long appId, Dashboard dashboard) {
        App app = appRepository.getReferenceById(appId);
        dashboard.setApp(app);
        return dashboardRepository.save(dashboard);
    }

    public void removeDashboard(Long id) {
        dashboardRepository.deleteById(id);
    }


    @Transactional
    public List<Map<String, Long>> saveDashboardOrder(List<Map<String, Long>> dashboardList) {
        for (Map<String, Long> element : dashboardList) {
            Dashboard fi = dashboardRepository.findById(element.get("id")).orElseThrow(()->new ResourceNotFoundException("Form","id",element.get("id")));
            fi.setSortOrder(element.get("sortOrder"));
            dashboardRepository.save(fi);
        }
        return dashboardList;
    }


}
