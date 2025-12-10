package com.benzourry.leap.service;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
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

@Service
public class DatasetService {

    final DatasetRepository datasetRepository;

    final DatasetActionRepository datasetActionRepository;

    final ScreenRepository screenRepository;

    final DatasetItemRepository datasetItemRepository;

    final DatasetFilterRepository datasetFilterRepository;

    final AppRepository appRepository;

    final EntryService entryService;

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

    @Transactional
    public Dataset cloneDataset(Long datasetId, Long appId) {
        //// COPY DATASET

            Dataset oldDataset = datasetRepository.findById(datasetId).orElseThrow(()->new ResourceNotFoundException("Dataset","id",datasetId));
            App destApp = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id",appId));

            Dataset newDataset = new Dataset();
            newDataset.setApp(destApp);
            BeanUtils.copyProperties(oldDataset, newDataset, "id");
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

            newDataset.setItems(newDatasetItemList);
            newDataset.setActions(newDatasetActionList);
            newDataset.setFilters(newDatasetFilterList);

        return datasetRepository.save(newDataset);
    }

    public DatasetAction saveAction(Long dsId, DatasetAction action) {
        Dataset ds = datasetRepository.getReferenceById(dsId);
        action.setDataset(ds);
        return datasetActionRepository.save(action);
    }

    public void removeAction(long daId) {
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
