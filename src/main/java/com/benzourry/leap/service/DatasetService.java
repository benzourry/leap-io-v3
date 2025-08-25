package com.benzourry.leap.service;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class DatasetService {

    final DatasetRepository datasetRepository;

    final DatasetActionRepository datasetActionRepository;

    final ScreenRepository screenRepository;

    final DatasetItemRepository datasetItemRepository;

    final DatasetFilterRepository datasetFilterRepository;

    final AppRepository appRepository;

    final EntryService entryService;

    @PersistenceContext
    private EntityManager entityManager;

    public DatasetService(DatasetRepository datasetRepository,
                          DatasetActionRepository datasetActionRepository,
                          ScreenRepository screenRepository,
                          DatasetItemRepository datasetItemRepository,
                          DatasetFilterRepository datasetFilterRepository,
                          EntryService entryService,
                          AppRepository appRepository){
        this.datasetRepository = datasetRepository;
        this.datasetActionRepository = datasetActionRepository;
        this.screenRepository = screenRepository;
        this.datasetItemRepository = datasetItemRepository;
        this.datasetFilterRepository = datasetFilterRepository;
        this.entryService = entryService;
        this.appRepository = appRepository;
    }

    public Dataset saveDataset(long appId, Dataset dataset){
        App app = appRepository.getReferenceById(appId);
        dataset.setApp(app);
        Dataset d = datasetRepository.save(dataset);
//        System.out.println(dataset.getQFilter());
        return datasetRepository.save(d);
    }

    @Transactional
    public List<Dataset> getByAppId(@RequestParam long appId, Pageable pageable){
        return datasetRepository.findByAppId(appId, pageable);
    }


    @Transactional
    public List<Dataset> getByFormId(@RequestParam long formId, Pageable pageable){
        return datasetRepository.findByFormId(formId, pageable);
    }


    @Transactional
    public void removeDataset(long datasetId) {
//        Form f = formRepository.findById(formId).get();
//        f.getDs().remove(key);
//        formRepository.save(f);

        List<Screen> screenList = screenRepository.findByDatasetId(datasetId);

        if (screenList.size()>0){
            screenRepository.deleteAll(screenList);
        }

        datasetRepository.deleteById(datasetId);
    }

    @Transactional
    public void removeDsItem(long datasetItemId) {

        DatasetItem di = datasetItemRepository.getReferenceById(datasetItemId);
        datasetFilterRepository.deleteByDatasetIdAndCode(di.getCode(),di.getDataset().getId());

        datasetItemRepository.deleteById(datasetItemId);
    }

    public Dataset getDataset(long id) {
        return datasetRepository.getReferenceById(id);
    }

    public List<Map<String,Long>> saveDsOrder(List<Map<String, Long>> dsItemList) {
        for(Map<String, Long> element: dsItemList){
            DatasetItem fi = datasetItemRepository.getReferenceById(element.get("id"));
            fi.setSortOrder(element.get("sortOrder"));
            datasetItemRepository.save(fi);
        }
        return dsItemList;
    }


    public DatasetItem saveDsItem(DatasetItem di) {
        DatasetItem ndi = datasetItemRepository.getReferenceById(di.getId());
        ndi.setLabel(di.getLabel());
        ndi.setSubs(di.getSubs());
        ndi.setPre(di.getPre());
        return datasetItemRepository.save(ndi);
    }



    // CANNOT! SBB Stream entry, then within it delete entry.
    @Transactional
    public Map<String, Object> clearEntry3(long datasetId, String email) {

        Map<String, Object> data = new HashMap<>();

//        AtomicInteger index = new AtomicInteger();
        AtomicInteger total = new AtomicInteger();

        try (Stream<Entry> entryStream = entryService.findListByDatasetStream(datasetId, "", email, null, null, null, null, null)) {
            entryStream.forEach(entry -> {
                total.getAndIncrement();
//                index.getAndIncrement();
                entryService.deleteEntry(entry.getId(), email);
                this.entityManager.detach(entry);
            });
        }

        data.put("rows", total.get());
        data.put("success", true);

        return data;

    }


    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> clearEntry(long datasetId, String email) {

        Pageable pageRequest = PageRequest.of(0, 4000);
        Page<Long> onePage = entryService.findIdListByDataset(datasetId,"",email,null,"AND",null,null,pageRequest,null);

        long total = onePage.getTotalElements();

        while (!onePage.isEmpty()) {
            pageRequest = pageRequest.next();

            //DO SOMETHING WITH ENTITIES
            onePage.forEach(id -> {
                entryService.deleteEntry(id, email);
            });

            onePage = entryService.findIdListByDataset(datasetId,"",email,null,"AND",null,null,pageRequest,null);

        }

        Map<String, Object> data = new HashMap<>();

        data.put("rows", total);
        data.put("success", true);

        return CompletableFuture.completedFuture(data);
    }

//    public Map<String, Object> resyncDataset(Long datasetId){
//        return Map.of("success", true);
//    }

    @Transactional
    public Dataset cloneDataset(Long datasetId, Long appId) {
        //// COPY DATASET
//        List<Dataset> datasetListOld = datasetRepository.findByAppId(appId);
//        Map<Long, Dataset> datasetMap = new HashMap<>();
//        List<Dataset> datasetListNew = new ArrayList<>();
//        datasetListOld.forEach(oldDataset -> {

            Dataset oldDataset = datasetRepository.findById(datasetId).orElseThrow(()->new ResourceNotFoundException("Dataset","id",datasetId));
            App destApp = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id",appId));

            Dataset newDataset = new Dataset();
            newDataset.setApp(destApp);
            BeanUtils.copyProperties(oldDataset, newDataset, "id");
//            if (oldDataset.getAccess() != null) {
//                newDataset.setAccess(null);
//            }
            if (oldDataset.getAccessList() != null) {
                newDataset.setAccessList(null);
            }

            List<DatasetItem> newDatasetItemList = new ArrayList<>();
            List<DatasetAction> newDatasetActionList = new ArrayList<>();
            Set<DatasetFilter> newDatasetFilterList = new HashSet<>();
            oldDataset.getItems().forEach(oldDatasetItem -> {
                DatasetItem newDatasetItem = new DatasetItem();
                BeanUtils.copyProperties(oldDatasetItem, newDatasetItem, "id");
                newDatasetItem.setDataset(newDataset);
                newDatasetItemList.add(newDatasetItem);
            });
            oldDataset.getActions().forEach(oldDatasetAction -> {
                DatasetAction newDatasetAction = new DatasetAction();
                BeanUtils.copyProperties(oldDatasetAction, newDatasetAction, "id");
                newDatasetAction.setDataset(newDataset);
                newDatasetActionList.add(newDatasetAction);
            });
            oldDataset.getFilters().forEach(oldDatasetFilter -> {
                DatasetFilter newDatasetFilter = new DatasetFilter();
                BeanUtils.copyProperties(oldDatasetFilter, newDatasetFilter, "id");
                newDatasetFilter.setDataset(newDataset);
                newDatasetFilterList.add(newDatasetFilter);
            });

//            if (oldDataset.getForm() != null) {
//                newDataset.setForm(formMap.get(oldDataset.getForm().getId()));
//            }
//            newDataset.setApp(newApp);
            newDataset.setItems(newDatasetItemList);
            newDataset.setActions(newDatasetActionList);
            newDataset.setFilters(newDatasetFilterList);

//            datasetListNew.add(newDataset);
//            datasetMap.put(oldDataset.getId(), newDataset);
//        });
//        datasetRepository.saveAll(datasetListNew);
        return datasetRepository.save(newDataset);
    }

    public DatasetAction saveAction(Long dsId, DatasetAction action) {
        Dataset ds = datasetRepository.getReferenceById(dsId);
        action.setDataset(ds);
        return datasetActionRepository.save(action);
    }

    public void removeAction(long daId) {
        DatasetAction da = datasetActionRepository.getReferenceById(daId);
        datasetActionRepository.deleteById(daId);
    }

    @Transactional
    public List<Map<String, Long>> saveDatasetOrder(List<Map<String, Long>> datasetList) {
        for (Map<String, Long> element : datasetList) {
            Dataset fi = datasetRepository.findById(element.get("id")).orElseThrow(()->new ResourceNotFoundException("Dataset","id",element.get("id")));
            fi.setSortOrder(element.get("sortOrder"));
            datasetRepository.save(fi);
        }
        return datasetList;
    }

}
