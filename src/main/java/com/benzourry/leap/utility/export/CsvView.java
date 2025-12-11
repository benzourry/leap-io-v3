package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        List<DatasetItem> headers = ((List<DatasetItem>) model.get("headers")).stream()
                .filter(h -> h != null && h.getCode() != null)
                .toList();
        List<EntryDto> results = (List<EntryDto>) model.get("results");
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

        for (EntryDto result : results) {
            Map<String, String> data = new HashMap<>();

            for (DatasetItem head : headers) {


                Object value = "";
                String textValue = "";
                try {
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

                    JsonNode element = cdata.get(head.getCode());

                    if (cdata != null && head != null && element != null) {
                        value = element.textValue();
                        Item item = iForm.getItems().get(head.getCode());
                        if (item != null) {
                            if (value == null) {
                                value = element.numberValue();
                            }

                            if (Arrays.asList("select", "radio").contains(item.getType())) {
                                if (element.get("name") != null) {
                                    value = element.get("name").textValue();
                                }
                            }

                            if (Arrays.asList("modelPicker").contains(item.getType())) {
                                if (element.get(item.getBindLabel()) != null) {
                                    value = element.get(item.getBindLabel()).textValue();
                                }
                            }

                            if (Arrays.asList("checkboxOption").contains(item.getType()) ||
                                    Arrays.asList("multiple").contains(item.getSubType())) {
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

                            if (Arrays.asList("file").contains(item.getType()) ||
                                    Arrays.asList("othermulti", "imagemulti").contains(item.getSubType())) {
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

                            if (Arrays.asList("checkbox").contains(item.getType())) {
                                if (element != null) {
                                    value = element.booleanValue() ? "Yes" : "No";
                                } else {
                                    value = "No";
                                }
                            }

                            if (Arrays.asList("number", "scale", "scaleTo10", "scaleTo5").contains(item.getType())) {
                                value = element.numberValue();
                            }

                            if (Arrays.asList("date").contains(item.getType())) {
                                LocalDateTime date = Instant.ofEpochMilli(element.longValue())
                                            .atZone(ZoneId.systemDefault())
                                            .toLocalDateTime();

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
                                value = element.textValue();
                            }
                            if (List.of("$id", "$counter").contains(head.getCode())) {
                                value = element.numberValue();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (value == null || value.toString().isEmpty()) {
                    // EMPTY CELL
                    textValue = "";
                } else {
                    textValue = value.toString().replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
                    textValue = textValue.replace("<br/>", "\n");
                    textValue = textValue.replace("<br>", "\n");
                    textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                }


                data.put(head.getLabel(), textValue);
            }
            csvWriter.write(data, header);
        }

        csvWriter.close();
    }


}