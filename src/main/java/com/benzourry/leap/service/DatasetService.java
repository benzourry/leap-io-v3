package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.*;

@Service
public class DatasetService {

    final DatasetRepository datasetRepository;

    final ScreenRepository screenRepository;

    final DatasetItemRepository datasetItemRepository;

    final DatasetFilterRepository datasetFilterRepository;

    final AppRepository appRepository;

    final EntryService entryService;

    public DatasetService(DatasetRepository datasetRepository,
                          ScreenRepository screenRepository,
                          DatasetItemRepository datasetItemRepository,
                          DatasetFilterRepository datasetFilterRepository,
                          EntryService entryService,
                          AppRepository appRepository){
        this.datasetRepository = datasetRepository;
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
        return datasetRepository.save(d);
    }

    public List<Dataset> getByAppId(@RequestParam long appId){
        return datasetRepository.findByAppId(appId);
    }


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
        return datasetItemRepository.save(ndi);
    }


    @Transactional
    public Map<String, Object> clearEntry(long datasetId, String email) {

//        Page<Entry> entryPage = entryService.findListByDataset(datasetId,"",email,null, Pageable.unpaged(),null);
//
//        entryPage.getContent().forEach(e->{
//            entryService.deleteEntry(e.getId());
//        });


        Pageable pageRequest = PageRequest.of(0, 200);
        Page<Entry> onePage = entryService.findListByDataset(datasetId,"",email,null,null,null,pageRequest,null);

        long total = onePage.getTotalElements();

        while (!onePage.isEmpty()) {
            pageRequest = pageRequest.next();

            //DO SOMETHING WITH ENTITIES
            onePage.forEach(entity -> {
                entryService.deleteEntry(entity.getId(), email);
//                System.out.println(entity.getId());
            });

            onePage = entryService.findListByDataset(datasetId,"",email,null,null,null,pageRequest,null);

        }

        Map<String, Object> data = new HashMap<>();

        data.put("rows", total);
        data.put("success", true);
        return data;
    }


    public Dataset cloneDataset(Long datasetId, Long appId) {
        //// COPY DATASET
//        List<Dataset> datasetListOld = datasetRepository.findByAppId(appId);
//        Map<Long, Dataset> datasetMap = new HashMap<>();
//        List<Dataset> datasetListNew = new ArrayList<>();
//        datasetListOld.forEach(oldDataset -> {

            Dataset oldDataset = datasetRepository.getReferenceById(datasetId);
            App destApp = appRepository.getReferenceById(appId);

            Dataset newDataset = new Dataset();
            newDataset.setApp(destApp);
            BeanUtils.copyProperties(oldDataset, newDataset, "id");
            if (oldDataset.getAccess() != null) {
                newDataset.setAccess(null);
            }

            List<DatasetItem> newDatasetItemList = new ArrayList<>();
            Set<DatasetFilter> newDatasetFilterList = new HashSet<>();
            oldDataset.getItems().forEach(oldDatasetItem -> {
                DatasetItem newDatasetItem = new DatasetItem();
                BeanUtils.copyProperties(oldDatasetItem, newDatasetItem, "id");
                newDatasetItem.setDataset(newDataset);
                newDatasetItemList.add(newDatasetItem);
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
            newDataset.setFilters(newDatasetFilterList);

//            datasetListNew.add(newDataset);
//            datasetMap.put(oldDataset.getId(), newDataset);
//        });
//        datasetRepository.saveAll(datasetListNew);
        return datasetRepository.save(newDataset);


    }
}
