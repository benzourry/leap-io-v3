package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.DatasetItem;
import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.Form;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

//import org.springframework.web.servlet.view.document.AbstractExcelView;
//import org.springframework.web.servlet.view.document.AbstractExcelView;

/**
 * Created by MohdRazif on 5/5/2017.
 */
@Transactional
public class ExcelViewStream extends AbstractXlsxStreamingView {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(ExcelViewStream.class);

    @Override
    protected void buildExcelDocument(Map<String, Object> model,
                                      Workbook workbook,
                                      HttpServletRequest httpServletRequest,
                                      HttpServletResponse httpServletResponse) throws Exception {


//        workbook = (HSSFWorkbook) workbook;
        //VARIABLES REQUIRED IN MODEL
        String sheetName = (String) model.get("sheetname");
        sheetName = sheetName.replaceAll("[\\[\\]\\\\\\/\\*\\:\\,\\?]+", " ");
        List<DatasetItem> headers = (List<DatasetItem>) model.get("headers");
//        List<Entry> results = (List<Entry>) model.get("results");
        Stream<Entry> streamResults = (Stream<Entry>) model.get("streams");
//        EntryService entryService = (EntryService) model.get("entryService");

        String searchText = (String) model.get("searchText");
        String email = (String) model.get("email");
        Map filters = (Map) model.get("filters");
        String cond = (String) model.get("cond");
        List<String> sorts = (List<String>) model.get("sorts");
        List<Long> ids = (List<Long>) model.get("ids");
        Dataset dataset = (Dataset) model.get("dataset");

        Form form = dataset.getForm();
        Form prevForm = (Form) model.get("prevForm");

        List<String> numericColumns = new ArrayList<String>();
        if (model.containsKey("numericcolumns"))
            numericColumns = (List<String>) model.get("numericcolumns");
        //BUILD DOC
        Sheet sheet = workbook.createSheet(sheetName);
        sheet.setDefaultColumnWidth((short) 12);
//        int currentRow = 0;
//        short currentColumn = 0;
        AtomicInteger currentRow = new AtomicInteger(0);
        AtomicInteger currentColumn = new AtomicInteger(0);


        CreationHelper createHelper = workbook.getCreationHelper();
        //CREATE STYLE FOR HEADER
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
//        headerFont.setFontHeightInPoints(12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        //POPULATE HEADER COLUMNS
        Row headerRow = sheet.createRow(currentRow.get());
//        CellStyle rowStyle = headerRow.getRowStyle();

        for (DatasetItem entry : headers) {
            Cell cell = headerRow.createCell(currentColumn.get());
            cell.setCellStyle(headerStyle);
            cell.setCellValue(entry.getLabel());
            currentColumn.getAndIncrement();
        }
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
//        headerStyle.setFillBackgroundColor(IndexedColors.DARK_BLUE.getIndex());

        CellStyle emptyStyle = workbook.createCellStyle();
        emptyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        emptyStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());

        CellStyle cs = workbook.createCellStyle();
        cs.setWrapText(true);
        cs.setVerticalAlignment(VerticalAlignment.TOP);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        //POPULATE VALUE ROWS/COLUMNS
        currentRow.getAndIncrement();//exclude header

//        Stream<Entry> streamResults = entryService.findListByDatasetStream(dataset.getId(), searchText, email, filters, cond, sorts, ids, httpServletRequest);

        try (Stream<Entry> entryStream = streamResults) {
            entryStream.forEach(result -> {
                currentColumn.set(0);
                Row row = sheet.createRow(currentRow.get());

                for (DatasetItem head : headers) {
                    Cell cell = row.createCell(currentColumn.get());

                    Object value = "";

                    JsonNode data = result.getData();
                    Form iForm = form;

                    if (Arrays.asList("prev", "data").contains(head.getRoot())) {
                        if ("prev".equals(head.getRoot())) {
                            data = result.getPrev();
                            iForm = prevForm;
                        }
                    } else {
                        if (head.getRoot() != null) {
                            if (result.getApproval() != null && result.getApproval().get(Long.parseLong(head.getRoot())) != null) {
                                data = result.getApproval().get(Long.parseLong(head.getRoot())).getData();
                            }
                        }
                    }

                    JsonNode element = data.get(head.getCode());

                    if (data != null && head != null && element != null) {
                        value = element.textValue();
                        if (value == null) {
                            value = element.numberValue();
                        }

                        if (Arrays.asList("select", "radio").contains(iForm.getItems().get(head.getCode()).getType())) {
                            logger.info("select:"+iForm.getItems().get(head.getCode()));
                            if (element.get("name") != null) {
                                value = element.get("name").textValue();
                            }
                        }

                        if (Arrays.asList("checkboxOption").contains(iForm.getItems().get(head.getCode()).getType()) ||
                                Arrays.asList("multiple").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                            if (element.isArray()) {
                                Iterator<JsonNode> inner = element.iterator();
                                List<String> vlist = new ArrayList<>();
                                while (inner.hasNext()) {
                                    JsonNode innerElement = inner.next();
                                    if (innerElement != null) {
                                        vlist.add(innerElement.get("name").textValue());
                                    }
                                }
                                value = String.join(", ", vlist);
                            }
                        }

                        if (Arrays.asList("file").contains(iForm.getItems().get(head.getCode()).getType()) ||
                                Arrays.asList("othermulti", "imagemulti").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                            if (element.isArray()) {
                                Iterator<JsonNode> inner = element.iterator();
                                List<String> vlist = new ArrayList<>();
                                while (inner.hasNext()) {
                                    JsonNode innerElement = inner.next();
                                    if (innerElement != null) {
                                        String filePath = innerElement.textValue();
                                        vlist.add(filePath);
                                    }
                                }
                                value = String.join(", ", vlist);
                            }
                        }

                        if (Arrays.asList("checkbox").contains(iForm.getItems().get(head.getCode()).getType())) {
                            if (element != null) {
                                value = element.booleanValue() ? "Yes" : "No";
                            } else {
                                value = "No";
                            }
                        }
                        if (Arrays.asList("number", "scale", "scaleTo10", "scaleTo5").contains(iForm.getItems().get(head.getCode()).getType())) {
                            value = element.numberValue();
                        }

                        if (Arrays.asList("date").contains(iForm.getItems().get(head.getCode()).getType())) {
                            LocalDate date = Instant.ofEpochMilli(element.longValue())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate();

                            value = date.format(formatter);
                        }
                    }

                    if (value == null || value.toString().isEmpty()) {
                        cell.setCellStyle(emptyStyle);
                    } else {
                        String textValue = Optional.ofNullable(value).orElse("").toString()
                                .replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
                        textValue = textValue.replace("<br/>", "\n");
                        textValue = textValue.replace("<br>", "\n");
                        textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                        cell.setCellStyle(cs);
                        cell.setCellValue(textValue);
                    }


                    currentColumn.getAndIncrement();
                }

                currentRow.getAndIncrement();
                this.entityManager.detach(result);
            });
        }


    }
}