package com.benzourry.leap.controller;

import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.mixin.DatasetMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.DatasetService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/dataset")
//@CrossOrigin(allowCredentials="true")
public class DatasetController {

    public final DatasetService datasetService;

    public DatasetController(DatasetService datasetService){
        this.datasetService = datasetService;
    }



    @PostMapping
    public Dataset saveDataset(@RequestParam("appId") long appId,
                               @RequestBody Dataset dataset){
        return datasetService.saveDataset(appId, dataset);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetOne.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class)
    })
    public Dataset getDataset(@PathVariable("id") long id){
        return datasetService.getDataset(id);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
    })
    public List<Dataset> getDatasetList(@RequestParam("appId") long appId,
                                        @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable){
        return datasetService.getByAppId(appId, pageable);
    }

//    @GetMapping("by-form")
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
//            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
//    })
//    public List<Dataset> getDatasetListByFormId(@RequestParam("formId") long formId,
//                                        @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable){
//        return datasetService.getByFormId(formId, pageable);
//    }


    @PostMapping("clone")
    public Dataset clone(@RequestParam("datasetId") Long datasetId,
                         @RequestParam("appId") Long appId){
        return datasetService.cloneDataset(datasetId, appId);
    }

    @PostMapping("{datasetId}/delete")
    public Map<String, Object> removeList(@PathVariable("datasetId") long datasetId){
        Map<String, Object> data = new HashMap<>();
        datasetService.removeDataset(datasetId);
        return data;
    }

//    @PostMapping("{datasetId}/resync")
//    public Map<String, Object> resyncDataset(@PathVariable("datasetId") long datasetId){
//        return datasetService.resyncDataset(datasetId);
//    }

    @PostMapping("{datasetId}/item")
    public DatasetItem saveDatasetItem(@PathVariable("datasetId") long datasetId,
                                       @RequestBody DatasetItem di){
//        Map<String, Object> data = new HashMap<>();
        return datasetService.saveDsItem(di);
    }

    @PostMapping("item/{diId}/delete")
    public Map<String, Object> removeListItem( @PathVariable("diId") long diId){
        Map<String, Object> data = new HashMap<>();
        datasetService.removeDsItem(diId);
        return data;
    }

    @PostMapping("save-ds-order")
    public List<Map<String, Long>> saveDsOrder(@RequestBody List<Map<String, Long>> dsItemList){
        return datasetService.saveDsOrder(dsItemList);
    }

    @PostMapping("{datasetId}/clear-entry")
    public Map<String, Object> clearEntry(@PathVariable("datasetId") long datasetId,
                                          @RequestParam("email") String email){
        return datasetService.clearEntry(datasetId, email);
    }

    @PostMapping("{datasetId}/actions")
    public DatasetAction saveAction(@PathVariable("datasetId") Long datasetId,
                                    @RequestBody DatasetAction action){
        return datasetService.saveAction(datasetId, action);
    }

    @PostMapping("actions/{daId}/delete")
    public Map<String, Object> removeAction(@PathVariable("daId") long daId){
        Map<String, Object> data = new HashMap<>();
        datasetService.removeAction(daId);
        return data;
    }

    @PostMapping("save-dataset-order")
    public List<Map<String, Long>> saveDatasetOrder(@RequestBody List<Map<String, Long>> datasetList){
        return datasetService.saveDatasetOrder(datasetList);
    }


}
