package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.DatasetItem;
import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.Form;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

//import org.springframework.web.servlet.view.document.AbstractExcelView;
//import org.springframework.web.servlet.view.document.AbstractExcelView;

/**
 * Created by MohdRazif on 5/5/2017.
 */
public class ExcelView extends AbstractXlsxStreamingView {


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
        List<Entry> results = (List<Entry>) model.get("results");
        Dataset dataset = (Dataset) model.get("dataset");
        Form form = dataset.getForm();
        Form prevForm = (Form) model.get("prevForm");

//        System.out.println(headers);

        List<String> numericColumns = new ArrayList<String>();
        if (model.containsKey("numericcolumns"))
            numericColumns = (List<String>) model.get("numericcolumns");
        //BUILD DOC
        Sheet sheet = workbook.createSheet(sheetName);
        sheet.setDefaultColumnWidth((short) 12);
        int currentRow = 0;
        short currentColumn = 0;

        CreationHelper createHelper = workbook.getCreationHelper();
        //CREATE STYLE FOR HEADER
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
//        headerFont.setFontHeightInPoints(12);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        //POPULATE HEADER COLUMNS
        Row headerRow = sheet.createRow(currentRow);
//        CellStyle rowStyle = headerRow.getRowStyle();

        for (DatasetItem entry : headers) {
            Cell cell = headerRow.createCell(currentColumn);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(entry.getLabel());
            currentColumn++;
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
        currentRow++;//exclude header
        for (Entry result : results) { /// row
            currentColumn = 0;
            Row row = sheet.createRow(currentRow);

            for (DatasetItem head : headers) {
                Cell cell = row.createCell(currentColumn);

                Object value = "";

                JsonNode data = result.getData();
                Form iForm = form;

                if (Arrays.asList("prev", "data").contains(head.getRoot())) {
                    if ("prev".equals(head.getRoot())) {
                        data = result.getPrev();
                        iForm = prevForm;
                    }
                }else {
                    if (head.getRoot()!=null) {
                        if (result.getApproval()!=null && result.getApproval().get(Long.parseLong(head.getRoot())) != null) {
                            data = result.getApproval().get(Long.parseLong(head.getRoot())).getData();
                        }
                    }
                }


//                    JsonNode data = result.getData();
//                    Form iForm = form;


//                if ("prev".equals(head.getRoot())){
//                    data = result.getPrev();
//                    iForm = prevForm;
//                }

                    if (data != null && head != null && data.get(head.getCode()) != null) {
                        value = data.get(head.getCode()).textValue();
                        if (value == null) {
                            value = data.get(head.getCode()).numberValue();
                        }

                        if (Arrays.asList("select", "radio").contains(iForm.getItems().get(head.getCode()).getType())) {
                            if (data.get(head.getCode()).get("name") != null) {
                                value = data.get(head.getCode()).get("name").textValue();
                            }
                        }

                        if (Arrays.asList("checkboxOption").contains(iForm.getItems().get(head.getCode()).getType()) ||
                                Arrays.asList("multiple").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                            JsonNode element = data.get(head.getCode());
                            if (element != null) {
                                if (element.isArray()) {
                                    Iterator<JsonNode> inner = element.iterator();
                                    List<String> vlist = new ArrayList<>();
                                    while (inner.hasNext()) {
                                        JsonNode innerElement = inner.next();
                                        if (innerElement != null) {
                                            vlist.add(innerElement.get("name").textValue());
                                        }
                                    }
                                    value= String.join(", ", vlist);
                                }
                            }
                        }

                        if (Arrays.asList("file").contains(iForm.getItems().get(head.getCode()).getType()) ||
                                Arrays.asList("othermulti","imagemulti").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                            JsonNode element = data.get(head.getCode());
                            if (element != null) {
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
                        }

                        if (Arrays.asList("checkbox").contains(iForm.getItems().get(head.getCode()).getType())) {
                            if (data.get(head.getCode()) != null) {
                                value = data.get(head.getCode()).booleanValue()?"Yes":"No";
                            }else{
                                value = "No";
                            }
                        }
                        if (Arrays.asList("number", "scaleTo10", "scaleTo5").contains(iForm.getItems().get(head.getCode()).getType())) {
                            value = data.get(head.getCode()).numberValue();
                        }

                        if (Arrays.asList("date").contains(iForm.getItems().get(head.getCode()).getType())) {
                            LocalDate date = null;
                            if (data.get(head.getCode()) != null) {
                                date = Instant.ofEpochMilli(data.get(head.getCode()).longValue())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate();
                            }

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


                currentColumn++;
            }

            currentRow++;
        }
    }
}