package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.EntryMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.FormService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("api/form")
//@CrossOrigin(allowCredentials="true")
public class FormController {

    final FormService formService;

//    AppService appService;

    public FormController(FormService formService){
        this.formService = formService;
//        this.appService = appService;
    }

    /** FORM **/
    @PostMapping
    public Form save(@RequestParam Long appId, @RequestBody Form form){

        return formService.save(appId, form);
    }

    @GetMapping("{formId}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormOne.class)
    })
    public Form findById(@PathVariable Long formId){
        return formService.findFormById(formId);
    }

    @PostMapping("clone")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormOne.class)
    })
    public Form clone(@RequestParam Long formId, @RequestParam Long appId){
        return formService.cloneForm(formId, appId);
    }

//    @GetMapping("{formId}/columns")
//    public List<String> findColumns(@PathVariable Long formId){
//        return formService.getColumns(formId);
//    }

    @PostMapping("{formId}/delete")
    public Map<String, Object> removeForm(@PathVariable long formId){

        return formService.removeForm(formId);
    }

    @PostMapping("{formId}/clear-entry")
    public Map<String, Object> clearEntry(@PathVariable long formId){


        return formService.clearEntry(formId);
    }


    @GetMapping({"basic",""})
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class)
    })
    public Page<Form> findFormList(@RequestParam Long appId,Pageable pageable){
        return formService.findFormByAppId(appId, PageRequest.of(0, Integer.MAX_VALUE));
    }

//    @GetMapping
//    @JsonResponse(mixins = {
//            @JsonMixin(target = Form.class, mixin = FormMixin.FormList.class)
//    })
//    public Page<Form> findFormListWithDetails(@RequestParam Long appId,Pageable pageable){
//        return formService.findFormByAppId(appId, pageable);
//    }

    /** DATASET **/

    @GetMapping("by-dataset/{id}")
    public Form getFormByDatasetId(@PathVariable long id){
        return formService.getFormByDatasetId(id);
    }

    /** FORM SECTION **/
    @PostMapping("section/{sectionId}/delete")
    public Map<String,Object> removeSection(@PathVariable long sectionId){
        Map<String, Object> data = new HashMap<>();
        formService.removeSection(sectionId);
        return data;
    }

    @GetMapping("{formId}/section")
    public Page<Section> getSectionList(@PathVariable long formId, Pageable pageable){
        return formService.findSectionByFormId(formId, pageable);
    }

    @PostMapping("{formId}/section")
    public Section saveSection(@PathVariable long formId, @RequestBody Section section){
        return formService.saveSection(formId, section);
    }

    @PostMapping("save-section-order")
    public List<Map<String, Long>> saveSectionOrder(@RequestBody List<Map<String, Long>> formSectionList){
        return formService.saveSectionOrder(formSectionList);
    }

    /** ## TABS **/
    @PostMapping("{formId}/tab")
    public Tab saveTab(@PathVariable long formId, @RequestBody Tab tab){
        return formService.saveTab(formId, tab);
    }

    @GetMapping("{formId}/tab")
    public Page<Tab> getTabList(@PathVariable long formId, Pageable pageable){
        return formService.findTabByFormId(formId, pageable);
    }

    @PostMapping("tab/{id}/delete")
    public Map<String, Object> removeTab(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        formService.removeTab(id);
        return data;
    }

    @PostMapping("save-tab-order")
    public List<Map<String, Long>> saveTabOrder(@RequestBody List<Map<String, Long>> formTabList){
        return formService.saveTabOrder(formTabList);
    }


    /** ## DASHBOARD **/

    /**  FORM ITEMS **/
    @GetMapping("{formId}/item")
    public Page<Item> getItemList(@PathVariable long formId,
                                  Pageable pageable){
        return formService.findItemByFormId(formId, pageable);
    }

    @PostMapping("{formId}/items")
    public Item saveItem(@PathVariable long formId,
                         @RequestParam long sectionId,
                         @RequestBody Item item,
                         @RequestParam long sortOrder){
        return formService.saveItem(formId, sectionId, item, sortOrder);
    }

//    @PostMapping("{formId}/update-item")
//    public Item saveItem(@PathVariable long formId,@RequestBody Item item){
//        return formService.updateItem(formId,item);
//    }

    @PostMapping("{formId}/move-item")
    public SectionItem moveItem(@PathVariable long formId,
                         @RequestParam long sectionItemId,
                         @RequestParam long newSectionId,
                         @RequestParam long sortOrder){
        return formService.moveItem(formId, sectionItemId, newSectionId, sortOrder);
    }
