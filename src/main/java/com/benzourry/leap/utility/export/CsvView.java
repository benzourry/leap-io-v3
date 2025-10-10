package com.benzourry.leap.utility.export;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.*;
import com.benzourry.leap.service.MailService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Created by MohdRazif on 5/8/2017.
 */
public class CsvView extends AbstractCsvView {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    protected void buildCsvDocument(Map<String, Object> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

//        response.setHeader("Content-Disposition", "attachment; filename=\"my-csv-file.csv\"");

        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm a");
        List<DatasetItem> headers = (List<DatasetItem>) model.get("headers");
        List<Entry> results = (List<Entry>) model.get("results");
        Stream<Entry> stream = (Stream<Entry>) model.get("streams");

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


//        try (Stream<Entry> entryStream = stream) {
//            entryStream.forEach(result -> {
//                System.out.println("stream out:"+result);
//                Map<String, String> data = new HashMap<>();
//
//                for (DatasetItem head : headers) {
//
//                    Object value = "";
//                    String textValue = "";
//
//                    JsonNode cdata = result.getData();
//                    Form iForm = form;
//
//                    if (Arrays.asList("prev", "data").contains(head.getRoot())) {
//                        if ("prev".equals(head.getRoot())) {
//                            cdata = result.getPrev();
//                            iForm = prevForm;
//                        }
//                    }else {
//                        if (head.getRoot()!=null) {
//                            if (result.getApproval() != null && result.getApproval().get(Long.parseLong(head.getRoot())) != null) {
//                                cdata = result.getApproval().get(Long.parseLong(head.getRoot())).getData();
//                            }
//                        }
//                    }
//
//                    if (cdata != null && head != null && cdata.get(head.getCode())!=null) {
//                        value = cdata.get(head.getCode()).textValue();
//                        if (value == null) {
//                            value = cdata.get(head.getCode()).numberValue();
//                        }
//
//                        if (Arrays.asList("select", "radio").contains(iForm.getItems().get(head.getCode()).getType())) {
//                            if (cdata.get(head.getCode()).get("name")!=null) {
//                                value = cdata.get(head.getCode()).get("name").textValue();
//                            }
//                        }
//
//                        if (Arrays.asList("checkboxOption").contains(iForm.getItems().get(head.getCode()).getType()) ||
//                                Arrays.asList("multiple").contains(iForm.getItems().get(head.getCode()).getSubType())) {
//                            JsonNode element = cdata.get(head.getCode());
//                            if (element != null) {
//                                if (element.isArray()) {
//                                    Iterator<JsonNode> inner = element.iterator();
//                                    List<String> vlist = new ArrayList<>();
//                                    while (inner.hasNext()) {
//                                        JsonNode innerElement = inner.next();
//                                        if (innerElement != null) {
//                                            vlist.add(innerElement.get("name").textValue());
//                                        }
//                                    }
//                                    value= String.join(", ", vlist);
//                                }
//                            }
//                        }
//
//                        if (Arrays.asList("file").contains(iForm.getItems().get(head.getCode()).getType()) ||
//                                Arrays.asList("othermulti","imagemulti").contains(iForm.getItems().get(head.getCode()).getSubType())) {
//                            JsonNode element = cdata.get(head.getCode());
//                            if (element != null) {
//                                if (element.isArray()) {
//                                    Iterator<JsonNode> inner = element.iterator();
//                                    List<String> vlist = new ArrayList<>();
//                                    while (inner.hasNext()) {
//                                        JsonNode innerElement = inner.next();
//                                        if (innerElement != null) {
//                                            String filePath = innerElement.textValue();
//                                            vlist.add(filePath);
//                                        }
//                                    }
//                                    value = String.join(", ", vlist);
//                                }
//                            }
//                        }
//
//                        if (Arrays.asList("checkbox").contains(iForm.getItems().get(head.getCode()).getType())) {
//                            if (cdata.get(head.getCode()) != null) {
//                                value = cdata.get(head.getCode()).booleanValue()?"Yes":"No";
//                            }else{
//                                value = "No";
//                            }
//                        }
//
//                        if (Arrays.asList("number", "scale", "scaleTo10", "scaleTo5").contains(iForm.getItems().get(head.getCode()).getType())) {
//                            value = cdata.get(head.getCode()).numberValue();
//                        }
//
//                        if (Arrays.asList("date").contains(iForm.getItems().get(head.getCode()).getType())) {
//                            LocalDate date = null;
//                            if (cdata.get(head.getCode()) != null) {
//                                date = Instant.ofEpochMilli(cdata.get(head.getCode()).longValue())
//                                        .atZone(ZoneId.systemDefault())
//                                        .toLocalDate();
//                            }
//
//                            value = date.format(formatter);
//                        }
//                    }
//
//                    if (value == null || value.toString().isEmpty()) {
//                        textValue = "";
//                    }else{
//                        textValue = value.toString().replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
//                        textValue = textValue.replace("<br/>", "\n");
//                        textValue = textValue.replace("<br>", "\n");
//                        textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
//                    }
//                    data.put(head.getLabel(), textValue);
//                }
//                try {
//                    csvWriter.write(data, header);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//                this.entityManager.detach(result);
//            });
//        }


        for (Entry result : results) {
            Map<String, String> data = new HashMap<>();

            for (DatasetItem head : headers) {


                    Object value = "";
                    String textValue = "";
                    try{
                        JsonNode cdata = result.getData();
                        Form iForm = form;

                        if (Arrays.asList("prev", "data").contains(head.getRoot())) {
                            if ("prev".equals(head.getRoot())) {
                                cdata = result.getPrev();
                                iForm = prevForm;
                            }
                        } else {
                            if (head.getRoot() != null) {
                                if (result.getApproval() != null && result.getApproval().get(Long.parseLong(head.getRoot())) != null) {
                                    cdata = result.getApproval().get(Long.parseLong(head.getRoot())).getData();
                                }
                            }
                        }

                        if (cdata != null && head != null && cdata.get(head.getCode()) != null) {
                            value = cdata.get(head.getCode()).textValue();
                            Item item = iForm.getItems().get(head.getCode());
                            if (item != null) {
                                if (value == null) {
                                    value = cdata.get(head.getCode()).numberValue();
                                }

                                if (Arrays.asList("select", "radio").contains(item.getType())) {
                                    if (cdata.get(head.getCode()).get("name") != null) {
                                        value = cdata.get(head.getCode()).get("name").textValue();
                                    }
                                }

                                if (Arrays.asList("modelPicker").contains(item.getType())) {
        //                        System.out.println("head:"+head.getCode());
                                    if (cdata.get(head.getCode()).get(item.getBindLabel()) != null) {
                                        value = cdata.get(head.getCode()).get(item.getBindLabel()).textValue();
                                    }
                                }

                                if (Arrays.asList("checkboxOption").contains(item.getType()) ||
                                        Arrays.asList("multiple").contains(item.getSubType())) {
                                    JsonNode element = cdata.get(head.getCode());
                                    if (element != null) {
                                        if (element.isArray()) {
                                            Iterator<JsonNode> inner = element.iterator();
                                            List<String> vlist = new ArrayList<>();
                                            while (inner.hasNext()) {
                                                JsonNode innerElement = inner.next();
                                                if (innerElement != null && innerElement.get("name") != null) {
                                                    vlist.add(innerElement.get("name").textValue());
                                                }
                                            }
                                            value = String.join(", ", vlist);
                                        }
                                    }
                                }

                                if (Arrays.asList("file").contains(item.getType()) ||
                                        Arrays.asList("othermulti", "imagemulti").contains(item.getSubType())) {
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

                                if (Arrays.asList("checkbox").contains(item.getType())) {
                                    if (cdata.get(head.getCode()) != null) {
                                        value = cdata.get(head.getCode()).booleanValue() ? "Yes" : "No";
                                    } else {
                                        value = "No";
                                    }
                                }

                                if (Arrays.asList("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())) {
                                    value = cdata.get(head.getCode()).numberValue();
                                }

                                if (Arrays.asList("date").contains(item.getType())) {
                                    LocalDateTime date = null;
                                    if (cdata.get(head.getCode()) != null) {
                                        date = Instant.ofEpochMilli(cdata.get(head.getCode()).longValue())
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDateTime();
                                    }

                                    if (Arrays.asList("datetime", "datetime-inline").contains(item.getSubType())) {
                                        value = date.format(formatterDateTime).toUpperCase(Locale.ROOT);
                                    } else if (Arrays.asList("time").contains(item.getSubType())) {
                                        value = date.format(formatterTime).toUpperCase(Locale.ROOT);
                                    } else {
                                        value = date.format(formatterDate).toUpperCase(Locale.ROOT);
                                    }
                                }
                            } else {
                                if (List.of("$code").contains(head.getCode())) {
                                    value = cdata.get(head.getCode()).textValue();
                                }
                                if (List.of("$id", "$counter").contains(head.getCode())) {
                                    value = cdata.get(head.getCode()).numberValue();
                                }
                            }
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    if (value == null || value.toString().isEmpty()) {
                        // EMPTY CELL
    //                    emptyCell.setPhrase(new Phrase("", blackFont));
    //                    table.addCell(emptyCell);
                        textValue = "";
                    } else {
                        textValue = value.toString().replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
                        textValue = textValue.replace("<br/>", "\n");
                        textValue = textValue.replace("<br>", "\n");
                        textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
    //                    System.out.println(textValue);
    //                    valueCell.setPhrase(new Phrase(textValue, blackFont));
    //                    table.addCell(valueCell);
                    }


                    data.put(head.getLabel(), textValue);
            }
            csvWriter.write(data, header);
        }

        csvWriter.close();
    }


}