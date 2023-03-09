package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.Lookup;
import com.benzourry.leap.model.LookupEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.util.*;

//import org.springframework.web.servlet.view.document.AbstractExcelView;
//import org.springframework.web.servlet.view.document.AbstractExcelView;

/**
 * Created by MohdRazif on 5/5/2017.
 */
public class ExcelViewLookup extends AbstractXlsxView {


    @Override
    protected void buildExcelDocument(Map<String, Object> model,
                                      Workbook workbook,
                                      HttpServletRequest httpServletRequest,
                                      HttpServletResponse httpServletResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
//        workbook = (HSSFWorkbook) workbook;
        //VARIABLES REQUIRED IN MODEL
        String sheetName = (String) model.get("sheetname");
        sheetName = sheetName.replaceAll("[\\[\\]\\\\\\/\\*\\:\\,\\?]+", " ");
//        List<DatasetItem> headers = (List<DatasetItem>) model.get("headers");
        List<LookupEntry> results = (List<LookupEntry>) model.get("results");

        Lookup lookup = (Lookup) model.get("lookup");

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

        // if data enabled, also create header for data columns
        List<String> allFields = new ArrayList(Arrays.asList("code","name","extra"));
//        List<String> allheaders =
//        allheaders.addAll(dataField);
        if (lookup.isDataEnabled()){
            Map<String,Object> result = mapper.convertValue(results.get(0), Map.class);
            Map<String,Object> d = mapper.convertValue(result.get("data"),Map.class);
//            System.out.println(d.keySet());
            allFields.addAll(d.keySet());
        }
        for (String h: allFields) {
            Cell cell = headerRow.createCell(currentColumn);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(h);
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
        for (int i=0; i<results.size();i++){

            Map<String,Object> result = mapper.convertValue(results.get(i), Map.class);
//        for (Map<String,Object> result : results) { /// row
            currentColumn = 0;
            Row row = sheet.createRow(currentRow);

            for (String h: allFields){
                Cell cell = row.createCell(currentColumn);
                Object value;
                if (Arrays.asList("code","name","extra","enabled").contains(h)){
                    value = result.get(h);
                }else{
                    value = ((Map)result.get("data")).get(h);
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