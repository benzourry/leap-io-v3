package com.benzourry.leap.controller;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.exception.ResourceNotFoundException;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.*;
import com.benzourry.leap.service.EntryService;
import com.benzourry.leap.service.FormService;
import com.benzourry.leap.service.LookupService;
import com.benzourry.leap.utility.Helper;
import com.benzourry.leap.utility.export.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Created by MohdRazif on 5/5/2017.
 */
@Controller
public class ExportController {

    private static final Logger logger = LoggerFactory.getLogger(ExportController.class);

    public final DatasetRepository datasetRepository;
    public final DashboardRepository dashboardRepository;
    public final EntryService entryService;
    public final LookupService lookupService;
    public final EntryRepository entryRepository;
    public final EntryAttachmentRepository entryAttachmentRepository;
    public final FormRepository formRepository;
    public final LookupEntryRepository lookupEntryRepository;
    public final LookupRepository lookupRepository;
    public final FormService formService;
    public final SectionItemRepository sectionItemRepository;
    public final SectionRepository sectionRepository;
    public final AppRepository appRepository;
    public final NaviGroupRepository naviGroupRepository;
    private final ObjectMapper MAPPER;

    public ExportController(DatasetRepository datasetRepository, DashboardRepository dashboardRepository, EntryService entryService, NaviGroupRepository naviGroupRepository, LookupService lookupService, EntryRepository entryRepository, EntryAttachmentRepository entryAttachmentRepository, FormRepository formRepository, AppRepository appRepository, LookupEntryRepository lookupEntryRepository, LookupRepository lookupRepository, FormService formService, SectionItemRepository sectionItemRepository, SectionRepository sectionRepository, ObjectMapper MAPPER) {
        this.datasetRepository = datasetRepository;
        this.dashboardRepository = dashboardRepository;
        this.entryService = entryService;
        this.naviGroupRepository = naviGroupRepository;
        this.lookupService = lookupService;
        this.entryRepository = entryRepository;
        this.entryAttachmentRepository = entryAttachmentRepository;
        this.formRepository = formRepository;
        this.appRepository = appRepository;
        this.lookupEntryRepository = lookupEntryRepository;
        this.lookupRepository = lookupRepository;
        this.formService = formService;
        this.sectionItemRepository = sectionItemRepository;
        this.sectionRepository = sectionRepository;
        this.MAPPER = MAPPER;
    }


    @RequestMapping(value = "/report/export/{id}/{format}", method = RequestMethod.GET)
    public ModelAndView getMyData(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @PathVariable("id") Long id,
                                  @PathVariable("format") String format,
                                  @RequestParam(value = "email", required = false) String email,
                                  @RequestParam(value = "searchText", required = false) String searchText,
                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                  @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                  @RequestParam(value = "sorts", required = false) List<String> sorts,
                                  @RequestParam(value = "ids", required = false) List<Long> ids,
//                                  @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                  @RequestParam(value = "size", required = false) Integer size,
                                  @RequestParam(value = "page", required = false) Integer page) {
        Map<String, Object> model = new HashMap<>();

        Map p = new HashMap();
        try {
            p = MAPPER.readValue(filters, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Dataset dataset = datasetRepository.findById(id).get();

        Page<EntryDto> entries = entryService.findListByDataset(dataset.getId(), searchText, email, p, cond, sorts, ids, PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)), request);

        //Sheet Name
        model.put("sheetname", dataset.getTitle());

        List<EntryDto> result = entries.getContent();

        Form curForm = dataset.getForm();
        Form prevForm = null;
        if (dataset.getForm().getPrev() != null) {
            prevForm = dataset.getForm().getPrev();
        }

        String filename = URLEncoder
                .encode(dataset.getTitle().replaceAll("[^a-zA-Z0-9.]",""), StandardCharsets.UTF_8)
                .toLowerCase();
//        String filename = dataset.getTitle().replace(" ", "-").toLowerCase();

        model.put("headers", dataset.getItems());
        model.put("results", result);
        model.put("dataset", dataset);
        model.put("prevForm", prevForm);
        model.put("attachmentRepository", entryAttachmentRepository);

        ModelAndView mv = null;
        if ("xlsx".equals(format)) {
            response.setContentType("application/ms-excel");
            mv = new ModelAndView(new ExcelView(), model);
        } else if ("csv".equals(format)) {
            response.setContentType("text/csv");
            mv = new ModelAndView(new CsvView(), model);
        } else if ("pdf".equals(format)) {
            response.setContentType("application/pdf");
            mv = new ModelAndView(new PdfView(), model);
        }
        response.setHeader("Content-disposition", "attachment; filename=" + filename + "." + format);
        return mv;
    }

    @RequestMapping(value = "/report/export-async/{id}/{format}", method = RequestMethod.GET)
    public CompletableFuture<ModelAndView> getMyDataCtrl(HttpServletRequest request,
                                                         HttpServletResponse response,
                                                         @PathVariable("id") Long id,
                                                         @PathVariable("format") String format,
                                                         @RequestParam(value = "email", required = false) String email,
                                                         @RequestParam(value = "searchText", required = false) String searchText,
                                                         @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
                                                         @RequestParam(value = "@cond", required = false, defaultValue = "AND") String cond,
                                                         @RequestParam(value = "sorts", required = false) List<String> sorts,
                                                         @RequestParam(value = "ids", required = false) List<Long> ids,
//                                  @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                                         @RequestParam(value = "size", required = false) Integer size,
                                                         @RequestParam(value = "page", required = false) Integer page) {


        return getMyDataProcess(request, response, id, format, email, searchText, filters, cond, sorts, ids, size, page)
                .thenApply(model -> {
                    response.setHeader("Content-disposition", "attachment; filename=" + model.get("filename") + "." + format);
                    if ("xlsx".equals(format)) {
                        response.setContentType("application/ms-excel");
                        return new ModelAndView(new ExcelViewStream(), model);
                    } else if ("csv".equals(format)) {
                        response.setContentType("text/csv");
                        return new ModelAndView(new CsvView(), model);
                    } else if ("pdf".equals(format)) {
                        response.setContentType("application/pdf");
                        return new ModelAndView(new PdfView(), model);
                    } else {
                        return null;
                    }
                });
    }

