package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
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

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ScreenService(ScreenRepository screenRepository,
                         FormRepository formRepository,
                         DatasetRepository datasetRepository,
                         DashboardRepository dashboardRepository,
                         ScreenActionRepository screenActionRepository,
                         AppRepository appRepository) {
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

        ///// COPY SCREEN
//        List<Screen> screenListOld = screenRepository.findByAppId(appId);
//        List<Screen> screenListNew = new ArrayList<>();
//        Map<Long, Screen> screenMap = new HashMap<>();
//        screenListOld.forEach(oldScreen -> {
        Screen oldScreen = screenRepository.findById(screenId).orElseThrow(() -> new ResourceNotFoundException("Screen", "id", screenId));
        App destApp = appRepository.findById(appId).orElseThrow(() -> new ResourceNotFoundException("App", "id", appId));

        Screen newScreen = new Screen();
        newScreen.setApp(destApp);
        BeanUtils.copyProperties(oldScreen, newScreen, "id", "actions");
//        if (oldScreen.getAccess() != null) {
//            newScreen.setAccess(null);
//        }

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

//    public String replaceMulti(String text, Map<String, String> maps){
//        int size = maps.size();
//        String[] keys = maps.keySet().toArray(new String[size]);
//        String[] values = maps.values().toArray(new String[size]);
//        return StringUtils.replaceEach(text, keys, values);
//    }

    public void removeAction(Long id) {
        screenActionRepository.deleteById(id);
    }

    @Transactional
    public Map<String, Object> getActionComps(Long id) {
        Screen screen = screenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Screen", "id", id));

        Set<Action> actionList = screen.getActions();
        List<Long> formIdList = actionList.stream().filter(a -> List.of("view", "view-single", "form", "edit", "edit-single", "prev", "facet").contains(a.getNextType()))
                .map(a -> a.getNext())
                .collect(Collectors.toList());
        Map<Long, Form> formMap = this.formRepository.findAllById(formIdList)
                .stream().collect(Collectors.toMap(Form::getId, Function.identity()));

        List<Long> screenIdList = actionList.stream().filter(a -> List.of("screen", "static").contains(a.getNextType()))
                .map(a -> a.getNext())
                .collect(Collectors.toList());
        Map<Long, Screen> screenMap = this.screenRepository.findAllById(screenIdList)
                .stream().collect(Collectors.toMap(Screen::getId, Function.identity()));

        List<Long> datasetIdList = actionList.stream().filter(a -> List.of("dataset").contains(a.getNextType()))
                .map(a -> a.getNext())
                .collect(Collectors.toList());
        Map<Long, Dataset> datasetMap = this.datasetRepository.findAllById(datasetIdList)
                .stream().collect(Collectors.toMap(Dataset::getId, Function.identity()));

        List<Long> dashboardIdList = actionList.stream().filter(a -> List.of("dashboard").contains(a.getNextType()))
                .map(a -> a.getNext())
                .collect(Collectors.toList());
        Map<Long, Dashboard> dashboardMap = this.dashboardRepository.findAllById(dashboardIdList)
                .stream().collect(Collectors.toMap(Dashboard::getId, Function.identity()));
        return Map.of("forms", formMap,
                "screens", screenMap,
                "datasets", datasetMap,
                "dashboards", dashboardMap);
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
