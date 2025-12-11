package com.benzourry.leap.utility.export;

import com.benzourry.leap.config.Constant;
import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.JsonNode;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.*;

import static com.itextpdf.text.Element.ALIGN_CENTER;

/**
 * Created by MohdRazif on 5/8/2017.
 */
public class PdfView extends AbstractPdfView {

//    @Autowired
//    EntryAttachmentRepository entryAttachmentRepository;

    @Override
    protected void buildPdfDocument(Map<String, Object> model, Document doc,
                                    PdfWriter writer,
                                    HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter formatterDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm a");
        DateTimeFormatter formatterTime = DateTimeFormatter.ofPattern("HH:mm a");
        List<DatasetItem> headers = ((List<DatasetItem>) model.get("headers")).stream()
                .filter(h -> h!=null && h.getCode()!=null)
                .toList();
        List<EntryDto> results = (List<EntryDto>) model.get("results");

        Dataset dataset = (Dataset) model.get("dataset");
        Form form = dataset.getForm();
        Form prevForm = (Form) model.get("prevForm");
        EntryAttachmentRepository entryAttachmentRepository = (EntryAttachmentRepository) model.get("attachmentRepository");

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA, 18);
        titleFont.setColor(BaseColor.BLACK);
        Paragraph title = new Paragraph(dataset.getTitle(), titleFont);
        title.setAlignment(ALIGN_CENTER);


//        titleFont.setSize(24f);
//        title.setFont(titleFont);
        doc.add(title);
        doc.setMargins(20, 20, 20, 20);

        Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        descFont.setColor(BaseColor.GRAY);
        Paragraph desc = new Paragraph("Generated on " + new Date(), descFont);
        desc.setAlignment(ALIGN_CENTER);
        doc.add(desc);

        PdfPTable table = new PdfPTable(headers.size());
        table.setWidthPercentage(100.0f);
        table.setSpacingBefore(20);

        // define font for table header row
        Font whiteFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD);
        whiteFont.setColor(BaseColor.WHITE);

//        whiteFont.setSize(12f);

        Font blackFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        blackFont.setColor(BaseColor.BLACK);
//        blackFont.setSize(12f);

        // define table header cell
        PdfPCell headerCell = new PdfPCell();
        headerCell.setBackgroundColor(new BaseColor(35, 35, 35));
        headerCell.setPadding(5);
        headerCell.setBorderColor(new BaseColor(35, 35, 35));

//        headerCell.setBorderWidth(1);

        PdfPCell emptyCell = new PdfPCell();
        emptyCell.setBackgroundColor(BaseColor.PINK);
        emptyCell.setPadding(5);
        emptyCell.setBorderColor(new BaseColor(35, 35, 35));
//        emptyCell.setBorderWidth(1);

        PdfPCell valueCell = new PdfPCell();
        valueCell.setBackgroundColor(BaseColor.WHITE);
        valueCell.setPadding(5);
        valueCell.setBorderColor(new BaseColor(35, 35, 35));
