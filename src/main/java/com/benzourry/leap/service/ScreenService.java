package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final ObjectMapper MAPPER;

    public ScreenService(ScreenRepository screenRepository,
                         FormRepository formRepository,
                         DatasetRepository datasetRepository,
                         DashboardRepository dashboardRepository,
                         ScreenActionRepository screenActionRepository,
                         AppRepository appRepository, ObjectMapper MAPPER) {
        this.appRepository = appRepository;
        this.screenRepository = screenRepository;
        this.datasetRepository = datasetRepository;
        this.dashboardRepository = dashboardRepository;
        this.formRepository = formRepository;
        this.screenActionRepository = screenActionRepository;

        this.MAPPER = MAPPER;
    }

    public Screen saveScreen(long appId, Screen screen) {
        App app = appRepository.getReferenceById(appId);
        screen.setApp(app);
        return screenRepository.save(screen);
    }

    public void removeScreen(Long id) {
        screenRepository.deleteById(id);
    }

    public List<Screen> findByAppId(long appId, Pageable pageable) {
        return screenRepository.findByAppId(appId, pageable);
    }

    public Screen getScreen(long id) {
        return screenRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Screen", "id", id));
    }

    public Action saveAction(Long screenId, Action action) {
        Screen screen = screenRepository.getReferenceById(screenId);
        action.setScreen(screen);
        return screenActionRepository.save(action);

    }

    @Transactional
    public Screen cloneScreen(Long screenId, Long appId) {

        Screen oldScreen = screenRepository.findById(screenId).orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));
        App destApp = appRepository.findById(appId).orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));

        Screen newScreen = new Screen();
        newScreen.setApp(destApp);
        BeanUtils.copyProperties(oldScreen, newScreen, "id", "actions");

        Set<Action> actions = new HashSet<>();

        Map<String, String> actionStrMaps = new HashMap<>();

        screenRepository.save(newScreen);

        oldScreen.getActions().forEach(sa -> {
            Action sa2 = new Action();
            BeanUtils.copyProperties(sa, sa2, "id");
            sa2.setScreen(newScreen);
            Action action = screenActionRepository.save(sa2);
            actions.add(action);

            actionStrMaps.put("$go['"+sa.getId()+"']","$go['"+action.getId()+"']");

        });
        newScreen.setActions(actions);

        Map<String, Object> map = MAPPER.convertValue(newScreen.getData(), Map.class);

        map.keySet().forEach(k->{
            if (map.get(k) instanceof String){
                String newV = Helper.replaceMulti((String) map.get(k),actionStrMaps);
                map.put(k,newV);
            }
        });

        newScreen.setData(MAPPER.valueToTree(map));

        return screenRepository.save(newScreen);

    }

    public void removeAction(Long id) {
        screenActionRepository.deleteById(id);
    }
    private static final Set<String> FORM_TYPES = Set.of("view", "view-single", "form", "edit", "edit-single", "prev", "facet");
    private static final Set<String> SCREEN_TYPES = Set.of("screen", "static");
    private static final String DATASET_TYPE = "dataset";
    private static final String DASHBOARD_TYPE = "dashboard";

    public Map<String, Object> getActionComps(Long id) {

        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", id));

        Set<Action> actionList = screen.getActions();

        // Pre-sized lists to reduce resizing cost
        int size = actionList.size();
        List<Long> formIds = new ArrayList<>(size);
        List<Long> screenIds = new ArrayList<>(size);
        List<Long> datasetIds = new ArrayList<>(size);
        List<Long> dashboardIds = new ArrayList<>(size);

        // 🔥 Single pass
        for (Action a : actionList) {
            String type = a.getNextType();
            Long next = a.getNext();
            if (next == null) continue;

            if (FORM_TYPES.contains(type)) {
                formIds.add(next);
            } else if (SCREEN_TYPES.contains(type)) {
                screenIds.add(next);
            } else if (DATASET_TYPE.equals(type)) {
                datasetIds.add(next);
            } else if (DASHBOARD_TYPE.equals(type)) {
                dashboardIds.add(next);
            }
        }

        // Fetch from database in the most compact way
        Map<Long, Form> formMap = formIds.isEmpty() ? Map.of()
                : formRepository.findAllById(formIds)
                .stream().collect(Collectors.toMap(Form::getId, Function.identity()));

        Map<Long, Screen> screenMap = screenIds.isEmpty() ? Map.of()
                : screenRepository.findAllById(screenIds)
                .stream().collect(Collectors.toMap(Screen::getId, Function.identity()));

        Map<Long, Dataset> datasetMap = datasetIds.isEmpty() ? Map.of()
                : datasetRepository.findAllById(datasetIds)
                .stream().collect(Collectors.toMap(Dataset::getId, Function.identity()));

        Map<Long, Dashboard> dashboardMap = dashboardIds.isEmpty() ? Map.of()
                : dashboardRepository.findAllById(dashboardIds)
                .stream().collect(Collectors.toMap(Dashboard::getId, Function.identity()));

        return Map.of(
                "forms", formMap,
                "screens", screenMap,
                "datasets", datasetMap,
                "dashboards", dashboardMap
        );
    }


    @Transactional
    public List<Map<String, Long>> saveScreenOrder(List<Map<String, Long>> screenList) {
        for (Map<String, Long> element : screenList) {
            Screen fi = screenRepository.findById(element.get("id")).orElseThrow(()->new ResourceNotFoundException("Screen","id",element.get("id")));
            fi.setSortOrder(element.get("sortOrder"));
            screenRepository.save(fi);
        }
        return screenList;
    }
}