    @Async("asyncExec")
    @Transactional
    public CompletableFuture<Map<String, Object>> getMyDataProcess(HttpServletRequest request,
                                                                   HttpServletResponse response,
                                                                   Long id,
                                                                   String format,
                                                                   String email,
                                                                   String searchText,
                                                                   String filters,
                                                                   String cond,
                                                                   List<String> sorts,
                                                                   List<Long> ids,
                                                                   Integer size,
                                                                   Integer page) {
        Map<String, Object> model = new HashMap<>();

        Map p = new HashMap();
        try {
            p = MAPPER.readValue(filters, Map.class);
//            s = mapper.readValue(status, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Dataset dataset = datasetRepository.findById(id).get();

//        Page<Entry> entries = entryService.findListByDataset(dataset.getId(), searchText, email, p, cond, sorts, ids, PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)), request);
//
        Stream<Entry> streams = entryService.findListByDatasetStream(dataset.getId(), searchText, email, p, cond, sorts, ids, request);
        model.put("streams", streams);
        model.put("searchText", searchText);
        model.put("email", searchText);
        model.put("filters", p);
        model.put("cond", cond);
        model.put("sorts", sorts);
        model.put("ids", ids);
        model.put("request", request);

        //Sheet Name
        model.put("sheetname", dataset.getTitle());

//        List<Entry> result = entries.getContent();

        Form curForm = dataset.getForm();
        Form prevForm = null;
        if (dataset.getForm().getPrev() != null) {
            prevForm = dataset.getForm().getPrev();
        }

//        String filename = dataset.getTitle().replace(" ", "-").toLowerCase();
        String filename = URLEncoder
                .encode(dataset.getTitle().replaceAll("[^a-zA-Z0-9.]",""), StandardCharsets.UTF_8)
                .toLowerCase();

        model.put("headers", dataset.getItems());
//        model.put("results", result);
//        model.put("streams", stream);
        model.put("dataset", dataset);
        model.put("prevForm", prevForm);
        model.put("filename", filename);
        model.put("entryService", this.entryService);
        model.put("attachmentRepository", this.entryAttachmentRepository);

        return CompletableFuture.completedFuture(model);
    }


    @GetMapping
    public String index(Model model) {
        model.addAttribute("UI_BASE_DOMAIN", Constant.UI_BASE_DOMAIN);
        return "index";
    }

    @RequestMapping(value = "/report/export-lookup/{id}/{format}", method = RequestMethod.GET)
    public ModelAndView getMyLookupData(HttpServletRequest request,
                                        HttpServletResponse response,
                                        @PathVariable("id") Long id,
                                        @PathVariable("format") String format,
                                        @RequestParam(value = "email", required = false) String email,
//                                  @RequestParam(value = "filters", required = false, defaultValue = "{}") String filters,
//                                  @RequestParam(value = "status", required = false, defaultValue = "{}") String status,
                                        @RequestParam(value = "size", required = false) Integer size,
                                        @RequestParam(value = "page", required = false) Integer page) throws Exception {
        Map<String, Object> model = new HashMap<>();

//        Map p = new HashMap();
//        Map s = new HashMap();
//        try {
//            p = mapper.readValue(filters, Map.class);
////            s = mapper.readValue(status, Map.class);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        Lookup lookup = lookupRepository.findById(id)
                .orElseThrow(()->new ResourceNotFoundException("Lookup","id",id));

        Map<String, Object> entries = lookupService.findAllEntry(id, null, request, false, PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)));//.findListByDataset(dataset.getId(), "%", email, p, PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)), request);
//        Page<LookupEntry> entries = lookupEntryRepository.findByLookupId(id, null,  PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)));//.findListByDataset(dataset.getId(), "%", email, p, PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)), request);

//        Page<Entry> entries = entryService.findListByDataset(dataset.getType(),dataset.getForm().getId(),"%",email,
//                Arrays.asList(Optional.ofNullable(dataset.getStatus()).orElse("").split(",")),p, PageRequest.of(Optional.ofNullable(page).orElse(0), Optional.ofNullable(size).orElse(Integer.MAX_VALUE)), request);

        //Sheet Name
        model.put("sheetname", lookup.getName());

        List<LookupEntry> result = (List<LookupEntry>) MAPPER.convertValue(entries.get("content"), List.class);

//        Form curForm = dataset.getForm();
//        Form prevForm = null;
//        if (dataset.getForm().getPrev() != null) {
//            prevForm = dataset.getForm().getPrev();
//        }

        String filename = lookup.getName().replace(" ", "-").toLowerCase();

//        model.put("headers", dataset.getItems());
//        model.put("headersStr",)
        model.put("results", result);
        model.put("lookup", lookup);
//        model.put("dataset", dataset);
//        model.put("prevForm", prevForm);

