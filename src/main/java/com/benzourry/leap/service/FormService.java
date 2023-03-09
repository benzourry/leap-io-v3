package com.benzourry.leap.service;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.config.Constant;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.*;
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
                       EntryAttachmentRepository entryAttachmentRepository
    ) {
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
        this.entryAttachmentRepository = entryAttachmentRepository;
    }

    public Form save(Long appId, Form form) {
        App app = appRepository.findById(appId).get();
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

        form.setInactive(!dateBetween(new Date(), form.getStartDate(), form.getEndDate()));
//        form.setActive(true);
        return formRepository.save(form);
    }

    public Date setTime(Date date, int hour, int minute, int second) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, second);
        return c.getTime();
    }

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
                Item oldItem = itemRepository.getReferenceById(item.getId());
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

    public SectionItem moveItem(long formId, long siId, long newSectionId, Long sortOrder) {
        SectionItem si = sectionItemRepository.getReferenceById(siId);


//        Section oldSection = sectionRepository.getReferenceById(oldSectionId);
        Section newSection = sectionRepository.getReferenceById(newSectionId);

        si.setSection(newSection);
        si.setSortOrder(sortOrder);

        return sectionItemRepository.save(si);

////        Form form = formRepository.findById(formId).get();
//        item.setForm(section.getForm());
//
////        form.getItems().put(item.getCode(), item);
////        formRepository.save(form);
//
//        if (item.getId()==null) {
//            SectionItem si = new SectionItem();
//            si.setSection(section);
//            si.setCode(item.getCode());
//            si.setSortOrder(sortOrder);
//            sectionItemRepository.save(si);
//        }else{
//            Item oldItem = itemRepository.getReferenceById(item.getId());
//            SectionItem si = sectionItemRepository.findBySectionIdAndCode(sectionId, oldItem.getCode());
//            si.setSection(section);
//            si.setCode(item.getCode());
//            sectionItemRepository.save(si);
//        }
//
//        item = itemRepository.save(item);
//        return item;
    }


//    public Element saveElement(long formId, Long parentId, Element item, Long sortOrder){
////        Section section = sectionRepository.findById(sectionId).get();
//
//        Form form = formRepository.findById(formId).get();
//        if (parentId!=null){
//            Element element = elementRepository.findById(parentId).get();
//            item.setElement(element);
////            item.setSortOrder(sortOrder);
//        }else{
//            item.setForm(form);
//        }
//
//        item = elementRepository.save(item);
//        return item;
//    }


//    public Model saveModel(long formId, Long parentId, Model item, Long sortOrder){
////        Section section = sectionRepository.findById(sectionId).get();
//
//        Form form = formRepository.findById(formId).get();
//        if (parentId!=null){
//            Model model = modelRepository.findById(parentId).get();
//            item.setModel(model);
////            item.setSortOrder(sortOrder);
//        }else{
//            item.setForm(form);
//        }
//
//        item = modelRepository.save(item);
//        return item;
//    }


