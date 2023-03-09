package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.*;
import com.benzourry.leap.repository.EntryAttachmentRepository;
import com.benzourry.leap.config.Constant;
import com.benzourry.leap.utility.Helper;
import com.fasterxml.jackson.databind.JsonNode;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDate;
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<DatasetItem> headers = (List<DatasetItem>) model.get("headers");
        List<Entry> results = (List<Entry>) model.get("results");

        Dataset dataset = (Dataset) model.get("dataset");
        Form form = dataset.getForm();
        Form prevForm = (Form) model.get("prevForm");
        EntryAttachmentRepository entryAttachmentRepository = (EntryAttachmentRepository) model.get("attachmentRepository");

//        System.out.println(results.size());

//        System.out.println("LAYOUT:::::::::"+dataset.getExportPdfLayout());

//        if ("a4_landscape".equals(dataset.getExportPdfLayout())){
//            super.setPageSize(PageSize.A4.rotate());
//        }

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

        for (Entry result : results) {
//            System.out.println(result);
            for (DatasetItem head : headers) {
                Object value = "";

//                JsonNode data = result.getData();
//                Form iForm = form;
//
//                if ("prev".equals(head.getRoot())){
//                    data = result.getPrev();
//                    iForm = prevForm;
//                }
                JsonNode data = result.getData();
                Form iForm = form;

                if (Arrays.asList("prev", "data").contains(head.getRoot())) {
                    if ("prev".equals(head.getRoot())) {
                        data = result.getPrev();
                        iForm = prevForm;
                    }
                } else {
                    if (head.getRoot()!=null) {
                        if (result.getApproval() != null && result.getApproval().get(Long.parseLong(head.getRoot())) != null) {
                            data = result.getApproval().get(Long.parseLong(head.getRoot())).getData();
                        }
                    }
                }

//                System.out.println("data:"+data);
//                System.out.println("head:"+head);

                if (data != null && head != null && data.get(head.getCode()) != null) {

//                    System.out.println(iForm.getItems().get(head.getCode()).getType());

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
                                value = String.join(", ", vlist);
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
                            value = data.get(head.getCode()).booleanValue() ? "Yes" : "No";
                        } else {
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

//                System.out.println(head.getCode()+":"+value);

                if (value == null || value.toString().isEmpty()) {
                    // EMPTY CELL
                    table.addCell(emptyCell);
                } else if (Arrays.asList("imagePreview").contains(iForm.getItems().get(head.getCode()).getType())) {
                    try {
                        System.setProperty("http.agent", "Chrome");
                        Image image = Image.getInstance(value.toString());
                        PdfPCell cell2 = new PdfPCell(image, true);
                        table.addCell(cell2);
                    } catch (Exception e) {
//                        String filename = "static/placeholder-128.png";
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

                } else if (Arrays.asList("file").contains(iForm.getItems().get(head.getCode()).getType()) &&
                        Arrays.asList("image").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                    try {
                        System.setProperty("http.agent", "Chrome");
                        String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
                        if (!Helper.isNullOrEmpty(value.toString())) {
                            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(value.toString());
                            if (entryAttachment.getBucketId()!=null){
                                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
                            }
                        }
                        Image image = Image.getInstance( destStr + value);
                        PdfPCell cell2 = new PdfPCell(image, true);
                        table.addCell(cell2);
                    } catch (Exception e) {
//                        String filename = "static/placeholder-128.png";
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
                } else if (Arrays.asList("file").contains(iForm.getItems().get(head.getCode()).getType()) &&
                        Arrays.asList("imagemulti").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                    JsonNode element = data.get(head.getCode());
                    PdfPCell cell2 = new PdfPCell();
                    if (element != null) {
                        System.setProperty("http.agent", "Chrome");

                        if (element.isArray()) {
//                            System.out.println("element is array");
                            Resource resource = new ClassPathResource("static/placeholder-128.png");
                            Image placeholderImg = Image.getInstance(resource.getURL());
                            Iterator<JsonNode> inner = element.iterator();
                            while (inner.hasNext()) {
                                JsonNode innerElement = inner.next();
                                if (innerElement != null) {
                                    String filePath = innerElement.textValue();
                                    String destStr = Constant.UPLOAD_ROOT_DIR + "/attachment/";
//                                    System.out.println(filePath);
                                    try {

//                                        String filePath = "";
                                        if (!Helper.isNullOrEmpty(filePath)) {
                                            EntryAttachment entryAttachment = entryAttachmentRepository.findByFileUrl(filePath);
                                            if (entryAttachment.getBucketId()!=null){
                                                destStr += "bucket-" + entryAttachment.getBucketId() + "/";
                                            }
                                        }

                                        Image image = Image.getInstance(destStr + filePath);
                                        cell2.addElement(image);

                                    } catch (Exception e) {
                                        try {

//                                PdfPCell cell2 = new PdfPCell(image, true);
                                            cell2.addElement(placeholderImg);
                                            cell2.setPadding(2);
//                                            table.addCell(cell2);
                                            e.printStackTrace();
                                        } catch (Exception e2) {
                                            e2.printStackTrace();
                                            cell2.addElement(new Phrase(filePath + "", blackFont));
//                                            table.addCell(valueCell);
                                        }

                                    }
                                }
                            }
                        }
                    }

                    table.addCell(cell2);

                } else if (Arrays.asList("eval").contains(iForm.getItems().get(head.getCode()).getType()) &&
                        Arrays.asList("qr").contains(iForm.getItems().get(head.getCode()).getSubType())) {
                    try {
                        System.setProperty("http.agent", "Chrome");
                        Image image = Image.getInstance(Helper.generateQRCode(value + "", 255, 255));
                        PdfPCell cell2 = new PdfPCell(image, true);
                        table.addCell(cell2);
                    } catch (Exception e) {
//                        String filename = "static/placeholder-128.png";
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
                } else if (Arrays.asList("static").contains(iForm.getItems().get(head.getCode()).getType())) {
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

//                    PdfPCell cell = new PdfPCell();
//                    for (Element e : XMLWorkerHelper.parseToElementList(value.toString(), "")) {
//                        cell.addElement(e);
//                    }
//                    table.addCell(cell);
                    String textValue = value.toString().replaceAll("(?s)<\\/li[^>]*>.*?<li[^>]*>", ", ");
                    textValue = textValue.replace("<br/>", "\n");
                    textValue = textValue.replace("<br>", "\n");
                    textValue = textValue.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                    valueCell.setPhrase(new Phrase(textValue, blackFont));
                    table.addCell(valueCell);
                }

            }
        }

        doc.add(table);

    }
}