//        valueCell.setBorderWidth(1);

        List<String> headerList = new ArrayList<>();
        // write table header
        for (DatasetItem entry : headers) {
            headerCell.setPhrase(new Phrase(entry.getLabel(), whiteFont));
            table.addCell(headerCell);
            headerList.add(entry.getLabel());
        }

        for (EntryDto result : results) {
            for (DatasetItem head : headers) {
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

                Item item = iForm.getItems().get(head.getCode());
                JsonNode element = data.get(head.getCode());

                if (item != null) {

                    if (data != null && head != null && element != null) {

                        value = element.textValue();

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

                            if (Arrays.asList("datetime", "datetime-inline").contains(item.getSubType())){
                                value = date.format(formatterDateTime).toUpperCase(Locale.ROOT);
                            }else if (Arrays.asList("time").contains(item.getSubType())){
                                value = date.format(formatterTime).toUpperCase(Locale.ROOT);
                            }else{
                                value = date.format(formatterDate).toUpperCase(Locale.ROOT);
                            }
                        }
                    }

                    if (value == null || value.toString().isEmpty()) {
                        // EMPTY CELL
                        table.addCell(emptyCell);
                    } else if (Arrays.asList("imagePreview").contains(item.getType())) {
                        try {
                            System.setProperty("http.agent", "Chrome");
                            Image image = Image.getInstance(value.toString());
                            PdfPCell cell2 = new PdfPCell(image, true);
                            table.addCell(cell2);
                        } catch (Exception e) {
                            try {
                                Resource resource = new ClassPathResource("static/placeholder-128.png");
                                Image image = Image.getInstance(resource.getURL());
                                PdfPCell cell2 = new PdfPCell(image, true);
                                cell2.setPadding(2);
                                table.addCell(cell2);
                                e.printStackTrace();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                                valueCell.setPhrase(new Phrase(value + "", blackFont));
                                table.addCell(valueCell);
                            }
                        }

                    } else if (Arrays.asList("file").contains(item.getType()) &&
                            Arrays.asList("image").contains(item.getSubType())) {
                        try {
                            System.setProperty("http.agent", "Chrome");
                            String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
                            if (!Helper.isNullOrEmpty(value.toString())) {
                                EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(value.toString());
                                if (entryAttachment.getBucketId() != null) {
                                    destStr += "bucket-" + entryAttachment.getBucketId() + "/";
                                }
                            }
                            Image image = Image.getInstance(destStr + value);
                            PdfPCell cell2 = new PdfPCell(image, true);
                            table.addCell(cell2);
                        } catch (Exception e) {
                            try {
                                Resource resource = new ClassPathResource("static/placeholder-128.png");
                                Image image = Image.getInstance(resource.getURL());
                                PdfPCell cell2 = new PdfPCell(image, true);
                                cell2.setPadding(2);
                                table.addCell(cell2);
                                e.printStackTrace();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                                valueCell.setPhrase(new Phrase(value + "", blackFont));
                                table.addCell(valueCell);
                            }

                        }
                    } else if (Arrays.asList("file").contains(item.getType()) &&
                            Arrays.asList("imagemulti").contains(item.getSubType())) {
                        PdfPCell cell2 = new PdfPCell();
                        if (element != null) {
                            System.setProperty("http.agent", "Chrome");

                            if (element.isArray()) {
                                Resource resource = new ClassPathResource("static/placeholder-128.png");
                                Image placeholderImg = Image.getInstance(resource.getURL());
                                Iterator<JsonNode> inner = element.iterator();
                                while (inner.hasNext()) {
                                    JsonNode innerElement = inner.next();
                                    if (innerElement != null) {
                                        String filePath = innerElement.textValue();
                                        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
                                        try {

                                            if (!Helper.isNullOrEmpty(filePath)) {
                                                EntryAttachment entryAttachment = entryAttachmentRepository.findFirstByFileUrl(filePath);
                                                if (entryAttachment.getBucketId() != null) {
                                                    destStr += "bucket-" + entryAttachment.getBucketId() + "/";
                                                }
                                            }

                                            Image image = Image.getInstance(destStr + filePath);
                                            cell2.addElement(image);

                                        } catch (Exception e) {
                                            try {

                                                cell2.addElement(placeholderImg);
                                                cell2.setPadding(2);
                                                e.printStackTrace();
                                            } catch (Exception e2) {
                                                e2.printStackTrace();
                                                cell2.addElement(new Phrase(filePath + "", blackFont));
                                            }

                                        }
                                    }
                                }
                            }
                        }

                        table.addCell(cell2);

                    } else if (Arrays.asList("eval").contains(item.getType()) &&
                            Arrays.asList("qr").contains(item.getSubType())) {
                        try {
                            System.setProperty("http.agent", "Chrome");
                            Image image = Image.getInstance(Helper.generateQRCode(value + "", 255, 255));
                            PdfPCell cell2 = new PdfPCell(image, true);
                            table.addCell(cell2);
                        } catch (Exception e) {
                            try {
                                Resource resource = new ClassPathResource("static/placeholder-128.png");
                                Image image = Image.getInstance(resource.getURL());
                                PdfPCell cell2 = new PdfPCell(image, true);
                                cell2.setPadding(2);
                                table.addCell(cell2);
                                e.printStackTrace();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                                valueCell.setPhrase(new Phrase(value + "", blackFont));
                                table.addCell(valueCell);
                            }

                        }
                    } else if (Arrays.asList("static").contains(item.getType())) {
                        PdfPCell cell = new PdfPCell();
                        String css = "table,th,td{}" +
                                "table {border-collapse: collapse;width:100%;font-size:11px;border-bottom:1px solid gray;border-right:1px solid gray}" +
                                "tr {}" +
                                "th, td {text-align: left;vertical-align:top;padding: 1px 3px;border-top:solid 1px gray;border-left:solid 1px gray;min-width:30px}";
                        for (Element e : XMLWorkerHelper.parseToElementList(value.toString(), css)) {
                            cell.addElement(e);
                        }
                        table.addCell(cell);
                    } else {

                        String textValue = value.toString().replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
                        textValue = textValue.replace("<br/>", "\n");
                        textValue = textValue.replace("<br>", "\n");
                        textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                        valueCell.setPhrase(new Phrase(textValue, blackFont));
                        table.addCell(valueCell);
                    }

                } else {
                    if (List.of("$id", "$counter").contains(head.getCode())) {
                        value = element.numberValue();
                    }
                    if (List.of("$code").contains(head.getCode())) {
                        value = element.textValue();
                    }
                    valueCell.setPhrase(new Phrase(value.toString(), blackFont));
                    table.addCell(valueCell);
                }
            }
        }

        doc.add(table);

    }
}