//    public List<String> getColumns(long formId) {
//        Form form = formRepository.findById(formId).get();
//        return Arrays.stream(form.getRdQualifier().split(","))
//                .map(c -> {
//                    String[] arr = c.trim().split(" ");
//                    return (arr.length == 2 ? arr[1] : arr[0]).replace("/", "");
//                })
//                .collect(Collectors.toList());
//    }

    public Section saveSection(long formId, Section section) {
        Form form = formRepository.findById(formId).get();
        section.setForm(form);
        return sectionRepository.save(section);
    }


    public Page<Form> findFormByAppId(Long appId, Pageable pageable) {
        return formRepository.findByAppId(appId, pageable);
    }

    public Form findFormById(Long formId) {
        return formRepository.findById(formId).get();
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

    public List<Map<String, Long>> saveSectionOrder(List<Map<String, Long>> elementList) {
        for (Map<String, Long> element : elementList) {
            Section fs = sectionRepository.findById(element.get("id")).get();
            fs.setSortOrder(element.get("sortOrder"));
            sectionRepository.save(fs);
        }
        return elementList;
    }

    public List<Map<String, Long>> saveItemOrder(List<Map<String, Long>> formItemList) {
        for (Map<String, Long> element : formItemList) {
            SectionItem fi = sectionItemRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            sectionItemRepository.save(fi);
        }
        return formItemList;
    }

    public void removeItem(long formId, long sectionItemId) {
        SectionItem si = sectionItemRepository.findById(sectionItemId).get();
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

    public void removeItemSource(long formId, long itemId) {
        Item item = itemRepository.getReferenceById(itemId);
        Form f = formRepository.getReferenceById(formId);
        itemRepository.deleteById(item.getId());
        f.getItems().remove(item.getCode());

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

    public Tier saveTier(Long formId, Tier tier) {
        Form f = formRepository.findById(formId).get();
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
            Tier fi = tierRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            tierRepository.save(fi);
        }
        return formTierList;
    }

    public Map<String, TierAction> saveTierActionOrder(Long tierId, List<Map<String, Long>> formTierActionList) {
        for (Map<String, Long> element : formTierActionList) {
            TierAction fi = tierActionRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            tierActionRepository.save(fi);
        }
        return tierRepository.getReferenceById(tierId).getActions();
    }

    public Page<Item> findItemByFormId(long formId, Pageable pageable) {
        return itemRepository.findByFormId(formId, pageable);
    }

    public Form getFormByDatasetId(long id) {
        return formRepository.getByDatasetId(id);
    }


    public String getOrgMapApprover(Tier tier, String email, Entry entry) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();

        Tier at = tier;//tierRepository.getReferenceById(id);


//        System.out.println(at.getOrgMapParam());

//        System.out.println(at.getOrgMapParam().at("/code").asText("NA"));
//        String json = "";

        /**
         * PROBLEM!!!
         * On submit, action, etc, success to retrieve orgMapParam as proper JsonNode
         * On bulk update, orgMapParam jd string "{json_content}"
         */
        Map<String, String> orgParam;
//        System.out.println("orgMapParam:"+at.getOrgMapParam());
        if (at.getOrgMapParam().isObject()) {
            //System.out.println("orgMapParam(obj):"+at.getOrgMapParam());
            orgParam = mapper.convertValue(at.getOrgMapParam(), HashMap.class);
        } else {
            //System.out.println("orgMapParam(str):"+at.getOrgMapParam());
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

//        System.out.println("param:" + param);

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

    public List<Map<String, Long>> saveTabOrder(List<Map<String, Long>> formTabList) {
        for (Map<String, Long> element : formTabList) {
            Tab fi = tabRepository.findById(element.get("id")).get();
            fi.setSortOrder(element.get("sortOrder"));
            tabRepository.save(fi);
        }
        return formTabList;
    }


//    public void removeElement(long formId, long elementId) {
//        elementRepository.deleteById(elementId);
//    }

//    public void removeModel(long formId, long modelId) {
//        modelRepository.deleteById(modelId);
//    }

    @Scheduled(cron = "0 30 0 * * ?")
    @Transactional
    public void updateFormStatusSched() {
        Date now = new Date();
        formRepository.updateInactive(now);
    }

    public TierAction saveTierAction(Long tierId, TierAction tierAction) {
        Tier t = tierRepository.getReferenceById(tierId);
        tierAction.setTier(t);
//        f.getTiers().add(tier);
        return tierActionRepository.save(tierAction);
    }

    public void removeTierAction(Long tierActionId) {
        tierActionRepository.deleteById(tierActionId);
    }

    @Transactional
    public Map<String, Object> clearEntry(long formId) {

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
        return data;
    }

    public Form cloneForm(Long formId, Long appId) {
//        Form oriFormOpt = formRepository.getReferenceById(formId);

        App destApp = appRepository.getReferenceById(appId);
        Form oldForm = formRepository.getReferenceById(formId);
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
//        }
    }


    //    public App cloneApp (App app, String email){
//        app.setId(null);
//        App k = appRepository.save(app);
//        k.setEmail(email);
//        Page<Form> f = findFormByAppId(app.getId(), PageRequest.of(0, Integer.MAX_VALUE));
//
//        List<Form> flist = f.getContent();
//
//        List<Form> ggg = new ArrayList<>();
//
//        flist.forEach(f0->{
//            Form f1 = Helper.clone(Form.class, f0);
//            f1.setApp(k);
//            ggg.add(f1);
//        });
//
//        formRepository.saveAll(ggg);
//
//        return k;
//    }
    public Page<EntryApprovalTrail> findTrailByFormId(long id, String searchText, Pageable pageable) {
        searchText = "%"+searchText.toUpperCase()+"%";
        return entryApprovalTrailRepository.findTrailByFormId(id, searchText, pageable);
    }

    public Page<EntryApprovalTrail> findTrailByAppId(long id, Pageable pageable) {
        return entryApprovalTrailRepository.findTrailByAppId(id, pageable);
    }
}
