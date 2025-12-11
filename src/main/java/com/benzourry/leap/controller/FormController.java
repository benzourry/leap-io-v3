package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.EntryMixin;
import com.benzourry.leap.mixin.FormMixin;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.FormService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("api/form")
//@CrossOrigin(allowCredentials="true")
public class FormController {

    final FormService formService;

    public FormController(FormService formService){
        this.formService = formService;
    }

    /** FORM **/
    @PostMapping
    public Form save(@RequestParam("appId") Long appId,
                     @RequestBody Form form){

        return formService.save(appId, form);
    }

    @GetMapping("{formId}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormOne.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOne.class),
    })
    public Form findById(@PathVariable("formId") Long formId){
        return formService.findFormById(formId);
    }

    @PostMapping("clone")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormOne.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOne.class),
    })
    public Form clone(@RequestParam("formId") Long formId,
                      @RequestParam("appId") Long appId){
        return formService.cloneForm(formId, appId);
    }

    @PostMapping("{formId}/delete")
    public Map<String, Object> removeForm(@PathVariable("formId") long formId){

        return formService.removeForm(formId);
    }

    @PostMapping("{formId}/unlink-prev")
    public Map<String, Object> unlinkPrev(@PathVariable("formId") long formId){

        return formService.unlinkPrev(formId);
    }

    @PostMapping("{formId}/clear-entry")
    public CompletableFuture<Map<String, Object>> clearEntry(@PathVariable("formId") long formId){
        return formService.clearEntry(formId);
    }

    @GetMapping("{formId}/related-comps")
    public Map<String, Object> relatedComps(@PathVariable("formId") long formId){
        return formService.relatedComps(formId);
    }

    public record FormMoveToApp(long appId, List<Long> datasetIds, List<Long> screenIds){}
    @PostMapping("{formId}/move-to-app")
    public Map<String, Object> moveToOtherApp(@PathVariable("formId") long formId, @RequestBody FormMoveToApp request){
        return formService.moveToOtherApp(formId, request);
    }


    @GetMapping({"basic",""})
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOne.class),
    })
    public Page<Form> findFormList(@RequestParam("appId") Long appId,
                                   @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable){
        return formService.findFormByAppId(appId, pageable);
    }


    /** DATASET **/

    @GetMapping("by-dataset/{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Form.class, mixin = FormMixin.FormBasicList.class),
            @JsonMixin(target = Item.class, mixin = FormMixin.FormItemOne.class),
    })
    public Form getFormByDatasetId(@PathVariable("id") long id){
        return formService.getFormByDatasetId(id);
    }

    /** FORM SECTION **/
    @PostMapping("section/{sectionId}/delete")
    public Map<String,Object> removeSection(@PathVariable("sectionId") long sectionId){
        Map<String, Object> data = new HashMap<>();
        formService.removeSection(sectionId);
        return data;
    }

    @GetMapping("{formId}/section")
    public Page<Section> getSectionList(@PathVariable("formId") long formId,
                                        Pageable pageable){
        return formService.findSectionByFormId(formId, pageable);
    }

    @PostMapping("{formId}/section")
    public Section saveSection(@PathVariable("formId") long formId, @RequestBody Section section){
        return formService.saveSection(formId, section);
    }

    @PostMapping("save-section-order")
    public List<Map<String, Long>> saveSectionOrder(@RequestBody List<Map<String, Long>> formSectionList){
        return formService.saveSectionOrder(formSectionList);
    }

    /** ## TABS **/
    @PostMapping("{formId}/tab")
    public Tab saveTab(@PathVariable("formId") long formId, @RequestBody Tab tab){
        return formService.saveTab(formId, tab);
    }

    @GetMapping("{formId}/tab")
    public Page<Tab> getTabList(@PathVariable("formId") long formId, Pageable pageable){
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
    public Page<Item> getItemList(@PathVariable("formId") long formId,
                                  Pageable pageable){
        return formService.findItemByFormId(formId, pageable);
    }

    @PostMapping("{formId}/items")
    public Item saveItem(@PathVariable("formId") long formId,
                         @RequestParam("sectionId") long sectionId,
                         @RequestBody Item item,
                         @RequestParam("sortOrder") long sortOrder){
        return formService.saveItem(formId, sectionId, item, sortOrder);
    }

    @PostMapping("{formId}/items-obj")
    public Item saveItem(@RequestBody Item item){
        return formService.saveItemOnly(item);
    }


    @PostMapping("{formId}/move-item")
    public SectionItem moveItem(@PathVariable("formId") long formId,
                         @RequestParam("sectionItemId") long sectionItemId,
                         @RequestParam("newSectionId") long newSectionId,
                         @RequestParam("sortOrder") long sortOrder){
        return formService.moveItem(formId, sectionItemId, newSectionId, sortOrder);
    }

    @PostMapping("{formId}/items/{itemId}/delete")
    public Map<String, Object> removeItem(@PathVariable("formId") long formId,
                                          @PathVariable("itemId") long itemId){
        Map<String, Object> data = new HashMap<>();
        formService.removeItem(formId,itemId);
        return data;
    }

    @PostMapping("save-item-order")
    public List<Map<String, Long>> saveItemOrder(@RequestBody List<Map<String, Long>> formItemList){
        return formService.saveItemOrder(formItemList);
    }

    @PostMapping("{formId}/items-source/{itemId}/delete")
    public Map<String, Object> removeItemSource(@PathVariable("formId") long formId,
                                                @PathVariable("itemId") long itemId){
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
    public Map<String, TierAction> saveTierActionOrder(@RequestParam("tierId") Long tierId,
                                                       @RequestBody List<Map<String, Long>> formTierActionList){
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


    @GetMapping(value = "qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRCode(@RequestParam(value = "code") String code,
                                            @RequestParam(value="h", defaultValue = "256") int h,
                                            @RequestParam(value = "w", defaultValue = "256") int w) {
        try {
            System.out.println("code:"+code);
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
    public Page<EntryTrail> findTrailByFormId(@PathVariable("id") long id,
                                              @RequestParam(value = "searchText", defaultValue = "") String searchText,
                                              @RequestParam(value = "actions") List<String> actions,
                                              @RequestParam(value = "dateFrom", required = false) Long dateFrom,
                                              @RequestParam(value = "dateTo", required = false) Long dateTo,
                                              Pageable pageable) {
        return formService.findTrailByFormId(id, searchText, actions, dateFrom!=null?new Date(dateFrom):null, dateTo!=null?new Date(dateTo):null, pageable);
    }

    @PostMapping("{id}/gen-view")
    public int generateDbView(@PathVariable("id") long id) throws Exception {
        return formService.generateView(id);
    }


    @PostMapping("save-form-order")
    public List<Map<String, Long>> saveFormOrder(@RequestBody List<Map<String, Long>> formList){
        return formService.saveFormOrder(formList);
    }


}
