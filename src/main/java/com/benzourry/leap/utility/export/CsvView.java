package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.Dataset;
import com.benzourry.leap.model.DatasetItem;
import com.benzourry.leap.model.Entry;
import com.benzourry.leap.model.Form;
import com.fasterxml.jackson.databind.JsonNode;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by MohdRazif on 5/8/2017.
 */
public class CsvView extends AbstractCsvView {
    @Override
    protected void buildCsvDocument(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

//        response.setHeader("Content-Disposition", "attachment; filename=\"my-csv-file.csv\"");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<DatasetItem> headers = (List<DatasetItem>) model.get("headers");
        List<Entry> results = (List<Entry>) model.get("results");

        Dataset dataset = (Dataset) model.get("dataset");
        Form form = dataset.getForm();

        Form prevForm = (Form) model.get("prevForm");

        String[] header = headers.stream()
                .map(m -> m.getLabel())
                .collect(Collectors.toList())
                .toArray(new String[0]);

        ICsvMapWriter csvWriter = new CsvMapWriter(response.getWriter(),
                CsvPreference.STANDARD_PREFERENCE);


        csvWriter.writeHeader(header);


        for (Entry result : results) {
            Map<String, String> data = new HashMap<>();

            for (DatasetItem head : headers) {

                Object value = "";
                String textValue = "";

//                JsonNode cdata = result.getData();
//                Form iForm = form;
//
//                if ("prev".equals(head.getRoot())){
//                    cdata = result.getPrev();
//                    iForm = prevForm;
//                }
                JsonNode cdata = result.getData();
                Form iForm = form;

                if (Arrays.asList("prev", "data").contains(head.getRoot())) {
                    if ("prev".equals(head.getRoot())) {
                        cdata = result.getPrev();
                        iForm = prevForm;
                    }
                }else {
                    if (head.getRoot()!=null) {
                        if (result.getApproval() != null && result.getApproval().get(Long.parseLong(head.getRoot())) != null) {
                            cdata = result.getApproval().get(Long.parseLong(head.getRoot())).getData();
                        }
                    }
                }


                if (cdata != null && head != null && cdata.get(head.getCode())!=null) {
                    value = cdata.get(head.getCode()).textValue();
                    if (value == null) {
                        value = cdata.get(head.getCode()).numberValue();
                    }

                    if (Arrays.asList("select", "radio").contains(iForm.getItems().get(head.getCode()).getType())) {
                        if (cdata.get(head.getCode()).get("name")!=null) {
                            value = cdata.get(head.getCode()).get("name").textValue();
                        }
                    }

                    if (Arrays.asList("checkboxOption").contains(iForm.getItems().get(head.getCode()).getType()) ||
                            Arrays.asList("multiple").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                        JsonNode element = cdata.get(head.getCode());
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
                        JsonNode element = cdata.get(head.getCode());
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
                        if (cdata.get(head.getCode()) != null) {
                            value = cdata.get(head.getCode()).booleanValue()?"Yes":"No";
                        }else{
                            value = "No";
                        }
                    }

                    if (Arrays.asList("number", "scaleTo10", "scaleTo5").contains(iForm.getItems().get(head.getCode()).getType())) {
                        value = cdata.get(head.getCode()).numberValue();
                    }

                    if (Arrays.asList("date").contains(iForm.getItems().get(head.getCode()).getType())) {
                        LocalDate date = null;
                        if (cdata.get(head.getCode()) != null) {
                            date = Instant.ofEpochMilli(cdata.get(head.getCode()).longValue())
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                        }

                        value = date.format(formatter);
                    }
                }

                if (value == null || value.toString().isEmpty()) {
                    // EMPTY CELL
//                    emptyCell.setPhrase(new Phrase("", blackFont));
//                    table.addCell(emptyCell);
                    textValue = "";
                }else{
                    textValue = value.toString().replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
                    textValue = textValue.replace("<br/>", "\n");
                    textValue = textValue.replace("<br>", "\n");
                    textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
//                    System.out.println(textValue);
//                    valueCell.setPhrase(new Phrase(textValue, blackFont));
//                    table.addCell(valueCell);
                }


                data.put(head.getLabel(), textValue);
//                String value = "";
//                if (entry.getData().get(head) != null) {
//                    value = entry.getData().get(head).textValue();
//                    data.put(head, value);
//                }
            }
            csvWriter.write(data, header);
        }

        csvWriter.close();
    }


}