        ModelAndView mv = null;
        if ("xlsx".equals(format)) {
            response.setContentType("application/ms-excel");
            mv = new ModelAndView(new ExcelViewLookup(), model);
        }
//        else if ("csv".equals(format)) {
//            response.setContentType("text/csv");
//            mv = new ModelAndView(new CsvView(), model);
//        } else if ("pdf".equals(format)) {
//            response.setContentType("application/pdf");
//            mv = new ModelAndView(new PdfView(), model);
//        }
        response.setHeader("Content-disposition", "attachment; filename=" + filename + "." + format);
        return mv;
    }


    /**
     * Feature to import excel into database
     *
     * @param formId
     * @param reapExcelDataFile
     * @param email
     * @throws IOException
     */
    @PostMapping("/api/import/entry/{formId}")
    public @ResponseBody
    Map<String, Object> mapReapExcelDatatoEntry(@PathVariable("formId") Long formId,
                                                @RequestParam("file") MultipartFile reapExcelDataFile,
                                                @RequestParam("email") String email,
                                                @RequestParam(value = "create-field", defaultValue = "false") boolean create,
                                                @RequestParam(value = "create-dataset", defaultValue = "false") boolean createDataset,
                                                @RequestParam(value = "create-dashboard", defaultValue = "false") boolean createDashboard,
                                                @RequestParam(value = "import-live", defaultValue = "false") boolean importToLive) throws IOException {

        Map<String, Object> result = new HashMap<>();

        Form form = formRepository.findById(formId)
                .orElseThrow(()->new ResourceNotFoundException("Form","id",formId));

        // get counter
        long counter = form.getCounter();


        List<Entry> tempEntryList = new ArrayList<>();
//        File file = new File("C:/var/leap-files/attachment/lookup-daerah.xlsx");
        XSSFWorkbook workbook = new XSSFWorkbook(reapExcelDataFile.getInputStream());
//        XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(file));
        DataFormatter dataFormatter = new DataFormatter();
        FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
        XSSFSheet worksheet = workbook.getSheetAt(0);

        XSSFRow hrow = worksheet.getRow(0);
        List<String[]> logs = new ArrayList<>();

        List<String> header = new ArrayList<>();

        hrow.cellIterator().forEachRemaining(cell -> header.add(cell.getStringCellValue()));

        Section newSection = new Section();
        if (create) {

        }
        Map<String, Item> itemMap = new HashMap<>();
        List<SectionItem> sectionItemList = new ArrayList<>();

        Map<String, Set<String>> optionMap = new HashMap<>();

        int curRow = 0;
        String curField = "";

        try {
            for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {

                curRow = i;

                XSSFRow row = worksheet.getRow(i);

                Entry entry = new Entry();
                entry.setForm(form);
                entry.setEmail(email);
                entry.setLive(importToLive);
                Map data = new HashMap();

                Map<String, Map<String, Object>> lookup = new HashMap<>();

//                for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
                for (int j = 0; j < header.size(); j++) {
                    Cell cellValue = row.getCell(j);

                    String key = header.get(j);
                    curField = key;
                    String[] splitted = key.split(":");
                    String code = splitted[0].trim().toLowerCase().replaceAll(" ", "_");
                    if ("_".equals(splitted[0])) {
                        if ("email".equals(splitted[1])) {
                            entry.setEmail(cellValue.getStringCellValue());
                        } else if ("id".equals(splitted[1])) {
                            entry.setId((long) cellValue.getNumericCellValue());
                        } else if ("current_status".equals(splitted[1])) {
                            entry.setCurrentStatus(cellValue.getStringCellValue());
                        }
                    } else if (form.getItems().get(code) != null) {
                        String cval = "";
                        boolean notNumber = false;
                        if (cellValue != null) {
                            if (Arrays.asList("scaleTo5", "scaleTo10", "scale", "number").contains(form.getItems().get(code).getType())) {
                                if (cellValue.getCellType() == CellType.NUMERIC) {
                                    data.put(code, cellValue.getNumericCellValue());
                                } else if (cellValue.getCellType() == CellType.STRING && cellValue.getCellType() == CellType.BOOLEAN) {
                                    formulaEvaluator.evaluate(cellValue);
                                    String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                    data.put(code, cellValueStr);
                                    notNumber = true;
                                }
                            } else if (Arrays.asList("date").contains(form.getItems().get(code).getType())) {
                                if (cellValue.getCellType() == CellType.NUMERIC) {//
                                    if (DateUtil.isCellDateFormatted(cellValue)) {
                                        data.put(code, cellValue.getDateCellValue().toInstant().toEpochMilli());
                                    }
                                }

                            } else if (Arrays.asList("text", "textarea", "eval", "qr").contains(form.getItems().get(code).getType())) {
                                formulaEvaluator.evaluate(cellValue);
                                String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                cval = cellValueStr;
                                data.put(code, cellValueStr);
//                                if (row.getCell(j).getCellType() == Cell.CELL_TYPE_STRING) {
//                                    cval = row.getCell(j).getStringCellValue();
//                                    data.put(code, row.getCell(j).getStringCellValue());
//                                } else if (row.getCell(j).getCellType() == Cell.CELL_TYPE_NUMERIC) {
//                                    data.put(code, Double.valueOf(row.getCell(j).getNumericCellValue()).intValue() + "");
//                                }
                            } else if (List.of("simpleOption").contains(form.getItems().get(code).getType())) {
                                String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                if (optionMap.get(code) != null) {
                                    optionMap.get(code).add(cellValueStr);
                                }
                                data.put(code, cellValueStr);
                            } else if (Arrays.asList("select", "radio", "modelPicker").contains(form.getItems().get(code).getType())) {
                                formulaEvaluator.evaluate(cellValue);
                                String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                if (lookup.get(code) != null) {
                                    lookup.get(code).put(splitted[1], cellValueStr);
                                } else {
                                    Map<String, Object> s1 = new HashMap<>();
                                    s1.put(splitted[1], cellValueStr);
                                    lookup.put(code, s1);
                                }
                            } else if (List.of("checkboxOption").contains(form.getItems().get(code).getType())) {

                            } else if (List.of("checkbox").contains(form.getItems().get(code).getType())) {
                                data.put(code, cellValue.getBooleanCellValue());
                            }

                            if (form.getItems().get(code).getId() == null) {
                                if ("text".equals(form.getItems().get(code).getType())) {
                                    if (cval.length() > 80) {
                                        form.getItems().get(code).setSubType("textarea");
                                    }
                                }
                                if ("number".equals(form.getItems().get(code).getType())) {
                                    if (notNumber) {
                                        form.getItems().get("code").setType("text");
                                        form.getItems().get("code").setSubType("input");
                                    }
                                }
                            }

                            // check if annotated with :json decorator
                            if (splitted.length>1 && "json".equals(splitted[1])){
                                formulaEvaluator.evaluate(cellValue);
                                String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                data.put(code,MAPPER.readTree(cellValueStr));
                            }
                        }
                    } else if (form.getItems().get(splitted[0]) == null && create) {

                        if (newSection.getId() == null) {
                            newSection.setTitle(worksheet.getSheetName());
                            newSection.setForm(form);
                            newSection.setType("section");
                            newSection.setSize("col-sm-12");
                            newSection.setSortOrder((long) form.getSections().size());
                            sectionRepository.save(newSection);
                            logs.add(new String[]{"Created Section: " + worksheet.getSheetName(), "success", "OK"});

                        }

//                        IF ITEM NOT EXIST, CREATE NEW ONE
                        Item item = new Item();
                        item.setLabel(splitted[0].replaceAll("_", " "));
                        item.setCode(code);
//                        item.setType("text");
                        item.setSize("col-sm-12");
                        item.setForm(form);

                        form.getItems().put(item.getCode(), item);

                        SectionItem si = new SectionItem();
                        si.setSection(newSection);
                        si.setCode(item.getCode());
                        si.setSortOrder((long) sectionItemList.size());
                        // END SAVING ITEM

                        if (cellValue.getCellType() == CellType.NUMERIC) {//
                            item.setType("number");
                            item.setSubType("number");
                            data.put(code, cellValue.getNumericCellValue());
                            if (DateUtil.isCellDateFormatted(cellValue)) {
                                item.setType("date");
                                item.setSubType("date");
                                data.put(code, cellValue.getDateCellValue().toInstant().toEpochMilli());
                            }
                        } else if (cellValue.getCellType() == CellType.STRING) {

                            if (splitted.length > 1 && "simpleOption".equals(splitted[1])) {
                                item.setType("simpleOption");
                                item.setSubType("radio");
                                Set<String> options = new HashSet<>();
                                options.add(cellValue.getStringCellValue());
                                optionMap.put(code, options);
                            } else {
                                item.setType("text");
                                String value = cellValue.getStringCellValue();
                                if (value.length() > 80) {
                                    item.setSubType("textarea");
                                } else {
                                    item.setSubType("input");
                                }
                            }

                            data.put(code, cellValue.getStringCellValue());
                        } else if (row.getCell(j).getCellType() == CellType.BOOLEAN) {
                            item.setType("checkbox");
                            item.setPlaceholder(item.getLabel());
                            data.put(code, cellValue.getBooleanCellValue());
                        } else {
                            item.setType("text");
                            item.setSubType("input");
                        }

                        sectionItemList.add(si);
//                        sectionItemRepository.save(si);

                    }
                }

                data.putAll(lookup);


                // UTK SET COUNTER
//                Map<String, Object> dataMap = new HashMap<>();
//                dataMap.put("data", data);
                counter++; // increment counter before save
                if (form.getCodeFormat()!=null && !form.getCodeFormat().isEmpty()){
                    String codeFormat = form.getCodeFormat();
                    if (codeFormat.contains("{{")){
                        Map<String, Object> dataMap = new HashMap<>();
                        dataMap.put("data", data);
                        codeFormat = Helper.compileTpl(codeFormat, dataMap);
                    }
                    data.put("$code",String.format(codeFormat, counter));
                    data.put("$counter",counter);
                }else{
                    data.put("$code",String.valueOf(counter));
                    data.put("$counter",counter);
                }

                ///////

                JsonNode node = MAPPER.valueToTree(data);
                entry.setData(node);

                if (entry.getCurrentStatus() == null) {
                    entry.setCurrentStatus(Entry.STATUS_DRAFTED);
                }
                if (entry.getSubmissionDate() == null) {
                    entry.setSubmissionDate(new Date());
                }
                // Even set id, cannot saved as update but always as new.
                tempEntryList.add(entry);

            }

            optionMap.keySet().forEach(code -> {
                form.getItems().get(code).setOptions(String.join(",", optionMap.get(code)));
            });

            sectionItemRepository.saveAll(sectionItemList);
            form.setCounter(counter);
            formRepository.save(form);
            tempEntryList = entryRepository.saveAll(tempEntryList);
            entryRepository.saveAll(tempEntryList); // to save the set $id in @PostPersist




            logs.add(new String[]{tempEntryList.size() + " Entry Imported: " + form.getTitle(), "success", "OK"});

            if (createDataset) {
                Dataset dataset = new Dataset();
                List<DatasetItem> diList = new ArrayList<>();
                Set<DatasetFilter> dfList = new HashSet<>();
                dataset.setTitle(form.getTitle() + " List");
                dataset.setType("all");
                dataset.setForm(form);
                dataset.setApp(form.getApp());
                dataset.setShowAction(true);
//                datasetRepository.save(dataset);

                form.getItems().keySet().forEach(key -> {
                    Item fItem = form.getItems().get(key);
                    if (!Arrays.asList("static", "btn").contains(form.getItems().get(key).getType())) {
                        DatasetItem di = new DatasetItem();
                        di.setCode(fItem.getCode());
                        di.setDataset(dataset);
                        di.setLabel(fItem.getLabel());
                        di.setRoot("data");
                        di.setPrefix("$");
                        di.setSortOrder((long) diList.size());
                        diList.add(di);
                    }

                    if (!Arrays.asList("static", "btn", "modelPicker").contains(form.getItems().get(key).getType())) {
                        DatasetFilter df = new DatasetFilter();
                        df.setLabel(fItem.getLabel());
                        df.setCode(fItem.getCode());
                        df.setFormId(form.getId());
                        df.setSortOrder((long) dfList.size());
                        df.setRoot("data");
                        df.setPrefix("$");
                        df.setDataset(dataset);
                        dfList.add(df);
                    }
                });
                dataset.setItems(diList);
                dataset.setFilters(dfList);
//                dataset.setApp(form.getApp());
//                dataset.setShowAction(true);

                List<DatasetAction> actions = List.of(new DatasetAction("View",
                                DatasetAction.ACTION_VIEW,
                                DatasetAction.TYPE_DROPDOWN,
                                true,
                                "fas:file", 0l, dataset),
                        new DatasetAction("Edit",
                                DatasetAction.ACTION_EDIT,
                                DatasetAction.TYPE_DROPDOWN,
                                true,
                                "fas:pencil-alt", 1l, dataset),
                        new DatasetAction("Delete",
                                DatasetAction.ACTION_DELETE,
                                DatasetAction.TYPE_DROPDOWN,
                                true,
                                "fas:trash", 2l, dataset)
                        );
                dataset.setActions(actions);
                dataset.setStatusFilter(MAPPER.readTree("{\"-1\":\"submitted,drafted\"}"));
                dataset.setPresetFilters(MAPPER.readTree("{}"));
                dataset.setExportPdf(true);
                dataset.setExportXls(true);
                dataset.setWide(true);
                dataset.setShowIndex(true);
                dataset.setX(MAPPER.readTree("{\"tblcard\": true }"));
                datasetRepository.save(dataset);

                logs.add(new String[]{"Created Dataset: " + dataset.getTitle(), "success", "OK"});
            }

            if (createDashboard) {
                Dashboard dashboard = new Dashboard();
                Set<Chart> chartList = new HashSet<>();
                dashboard.setTitle(form.getTitle() + " Dashboard");
                form.getItems().keySet().forEach(key -> {
                    Item fItem = form.getItems().get(key);
//                   if (Arrays.asList("select", "radio", "modelPicker").contains(fItem.getType())){
                    Chart chart = new Chart();
                    chart.setAgg("count");
                    chart.setTitle("By " + fItem.getLabel());
                    chart.setFieldCode("data#" + fItem.getCode());
                    chart.setFieldValue("data#$id");
                    chart.setType("pie");
                    chart.setSize("col-sm-12");
                    chart.setForm(form);
                    chart.setHeight("450");
                    try {
                        chart.setStatusFilter(MAPPER.readTree("{\"-1\":\"submitted,drafted\"}"));
                        chart.setPresetFilters(MAPPER.readTree("{}"));
                    } catch (IOException e) {
                    }
                    chart.setSortOrder((long) chartList.size());
                    chart.setDashboard(dashboard);
                    chartList.add(chart);
//                   }
                });
                dashboard.setApp(form.getApp());
                dashboard.setCharts(chartList);
                dashboardRepository.save(dashboard);
                logs.add(new String[]{"Created Dashboard: " + dashboard.getTitle(), "success", "OK"});

            }


            result.put("logs", logs);
            result.put("success", true);

        } catch (Exception e) {
            e.printStackTrace();
            logs.add(new String[]{e.getMessage() + "[" + curRow + ":" + curField + "]", "danger", "Failed"});
            result.put("logs", logs);
            result.put("success", false);
            result.put("message", e.getMessage() + "[" + curRow + ":" + curField + "]");
        }

        return result;
    }

    /**
     * Feature to import excel into database
     *
     * @param lookupId
     * @param reapExcelDataFile
     * @throws IOException
     */
    @PostMapping("/api/import/lookup/{lookupId}")
    public @ResponseBody
    Map<String, Object> mapReapExcelDataToLookup(@PathVariable("lookupId") Long lookupId,
                                                 @RequestParam("file") MultipartFile reapExcelDataFile) throws IOException {

        Map<String, Object> result = new HashMap<>();
        Lookup lookup = lookupRepository.getReferenceById(lookupId);
        XSSFWorkbook workbook = new XSSFWorkbook(reapExcelDataFile.getInputStream());
        XSSFSheet worksheet = workbook.getSheetAt(0);

        XSSFRow hrow = worksheet.getRow(0);

        List<String> header = new ArrayList<>();

        System.out.print("##Header");
        hrow.cellIterator().forEachRemaining(cell -> {
            System.out.print("," + cell.getStringCellValue());
            header.add(cell.getStringCellValue());
        });

        List<LookupEntry> lookupEntryList = new ArrayList<>();

        final DataFormatter df = new DataFormatter();

        try {
            for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {

                XSSFRow row = worksheet.getRow(i);
                Map data = new HashMap();
                LookupEntry le = new LookupEntry();
                le.setEnabled(1);

                for (int j = 0; j < header.size(); j++) {
                    String key = header.get(j);

                    if ("code".equalsIgnoreCase(key.trim())) {
                        le.setCode(df.formatCellValue(row.getCell(j)));
                    } else if ("name".equalsIgnoreCase(key.trim())) {
                        le.setName(df.formatCellValue(row.getCell(j)));
                    } else if ("extra".equalsIgnoreCase(key.trim())) {
                        le.setExtra(df.formatCellValue(row.getCell(j)));
                    } else if ("enabled".equalsIgnoreCase(key.trim())) {
                        Integer d = Double.valueOf(row.getCell(j).getNumericCellValue()).intValue();
                        le.setEnabled(d == null ? 1 : d);
                    } else {
                        data.put(key.trim(), df.formatCellValue(row.getCell(j)));
                    }
                }

                JsonNode node = MAPPER.valueToTree(data);
                le.setData(node);

                le.setLookup(lookup);
                le.setOrdering((long) i);

                if (le.getCode() != null && !le.getCode().isEmpty()) {
                    if (le.getName() != null && !le.getName().isEmpty()) {
                        lookupEntryList.add(le);
                        logger.info("code:" + le.getCode() + ",name:" + le.getName() + ",lookup:" + le.getLookup());
                    }
                }

            }
            lookupEntryRepository.saveAll(lookupEntryList);
            result.put("success", true);
            result.put("data", lookupEntryList);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }


    /**
     * Feature to import excel into database
     *
     * @param appId
     * @param reapExcelDataFile
     * @param email
     * @throws IOException
     */
    @PostMapping("/api/import/app/{appId}")
    public @ResponseBody
    Map<String, Object> createAppFromExcel(@PathVariable("appId") Long appId,
                                           @RequestParam("file") MultipartFile reapExcelDataFile,
                                           @RequestParam("email") String email,
//                                                                      @RequestParam(value = "create-field", defaultValue = "false") boolean create,
                                           @RequestParam(value = "create-dataset", defaultValue = "false") boolean createDataset,
                                           @RequestParam(value = "create-dashboard", defaultValue = "false") boolean createDashboard,
                                           @RequestParam(value = "import-live", defaultValue = "false") boolean importToLive) throws IOException {

        App app = appRepository.getReferenceById(appId);

        Map<String, Object> result = new HashMap<>();


        List<String[]> logs = new ArrayList<>();

        XSSFWorkbook workbook = new XSSFWorkbook(reapExcelDataFile.getInputStream());
        DataFormatter dataFormatter = new DataFormatter();
        FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();

        List<NaviGroup> naviGroupList = new ArrayList<>();

        workbook.iterator().forEachRemaining(sheet -> {

            List<Entry> tempEntryList = new ArrayList<>();

            XSSFSheet worksheet = (XSSFSheet) sheet;

            XSSFRow hrow = worksheet.getRow(0);

            if (hrow != null) {

                NaviGroup ngroup = new NaviGroup();
                ngroup.setTitle(worksheet.getSheetName());
                ngroup.setApp(app);
                ngroup.setSortOrder((long) naviGroupList.size());
                List<NaviItem> itemList = new ArrayList<>();

                logs.add(new String[]{"Added New Group: " + worksheet.getSheetName(), "secondary", "OK"});

                //CREATE FORM
                Form form = new Form();
                form.setApp(app);
//                form.setType("db");
                form.setNav("simple");
                form.setCanEdit(true);
                form.setCanRetract(true);
                form.setCanSave(true);
                form.setX(MAPPER.createObjectNode());
                form.setValidateSave(true);
                form.setTitle(worksheet.getSheetName());
                formRepository.save(form);
                logs.add(new String[]{"Created Form: " + form.getTitle(), "success", "OK"});

                NaviItem niForm = new NaviItem();
                niForm.setScreenId(form.getId());
                niForm.setTitle("Add " + form.getTitle());
                niForm.setGroup(ngroup);
                niForm.setSortOrder((long) itemList.size());
                niForm.setType("form");
                itemList.add(niForm);
                logs.add(new String[]{"Created Form Link: " + worksheet.getSheetName(), "success", "OK"});

                List<String> header = new ArrayList<>();

                hrow.cellIterator().forEachRemaining(cell -> header.add(cell.getStringCellValue()));

                Section newSection = new Section();
                newSection.setTitle(worksheet.getSheetName());
                newSection.setForm(form);
                newSection.setType("section");
                newSection.setSize("col-sm-12");
                newSection.setSortOrder((long) form.getSections().size());
                sectionRepository.save(newSection);


                Map<String, Item> itemMap = new HashMap<>();
                List<SectionItem> sectionItemList = new ArrayList<>();

                Map<String, Set<String>> optionMap = new HashMap<>();

                int curRow = 0;
                String curField = "";

                try {
                    for (int i = 1; i < worksheet.getPhysicalNumberOfRows(); i++) {

                        curRow = i;

                        XSSFRow row = worksheet.getRow(i);

                        Entry entry = new Entry();
                        entry.setForm(form);
                        entry.setEmail(email);
                        entry.setLive(importToLive);
                        Map data = new HashMap();

                        Map<String, Map<String, Object>> lookup = new HashMap<>();

//                for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
                        for (int j = 0; j < header.size(); j++) {
                            Cell cellValue = row.getCell(j);

                            String key = header.get(j);
                            curField = key;
                            String[] splitted = key.split(":");
                            // "[\\s\\W_]+(.|$)"g
                            String code = splitted[0].trim().toLowerCase().replaceAll("s/^[^a-zA-Z]+|[^a-zA-Z0-9]+", "_");
                            if ("_".equals(splitted[0])) {
                                if ("email".equals(splitted[1])) {
                                    entry.setEmail(cellValue.getStringCellValue());
                                } else if ("id".equals(splitted[1])) {
                                    entry.setId((long) cellValue.getNumericCellValue());
                                } else if ("current_status".equals(splitted[1])) {
                                    entry.setCurrentStatus(cellValue.getStringCellValue());
                                }
                            } else if (form.getItems().get(code) != null) {
                                String cval = "";
                                boolean notNumber = false;
                                if (cellValue != null) {
                                    if (Arrays.asList("scaleTo5", "scaleTo10","scale", "number").contains(form.getItems().get(code).getType())) {
                                        if (cellValue.getCellType() == CellType.NUMERIC) {
                                            data.put(code, cellValue.getNumericCellValue());
                                        } else if (cellValue.getCellType() == CellType.STRING && cellValue.getCellType() == CellType.BOOLEAN) {
                                            formulaEvaluator.evaluate(cellValue);
                                            String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                            data.put(code, cellValueStr);
                                            notNumber = true;
                                        }
                                    } else if (Arrays.asList("date").contains(form.getItems().get(code).getType())) {
                                        if (cellValue.getCellType() == CellType.NUMERIC) {//
                                            if (DateUtil.isCellDateFormatted(cellValue)) {
                                                data.put(code, cellValue.getDateCellValue().toInstant().toEpochMilli());
                                            }
                                        }

                                    } else if (Arrays.asList("text", "textarea", "eval", "qr").contains(form.getItems().get(code).getType())) {
                                        formulaEvaluator.evaluate(cellValue);
                                        String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                        cval = cellValueStr;
                                        data.put(code, cellValueStr);
//                                if (row.getCell(j).getCellType() == Cell.CELL_TYPE_STRING) {
//                                    cval = row.getCell(j).getStringCellValue();
//                                    data.put(code, row.getCell(j).getStringCellValue());
//                                } else if (row.getCell(j).getCellType() == Cell.CELL_TYPE_NUMERIC) {
//                                    data.put(code, Double.valueOf(row.getCell(j).getNumericCellValue()).intValue() + "");
//                                }
                                    } else if (Arrays.asList("simpleOption").contains(form.getItems().get(code).getType())) {
                                        String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                        if (optionMap.get(code) != null) {
                                            optionMap.get(code).add(cellValueStr);
                                        }
                                        data.put(code, cellValueStr);
                                    } else if (Arrays.asList("select", "radio", "modelPicker").contains(form.getItems().get(code).getType())) {
                                        formulaEvaluator.evaluate(cellValue);
                                        String cellValueStr = dataFormatter.formatCellValue(cellValue, formulaEvaluator);
                                        if (lookup.get(code) != null) {
                                            lookup.get(code).put(splitted[1], cellValueStr);
                                        } else {
                                            Map<String, Object> s1 = new HashMap<>();
                                            s1.put(splitted[1], cellValueStr);
                                            lookup.put(code, s1);
                                        }
                                    } else if (List.of("checkboxOption").contains(form.getItems().get(code).getType())) {

                                    } else if (List.of("checkbox").contains(form.getItems().get(code).getType())) {
                                        data.put(code, cellValue.getBooleanCellValue());
                                    }

                                    if (form.getItems().get(code).getId() == null) {
                                        if ("text".equals(form.getItems().get(code).getType())) {
                                            if (cval.length() > 80) {
                                                form.getItems().get(code).setSubType("textarea");
                                            }
                                        }
                                        if ("number".equals(form.getItems().get(code).getType())) {
                                            if (notNumber) {
                                                form.getItems().get("code").setType("text");
                                                form.getItems().get("code").setSubType("input");
                                            }
                                        }
                                    }
                                }
                            } else if (form.getItems().get(splitted[0]) == null) {

//                        IF ITEM NOT EXIST, CREATE NEW ONE
                                Item item = new Item();
                                item.setLabel(splitted[0].replaceAll("_", " "));
                                item.setCode(code);
                                item.setSize("col-sm-12");
                                item.setForm(form);

                                form.getItems().put(item.getCode(), item);

                                SectionItem si = new SectionItem();
                                si.setSection(newSection);
                                si.setCode(item.getCode());
                                si.setSortOrder((long) sectionItemList.size());
                                // END SAVING ITEM

                                if (cellValue.getCellType() == CellType.NUMERIC) {//
                                    item.setType("number");
                                    item.setSubType("number");
                                    data.put(code, cellValue.getNumericCellValue());
                                    if (DateUtil.isCellDateFormatted(cellValue)) {
                                        item.setType("date");
                                        item.setSubType("date");
                                        data.put(code, cellValue.getDateCellValue().toInstant().toEpochMilli());
                                    }
                                } else if (cellValue.getCellType() == CellType.STRING) {
                                    if (splitted.length > 1 && "simpleOption".equals(splitted[1])) {
                                        item.setType("simpleOption");
                                        item.setSubType("radio");
                                        Set<String> options = new HashSet<>();
                                        options.add(cellValue.getStringCellValue());
                                        optionMap.put(code, options);
                                    } else {
                                        item.setType("text");
                                        String value = cellValue.getStringCellValue();
                                        if (value.length() > 80) {
                                            item.setSubType("textarea");
                                        } else {
                                            item.setSubType("input");
                                        }
                                    }
                                    data.put(code, cellValue.getStringCellValue());
                                } else if (row.getCell(j).getCellType() == CellType.BOOLEAN) {
                                    item.setType("checkbox");
                                    item.setPlaceholder(item.getLabel());
                                    data.put(code, cellValue.getBooleanCellValue());
                                } else {
                                    item.setType("text");
                                    item.setSubType("input");
                                }

                                sectionItemList.add(si);
//                        sectionItemRepository.save(si);

                            }
                        }

                        data.putAll(lookup);

                        JsonNode node = MAPPER.valueToTree(data);
                        entry.setData(node);

                        if (entry.getCurrentStatus() == null) {
                            entry.setCurrentStatus(Entry.STATUS_DRAFTED);
                        }
                        if (entry.getSubmissionDate() == null) {
                            entry.setSubmissionDate(new Date());
                        }
                        // Even set id, cannot saved as update but always as new.

                        tempEntryList.add(entry);

                    }

                    optionMap.keySet().forEach(code -> form.getItems().get(code).setOptions(String.join(",", optionMap.get(code))));

                    sectionItemRepository.saveAll(sectionItemList);
                    formRepository.save(form);
                    tempEntryList = entryRepository.saveAll(tempEntryList);
                    entryRepository.saveAll(tempEntryList); // to resave $id in @PostPersist
                    logs.add(new String[]{tempEntryList.size() + " Entry Imported: " + form.getTitle(), "success", "OK"});


                    // CREATE DATASET
                    if (createDataset) {
                        Dataset dataset = new Dataset();
                        List<DatasetItem> diList = new ArrayList<>();
                        Set<DatasetFilter> dfList = new HashSet<>();
                        dataset.setTitle(form.getTitle() + " List");
                        dataset.setType("all");
                        dataset.setForm(form);
                        dataset.setApp(form.getApp());
                        dataset.setShowAction(true);

//                        datasetRepository.save(dataset);
                        form.getItems().keySet().forEach(key -> {
                            Item fItem = form.getItems().get(key);
                            if (!Arrays.asList("static", "btn").contains(form.getItems().get(key).getType())) {
                                DatasetItem di = new DatasetItem();
                                di.setCode(fItem.getCode());
                                di.setDataset(dataset);
                                di.setLabel(fItem.getLabel());
                                di.setRoot("data");
                                di.setPrefix("$");
                                di.setSortOrder((long) diList.size());
                                diList.add(di);
                            }

                            if (!Arrays.asList("static", "btn", "modelPicker").contains(form.getItems().get(key).getType())) {
                                DatasetFilter df = new DatasetFilter();
                                df.setLabel(fItem.getLabel());
                                df.setCode(fItem.getCode());
                                df.setFormId(form.getId());
                                df.setSortOrder((long) dfList.size());
                                df.setRoot("data");
                                df.setPrefix("$");
                                df.setDataset(dataset);
                                dfList.add(df);
                            }
                        });
                        dataset.setItems(diList);
                        dataset.setFilters(dfList);

                        List<DatasetAction> actions = List.of(new DatasetAction("View",
                                        DatasetAction.ACTION_VIEW,
                                        DatasetAction.TYPE_DROPDOWN,
                                        true,
                                        "fas:file", 0l, dataset),
                                new DatasetAction("Edit",
                                        DatasetAction.ACTION_EDIT,
                                        DatasetAction.TYPE_DROPDOWN,
                                        true,
                                        "fas:pencil-alt", 1l, dataset),
                                new DatasetAction("Delete",
                                        DatasetAction.ACTION_DELETE,
                                        DatasetAction.TYPE_DROPDOWN,
                                        true,
                                        "fas:trash", 2l, dataset)
                        );
                        dataset.setActions(actions);
//                        dataset.setCanEdit(true);
//                        dataset.setCanDelete(true);
//                        dataset.setCanView(true);
                        dataset.setStatusFilter(MAPPER.readTree("{\"-1\":\"submitted,drafted\"}"));
                        dataset.setPresetFilters(MAPPER.readTree("{}"));
                        dataset.setExportPdf(true);
                        dataset.setExportXls(true);
                        datasetRepository.save(dataset);
                        logs.add(new String[]{"Created Dataset: " + dataset.getTitle(), "success", "OK"});

                        NaviItem niDataset = new NaviItem();
                        niDataset.setScreenId(dataset.getId());
                        niDataset.setTitle(dataset.getTitle());
                        niDataset.setGroup(ngroup);
                        niDataset.setSortOrder(1L);
                        niDataset.setType("dataset");
                        itemList.add(niDataset);
                        logs.add(new String[]{"Created Dataset Link: " + dataset.getTitle(), "success", "OK"});

                    }

                    // CREATE DASHBOARD
                    if (createDashboard) {
                        Dashboard dashboard = new Dashboard();
                        Set<Chart> chartList = new HashSet<>();
                        dashboard.setTitle(form.getTitle() + " Dashboard");
                        form.getItems().keySet().forEach(key -> {
                            Item fItem = form.getItems().get(key);
//                   if (Arrays.asList("select", "radio", "modelPicker").contains(fItem.getType())){
                            Chart chart = new Chart();
                            chart.setAgg("count");
                            chart.setTitle("By " + fItem.getLabel());
                            chart.setFieldCode("data#" + fItem.getCode());
                            chart.setFieldValue("data#$id");
                            chart.setType("pie");
                            chart.setSourceType("db");
                            chart.setSize("col-sm-12");
                            chart.setForm(form);
                            chart.setHeight("450");
                            try {
                                chart.setStatusFilter(MAPPER.readTree("{\"-1\":\"submitted,drafted\"}"));
                                chart.setPresetFilters(MAPPER.readTree("{}"));
                            } catch (IOException e) {
                                logs.add(new String[]{"Error setting filter (" + key + ")" + e.getMessage(), "danger", "Failed"});
                            }
                            chart.setSortOrder((long) chartList.size());
                            chart.setDashboard(dashboard);
                            chartList.add(chart);
//                   }
                        });
                        dashboard.setApp(form.getApp());
                        dashboard.setCharts(chartList);
                        dashboardRepository.save(dashboard);
                        logs.add(new String[]{"Created Dashboard: " + dashboard.getTitle(), "success", "OK"});

                        NaviItem niDashboard = new NaviItem();
                        niDashboard.setScreenId(dashboard.getId());
                        niDashboard.setTitle(dashboard.getTitle());
                        niDashboard.setGroup(ngroup);
                        niDashboard.setSortOrder(1L);
                        niDashboard.setType("dashboard");
                        itemList.add(niDashboard);
                        logs.add(new String[]{"Created Dashboard Link: " + dashboard.getTitle(), "success", "OK"});
                    }


                    ngroup.setItems(itemList);

                    naviGroupRepository.save(ngroup);


//                    result.put("logs", logs);
                    result.put("success", true);
//                    result.put("message", "E");

                } catch (Exception e) {
                    e.printStackTrace();
                    logs.add(new String[]{e.getMessage() + "[" + sheet.getSheetName() + ":" + curRow + ":" + curField + "]", "danger", "Failed"});
//                    result.put("logs", logs);
                    result.put("success", false);
                    result.put("message", e.getMessage() + "[" + curRow + ":" + curField + "]");
                }

            } else {
                logs.add(new String[]{"Encountered an empty first row sheet, sheet skipped", "warning", "Warning"});
            }

        });

        result.put("logs", logs);


        return result;
    }

}
