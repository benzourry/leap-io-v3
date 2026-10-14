package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.App;
import com.benzourry.leap.model.Chart;
import com.benzourry.leap.model.ChartFilter;
import com.benzourry.leap.model.Dashboard;
import com.benzourry.leap.repository.AppRepository;
import com.benzourry.leap.repository.ChartRepository;
import com.benzourry.leap.repository.DashboardRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        Dashboard d = dashboardRepository.getReferenceById(dashboardId);
        chart.setDashboard(d);
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


    @Transactional
    public Dashboard cloneDashboard(Long dashboardId, Long appId) {

        Dashboard oldDashboard = dashboardRepository.findById(dashboardId)
                .orElseThrow(() -> new ResourceNotFoundException("Dashboard", "id", dashboardId));
        App destApp = appRepository.findById(appId)
                .orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));

        Dashboard newDashboard = new Dashboard();
        newDashboard.setApp(destApp);

        // 1. CRITICAL FIX: Exclude the child collection "charts" from the root copy
        BeanUtils.copyProperties(oldDashboard, newDashboard, "id", "charts");

        if (oldDashboard.getAccessList() != null) {
            newDashboard.setAccessList(null);
        }

        Set<Chart> newDashboardChartList = new HashSet<>();

        if (oldDashboard.getCharts() != null) {
            oldDashboard.getCharts().forEach(oldDashboardChart -> {
                Chart newChart = new Chart();

                // 2. CRITICAL FIX: Exclude child collection "filters" AND parent reference "dashboard"
                BeanUtils.copyProperties(oldDashboardChart, newChart, "id", "filters", "dashboard");

                Set<ChartFilter> newChartFilterList = new HashSet<>();

                if (oldDashboardChart.getFilters() != null) {
                    oldDashboardChart.getFilters().forEach(oldChartFilter -> {
                        ChartFilter newChartFilter = new ChartFilter();

                        // 3. CRITICAL FIX: Exclude parent reference "chart"
                        BeanUtils.copyProperties(oldChartFilter, newChartFilter, "id", "chart");

                        newChartFilter.setChart(newChart);
                        newChartFilterList.add(newChartFilter);
                    });
                }

                newChart.setFilters(newChartFilterList);
                newChart.setDashboard(newDashboard);
                newDashboardChartList.add(newChart);
            });
        }

        newDashboard.setCharts(newDashboardChartList);

        return dashboardRepository.save(newDashboard);
    }
}
