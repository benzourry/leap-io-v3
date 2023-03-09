package com.benzourry.leap.controller;

import com.benzourry.leap.mixin.LookupMixin;
import com.benzourry.leap.model.Lookup;
import com.benzourry.leap.model.LookupEntry;
import com.benzourry.leap.service.LookupService;
import com.benzourry.leap.utility.jsonresponse.JsonMixin;
import com.benzourry.leap.utility.jsonresponse.JsonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"api/lookup", "api/public/lookup"})
//@CrossOrigin(allowCredentials="true")
public class LookupController {

    final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("{id}")
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupOne.class)
    })
    public Lookup getLookup(@PathVariable long id) {
        return lookupService.getLookup(id);
    }

    @PostMapping
    public Lookup save(@RequestBody Lookup lookup, @RequestParam("appId") Long appId, @RequestParam("email") String email) {
        return lookupService.save(lookup, appId, email);
    }

    @GetMapping
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupBasicList.class)
    })
    public Page<Lookup> findByAppId(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                    @RequestParam(value = "appId", required = false) Long appId, Pageable pageable) {
        return lookupService.findByAppId(searchText, appId, pageable);
    }

    @GetMapping("all")
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupBasicList.class)
    })
    public Page<Lookup> findAllLookup(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                      @RequestParam(value = "email", required = false) String email,
                                      Pageable pageable) {
        return lookupService.findByQuery(searchText, email, pageable);
    }

    @GetMapping("full")
    @JsonResponse(mixins = {
            @JsonMixin(target = Lookup.class, mixin = LookupMixin.LookupBasicList.class)
    })
    public Page<Lookup> findAllFullLookup(@RequestParam(value = "searchText", defaultValue = "") String searchText,
                                          @RequestParam(value = "appId", required = false) Long appId) {
        return lookupService.findByAppId(searchText, appId, PageRequest.of(0, Integer.MAX_VALUE));
    }

    @PostMapping("{id}/delete")
    public Map<String, Object> delete(@PathVariable long id) {
        Map<String, Object> data = new HashMap<>();
        lookupService.removeLookup(id);
        return data;
    }

    @PostMapping("{id}/entry")
    public LookupEntry save(@PathVariable long id, @RequestBody LookupEntry lookup) {
        return lookupService.save(id, lookup);
    }


    @PostMapping("entry/field")
    public LookupEntry updateEntry(@RequestParam("entryId") long id, @RequestBody JsonNode lookup) {
        return lookupService.updateLookupEntry(id, lookup);
    }

    @PostMapping("entry/{id}/delete")
    public Map<String, Object> deleteEntry(@PathVariable long id) {
        Map<String, Object> data = new HashMap<>();
        lookupService.removeLookupEntry(id);
        return data;
    }

    @GetMapping("{id}/entry")
    @JsonResponse(mixins = {
            @JsonMixin(target = LookupEntry.class, mixin = LookupMixin.LookupEntryList.class)
    })
//    @Cacheable(value = "lookupEntry", key = "{#id,}")
    public ResponseEntity<Map<String, Object>> findAllEntry(@PathVariable long id,
                                            @RequestParam(value = "searchText", required = false) String searchText,
                                            HttpServletRequest request, Pageable pageable) {
        try {
            return ResponseEntity
                    .ok(lookupService.findAllEntry(id, searchText, request, true, pageable));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(Map.of("error", "Problem loading lookup entry", "message", e.getMessage()));
        }
    }

    @GetMapping("{id}/entry-full")
    @JsonResponse(mixins = {
            @JsonMixin(target = LookupEntry.class, mixin = LookupMixin.LookupEntryListFull.class)
    })
    public ResponseEntity<Map<String, Object>> findAllEntryFull(@PathVariable long id,
                                                @RequestParam(value = "searchText", required = false) String searchText,
                                                HttpServletRequest request, Pageable pageable) {
        try {
            return ResponseEntity
                    .ok(lookupService.findAllEntry(id, searchText, request, false, pageable));
        } catch (Exception e) {
            return ResponseEntity
                    .status(400)
                    .body(Map.of("error", "Problem loading lookup entry", "message", e.getMessage()));
        }
    }

//    @GetMapping("{id}/entry-as-map")
//    public Map<String, String> findAllEntryAsMap(@PathVariable long id,Pageable pageable){
//        return lookupService.findAllEntryAsMap(id, pageable);
//    }

    @GetMapping("in-form/{formId}")
    public List<Map> findLookupInForm(@PathVariable("formId") Long formId,
                                      @RequestParam(name = "sectionType", defaultValue = "section,list,approval") List<String> sectionType) {
//        return lookupService.findIdByFormId(formId);
        return lookupService.findIdByFormIdAndSectionType(formId, sectionType);
    }


    @PostMapping("save-order")
    public List<Map<String, Long>> saveOrder(@RequestBody List<Map<String, Long>> lookupOrderList) {
        return lookupService.saveOrder(lookupOrderList);
    }

//    @GetMapping("in-form-bysection/{formId}")
//    public List<Map> findLookupInForm(@PathVariable("formId") Long formId,
//                                      @RequestParam(name = "sectionType", defaultValue = "section,approval") List<String> sectionType){
//        return lookupService.findIdByFormIdAndSectionType(formId, sectionType);
//    }

    @GetMapping("update-data")
    public void updateLookupData(@RequestParam("lookupId") Long lookupId,
                                 @RequestParam("refCol") String refCol) throws IOException {
//        Map<String, Object> data = new HashMap<>();
        this.lookupService.updateLookupDataNew(lookupId, refCol);
//        return this.entryService.execVal(formId, field, force);
//        return data;
    }
}