//
//    @PostMapping("{formId}/elements")
//    public Element saveItem(@PathVariable long formId,
//                            @RequestParam Long parentId,
//                            @RequestBody Element item,
//                            @RequestParam long sortOrder){
//        return formService.saveElement(formId, parentId, item, sortOrder);
//    }
//
//    @PostMapping("{formId}/elements/{elementId}/delete")
//    public Map<String, Object> removeElement(@PathVariable long formId,@PathVariable long elementId){
//        Map<String, Object> data = new HashMap<>();
//        formService.removeElement(formId,elementId);
//        return data;
//    }

//    @PostMapping("{formId}/model")
//    public Model saveModel(@PathVariable long formId,
//                            @RequestParam Long parentId,
//                            @RequestBody Model model,
//                            @RequestParam long sortOrder){
//        return formService.saveModel(formId, parentId, model, sortOrder);
//    }

//    @PostMapping("{formId}/model/{modelId}/delete")
//    public Map<String, Object> removeModel(@PathVariable long formId,@PathVariable long modelId){
//        Map<String, Object> data = new HashMap<>();
//        formService.removeModel(formId,modelId);
//        return data;
//    }

    @PostMapping("{formId}/items/{itemId}/delete")
    public Map<String, Object> removeItem(@PathVariable long formId,@PathVariable long itemId){
        Map<String, Object> data = new HashMap<>();
        formService.removeItem(formId,itemId);
        return data;
    }

    @PostMapping("save-item-order")
    public List<Map<String, Long>> saveItemOrder(@RequestBody List<Map<String, Long>> formItemList){
        return formService.saveItemOrder(formItemList);
    }

    @PostMapping("{formId}/items-source/{itemId}/delete")
    public Map<String, Object> removeItemSource(@PathVariable long formId,@PathVariable long itemId){
        Map<String, Object> data = new HashMap<>();
        formService.removeItemSource(formId,itemId);
        return data;
    }

    /** TIER **/
    @PostMapping("{formId}/tier")
    public Tier editTier(@PathVariable("formId") Long formId,
                         @RequestBody Tier tier){
        return formService.saveTier(formId, tier);
    }
    @PostMapping("save-tier-order")
    public List<Map<String, Long>> saveTierOrder(@RequestBody List<Map<String, Long>> formTierList){
        return formService.saveTierOrder(formTierList);
    }

    @PostMapping("save-tier-action-order")
    public Map<String, TierAction> saveTierActionOrder(@RequestParam("tierId") Long tierId,@RequestBody List<Map<String, Long>> formTierActionList){
        return formService.saveTierActionOrder(tierId, formTierActionList);
    }

    @PostMapping("tier/{id}/delete")
    public Map<String, Object> removeTier(@PathVariable("id") Long id){
        Map<String, Object> data = new HashMap<>();
        formService.removeTier(id);
        return data;
    }

    @PostMapping("tier/{tierId}/action")
    public TierAction editTierAction(@PathVariable("tierId") Long tierId,
                               @RequestBody TierAction tierAction){
        return formService.saveTierAction(tierId, tierAction);
    }

    @PostMapping("tier/action/{tierActionId}/delete")
    public Map<String, Object> removeTierAction(@PathVariable("tierActionId") Long tierActionId){
        Map<String, Object> data = new HashMap<>();
        formService.removeTierAction(tierActionId);
        return data;
    }

//    @GetMapping("tier/{id}/approver")
//    public Map<String, Object> getApprover(@PathVariable("id") Long id, @RequestParam("email") String email){
//        Map<String, Object> data = new HashMap<>();
//        data.put("approver",formService.getOrgMapApprover(id, email));
//        return data;
//    }

//    @RequestMapping(value="qr")
//    public FileSystemResource getFileInline(@RequestParam("code") String code, HttpServletResponse response){
//        return new FileSystemResource(Constant.UPLOAD_ROOT_DIR +"/attachment/" +path);
//    }

    @GetMapping(value = "qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRCode(@RequestParam(value = "code") String code,
                                            @RequestParam(value="h", defaultValue = "256") int h,
                                            @RequestParam(value = "w", defaultValue = "256") int w) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES))
                    .body(Helper.generateQRCode(code, w,h));
        } catch (Exception ex) {
            throw new RuntimeException("Error while generating QR code image.", ex);
        }
    }

    @GetMapping("{id}/trails")
    @JsonResponse(mixins = {
            @JsonMixin(target = Entry.class, mixin = EntryMixin.NoForm.class),
            @JsonMixin(target = Tier.class, mixin = EntryMixin.EntryListApprovalTier.class)
    })
    public Page<EntryApprovalTrail> findTrailByFormId(@PathVariable("id") long id,
                                                      @RequestParam(value = "searchText", defaultValue = "") String searchText, Pageable pageable) {
        return formService.findTrailByFormId(id, searchText, pageable);
    }



}
