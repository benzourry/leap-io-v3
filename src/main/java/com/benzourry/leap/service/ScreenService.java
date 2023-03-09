package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ScreenService {


    ScreenRepository screenRepository;

    FormRepository formRepository;

    DatasetRepository datasetRepository;

    DashboardRepository dashboardRepository;

    ScreenActionRepository screenActionRepository;

    AppRepository appRepository;

    public ScreenService(ScreenRepository screenRepository,
                         FormRepository formRepository,
                         DatasetRepository datasetRepository,
                         DashboardRepository dashboardRepository,
                         ScreenActionRepository screenActionRepository,
                         AppRepository appRepository){
        this.appRepository = appRepository;
        this.screenRepository = screenRepository;
        this.datasetRepository = datasetRepository;
        this.dashboardRepository = dashboardRepository;
        this.formRepository = formRepository;
        this.screenActionRepository = screenActionRepository;

    }

    public Screen saveScreen(long appId, Screen screen) {
        App app = appRepository.getReferenceById(appId);
        screen.setApp(app);
        return screenRepository.save(screen);
    }

    public void removeScreen(Long id) {
        screenRepository.deleteById(id);
    }

    public List<Screen> findByAppId(long appId) {
        return screenRepository.findByAppId(appId);
    }

    public Screen getScreen(long id) {
        return screenRepository.getReferenceById(id);
    }

    public Action saveAction (Long screenId, Action action){
        Screen screen = screenRepository.getReferenceById(screenId);
        action.setScreen(screen);
        return screenActionRepository.save(action);

    }


    public void removeAction(Long id) {
        screenActionRepository.deleteById(id);
    }

    public Map<String, Object> getActionComps(Long id) {
        Screen screen = screenRepository.getReferenceById(id);
        Set<Action> actionList = screen.getActions();
        List<Long> formIdList = actionList.stream().filter(a-> List.of("view","view-single","form","edit","edit-single","prev").contains(a.getNextType()))
                .map(a->a.getNext())
                .collect(Collectors.toList());
        Map<Long,Form> formMap = this.formRepository.findAllById(formIdList)
                .stream().collect(Collectors.toMap(Form::getId, Function.identity()));

        List<Long> screenIdList = actionList.stream().filter(a-> List.of("screen","static").contains(a.getNextType()))
                .map(a->a.getNext())
                .collect(Collectors.toList());
        Map<Long,Screen> screenMap = this.screenRepository.findAllById(screenIdList)
                .stream().collect(Collectors.toMap(Screen::getId, Function.identity()));

        List<Long> datasetIdList = actionList.stream().filter(a-> List.of("dataset").contains(a.getNextType()))
                .map(a->a.getNext())
                .collect(Collectors.toList());
        Map<Long,Dataset> datasetMap = this.datasetRepository.findAllById(datasetIdList)
                .stream().collect(Collectors.toMap(Dataset::getId, Function.identity()));

        List<Long> dashboardIdList = actionList.stream().filter(a-> List.of("dashboard").contains(a.getNextType()))
                .map(a->a.getNext())
                .collect(Collectors.toList());
        Map<Long,Dashboard> dashboardMap = this.dashboardRepository.findAllById(dashboardIdList)
                .stream().collect(Collectors.toMap(Dashboard::getId, Function.identity()));
        return Map.of("forms", formMap,
                "screens",screenMap,
                "datasets", datasetMap,
                "dashboards", dashboardMap);
    }
}
