package com.benzourry.leap.utility.export;

import com.benzourry.leap.model.Dataset;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.web.servlet.view.AbstractView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Created by MohdRazif on 5/8/2017.
 */
public abstract class AbstractPdfView extends AbstractView {

    private static final String CONTENT_TYPE = "application/pdf";


    private String url;

//    private Rectangle pageSize = PageSize.A4.rotate();


    public AbstractPdfView() {

        setContentType(CONTENT_TYPE);
    }

    @Override
    protected boolean generatesDownloadContent() {
        return true;
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model,
                                           HttpServletRequest request, HttpServletResponse response) throws Exception {
        // IE workaround: write into byte array first.
        ByteArrayOutputStream baos = createTemporaryOutputStream();

        Dataset dataset = (Dataset)model.get("dataset");

        Rectangle pageSize = PageSize.A4;

        if ("a4_landscape".equals(dataset.getExportPdfLayout())){
//            System.out.println("Dlm A4 Landscape");
            pageSize = PageSize.A4.rotate();
        }else if ("a3".equals(dataset.getExportPdfLayout())){
//            System.out.println("Dlm A3");
            pageSize = PageSize.A3;
        }else if ("a3_landscape".equals(dataset.getExportPdfLayout())){
//            System.out.println("Dlm A3 Landscape");
            pageSize = PageSize.A3.rotate();
        }

        // Apply preferences and build metadata.
        Document document = newDocument(pageSize);
        PdfWriter writer = newWriter(document, baos);
        prepareWriter(model, writer, request);
        buildPdfMetadata(model, document, request);

        // Build PDF document.
        document.open();
        buildPdfDocument(model, document, writer, request, response);
        document.close();

//        System.out.println("KKKKK:::"+document.getPageSize());

        // Flush to HTTP response.
        writeToResponse(response, baos);
    }

    protected Document newDocument(Rectangle pageSize) {
        return new Document(pageSize,15,15,15,15);
    }

    protected PdfWriter newWriter(Document document, OutputStream os) throws DocumentException {
        return PdfWriter.getInstance(document, os);
    }

    protected void prepareWriter(Map<String, Object> model, PdfWriter writer, HttpServletRequest request)
            throws DocumentException {

        writer.setViewerPreferences(getViewerPreferences());
    }

    protected int getViewerPreferences() {
        return PdfWriter.ALLOW_PRINTING | PdfWriter.PageLayoutSinglePage;
    }

    protected void buildPdfMetadata(Map<String, Object> model, Document document, HttpServletRequest request) {
    }

    protected abstract void buildPdfDocument(Map<String, Object> model, Document document, PdfWriter writer,
                                             HttpServletRequest request, HttpServletResponse response) throws Exception;

}