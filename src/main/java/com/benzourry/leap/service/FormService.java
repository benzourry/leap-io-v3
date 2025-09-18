package com.benzourry.leap.service;

import com.benzourry.leap.controller.FormController;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.config.Constant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class FormService {

    private final FormRepository formRepository;

    private final ItemRepository itemRepository;

    private final DatasetItemRepository datasetItemRepository;

//    ElementRepository elementRepository;

//    ModelRepository modelRepository;

    private final SectionRepository sectionRepository;

    private final SectionItemRepository sectionItemRepository;

    private final AppRepository appRepository;

    private final TierRepository tierRepository;

    private final TierActionRepository tierActionRepository;

//    DashboardRepository dashboardRepository;
//
//    ChartRepository chartRepository;

    private final TabRepository tabRepository;


    private final EntryRepository entryRepository;

    private final EntryAttachmentRepository entryAttachmentRepository;

    private final EntryApprovalTrailRepository entryApprovalTrailRepository;

    private final EntryTrailRepository entryTrailRepository;

    private final DynamicSQLRepository dynamicSQLRepository;

    private final LookupRepository lookupRepository;

    private final DatasetRepository datasetRepository;

    private final ScreenRepository screenRepository;


    public FormService(FormRepository formRepository,
                       ItemRepository itemRepository,
                       DatasetItemRepository datasetItemRepository,
//                       ElementRepository elementRepository,
//                       ModelRepository modelRepository,
                       SectionRepository sectionRepository,
                       SectionItemRepository sectionItemRepository,
                       AppRepository appRepository,
                       TierRepository tierRepository,
                       TierActionRepository tierActionRepository,
//                       DashboardRepository dashboardRepository,
//                       ChartRepository chartRepository,
                       TabRepository tabRepository,
                       EntryRepository entryRepository,
                       EntryApprovalTrailRepository entryApprovalTrailRepository,
                       EntryTrailRepository entryTrailRepository,
                       EntryAttachmentRepository entryAttachmentRepository,
                       DynamicSQLRepository dynamicSQLRepository,
                       LookupRepository lookupRepository,
                       DatasetRepository datasetRepository, ScreenRepository screenRepository) {
        this.formRepository = formRepository;
        this.itemRepository = itemRepository;
//        this.elementRepository = elementRepository;
//        this.modelRepository = modelRepository;
        this.sectionRepository = sectionRepository;
        this.sectionItemRepository = sectionItemRepository;
        this.appRepository = appRepository;
        this.tierRepository = tierRepository;
        this.tierActionRepository = tierActionRepository;
        this.datasetItemRepository = datasetItemRepository;
//        this.dashboardRepository = dashboardRepository;
//        this.chartRepository = chartRepository;
        this.tabRepository = tabRepository;
        this.entryRepository = entryRepository;
        this.entryApprovalTrailRepository = entryApprovalTrailRepository;
        this.entryTrailRepository = entryTrailRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.dynamicSQLRepository = dynamicSQLRepository;
        this.lookupRepository = lookupRepository;
        this.datasetRepository = datasetRepository;
        this.screenRepository = screenRepository;
    }


    @CacheEvict(value = "formJsonSchema", key = "#form.id")
    @Transactional
    public Form save(Long appId, Form form) {
        App app = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id",appId));
//        if (form.getAdmin()!=null){
//            if (Helper.isNullOrEmpty(form.getAdmin().getUsers())){
//                form.getAdmin().setUsers(form.getAdmin().getUsers().replaceAll(" ",""));
//            }
//        }
//        form.setAdmin(Optional.ofNullable(form.getAdmin()).orElse("").replaceAll(" ", ""));
        form.setApp(app);
//        if (form.getStartDate() != null) {
//            form.setStartDate(setTime(form.getStartDate(), 0, 0, 0));
//        }
//        if (form.getEndDate() != null) {
//            form.setEndDate(setTime(form.getEndDate(), 23, 59, 59));
//        }

        if (form.getX().get("extended") != null) {
            form.setTiers(List.of());
            form.setPrev(null);
        }

        form.setInactive(!dateBetween(new Date(), form.getStartDate(), form.getEndDate()));
//        form.setActive(true);
        return formRepository.save(form);
    }

//    public Date setTime(Date date, int hour, int minute, int second) {
//        Calendar c = Calendar.getInstance();
//        c.setTime(date);
//        c.set(Calendar.HOUR_OF_DAY, hour);
//        c.set(Calendar.MINUTE, minute);
//        c.set(Calendar.SECOND, second);
//        return c.getTime();
//    }

    public boolean dateBetween(Date d1, Date from, Date to) {
        if (from != null && to != null) {
            return d1.after(from) && d1.before(to);
        }
        if (from != null) {
            return d1.after(from);
        }
        if (to != null) {
            return d1.before(to);
        }
        return true;
    }

//    public Item updateItem(long formId,Item item){
//        item.setForm(formRepository.getReferenceById(formId));
//        return itemRepository.save(item);
//    }

    // solve orphan-remove error with adding @transactional, to-do: research!
    @CacheEvict(value = "formJsonSchema", key = "#item.form.id")
    @Transactional
    public Item saveItem(long formId, long sectionId, Item item, Long sortOrder) {
        if (item.getCode() != null) {
            Section section = sectionRepository.findById(sectionId).get();

//        Form form = formRepository.findById(formId).get();
            item.setForm(section.getForm());

            final String code = item.getCode();

//        form.getItems().put(item.getCode(), item);
//        formRepository.save(form);

            if (item.getId() == null) {
                SectionItem si = new SectionItem();
                si.setSection(section);
                si.setCode(item.getCode());
                si.setSortOrder(sortOrder);
                sectionItemRepository.save(si);
            } else {
                Item oldItem = itemRepository.findById(item.getId()).orElseThrow();
                SectionItem si = sectionItemRepository.findBySectionIdAndCode(sectionId, oldItem.getCode());
                si.setCode(item.getCode());
                sectionItemRepository.save(si);

                List<DatasetItem> diList = datasetItemRepository.findByCodeAndFormId(oldItem.getCode(), section.getForm().getId());
                List<DatasetItem> newDiList = diList.stream()
                        .filter(Objects::nonNull)
                        .peek(di -> di.setCode(code))
                        .collect(Collectors.toList());

                datasetItemRepository.saveAll(newDiList);

            }

            item = itemRepository.save(item);
        }
        return item;
    }

//    @CacheEvict(value = "formJsonSchema", key = "#item.form?.id")
    @Transactional
    public Item saveItemOnly(Item item) {
        Item newItem = item;
        // only run if items have code and have id
        if (item.getCode() != null && item.getId() !=null) {

            Item oldItem = itemRepository.getReferenceById(item.getId());
            oldItem.setLabel(item.getLabel());
            oldItem.setType(item.getType());
            oldItem.setSubType(item.getSubType());
            oldItem.setF(item.getF());

            newItem = itemRepository.save(oldItem);
        }
        return newItem;
    }

    @Transactional
    public SectionItem moveItem(long formId, long siId, long newSectionId, Long sortOrder) {
        SectionItem si = sectionItemRepository.getReferenceById(siId);

        Section newSection = sectionRepository.getReferenceById(newSectionId);

        si.setSection(newSection);
        si.setSortOrder(sortOrder);

        return sectionItemRepository.save(si);

    }

    @CacheEvict(value = "formJsonSchema", key = "#formId")
    public Section saveSection(long formId, Section section) {
        Form form = formRepository.findById(formId).get();
        section.setForm(form);
        return sectionRepository.save(section);
    }


    public Page<Form> findFormByAppId(Long appId, Pageable pageable) {
        return formRepository.findByAppId(appId, pageable);
    }

    public Form findFormById(Long formId) {
        Form form = formRepository.findById(formId).orElseThrow(()->new ResourceNotFoundException("Form","id",formId));
        if (form.getX().get("extended") != null && !form.getX().get("extended").isNull()) {
            Long extendedId = form.getX().get("extended").asLong();
            Optional<Form> extendedFormOpt = formRepository.findById(extendedId);
            if (extendedFormOpt.isPresent()) {
                Form extendedForm = extendedFormOpt.get();
                form.setTiers(extendedForm.getTiers());
                form.setPrev(extendedForm.getPrev());
            }
        }

        return form;
    }

    public Page<Section> findSectionByFormId(long formId, Pageable pageable) {
//        return formRepository.findById(formId).get().getSections();
        return sectionRepository.findByFormId(formId, pageable);
    }

//    public Page<Item> findItemsByFormId(long formId, Pageable pageable) {
//        return itemRepository.findByFormId(formId, pageable);
//    }

    public void removeSection(long sectionId) {
        sectionRepository.deleteById(sectionId);
    }

    @Transactional
    public List<Map<String, Long>> saveSectionOrder(List<Map<String, Long>> elementList) {
        for (Map<String, Long> element : elementList) {
            Section fs = sectionRepository.findById(element.get("id")).get();
            fs.setSortOrder(element.get("sortOrder"));
            sectionRepository.save(fs);
        }
        return elementList;
    }

    @Transactional
    public List<Map<String, Long>> saveItemOrder(List<Map<String, Long>> formItemList) {
        for (Map<String, Long> element : formItemList) {
            SectionItem fi = sectionItemRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            sectionItemRepository.save(fi);
        }
        return formItemList;
    }

    @Transactional
    @CacheEvict(value = "formJsonSchema", key = "#formId")
    public void removeItem(long formId, long sectionItemId) {
        SectionItem si = sectionItemRepository.findById(sectionItemId).orElseThrow(()->new ResourceNotFoundException("SectionItem","id",sectionItemId));
        Form f = formRepository.findById(formId).get();
        if (f.getItems().get(si.getCode()) != null) {
            itemRepository.deleteById(f.getItems().get(si.getCode()).getId());
            f.getItems().remove(si.getCode());
//            datasetItemRepository.deleteByCodeAndFormId(si.getCode(), f.getId());
        }
        List<DatasetItem> diList = datasetItemRepository.findByCodeAndFormId(si.getCode(), f.getId());
        datasetItemRepository.deleteAll(diList);
//        }
        sectionItemRepository.deleteById(si.getId());
//        f.getItems().remove(si.getCode());
    }

    @CacheEvict(value = "formJsonSchema", key = "#formId")
    @Transactional
    public void removeItemSource(long formId, long itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow(()->new ResourceNotFoundException("Item","id",itemId));
        Form f = formRepository.findById(formId).orElseThrow();
        f.getItems().remove(item.getCode());
        itemRepository.deleteById(item.getId());

        List<DatasetItem> diList = datasetItemRepository.findByCodeAndFormId(item.getCode(), f.getId());
//        if (di != null) {
        datasetItemRepository.deleteAll(diList);
//        }

        SectionItem si = sectionItemRepository.findByFormIdAndCode(f.getId(), item.getCode());
//        datasetItemRepository.deleteByCodeAndFormId(si.getCode(), f.getId());
        if (si != null) {
            sectionItemRepository.deleteById(si.getId());
        }
//        sectionItemRepository.deleteById(si.getId());
//        f.getItems().remove(si.getCode());
    }

    @CacheEvict(value = "formJsonSchema", key = "#formId")
    @Transactional
    public Map<String, Object> removeForm(long formId) {
        Map<String, Object> data = new HashMap<>();
        entryRepository.deleteApproverByFormId(formId);
        entryRepository.deleteApprovalByFormId(formId);
        entryRepository.deleteApprovalTrailByFormId(formId);
        entryRepository.deleteByFormId(formId);
        formRepository.deleteById(formId);
        return data;
    }

    @Transactional
    public Map<String, Object> unlinkPrev(long formId) {
        formRepository.unlinkPrev(formId);
        return Map.of("success", true);
    }

    @Transactional
    public Tier saveTier(Long formId, Tier tier) {
        Form f = formRepository.findById(formId).orElseThrow(()->new ResourceNotFoundException("Form","id",formId));
        tier.setForm(f);
//        f.getTiers().add(tier);
        return tierRepository.save(tier);
    }

    @Transactional
    public void removeTier(Long id) {
        entryRepository.deleteApprovalTrailByTierId(id);

        tierRepository.deleteById(id);
    }

    @Transactional
    public List<Map<String, Long>> saveTierOrder(List<Map<String, Long>> formTierList) {
        for (Map<String, Long> element : formTierList) {
            Tier fi = tierRepository.findById(element.get("id")).orElseThrow(()->new ResourceNotFoundException("Tier","id",element.get("id")));
            fi.setSortOrder(element.get("sortOrder"));
            tierRepository.save(fi);
        }
        return formTierList;
    }

    @Transactional
    public Map<String, TierAction> saveTierActionOrder(Long tierId, List<Map<String, Long>> formTierActionList) {
        for (Map<String, Long> element : formTierActionList) {
            TierAction fi = tierActionRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            tierActionRepository.save(fi);
        }
        return tierRepository.findById(tierId).orElse(new Tier()).getActions();
    }

    @Transactional(readOnly = true)
    public Page<Item> findItemByFormId(long formId, Pageable pageable) {
        return itemRepository.findByFormId(formId, pageable);
    }

    public Form getFormByDatasetId(long id) {
        return formRepository.getByDatasetId(id);
    }


    public String getOrgMapApprover(Tier tier, String email, Entry entry) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();

        Tier at = tier;//tierRepository.getReferenceById(id);

        /**
         * PROBLEM!!!
         * On submit, action, etc, success to retrieve orgMapParam as proper JsonNode
         * On bulk update, orgMapParam jd string "{json_content}"
         */
        Map<String, String> orgParam;
//        System.out.println("orgMapParam:"+at.getOrgMapParam());
        if (at.getOrgMapParam().isObject()) {
            orgParam = mapper.convertValue(at.getOrgMapParam(), HashMap.class);
        } else {
            orgParam = mapper.readValue(at.getOrgMapParam().asText("{}"), HashMap.class);
        }


        Map<String, String> translated = new HashMap<>();

        orgParam.keySet().stream().forEach(key -> {
            String value = orgParam.get(key);
            if (orgParam.get(key).contains("$")) {
                translated.put(key, entry.getData().at("/" + value.replace("$.", "").replace(".", "/")).asText());
            } else {
                translated.put(key, value);
            }
        });

        String param = translated.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));


        RestTemplate rt = new RestTemplate();
        String dm = at.getOrgMap().contains("?") ? "&" : "?";
        ResponseEntity<JsonNode> re = rt.getForEntity(at.getOrgMap() + dm + "email=" + email + "&" + param, JsonNode.class);
        return re.getBody().at(at.getOrgMapPointer()).asText();
    }

    public Tab saveTab(long formId, Tab tab) {
        Form form = formRepository.findById(formId).get();
        tab.setForm(form);
        return tabRepository.save(tab);
    }

    public Page<Tab> findTabByFormId(long formId, Pageable pageable) {
        return tabRepository.findByFormId(formId, pageable);
    }

    public void removeTab(Long id) {
        tabRepository.deleteById(id);
    }

    @Transactional
    public List<Map<String, Long>> saveTabOrder(List<Map<String, Long>> formTabList) {
        for (Map<String, Long> element : formTabList) {
            Tab fi = tabRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            tabRepository.save(fi);
        }
        return formTabList;
    }

    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void updateFormStatusSched() {
        Date now = new Date();
        formRepository.updateInactive(now);
    }

    @Transactional
    public TierAction saveTierAction(Long tierId, TierAction tierAction) {
        /*
        Tier t = tierRepository.getReferenceById(tierId);
//        System.out.println(t);
        tierAction.setTier(t);
        // Mystery: this is not needed for form:items but why is it needed here?
        // else it throw error, A collection with cascade=\"all-delete-orphan\" was no longer referenced by the owning entity instance:
        t.getActions().put(tierAction.getCode(),tierAction);
        tierRepository.save(t);
//        f.getTiers().add(tier);
        return tierActionRepository.save(tierAction);

         */

        Optional<Tier> tOpt = tierRepository.findById(tierId);
        if (tOpt.isPresent()){
            Tier t = tOpt.get();
            tierAction.setTier(t);
            tierActionRepository.save(tierAction);

        }
        return tierAction;
    }

    public void removeTierAction(Long tierActionId) {
        tierActionRepository.deleteById(tierActionId);
    }

    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> clearEntry(long formId) {

        AtomicInteger ai = new AtomicInteger();
        List<EntryAttachment> entryAttachmentList = entryAttachmentRepository.findByFormId(formId);
        entryAttachmentList.forEach(ea -> {
            String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";

            if (ea.getBucketId() != null) {
                destStr += "bucket-" + ea.getBucketId() + "/";
            }

            File dir = new File(destStr);
            dir.mkdirs();

            File dest = new File(destStr + ea.getFileUrl());
            dest.delete();
            ai.getAndIncrement();
            entryAttachmentRepository.delete(ea);
        });
        Map<String, Object> data = new HashMap<>();
        data.put("files", ai.get());
        this.entryRepository.deleteApproverByFormId(formId);
        this.entryRepository.deleteApprovalByFormId(formId);
        this.entryRepository.deleteApprovalTrailByFormId(formId);
        data.put("rows", this.entryRepository.deleteByFormId(formId));
        data.put("success", true);
        return CompletableFuture.completedFuture(data);
    }

    @Transactional
    public Form cloneForm(Long formId, Long appId) {
//        Form oriFormOpt = formRepository.getReferenceById(formId);

        App destApp = appRepository.findById(appId).orElseThrow(()->new ResourceNotFoundException("App","id",appId));
        Form oldForm = formRepository.findById(formId).orElseThrow(()->new ResourceNotFoundException("Form","id",formId));
        Form newForm = new Form();

        // Copy all properties
        BeanUtils.copyProperties(oldForm, newForm, "id", "prev");
//        if (oldForm.getAccess()!=null){
//            newForm.setAccess(groupMap.get(oldForm.getAccess().getId()));
//        }
//        if (oldForm.getAdmin()!=null){
//            newForm.setAdmin(groupMap.get(oldForm.getAdmin().getId()));
//        }
        // Initialize child components
        Map<String, Item> items = new HashMap<>();
        List<Section> sections = new ArrayList<>();
        List<Tab> tabs = new ArrayList<>();
        List<Tier> tiers = new ArrayList<>();

        newForm.setItems(items);
        newForm.setSections(sections);
        newForm.setTabs(tabs);
        newForm.setTiers(tiers);

        newForm.setApp(destApp);
        formRepository.save(newForm);


//        if (oriFormOpt.isPresent()){
        Map<Long, Tab> tabMap = new HashMap<>();
        Map<Long, Section> sectionMap = new HashMap<>();


        Map<String, Item> newItemMap = newForm.getItems();
        oldForm.getItems().forEach((name, oldItem) -> {
            Item newItem = new Item();
            BeanUtils.copyProperties(oldItem, newItem, "id");
            ObjectNode onode = (ObjectNode) oldItem.getX();
            // ## Maintain bucket
//                if (onode.get("bucket")!=null) {
//                    onode.put("bucket", bucketMap.get(onode.get("bucket").longValue()).getId());
//                    newItem.setX(onode);
//                }
            newItem.setForm(newForm);
            newItemMap.put(name, newItem);
        });

        List<Tab> newTabList = newForm.getTabs();
        oldForm.getTabs().forEach(oldTab -> {
            Tab newTab = new Tab();
            BeanUtils.copyProperties(oldTab, newTab, "id");
            newTab.setForm(newForm);
//                tabRepository.save(newTab);
            newTabList.add(newTab);
            tabMap.put(oldTab.getId(), newTab);
        });
        tabRepository.saveAll(newTabList);

        List<Section> newSectionList = newForm.getSections();
        oldForm.getSections().forEach(oldSection -> {
            Section newSection = new Section();
            BeanUtils.copyProperties(oldSection, newSection, "id");
            Set<SectionItem> siSet = new HashSet<>();
            oldSection.getItems().forEach(si -> {
                SectionItem si2 = new SectionItem();
                BeanUtils.copyProperties(si, si2, "id");
                si2.setSection(newSection);
                siSet.add(si2);
            });

            if (oldSection.getParent() != null) {
                Optional.ofNullable(tabMap.get(oldSection.getParent()))
                        .ifPresent(o -> newSection.setParent(o.getId()));
//                if (tabMap.get(oldSection.getParent())!=null) {
//                    newSection.setParent(tabMap.get(oldSection.getParent()).getId());
//                }
            }
            newSection.setForm(newForm);
            newSection.setItems(siSet);
            sectionMap.put(oldSection.getId(), newSection);
            newSectionList.add(newSection);
        });
        return formRepository.save(newForm);
    }

    @Transactional
    public Page<EntryTrail> findTrailByFormId(long id, String searchText, List<String> action, Date dateFrom, Date dateTo, Pageable pageable) {
        searchText = "%" + searchText.toUpperCase() + "%";
        return entryTrailRepository.findTrailByFormId(id, searchText, action, dateFrom, dateTo, pageable);
    }

    public Page<EntryApprovalTrail> findTrailByAppId(long id, Pageable pageable) {
        return entryApprovalTrailRepository.findTrailByAppId(id, pageable);
    }

    @Transactional
    public int generateView(Long formId) throws Exception {
        Form f = findFormById(formId);

        List<String> listField = new ArrayList<>();
        listField.add("`$.$code` longtext PATH 'lax $.$code'");
        listField.add("`$.$counter` int(25) PATH 'lax $.$counter'");
        f.getItems().forEach((code, item) -> {
            if (Arrays.asList("text", "file", "eval").contains(item.getType())) {
                listField.add("`$." + code + "` longtext PATH 'lax $." + code + "'");
            }
            if (Arrays.asList("select", "radio").contains(item.getType())) {
                Lookup lookup = null;
                listField.add("`$." + code + ".code` longtext PATH 'lax $." + code + ".code'");
                listField.add("`$." + code + ".name` longtext PATH 'lax $." + code + ".name'");
                listField.add("`$." + code + ".extra` longtext PATH 'lax $." + code + ".extra'");
                if (item.getDataSource() != null) {
                    lookup = lookupRepository.getReferenceById(item.getDataSource());
                    if ("db".equals(lookup.getSourceType()) && lookup.getDataFields() != null) {
                        String[] splitted = lookup.getDataFields().split(",");
                        Arrays.asList(splitted).forEach(sp -> {
                            String field = sp.split(":")[0].trim();
                            listField.add("`$." + code + ".data." + field + "` longtext PATH 'lax $." + code + ".data." + field + "'");
                        });
                    }
                }
            }

            if (Arrays.asList("checkboxOption").contains(item.getType()) ||
                    Arrays.asList("multiple").contains(item.getSubType())) {
                listField.add("`$." + code + "` longtext PATH 'lax $." + code + "'");
            }
            if (Arrays.asList("modelPicker").contains(item.getType())) {
                listField.add("`$." + code + "` longtext PATH 'lax $." + code + "'");
            }
            if (Arrays.asList("checkbox").contains(item.getType())) {
                listField.add("`$." + code + "` bit(1) PATH 'lax $." + code + "'");
            }
            if (Arrays.asList("number", "scaleTo10", "scaleTo5", "scale").contains(item.getType())) {
                listField.add("`$." + code + "` int(25) PATH 'lax $." + code + "'");
            }
            if (Arrays.asList("date").contains(item.getType())) {
                listField.add("`$." + code + "` int(25) PATH 'lax $." + code + "'");
            }
        });

        String sql = "create or replace view app_"+f.getAppId()+"_form_" + formId + " AS (SELECT e.id, e.current_status, e.current_tier, e.current_tier_id, e.email, e.final_tier_id, e.resubmission_date, e.submission_date, e.form, e.created_by, e.created_date, e.modified_by, e.modified_date, e.current_edit, e.prev_entry, t.* " +
                " FROM entry AS e JOIN JSON_TABLE(e.`data`,'$' COLUMNS( "
                + String.join(",", listField) +
                ") ) AS t WHERE e.form = " + formId + " and e.deleted=false)";


        return dynamicSQLRepository.executeQuery(sql, Map.of());
    }


    @Transactional
    public List<Map<String, Long>> saveFormOrder(List<Map<String, Long>> formList) {
        for (Map<String, Long> element : formList) {
            Form fi = formRepository.findById(element.get("id")).orElseThrow(()->new ResourceNotFoundException("Form","id",element.get("id")));
            fi.setSortOrder(element.get("sortOrder"));
            formRepository.save(fi);
        }
        return formList;
    }

    @Transactional
    public Map<String, Object> moveToOtherApp(long formId,FormController.FormMoveToApp request) {
        Form form = formRepository.findById(formId).orElseThrow(() -> new ResourceNotFoundException("Form", "id", formId));

        App app = appRepository.findById(request.appId()).orElseThrow(() -> new ResourceNotFoundException("App","id", request.appId()));


        form.setApp(app);
        form.setAppId(app.getId());


        List<Dataset> datasetList = datasetRepository.findByIds(request.datasetIds());
        List<Dataset> newDatasetList = datasetList.stream().map(ds->{
            ds.setApp(app);
            ds.setAppId(app.getId());
            return ds;
        }).toList();
        datasetRepository.saveAll(newDatasetList);

        List<Screen> screenList = screenRepository.findByIds(request.screenIds());
        List<Screen> newScreenList = screenList.stream().map(sc->{
            sc.setApp(app);
            sc.setAppId(app.getId());
            return sc;
        }).toList();
        screenRepository.saveAll(newScreenList);

        return Map.of("success", true);
    }

    public Map<String, Object> relatedComps(long formId) {
        List<Dataset> datasetList = datasetRepository.findByFormId(formId, PageRequest.of(0,Integer.MAX_VALUE));
        List<Screen> screenList = screenRepository.findByFormId(formId, PageRequest.of(0, Integer.MAX_VALUE));
        return Map.of("dataset", datasetList.stream().map(ds->Map.of("id", ds.getId(),"title", ds.getTitle())).toList(),
                "screen", screenList.stream().map(sc-> Map.of("id", sc.getId(), "title", sc.getTitle())).toList());
    }


    private static final ObjectMapper GETJSONSCHEMA_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    @Cacheable(value = "formJsonSchema", key = "#form.id")
    public String getJsonSchema(Form form){
        Map<String, Object> envelop = new HashMap<>();
//        envelop.put("$schema","https://json-schema.org/draft/2020-12/schema");
//        envelop.put("title", form.getTitle());
        envelop.put("type", "object");
//        Form form = formRepository.findById(formId).orElseThrow(()->new ResourceNotFoundException("Form","id",formId));
        Map<String, Object> properties = new HashMap<>();

        form.getSections().forEach(section->{
            if ("section".equals(section.getType())){
                    processFormatting(form, section, properties);
            }
            if ("list".equals(section.getType())){
                    Map<String, Object> schemaArray = new HashMap<>();
                    Map<String, Object> arrayProps = new HashMap<>();
                    schemaArray.put("type", "array");
                    processFormatting(form, section, arrayProps);
                    schemaArray.put("items", Map.of("type", "object", "properties", arrayProps));
                    properties.put(section.getCode(), schemaArray);
            }
        });

        envelop.put("properties", properties);
//        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        String jsonStr;
        try {
            jsonStr = GETJSONSCHEMA_MAPPER.writeValueAsString(envelop);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }


//        String fmtStr = convertMapToSchema(properties);
//        String jsonStr = convertMapToJson(properties);

        return jsonStr;
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int incrementAndGetCounter(Long formId) {
        formRepository.incrementCounter(formId);
//        return formRepository.findCounter(formId);
        return formRepository.getLatestCounter();
    }

    private void processFormatting(Form form, Section section, Map<String, Object> sFormatter) {
        List<String> requiredProp = new ArrayList<>();
        section.getItems().forEach(i->{
            Item item = form.getItems().get(i.getCode());

            if (item.getV()!=null && item.getV().at("/required").asBoolean(false)){
                requiredProp.add(i.getCode());
            }


            if (List.of("text").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",Optional.ofNullable(item.getLabel()).orElse("").trim(),
                        "type","string"));
            }else if (List.of("file").contains(item.getType())){
                if (List.of("imagemulti", "othermulti").contains(Optional.ofNullable(item.getSubType()).orElse(""))){
                    sFormatter.put(i.getCode(),Map.of(
                            "description",Optional.ofNullable(item.getLabel()).orElse("").trim(),
                            "type","array",
                            "items", Map.of("type","string")));
                }else{
                    sFormatter.put(i.getCode(),Map.of(
                            "description",Optional.ofNullable(item.getLabel()).orElse("").trim(),
                            "type","string"));
                }
            }else if (List.of("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",Optional.ofNullable(item.getLabel()).orElse("").trim(),
                        "type","number"));
            }else if (List.of("date").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",Optional.ofNullable(item.getLabel()).orElse("").trim() + " as UNIX timestamp in miliseconds",
                        "type","number"));
            }else if (List.of("checkbox").contains(item.getType())){
                sFormatter.put(i.getCode(),Map.of(
                        "description",Optional.ofNullable(item.getLabel()).orElse("").trim(),
                        "type","boolean"));
            }else if (List.of("select", "radio").contains(item.getType())){
                if (List.of("multiple").contains(item.getSubType())){
                    sFormatter.put(
                            i.getCode(),
                            Map.of("type","array",
                                    "items", Map.of(
                                            "type","object",
                                            "description", Optional.ofNullable(item.getLabel()).orElse("").trim(),
                                            "properties", Map.of(
                                                    "code", Map.of("type","string"),
                                                    "name",Map.of("type","string")
                                            )
                                    )
                            )
                    );
                }else{
                    sFormatter.put(
                            i.getCode(), Map.of(
                                    "type","object",
                                    "description", Optional.ofNullable(item.getLabel()).orElse("").trim(),
                                    "properties", Map.of(
                                            "code", Map.of("type","string"),
                                            "name", Map.of("type","string")
                                    )
                            )
                    );
                }

            }else if (List.of("modelPicker").contains(item.getType())){
                if (List.of("multiple").contains(item.getSubType())){
                    sFormatter.put(
                            i.getCode(),
                            Map.of("type","array",
                                    "items",Map.of(
                                            "type","object",
                                            "description", Optional.ofNullable(item.getLabel()).orElse("").trim(),
                                            "properties", Map.of()
                                    )
                            )
                    );
                }else{
                    sFormatter.put(
                            i.getCode(), Map.of(
                                    "type","object",
                                    "description", Optional.ofNullable(item.getLabel()).orElse("").trim(),
                                    "properties", Map.of()
                            )
                    );
                }

            }else if (List.of("map").contains(item.getType())){
                sFormatter.put(
                        i.getCode(), Map.of(
                                "type","object",
                                "description", Optional.ofNullable(item.getLabel()).orElse("").trim(),
                                "properties", Map.of(
                                        "longitude", Map.of("type","number"),
                                        "latitude",Map.of("type","number")
                                )
                        )
                );
            }else if(List.of("simpleOption").contains(item.getType())){
                sFormatter.put(i.getCode(), Map.of(
                        "type",List.of("integer","string"),
                        "description", Optional.ofNullable(item.getLabel()).orElse("").trim(),
                        "enum",Optional.ofNullable(item.getOptions())
                                .map(opts -> Arrays.stream(opts.split(","))
                                        .map(String::trim)
                                        .toList())
                                .orElse(List.of())
                ));
            }

        });
        sFormatter.put("required", requiredProp);
        sFormatter.put("additionalProperties", true);

    }

}
