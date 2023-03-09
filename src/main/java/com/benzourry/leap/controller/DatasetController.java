package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.DatasetMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.DatasetItem;
import com.benzourry.leap.model.Form;
import com.benzourry.leap.service.DatasetService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.web.bind.annotation.*;

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
    public Dataset saveDataset(@RequestParam long appId, @RequestBody Dataset dataset){
        return datasetService.saveDataset(appId, dataset);
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetOne.class),
            @JsonMixin(target = Form.class, mixin = DatasetMixin.DatasetOneForm.class)
    })
    public Dataset getDataset(@PathVariable long id){
        return datasetService.getDataset(id);
    }

    @GetMapping
    @JsonResponse(mixins = {
//            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Dataset.class, mixin = DatasetMixin.DatasetBasicList.class)
    })
    public List<Dataset> getDatasetList(@RequestParam long appId){
        return datasetService.getByAppId(appId);
    }


    @PostMapping("clone")
    public Dataset clone(@RequestParam Long datasetId, @RequestParam Long appId){
        return datasetService.cloneDataset(datasetId, appId);
    }

    @PostMapping("{datasetId}/delete")
    public Map<String, Object> removeList(@PathVariable long datasetId){
        Map<String, Object> data = new HashMap<>();
        datasetService.removeDataset(datasetId);
        return data;
    }
    @PostMapping("{datasetId}/item")
    public DatasetItem saveDatasetItem(@PathVariable long datasetId,@RequestBody DatasetItem di){
//        Map<String, Object> data = new HashMap<>();
        return datasetService.saveDsItem(di);
    }

    @PostMapping("item/{diId}/delete")
    public Map<String, Object> removeListItem( @PathVariable long diId){
        Map<String, Object> data = new HashMap<>();
        datasetService.removeDsItem(diId);
        return data;
    }

    @PostMapping("save-ds-order")
    public List<Map<String, Long>> saveDsOrder(@RequestBody List<Map<String, Long>> dsItemList){
        return datasetService.saveDsOrder(dsItemList);
    }

    @PostMapping("{datasetId}/clear-entry")
    public Map<String, Object> clearEntry(@PathVariable long datasetId, @RequestParam String email){
        return datasetService.clearEntry(datasetId, email);
    }


}
