package com.benzourry.leap.utility;

import com.benzourry.leap.config.Constant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.styledxmlparser.jsoup.Jsoup;
import com.networknt.schema.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.tesseract.global.tesseract;
import org.hibernate.internal.util.SerializationHelper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.stringtemplate.v4.ST;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixRead;

//import static org.bytedeco.leptonica.global.lept.pixDestroy;
//import static org.bytedeco.leptonica.global.lept.pixRead;
//import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
//import static org.bytedeco.leptonica.global.leptonica.pixRead;

//import org.bytedeco.javacpp.*;
//import org.bytedeco.leptonica.*;
//import org.bytedeco.tesseract.*;
//
//import static net.sourceforge.lept4j.Leptonica1.pixDestroy;
//import static org.bytedeco.leptonica.global.leptonica.*;
//import static org.bytedeco.tesseract.global.tesseract.*;

public class Helper {

//    private static Map<String, String> REPLACEMENTS = Map.ofEntries(
//        Map.entry("$$_", "approval_"),
//        Map.entry("$$", "approval"),
//        Map.entry("$uiUri$", "uiUri"),
//        Map.entry("$approval$", "approval"),
//        Map.entry("$viewUri$", "viewUri"),
//        Map.entry("$editUri$", "editUri"),
//        Map.entry("$tier$", "tier"),
//        Map.entry("$prev$.$code", "prev_code"),
//        Map.entry("$prev$.$id", "prev_id"),
//        Map.entry("$prev$.$counter", "prev_counter"),
//        Map.entry("$conf$", "conf"), // just to allow presetFilter with $conf$ dont throw error because of succcessive replace of '$'. Normally it will become $$confdata.category$
//        Map.entry("$prev$", "prev"),
//        Map.entry("$user$", "user"),
//        Map.entry("$_", "_"),
//        Map.entry("$.$code", "code"),
//        Map.entry("$.$id", "id"),
//        Map.entry("$.$counter", "counter"),
//        Map.entry("$.", "data.")
////        Map.entry("{{", "$"),
////        Map.entry("}}", "$")
//    );
//
//    public static String compileTpl(String text, Map<String, Object> obj){
//
//        Pattern pattern = Pattern.compile("\\{\\{(.*?)\\}\\}");
//        Matcher matcher = pattern.matcher(text);
//
//        StringBuffer result = new StringBuffer();
//
//        // Process setiap occurence of {{}}
//        while (matcher.find()) {
//            String placeholder = matcher.group(1); // content dalam {{}}
//            String replacement;
//            // Mn seluruh content ialah exact match dgn key, pake value nya directly (ie: $.$code)
//            if (REPLACEMENTS.containsKey(placeholder)) {
//                replacement = REPLACEMENTS.get(placeholder);
//            } else {
//                // Mn x, partial replace utk setiap occurence of key dalam content (ie: $.name, $. is partial match)
//                replacement = placeholder;
//                for (String key : REPLACEMENTS.keySet()) {
//                    if (replacement.contains(key)) {
//                        replacement = replacement.replace(key, REPLACEMENTS.get(key));
//                    }
//                }
//            }
//            // Make sure to escape replacement string properly n prepend/append { }
//            matcher.appendReplacement(result, Matcher.quoteReplacement('{' + replacement + '}'));
//        }
//        matcher.appendTail(result);
//
//        ST content = new ST(result.toString(), '{', '}');
//        for (Map.Entry<String, Object> entry : obj.entrySet()) {
//            content.add(entry.getKey(), entry.getValue());
//        }
//
//        System.out.println("content@@@@@@@@@@@@@:"+result);
//
//        content.groupThatCreatedThisInstance.registerRenderer(Object.class, new FieldRenderer());
//        return content.render();
//    }

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void writeWithCsvBeanWriter(Writer writer, List list, CellProcessor[] processors, String[] headers) throws IOException {
        ICsvBeanWriter beanWriter = null;
        try {
            beanWriter = new CsvBeanWriter(writer,
                    CsvPreference.STANDARD_PREFERENCE);

            // the header elements are used to map the bean values to each column (names must match)
//            final String[] header = new String[] { "customerNo", "firstName", "lastName", "birthDate",
//                    "mailingAddress", "married", "numberOfKids", "favouriteQuote", "email", "loyaltyPoints" };
//            final CellProcessor[] processors = getProcessors();

            // write the header
            beanWriter.writeHeader(headers);

            // write the beans
            for( final Object customer : list ) {
                beanWriter.write(customer, headers, processors);
            }

        }
        finally {
            if( beanWriter != null ) {
                beanWriter.close();
            }
        }
    }


    private static final FieldRenderer FIELD_RENDERER = new FieldRenderer();

//    private static final Map<String, String> REPLACEMENTS = Map.ofEntries(
//            Map.entry("\\$\\$_", "approval_"),
//            Map.entry("\\$\\$", "approval"),
//            Map.entry("\\$uiUri\\$", "uiUri"),
//            Map.entry("\\$approval\\$", "approval"),
//            Map.entry("\\$viewUri\\$", "viewUri"),
//            Map.entry("\\$editUri\\$", "editUri"),
//            Map.entry("\\$tier\\$", "tier"),
//            Map.entry("\\$prev\\$\\.\\$code", "prev_code"),
//            Map.entry("\\$prev\\$\\.\\$id", "prev_id"),
//            Map.entry("\\$prev\\$\\.\\$counter", "prev_counter"),
//            Map.entry("\\$conf\\$", "conf"),
//            Map.entry("\\$prev\\$", "prev"),
//            Map.entry("\\$user\\$", "user"),
//            Map.entry("\\$_", "_"),
//            Map.entry("\\$\\.\\$code", "code"),
//            Map.entry("\\$\\.\\$id", "id"),
//            Map.entry("\\$\\.\\$counter", "counter"),
//            Map.entry("\\$\\.", "data."),
//            Map.entry("\\{\\{", "{"),
//            Map.entry("\\}\\}", "}")
//    );
//
//    // Precompile pattern (matches any key from REPLACEMENTS)
//    private static final Pattern REPLACEMENT_PATTERN = Pattern.compile(
//            String.join("|", REPLACEMENTS.keySet())
//    );
    public static String compileTpl(String text, Map<String, Object> obj) {
        ST content = new ST(rewriteTemplate(text), '{', '}');

        obj.forEach(content::add);
//        for (Map.Entry<String, Object> entry : obj.entrySet()) {
//            content.add(entry.getKey(), entry.getValue());
//        }
        content.groupThatCreatedThisInstance.registerRenderer(Object.class, FIELD_RENDERER);
        return content.render();
    }

//    public static String rewriteTemplate(String input) {
//        if (input == null) return null;
//
//        Matcher matcher = REPLACEMENT_PATTERN.matcher(input);
//        StringBuffer sb = new StringBuffer();
//
//        while (matcher.find()) {
//            String replacement = REPLACEMENTS.get(matcher.group());
//            matcher.appendReplacement(sb, replacement != null ? Matcher.quoteReplacement(replacement) : matcher.group());
//        }
//        matcher.appendTail(sb);
//
//        return sb.toString();
//    }

    public static String rewriteTemplate(String str) {
        if (str != null) {
            str = str.replace("$$_", "approval_");
            str = str.replace("$$", "approval");
            str = str.replace("$uiUri$", "uiUri");
            str = str.replace("$approval$", "approval");
            str = str.replace("$viewUri$", "viewUri");
            str = str.replace("$editUri$", "editUri");
            str = str.replace("$tier$", "tier");
            str = str.replace("$prev$.$code", "prev_code");
            str = str.replace("$prev$.$id", "prev_id");
            str = str.replace("$prev$.$counter", "prev_counter");
            str = str.replace("$conf$", "conf"); // just to allow presetFilter with $conf$ dont throw error because of succcessive replace of '$'. Normally it will become $$confdata.category$
            str = str.replace("$prev$", "prev");
            str = str.replace("$user$", "user");
            str = str.replace("$_", "_");
            str = str.replace("$.$code", "code");
            str = str.replace("$.$id", "id");
            str = str.replace("$.$counter", "counter");
            str = str.replace("$.", "data.");
            str = str.replace("{{", "{");
            str = str.replace("}}", "}");

        }
        return str;
    }

    /**
     * Extracts text values from the JsonNode based on a pointer that may contain unlimited wildcards ([*]).
     *
     * @param root     the root JsonNode
     * @param jsonPath the JSON pointer with wildcards (e.g. "/jobs[*]/code")
     * @return a List of extracted String values
     */
    public static JsonNode jsonAtPath(JsonNode root, String jsonPath) {
        // Check if the jsonPath contains a wildcard.
        if (!jsonPath.contains("[*]")) {
            // No wildcard: simply use JsonNode.at() to get the node.
            return root.at(jsonPath);
        }

        // With wildcard: split the pointer by literal "[*]".
        String[] parts = jsonPath.split(Pattern.quote("[*]"));
        // Begin with the root node.
        List<JsonNode> currentNodes = new ArrayList<>();
        currentNodes.add(root);

        // Process each segment sequentially.
        for (String part : parts) {
            // Ensure the segment is a valid pointer. Prepend a slash if missing.
            if (!part.startsWith("/")) {
                part = "/" + part;
            }
            List<JsonNode> nextNodes = new ArrayList<>();
            for (JsonNode node : currentNodes) {
                JsonNode extracted = node.at(part);
                if (extracted.isMissingNode()) {
                    continue;
                }
                // If the extracted node is an array, add each element.
                if (extracted.isArray()) {
                    extracted.forEach(nextNodes::add);
                } else {
                    nextNodes.add(extracted);
                }
            }
            currentNodes = nextNodes;
        }

        // Convert the list of nodes into an ArrayNode.
        ArrayNode resultArray = JsonNodeFactory.instance.arrayNode();
        currentNodes.forEach(resultArray::add);
        return resultArray;
    }


    public static byte[] generateQRCode(String text, int width, int height) throws WriterException, IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height,Map.of(EncodeHintType.MARGIN,1));
        MatrixToImageWriter.writeToStream(matrix, MediaType.IMAGE_PNG.getSubtype(), baos, new MatrixToImageConfig());
        return baos.toByteArray();
    }

    public static String capitalize(String text) {
        String c = (text != null) ? text.trim() : "";
        String[] words = c.split(" ");
        StringBuilder result = new StringBuilder();
        for (String w : words) {
            result.append(w.length() > 1 ? w.substring(0, 1).toUpperCase(Locale.US) + w.substring(1, w.length()).toLowerCase(Locale.US) : w).append(" ");
        }
        return result.toString().trim();
    }

    public static Calendar calendarWithTime(Calendar calendar, int hour, int minute, int sec, int milis){
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, sec);
        calendar.set(Calendar.MILLISECOND, milis);
        return calendar;
    }

    // This is fancier than Map.putAll(Map)
    public static Map deepMerge(Map original, Map newMap) {
        for (Object key : newMap.keySet()) {
            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
                Map originalChild = (Map) original.get(key);
                Map newChild = (Map) newMap.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else if (newMap.get(key) instanceof List && original.get(key) instanceof List) {
                List originalChild = (List) original.get(key);
                List newChild = (List) newMap.get(key);
                for (Object each : newChild) {
                    if (!originalChild.contains(each)) {
                        originalChild.add(each);
                    }
                }
            } else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }

    public static JsonNode deepMerge(JsonNode target, JsonNode source) {
        if (target instanceof ObjectNode && source instanceof ObjectNode) {
            ObjectNode targetObjectNode = (ObjectNode) target;
            ObjectNode sourceObjectNode = (ObjectNode) source;

            sourceObjectNode.fields().forEachRemaining(entry -> {
                String fieldName = entry.getKey();
                JsonNode sourceValue = entry.getValue();

                if (targetObjectNode.has(fieldName)) {
                    JsonNode targetValue = targetObjectNode.get(fieldName);

                    if (targetValue instanceof ObjectNode && sourceValue instanceof ObjectNode) {
                        targetObjectNode.set(fieldName, deepMerge(targetValue, sourceValue));
                    } else {
                        targetObjectNode.set(fieldName, sourceValue);
                    }
                } else {
                    targetObjectNode.set(fieldName, sourceValue);
                }
            });
            return targetObjectNode;
        }

        return source;
    }

    public static String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    public static String ocr(String filePath, String lang){
        String rText = "";
        BytePointer outText;

        try (PointerScope scope = new PointerScope()){

//            TessBaseAPI api = new TessBaseAPI();
            try (TessBaseAPI api = new TessBaseAPI()) {

                // Initialize tesseract-ocr with English, without specifying tessdata path
                if (api.Init(Constant.UPLOAD_ROOT_DIR + "/tessdata", Optional.ofNullable(lang).orElse("eng")) != 0) {
                    System.err.println("Error Line 121: Could not initialize tesseract.");
                    throw new RuntimeException("Could not initialize tesseract");
//                    System.exit(1);
                }

                // Open input image with leptonica library
                PIX image = pixRead(filePath);
                api.SetImage(image);
                // Get OCR result
                outText = api.GetUTF8Text();
                if (outText!=null) {
                    rText = outText.getString();
                }
                System.out.println("OCR output:\n" + rText);

                // Destroy used object and release memory
                api.End();
                if (outText!=null) {
                    outText.deallocate();
                }
                pixDestroy(image);
            }
        }
        return rText;
    }


//    public static String ocr(String filePath, String lang) {
//        String rText = "";
//        File imageFile = new File(filePath);
//        ITesseract instance = new Tesseract();  // JNA Interface Mapping
//        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
//        instance.setDatapath(Constant.UPLOAD_ROOT_DIR + "/tessdata"); // path to tessdata directory
//
//        instance.setLanguage(Optional.ofNullable(lang).orElse("eng")); // language
//
//        try {
//            rText = instance.doOCR(imageFile);
//            System.out.println(rText);
//        } catch (TesseractException e) {
//            System.err.println(e.getMessage());
//        }
//
//        return rText;
//    }

    public static String replaceMulti(String text, Map<String, String> maps){
        int size = maps.size();
        String[] keys = maps.keySet().toArray(new String[size]);
        String[] values = maps.values().toArray(new String[size]);
        return StringUtils.replaceEach(text, keys, values);
    }

    public static Optional<Collection<?>> ofNullableList(Collection<?> c){
        return (c == null || c.isEmpty())?Optional.ofNullable(null):Optional.ofNullable(c);
    }

    public static Optional<Map<? extends String,?>> ofNullableMap(Map<? extends String,?> c){
        return (c == null || c.isEmpty())?Optional.ofNullable(null):Optional.ofNullable(c);
    }

    public static Calendar getCalendarDayStart(){
        return calendarWithTime(Calendar.getInstance(),0,0,0,0);
    }

    public static Calendar getCalendarDayEnd(){
        return calendarWithTime(Calendar.getInstance(),23,59,59,999);
    }

    public static boolean isNullOrEmpty(final Collection<?> c) {
        return c == null || c.isEmpty();
    }

    public static Collection<?> isNullOrEmptyThen(final Collection<?> c, final Collection<?> d) {
        return (c == null || c.isEmpty()) ? d : c;
    }

    public static boolean isNullOrEmpty(final Map<?, ?> m) {
        return m == null || m.isEmpty();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    public static <T> T clone(Class<T> clazz, T dtls) {
        return (T) SerializationHelper.clone((Serializable) dtls);
    }

    public static boolean isValidLong(String code) {
        try {
            Long.parseLong(code);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static BufferedImage resizeImage(BufferedImage originalImage, int width, int height, int type) {
        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        return resizedImage;
    }

    public static BufferedImage resizeImageWithHint(BufferedImage originalImage, int width, int height, int type) {

        BufferedImage resizedImage = new BufferedImage(width, height, type);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();
        g.setComposite(AlphaComposite.Src);


        return resizedImage;
    }

    public static BufferedImage cropImageSquare(byte[] image) throws IOException {
        // Get a BufferedImage object from a byte array
        InputStream in = new ByteArrayInputStream(image);
        BufferedImage originalImage = ImageIO.read(in);

        // Get image dimensions
        int height = originalImage.getHeight();
        int width = originalImage.getWidth();

        // The image is already a square
        if (height == width) {
            return originalImage;
        }

        // Compute the size of the square
        int squareSize = (Math.min(height, width));

        // Coordinates of the image's middle
        int xc = width / 2;
        int yc = height / 2;

        // Crop

        return originalImage.getSubimage(
                xc - (squareSize / 2), // x coordinate of the upper-left corner
                yc - (squareSize / 2), // y coordinate of the upper-left corner
                squareSize,            // widht
                squareSize             // height
        );
    }


//    public static Object jsEval(String script, Entry entry, User user) throws ScriptException, NoSuchMethodException, JsonProcessingException {
//        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
//
//        ObjectMapper mapper = new ObjectMapper();
//
//        Object result = null;
//
//        engine.eval("function fef($_,$,$prev$,$user$){ return " + script + "}");
//        Invocable invocable = (Invocable) engine;
//
//        engine.put("dataModel", mapper.writeValueAsString(entry.getData()));
//        engine.put("prevModel", mapper.writeValueAsString(entry.getPrev()));
//        JSObject data = (JSObject) engine.eval("JSON.parse(dataModel)");
//        JSObject prev = (JSObject) engine.eval("JSON.parse(prevModel)");
//
//        result = invocable.invokeFunction("fef", entry, data, prev, user);
//
//        return result;
//    }

    public static String getAlphaNumericString(int n)    {

        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNPQRSTUVWXYZ"
                + "23456789"
                + "abcdefghijkmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(n);

        for (int i = 0; i < n; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
    }

    public static String getApiKey(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
//        if (org.springframework.util.StringUtils.hasText(bearerToken) && bearerToken.startsWith("ApiKey ")) {
//            return bearerToken.substring(7);
//        }else if(request.getParameter("api_key")!=null){ //  && "local".equals(request.getParameter("provider")) && request.getParameter("provider")==null && "local".equals(request.getParameter("provider"))
//            return request.getParameter("api_key");
//        }
        // api_key first priority, then check header
        if (request.getParameter("api_key")!=null){ //  && "local".equals(request.getParameter("provider")) && request.getParameter("provider")==null && "local".equals(request.getParameter("provider"))
            return request.getParameter("api_key");
        }else if (org.springframework.util.StringUtils.hasText(bearerToken) && bearerToken.startsWith("ApiKey ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public static XSSFWorkbook readExcel(Path path){
        XSSFWorkbook workbook = null;
        try {
            workbook = new XSSFWorkbook(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InvalidFormatException e) {
            throw new RuntimeException(e);
        }
        return workbook;
    }

    // function to capitalize the first letter of each word
    public static String capitalizeWords(String input) {
        // split the input string into an array of words
        String[] words = input.split("\\s");

        // StringBuilder to store the result
        StringBuilder result = new StringBuilder();

        // iterate through each word
        for (String word : words) {
            // capitalize the first letter, append the rest of the word, and add a space
            result.append(Character.toTitleCase(word.charAt(0)))
                    .append(word.substring(1))
                    .append(" ");
        }

        // convert StringBuilder to String and trim leading/trailing spaces
        return result.toString().trim();
    }

    public static String getFullURL(HttpServletRequest request) {
        StringBuilder requestURL = new StringBuilder(request.getRequestURL().toString());
        String queryString = request.getQueryString();

        if (queryString == null) {
            return requestURL.toString();
        } else {
            return requestURL.append('?').append(queryString).toString();
        }
    }


    public static float[][][][] processImage(String imagePath, int batch, int channel, int h, int w) {
        try {
//            float[][][][] tensorData = new float[1][3][224][224]; // batch size, channel, h, w
            float[][][][] tensorData = new float[batch][channel][h][w]; // batch size, channel, h, w
//            float[][][][] tensorData = new float[batch][h][w][channel]; // batch size, h, w, channel
            var mean = new float[] { 0.485f, 0.456f, 0.406f };
            var standardDeviation = new float[] { 0.229f, 0.224f, 0.225f };

            // Read image
            File imageFile = new File(imagePath);
            BufferedImage image = ImageIO.read(imageFile);

            // Crop image
            int width = image.getWidth();
            int height = image.getHeight();
            int startX = 0;
            int startY = 0;
            if (width > height) {
                startX = (width - height) / 2;
                width = height;
            } else {
                startY = (height - width) / 2;
                height = width;
            }
            image = image.getSubimage(startX, startY, width, height);
            // ImageIO.write(image, "jpg", new File("C:\\Users\\nutiu\\IdeaProjects\\untitled\\src\\test\\java\\main\\resources\\debug.jpg"));

            // Resize image
            var resizedImage = image.getScaledInstance(h, w, 4);

            // Process image
            BufferedImage scaledImage = new BufferedImage(h, w, BufferedImage.TYPE_4BYTE_ABGR);
            scaledImage.getGraphics().drawImage(resizedImage, 0, 0, null);

            // if batch, h, w, channel
            for (var y = 0; y < scaledImage.getHeight(); y++) {
                for (var x = 0; x < scaledImage.getWidth(); x++) {
                    int pixel = scaledImage.getRGB(x,y);

                    // Get RGB values
//                    tensorData[0][y][x][0] = (((pixel >> 16) & 0xFF) / 255f - mean[0]) / standardDeviation[0];
//                    tensorData[0][y][x][1] = (((pixel >> 16) & 0xFF) / 255f - mean[1]) / standardDeviation[1];
//                    tensorData[0][y][x][2] = (((pixel >> 16) & 0xFF) / 255f - mean[2]) / standardDeviation[2];
                    tensorData[0][0][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[0]) / standardDeviation[0];
                    tensorData[0][1][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[1]) / standardDeviation[1];
                    tensorData[0][2][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[2]) / standardDeviation[2];
                }
            }


            return tensorData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//    public static float[][][][] processImageYolo(String imagePath, int batch, int channel, int h, int w) {
//        //            float[][][][] tensorData = new float[1][3][224][224]; // batch size, channel, h, w
//        float[][][][] tensorData = new float[batch][channel][h][w]; // batch size, channel, h, w
////            float[][][][] tensorData = new float[batch][h][w][channel]; // batch size, h, w, channel
//        var mean = new float[] { 0.485f, 0.456f, 0.406f };
//        var standardDeviation = new float[] { 0.229f, 0.224f, 0.225f };
//
//        // Process image
//        BufferedImage scaledImage = processBufferedImageYolo(imagePath,batch,channel,h,w);
//
////             batch, channel, h, w
//        for (var y = 0; y < scaledImage.getHeight(); y++) {
//            for (var x = 0; x < scaledImage.getWidth(); x++) {
//                int pixel = scaledImage.getRGB(x,y);
//
//                // Get RGB values
//                tensorData[0][0][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[0]) / standardDeviation[0];
//                tensorData[0][1][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[1]) / standardDeviation[1];
//                tensorData[0][2][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[2]) / standardDeviation[2];
//            }
//        }
//
//        return tensorData;
//    }
    public static float[][][][] convertToFloatBuffer(BufferedImage bi, int batch, int channel, int h, int w) {
        //            float[][][][] tensorData = new float[1][3][224][224]; // batch size, channel, h, w
        float[][][][] tensorData = new float[batch][channel][h][w]; // batch size, channel, h, w
//            float[][][][] tensorData = new float[batch][h][w][channel]; // batch size, h, w, channel
        var mean = new float[] { 0.485f, 0.456f, 0.406f };
        var standardDeviation = new float[] { 0.229f, 0.224f, 0.225f };

        // Process image
        BufferedImage scaledImage = bi;

//             batch, channel, h, w
        for (var y = 0; y < scaledImage.getHeight(); y++) {
            for (var x = 0; x < scaledImage.getWidth(); x++) {
                int pixel = scaledImage.getRGB(x,y);

                // Get RGB values
                tensorData[0][0][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[0]) / standardDeviation[0];
                tensorData[0][1][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[1]) / standardDeviation[1];
                tensorData[0][2][y][x] = (((pixel >> 16) & 0xFF) / 255f - mean[2]) / standardDeviation[2];
            }
        }

        return tensorData;
    }
    public static BufferedImage processBufferedImageYolo(String imagePath, int h, int w) {
        try {

            // Read image
            File imageFile = new File(imagePath);
            BufferedImage image = ImageIO.read(imageFile);

            var resizedImage = pad(image,w,h, Color.WHITE);

            // Process image
            BufferedImage scaledImage = new BufferedImage(h, w, BufferedImage.TYPE_4BYTE_ABGR);
            scaledImage.getGraphics().drawImage(resizedImage, 0, 0, null);

//            File f = new File("C:/var/MyFile.jpg");
//            ImageIO.write(scaledImage, "JPEG", f);
            try {
                // retrieve image
//                BufferedImage bi = getMyImage();
                File outputfile = new File("C:/var/saved.png");
                ImageIO.write(scaledImage, "png", outputfile);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }

            return scaledImage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static BufferedImage pad(BufferedImage image, double width, double height, Color pad) {
        double ratioW = image.getWidth() / width;
        double ratioH = image.getHeight() / height;
        double newWidth = width, newHeight = height;
        int fitW = 0, fitH = 0;
        BufferedImage resultImage;
        Image resize;

        //padding width
        if (ratioW < ratioH) {
            newWidth = image.getWidth() / ratioH;
            newHeight = image.getHeight() / ratioH;
            fitW = (int) ((width - newWidth) / 2.0);

        }//padding height
        else if (ratioH < ratioW) {
            newWidth = image.getWidth() / ratioW;
            newHeight = image.getHeight() / ratioW;
            fitH = (int) ((height - newHeight) / 2.0);
        }

        resize = image.getScaledInstance((int) newWidth, (int) newHeight, Image.SCALE_SMOOTH);
        resultImage = new BufferedImage((int) width, (int) height, image.getType());
        Graphics g = resultImage.getGraphics();
        g.setColor(pad);
        g.fillRect(0, 0, (int) width, (int) height);
        g.drawImage(resize, fitW, fitH, null);
        g.dispose();

        return resultImage;
    }

    public static String getBase64EncodedImage(String imageURL) throws IOException {
        byte[] fileContent = FileUtils.readFileToByteArray(new File(imageURL));
        String encodedString = Base64.getEncoder().encodeToString(fileContent);
        return encodedString;
//        return Base64.encodeBase64String(bytes);
    }


//    public static String encodeBase64(String text){
//        if (text==null || text.isBlank()) return null;
//        String originalString = text;
//        byte[] originalBytes = originalString.getBytes();
//
//        Base64.Encoder encoder = Base64.getEncoder();
//        byte[] encodedBytes = encoder.encode(originalBytes);
//        String encodedString = new String(encodedBytes, StandardCharsets.ISO_8859_1);
//        return encodedString;
//    }

//    public static String encodeBase64XOR(String input, char key) {
//        StringBuilder sb = new StringBuilder();
//        for (char c : input.toCharArray()) {
//            sb.append((char)(c ^ key));
//        }
//        return Base64.getEncoder().encodeToString(sb.toString().getBytes());
//    }

//    public static String encodeBase64(String input, Character key) {
//        if (input==null || input.isBlank()) return null;
//        String transformed = input;
//
//        if (key != null) {
//            StringBuilder sb = new StringBuilder();
//            for (char c : input.toCharArray()) {
//                sb.append((char)(c ^ key));
//            }
//            transformed = sb.toString();
//        }
//
//        return Base64.getEncoder().encodeToString(transformed.getBytes());
//    }

    public static String encodeBase64(String input, Character key) {
        if (input == null || input.isBlank()) return null;

        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);

        if (key != null) {
            byte keyByte = (byte) key.charValue();
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ keyByte);
            }
        }

        return Base64.getEncoder().encodeToString(bytes);
    }


//    public static String minifyJS(String jsCode){
//        Reader input = new StringReader(jsCode);
//        StringWriter output = new StringWriter();
//        Minifier min = new JSMinifier(input);
//        try {
//            min.minify(output);
//        } catch (MinificationException e) {
//            // Handle exception
//        }
//        return output.toString();
//    }

//    public static String minifyJSYUI(String jsCode){
//        Reader input = new StringReader(jsCode);
//        StringWriter output = new StringWriter();
//        Minifier min = new JSMinifier(input);
//        try {
//            JavaScriptCompressor jsCompressor = new JavaScriptCompressor(input, new YuiCompressorErrorReporter());
//            jsCompressor.compress(output, -1, true, false, false, false);
//            input.close();
//            output.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return output.toString();
//    }

    public static String optimizeJs(String jsCode) {
        if (jsCode==null || jsCode.isBlank()) return null;

        return minifyJs(jsCode).trim();
    }

    private static final Pattern JS_COMMENT_PATTERN = Pattern.compile("(?<!:)//.*|/\\*[^*]*\\*+(?:[^/*][^*]*\\*+)*/");
    private static final Pattern STRING_LITERAL_PATTERN = Pattern.compile("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|`(?:\\\\.|[^`\\\\])*`");
    private static final Pattern SYMBOL_SPACE_PATTERN = Pattern.compile("[ \\t]*([{};,:=\\(\\)\\[\\]\\+\\-\\*/<>\\|&!\\?])[ \\t]*");
    private static final Pattern MULTISPACE_PATTERN = Pattern.compile("[ \\t]{2,}");
    private static final Pattern TRIM_LINE_PATTERN = Pattern.compile("(?m)^\\h+|\\h+$");
    private static final Pattern MULTILINE_BLANK_PATTERN = Pattern.compile("(?m)(\\n[ \\t]*){2,}");
    private static final Pattern RESTORE_LITERAL_PATTERN = Pattern.compile("\"__STR(\\d+)__\"");

    public static String minifyJsOld(String jsCode) {
        if (jsCode == null || jsCode.isBlank()) return "";

        // Step 1: Extract string literals FIRST to protect them
        List<String> literals = new ArrayList<>();
        Matcher literalMatcher = STRING_LITERAL_PATTERN.matcher(jsCode);
        StringBuffer placeholderBuffer = new StringBuffer();
        int index = 0;
        while (literalMatcher.find()) {
            literals.add(literalMatcher.group());
            literalMatcher.appendReplacement(placeholderBuffer, "\"__STR" + (index++) + "__\"");
        }
        literalMatcher.appendTail(placeholderBuffer);
        String codeWithoutLiterals = placeholderBuffer.toString();

        // Step 2: Remove comments (after strings are protected)
        codeWithoutLiterals = JS_COMMENT_PATTERN.matcher(codeWithoutLiterals).replaceAll("");

        // Step 3: Minify whitespace and symbols
        codeWithoutLiterals = TRIM_LINE_PATTERN.matcher(codeWithoutLiterals).replaceAll("");
        codeWithoutLiterals = SYMBOL_SPACE_PATTERN.matcher(codeWithoutLiterals).replaceAll("$1");
        codeWithoutLiterals = MULTISPACE_PATTERN.matcher(codeWithoutLiterals).replaceAll(" ");
        codeWithoutLiterals = MULTILINE_BLANK_PATTERN.matcher(codeWithoutLiterals).replaceAll("\n");

        // Step 4: Restore string literals
        Matcher restoreMatcher = RESTORE_LITERAL_PATTERN.matcher(codeWithoutLiterals);
        StringBuffer finalOutput = new StringBuffer();
        while (restoreMatcher.find()) {
            int i = Integer.parseInt(restoreMatcher.group(1));
            restoreMatcher.appendReplacement(finalOutput, Matcher.quoteReplacement(literals.get(i)));
        }
        restoreMatcher.appendTail(finalOutput);

//        System.out.println("OLD:"+finalOutput);

        return finalOutput.toString();
    }



    /// Precompile patterns for whitespace/symbol steps:
    // Remove spaces around symbols: {} ; , : = () [] + - * / < > | & ! ?
//    private static final Pattern SYMBOL_SPACE_PATTERN = Pattern.compile("\\s*([{};,:=()\\[\\]\\+\\-\\*/<>\\|&!\\?])\\s*");
    // Collapse multiple spaces into one
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile(" {2,}");

    /**
     * Main entry: minify JS code, or return empty string if null/blank.
     */
    public static String minifyJs(String jsCode) {
        if (jsCode == null || jsCode.isBlank()) return "";
        // Phase 1: remove comments while preserving string literals
        String noComments = _removeCommentsPreserveStrings(jsCode);
        // Phase 2: per-line trimming, symbol-space removal, multi-space collapse, then collapse blank lines
        return  _minifyLinesAndCollapseBlank(noComments);
    }

    /**
     * Phase 1: scan input, copy string literals verbatim, skip //comments (unless preceded by ':') and comments.
     */
    private static String _removeCommentsPreserveStrings(String code) {
        int len = code.length();
        StringBuilder out = new StringBuilder(len);
        int i = 0;
        while (i < len) {
            char c = code.charAt(i);
            // String or template literal start?
            if (c == '"' || c == '\'' || c == '`') {
                char quote = c;
                out.append(c);
                i++;
                while (i < len) {
                    char d = code.charAt(i);
                    out.append(d);
                    if (d == '\\') {
                        // escape: copy next char if any
                        i++;
                        if (i < len) {
                            out.append(code.charAt(i));
                        }
                        i++;
                        continue;
                    }
                    if (d == quote) {
                        i++;
                        break;
                    }
                    i++;
                }
                continue;
            }
            // Potential comment?
            if (c == '/' && i + 1 < len) {
                char next = code.charAt(i + 1);
                if (next == '/') {
                    // check if preceded by ':'
                    boolean isComment = true;
                    if (i > 0 && code.charAt(i - 1) == ':') {
                        isComment = false;
                    }
                    if (isComment) {
                        // skip until end-of-line, preserving newline
                        i += 2;
                        while (i < len) {
                            char d = code.charAt(i);
                            if (d == '\n' || d == '\r') {
                                break;
                            }
                            i++;
                        }
                        continue;
                    }
                    // else fall through and treat '/' normally
                } else if (next == '*') {
                    // skip /* ... */
                    i += 2;
                    while (i + 1 < len) {
                        if (code.charAt(i) == '*' && code.charAt(i + 1) == '/') {
                            i += 2;
                            break;
                        }
                        i++;
                    }
                    continue;
                }
            }
            // Normal character
            out.append(c);
            i++;
        }
        return out.toString();
    }

//    private static String _removeCommentsPreserveStrings(String code) {
//        int len = code.length();
//        StringBuilder out = new StringBuilder(len);
//        int i = 0;
//        boolean afterRegexAllowed = true;
//
//        while (i < len) {
//            char c = code.charAt(i);
//
//            // String or template literal
//            if (c == '"' || c == '\'' || c == '`') {
//                char quote = c;
//                out.append(c);
//                i++;
//                while (i < len) {
//                    char d = code.charAt(i);
//                    out.append(d);
//                    if (d == '\\') {
//                        i++;
//                        if (i < len) out.append(code.charAt(i));
//                        i++;
//                        continue;
//                    }
//                    if (d == quote) {
//                        i++;
//                        break;
//                    }
//                    i++;
//                }
//                afterRegexAllowed = false;
//                continue;
//            }
//
//            // Regex literal
//            if (c == '/' && afterRegexAllowed && i + 1 < len && code.charAt(i + 1) != '/' && code.charAt(i + 1) != '*') {
//                out.append(c);
//                i++;
//                boolean inClass = false;
//                while (i < len) {
//                    char d = code.charAt(i);
//                    out.append(d);
//                    if (d == '\\') {
//                        i++;
//                        if (i < len) out.append(code.charAt(i));
//                    } else {
//                        if (d == '[') inClass = true;
//                        else if (d == ']') inClass = false;
//                        else if (d == '/' && !inClass) {
//                            i++;
//                            // Copy any trailing flags (e.g. gmi)
//                            while (i < len && Character.isLetter(code.charAt(i))) {
//                                out.append(code.charAt(i));
//                                i++;
//                            }
//                            break;
//                        }
//                    }
//                    i++;
//                }
//                afterRegexAllowed = false;
//                continue;
//            }
//
//            // Line comment
//            if (c == '/' && i + 1 < len && code.charAt(i + 1) == '/') {
//                boolean isComment = true;
//                if (i > 0 && code.charAt(i - 1) == ':') isComment = false;
//                if (isComment) {
//                    i += 2;
//                    while (i < len && code.charAt(i) != '\n' && code.charAt(i) != '\r') i++;
//                    continue;
//                }
//            }
//
//            // Block comment
//            if (c == '/' && i + 1 < len && code.charAt(i + 1) == '*') {
//                i += 2;
//                while (i + 1 < len && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
//                i += 2;
//                continue;
//            }
//
//            // Normal character
//            out.append(c);
//            afterRegexAllowed = Character.isWhitespace(c) || "([{:;,=!?&|".indexOf(c) >= 0;
//            i++;
//        }
//        return out.toString();
//    }


    /**
     * Phase 2: split into lines, trim each line, remove spaces around symbols, collapse multiple spaces,
     * then collapse multiple blank lines into one.
     */
//    private static String _minifyLinesAndCollapseBlank(String code) {
//        // Split on any line break sequence; normalize output to '\n'
//        // Using split("\\R", -1) to keep trailing empty lines if any
//        String[] lines = code.split("\\R", -1);
//
//        StringBuilder out = new StringBuilder(code.length());
//        for (String line : lines) {
//            // Trim leading/trailing horizontal whitespace
//            String trimmed = line.trim();
//            if (trimmed.isEmpty()) {
//                // Append a blank line marker: we'll collapse runs later
//                out.append('\n');
//            } else {
//                // Remove spaces around symbols
//                String s = SYMBOL_SPACE_PATTERN.matcher(trimmed).replaceAll("$1");
//                // Collapse multiple spaces into one
//                s = MULTI_SPACE_PATTERN.matcher(s).replaceAll(" ");
//                out.append(s).append('\n');
//            }
//        }
//        // Now collapse any sequence of 2 or more '\n' into a single '\n'
//        // This collapses multiple blank lines into one.
//        // Using (?m) is optional here since we just match \n; plain regex works:
//        String result = out.toString().replaceAll("\\n{2,}", "\n");
//        return result;
//    }

    private static String _minifyLinesAndCollapseBlank(String code) {
        String[] lines = code.split("\\R", -1);
        StringBuilder out = new StringBuilder(code.length());

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                out.append('\n');
            } else if (looksLikeLiteralLine(trimmed)) {
                // Don't touch lines with regex or strings
                out.append(trimmed).append('\n');
            } else {
                // Safe to minify
                String s = SYMBOL_SPACE_PATTERN.matcher(trimmed).replaceAll("$1");
                s = MULTI_SPACE_PATTERN.matcher(s).replaceAll(" ");
                out.append(s).append('\n');
            }
        }

        return out.toString().replaceAll("\\n{2,}", "\n");
    }

    private static boolean looksLikeLiteralLine(String line) {
        return line.contains("\"") || line.contains("'") || line.contains("`") || line.matches(".*=[^=]/.*?/[gimsuy]*\\s*;?.*");
    }


//    public static String minifyJsShorter(String input) {
//        if (input == null || input.isBlank()) return "";
//
//        StringBuilder output = new StringBuilder(input.length());
//        List<String> stringLiterals = new ArrayList<>();
//        int length = input.length();
//        char[] chars = input.toCharArray();
//        int i = 0;
//        int literalIndex = 0;
//
//        // Step 1: Extract string literals and replace with placeholders
//        while (i < length) {
//            char c = chars[i];
//
//            // Detect string literals
//            if (c == '"' || c == '\'' || c == '`') {
//                char quote = c;
//                int start = i;
//                i++;
//                boolean escaped = false;
//                while (i < length) {
//                    char ch = chars[i];
//                    if (escaped) {
//                        escaped = false;
//                    } else if (ch == '\\') {
//                        escaped = true;
//                    } else if (ch == quote) {
//                        break;
//                    }
//                    i++;
//                }
//                if (i < length) i++; // skip closing quote
//                String literal = new String(chars, start, i - start);
//                stringLiterals.add(literal);
//                output.append("\"__STR").append(literalIndex++).append("__\"");
//            }
//            // Skip comments
//            else if (c == '/') {
//                if (i + 1 < length) {
//                    if (chars[i + 1] == '/') {
//                        i += 2;
//                        while (i < length && chars[i] != '\n') i++;
//                    } else if (chars[i + 1] == '*') {
//                        i += 2;
//                        while (i + 1 < length && !(chars[i] == '*' && chars[i + 1] == '/')) i++;
//                        i += 2;
//                    } else {
//                        output.append(c);
//                        i++;
//                    }
//                } else {
//                    output.append(c);
//                    i++;
//                }
//            }
//            // Copy other characters
//            else {
//                output.append(c);
//                i++;
//            }
//        }
//
//        // Step 2: Whitespace minification
//        StringBuilder cleaned = new StringBuilder(output.length());
//        String[] lines = output.toString().split("\n");
//        for (String line : lines) {
//            String trimmed = line.trim();
//            if (!trimmed.isEmpty()) {
//                int len = trimmed.length();
//                for (int j = 0; j < len; j++) {
//                    char ch = trimmed.charAt(j);
//                    if ("{};,:=()[]+-*/<>|&!?".indexOf(ch) >= 0) {
//                        // remove spaces around symbols
//                        if (cleaned.length() > 0 && cleaned.charAt(cleaned.length() - 1) == ' ') {
//                            cleaned.setLength(cleaned.length() - 1);
//                        }
//                        cleaned.append(ch);
//                        // remove next space if any
//                        if (j + 1 < len && trimmed.charAt(j + 1) == ' ') j++;
//                    } else {
//                        cleaned.append(ch);
//                    }
//                }
//                cleaned.append('\n');
//            }
//        }
//
//        // Step 3: Restore string literals
//        String result = cleaned.toString();
//        for (int idx = 0; idx < stringLiterals.size(); idx++) {
//            result = result.replace("\"__STR" + idx + "__\"", stringLiterals.get(idx));
//        }
//
//        System.out.println("JS:"+result);
//
//        return result;
//    }


//    private static final Pattern HTML_COMMENT = Pattern.compile("(?s)<!--(?!\\[if).*?-->");
//    private static final Pattern INTER_TAG_SPACE = Pattern.compile(">\\s+<");
//    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
//    public static String optimizeHtmlNow(String html) {
//        if (html == null || html.isBlank()) return null;
//
//        // 0. Temporarily extract <x-markdown> blocks
//        List<String> markdownBlocks = new ArrayList<>();
//        html = extractMarkdownBlocks(html, markdownBlocks);
//
//        String result = html;
//
//        // 1. Remove HTML comments
//        result = HTML_COMMENT.matcher(result).replaceAll("");
//        // 2. Remove whitespace between tags
//        result = INTER_TAG_SPACE.matcher(result).replaceAll("><");
//        // 3. Remove CSS comments inside <style>...</style>
//        result = removeCssCommentsInStyleTags(result);
//        // 4. Remove JS comments inside [# ... #]
//        result = removeJsCommentsInCustomTags(result);
//        // 5. Collapse multiple spaces (optional)
//        result = MULTI_SPACE.matcher(result).replaceAll(" ");
//        // 6. Restore <x-markdown> blocks
//        result = restoreMarkdownBlocks(result, markdownBlocks);
//
//        System.out.println("TRIM-NOW:"+result);
//
//        return result.trim();
//    }
//
//    private static final Pattern CSS_COMMENT_PATTERN = Pattern.compile("(?s)/\\*.*?\\*/");
//    private static final Pattern CSS_PATTERN = Pattern.compile("(?is)(<style[^>]*>)(.*?)(</style>)");
//    private static String removeCssCommentsInStyleTags(String html) {
//
//        Matcher matcher = CSS_PATTERN.matcher(html);
//        StringBuffer sb = new StringBuffer();
//        while (matcher.find()) {
//            String start = matcher.group(1);
//            String content = matcher.group(2);
//            String end = matcher.group(3);
//
//            // Use non-greedy regex to remove all /* ... */ including multiline
//            content = CSS_COMMENT_PATTERN.matcher(content).replaceAll("");
//
//            matcher.appendReplacement(sb, Matcher.quoteReplacement(start + content + end));
//        }
//        matcher.appendTail(sb);
//        return sb.toString();
//    }
//
//    private static final Pattern JS_SCRIPT_PATTERN = Pattern.compile("(?is)(\\[#)(.*?)(#\\])");
//    private static String removeJsCommentsInCustomTags(String html) {
//
//        Matcher matcher = JS_SCRIPT_PATTERN.matcher(html);
//        StringBuffer sb = new StringBuffer();
//        while (matcher.find()) {
//            String start = matcher.group(1);
//            String content = matcher.group(2);
//            String end = matcher.group(3);
//
//            content = minifyJS(content);
//            matcher.appendReplacement(sb, Matcher.quoteReplacement(start + content + end));
//        }
//        matcher.appendTail(sb);
//        return sb.toString();
//    }
//
//    private static final Pattern MARKDOWN_BLOCK_PATTERN = Pattern.compile("(?is)<x-markdown(?:\\s[^>]*)?>(.*?)</x-markdown>");
//    private static final String MARKDOWN_PLACEHOLDER = "__MARKDOWN_BLOCK__";
//
//    // Extracts and replaces <x-markdown> blocks with a placeholder
//    private static String extractMarkdownBlocks(String html, List<String> blocks) {
//        Matcher matcher = MARKDOWN_BLOCK_PATTERN.matcher(html);
//        StringBuffer sb = new StringBuffer();
//        while (matcher.find()) {
//            blocks.add(matcher.group()); // Entire <x-markdown>...</x-markdown>
//            matcher.appendReplacement(sb, MARKDOWN_PLACEHOLDER + blocks.size()); // e.g. __MARKDOWN_BLOCK__1
//        }
//        matcher.appendTail(sb);
//        return sb.toString();
//    }
//
//    // Restores the markdown content back to its original location
//    private static String restoreMarkdownBlocks(String html, List<String> blocks) {
//        for (int i = 0; i < blocks.size(); i++) {
//            html = html.replace(MARKDOWN_PLACEHOLDER + (i + 1), blocks.get(i));
//        }
//        return html;
//    }


    public static String optimizeHtml(String html) {
        if (html == null || html.isBlank()) return null;

        // Step 0: Temporarily extract <x-markdown> blocks
        List<String> markdownBlocks = new ArrayList<>();
        html = _extractMarkdownBlocksManual(html, markdownBlocks);

        // Extract JS blocks
        List<String> rawJsBlocks = new ArrayList<>();
        html = _extractJsBlocksManual(html, rawJsBlocks);

        // Step 1: Remove HTML comments (except conditional ones)
        html = _removeHtmlCommentsManual(html);

        // Step 2: Remove whitespace between tags (e.g. >   < to ><)
        html = _removeInterTagWhitespaceManual(html);

        // Step 3: Remove CSS comments in <style> tags
        html = _removeCssCommentsInStyleTagsManual(html);

        // Step 4: Remove JS comments in [# #] blocks
//        html = removeJsCommentsInCustomTagsManual(html);

        // Step 5: Collapse multiple spaces
        html = _collapseSpacesManual(html);

        // Process and restore JS blocks
        html = _restoreJsBlocksManual(html, rawJsBlocks);

        // Step 6: Restore markdown blocks
        html = _restoreMarkdownBlocksManual(html, markdownBlocks);

        return html.trim();
    }
    private static String _extractMarkdownBlocksManual(String html, List<String> storage) {
        StringBuilder result = new StringBuilder();
        int index = 0;

        while (true) {
            int start = html.indexOf("<x-markdown>", index);
            if (start == -1) {
                result.append(html.substring(index)); // append the rest
                break;
            }

            // Append everything before this markdown block
            result.append(html, index, start);

            int end = html.indexOf("</x-markdown>", start);
            if (end == -1) {
                // Invalid/unclosed, append rest and break
                result.append(html.substring(start));
                break;
            }

            end += "</x-markdown>".length(); // include closing tag
            storage.add(html.substring(start, end));
            result.append("__MARKDOWN__BLOCK_").append(storage.size() - 1).append("__");
            index = end;
        }

        return result.toString();
    }
    private static String _restoreMarkdownBlocksManual(String html, List<String> storage) {
        for (int i = 0; i < storage.size(); i++) {
            html = html.replace("__MARKDOWN__BLOCK_" + i + "__", storage.get(i));
        }
        return html;
    }
    private static String _extractJsBlocksManual(String html, List<String> storage) {
        StringBuilder result = new StringBuilder();
        int index = 0, len = html.length();
        while (index < len) {
            int start = html.indexOf("[#", index);
            if (start == -1) {
                result.append(html, index, len);
                break;
            }
            result.append(html, index, start);
            int end = html.indexOf("#]", start + 2);
            if (end == -1) {
                result.append(html.substring(start));
                break;
            }
            end += 2;
            String block = html.substring(start, end);
            storage.add(block);
            result.append("__JS_BLOCK_").append(storage.size() - 1).append("__");
            index = end;
        }
        return result.toString();
    }
    private static String _restoreJsBlocksManual(String html, List<String> storage) {
        for (int i = 0; i < storage.size(); i++) {
            String placeholder = "__JS_BLOCK_" + i + "__";
            html = html.replace(placeholder, minifyJs(storage.get(i)));
        }
        return html;
    }
    private static String _removeHtmlCommentsManual(String html) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < html.length()) {
            int start = html.indexOf("<!--", i);
            if (start == -1) {
                sb.append(html.substring(i));
                break;
            }
            sb.append(html, i, start);
            int end = html.indexOf("-->", start + 4);
            if (end == -1) break;

            if (start + 5 < html.length() && html.charAt(start + 4) == '[') {
                sb.append(html, start, end + 3); // conditional comment
            }
            i = end + 3;
        }
        return sb.toString();
    }
    private static String _removeInterTagWhitespaceManual(String html) {
        StringBuilder sb = new StringBuilder(html.length());
        int len = html.length();
        int i = 0;

        while (i < len) {
            char c = html.charAt(i);

            if (c == '>') {
                sb.append(c);
                i++;

                int temp = i;
                // Skip whitespace
                while (temp < len && Character.isWhitespace(html.charAt(temp))) {
                    temp++;
                }

                // Only remove if followed by '<' (i.e. inter-tag whitespace)
                if (temp < len && html.charAt(temp) == '<') {
                    i = temp;
                    continue; // skip appending any whitespace
                }
            }

            if (i < len) {
                sb.append(html.charAt(i));
                i++;
            }
        }

        return sb.toString();
    }
    private static String _removeCssCommentsInStyleTagsManual(String html) {
        StringBuilder sb = new StringBuilder();
        int index = 0;
        while (true) {
            int start = html.toLowerCase().indexOf("<style", index);
            if (start == -1) {
                sb.append(html.substring(index));
                break;
            }
            int tagEnd = html.indexOf('>', start);
            int close = html.toLowerCase().indexOf("</style>", tagEnd);
            if (close == -1) break;

            sb.append(html, index, tagEnd + 1);
            String styleContent = html.substring(tagEnd + 1, close);
            sb.append(styleContent.replaceAll("/\\*.*?\\*/", ""));
            sb.append("</style>");
            index = close + 8;
        }
        return sb.toString();
    }
    private static String _collapseSpacesManual(String html) {
        StringBuilder sb = new StringBuilder();
        boolean inSpace = false;
        for (char c : html.toCharArray()) {
            if (Character.isWhitespace(c)) {
                if (!inSpace) {
                    sb.append(' ');
                    inSpace = true;
                }
            } else {
                sb.append(c);
                inSpace = false;
            }
        }
        return sb.toString();
    }






//    public static final String SHA_CRYPT = "SHA-256";
//    public static final String AES_ALGORITHM = "AES";
//    public static final String AES_ALGORITHM_GCM = "AES/GCM/NoPadding";
//    public static final Integer IV_LENGTH_ENCRYPT = 12;
//    public static final Integer TAG_LENGTH_ENCRYPT = 16;
//    public static final String LOCAL_PASSPHRASE = "mySecurePassphrase123!"; // Store securely



//    public static String localEncrypt(String plainText) {
//        if (plainText==null || plainText.isBlank()) return null;
//        byte[] combinedIvAndCipherText = null;
//
//        try {
//
//            byte[] iv = new byte[IV_LENGTH_ENCRYPT];
//            SecureRandom secureRandom = new SecureRandom();
//            secureRandom.nextBytes(iv);
//
//            SecretKeySpec aesKey = generateAesKeyFromPassphrase();
//
//            Cipher cipher = Cipher.getInstance(AES_ALGORITHM_GCM);
//            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_ENCRYPT * 8, iv);
//            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
//
//            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
//
//            combinedIvAndCipherText = new byte[iv.length + encryptedBytes.length];
//            System.arraycopy(iv, 0, combinedIvAndCipherText, 0, iv.length);
//            System.arraycopy(encryptedBytes, 0, combinedIvAndCipherText, iv.length, encryptedBytes.length);
//
//        }catch(Exception e){}
//        return Base64.getEncoder().encodeToString(combinedIvAndCipherText);
//    }
//
//    public static String localDecrypt(String cipherText) throws Exception {
//        byte[] decodedCipherText = Base64.getDecoder().decode(cipherText);
//
//        SecretKeySpec aesKey = generateAesKeyFromPassphrase();
//
//        byte[] iv = new byte[IV_LENGTH_ENCRYPT];
//        System.arraycopy(decodedCipherText, 0, iv, 0, iv.length);
//        byte[] encryptedText = new byte[decodedCipherText.length - IV_LENGTH_ENCRYPT];
//        System.arraycopy(decodedCipherText, IV_LENGTH_ENCRYPT, encryptedText, 0, encryptedText.length);
//
//        GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH_ENCRYPT * 8, iv);
//        Cipher cipher = Cipher.getInstance(AES_ALGORITHM_GCM);
//        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);
//
//        byte[] decryptedBytes = cipher.doFinal(encryptedText);
//
//        return new String(decryptedBytes, StandardCharsets.UTF_8);
//    }

//    private static SecretKeySpec generateAesKeyFromPassphrase() throws Exception {
//        MessageDigest sha256 = MessageDigest.getInstance(SHA_CRYPT);
//        byte[] keyBytes = sha256.digest(LOCAL_PASSPHRASE.getBytes(StandardCharsets.UTF_8));
//        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
//    }

//    private static class YuiCompressorErrorReporter implements org.mozilla.javascript.ErrorReporter {
//        public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
//            if (line < 0) {
//                System.out.println(message);
//            } else {
//                System.out.println(line + ':' + lineOffset + ':' + message);
//            }
//        }
//        public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
//            if (line < 0) {
//                System.out.println(message);
//            } else {
//                System.out.println(line + ':' + lineOffset + ':' + message);
//            }
//        }
//
//        public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
//            error(message, sourceName, line, lineSource, lineOffset);
//            return new EvaluatorException(message);
//        }
//    }

    public static String compressString(String input) {
        String output = "";
        if(input == null || input.isEmpty()){
            return "";
        }
        try{
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream)) {
                gzipOutputStream.write(input.getBytes(StandardCharsets.UTF_8));
            }
//            output = URLEncoder.encode(outputStream.toString("ISO-8859-1"), "UTF-8");;// Base64.getEncoder().encodeToString(outputStream.toByteArray());
//            output = outputStream.toString("UTF-8");// Base64.getEncoder().encodeToString(outputStream.toByteArray());
            output = Base64.getEncoder().encodeToString(outputStream.toByteArray());

        }catch (IOException e){

        }
        return output;
    }


    public static ValidationResult validateJson(String schemaString, JsonNode jsonNode) {
        try {
            SchemaValidatorsConfig schemaValidatorsConfig = new SchemaValidatorsConfig ();
            schemaValidatorsConfig.setHandleNullableField(true);
            schemaValidatorsConfig.setTypeLoose(false);

            JsonNode schemaNode = mapper.readTree(schemaString);
            JsonSchemaFactory factory = JsonSchemaFactory
                    .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012))
                    .objectMapper(mapper)
                    .build();

            JsonSchema schema = factory.getSchema(schemaNode,schemaValidatorsConfig);
            Set<ValidationMessage> errors = schema.validate(jsonNode);

//            System.out.println("ERRORS!!!!!!!!"+errors.size());

            return new ValidationResult(errors.isEmpty(), errors);
        } catch (Exception e) {
            return new ValidationResult(false, Set.of(
                    new ValidationMessage.Builder()
                            .customMessage("Schema processing error: " + e.getMessage())
                            .build()
            ));
        }
    }


    public record ValidationResult(boolean valid, Set<ValidationMessage> errors) {
        public String errorMessagesAsString() {
            return errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.joining("; "));
        }
    }


//    public static ObjectNode filterJsonNode(JsonNode original, Collection<String> allowedFields) {
//        if (!(original instanceof ObjectNode)) return JsonNodeFactory.instance.objectNode();
//
//        ObjectNode filtered = JsonNodeFactory.instance.objectNode();
//        ObjectNode obj = (ObjectNode) original;
//
//        Set<String> allowed = new HashSet<>(allowedFields);
//        Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
//
//        while (fields.hasNext()) {
//            Map.Entry<String, JsonNode> entry = fields.next();
//            if (allowed.contains(entry.getKey())) {
//                filtered.set(entry.getKey(), entry.getValue().deepCopy());
//            }
//        }
//
//        return filtered;
//    }

    public static ObjectNode filterJsonNode(JsonNode original, Set<String> allowedFields) {
        if (!(original instanceof ObjectNode obj)) return JsonNodeFactory.instance.objectNode();

        ObjectNode filtered = JsonNodeFactory.instance.objectNode();
        obj.fields().forEachRemaining(entry -> {
            if (allowedFields.contains(entry.getKey())) {
                filtered.set(entry.getKey(), entry.getValue()); // no deepCopy needed
            }
        });
        return filtered;
    }




    private static final Map<Set<String>, Pattern> patternCache = new HashMap<>();

    public static Map<String, Set<String>> extractVariables(Set<String> prefixes, String input) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (prefixes == null || prefixes.isEmpty() || input == null || input.isEmpty()) {
            return result;
        }

        for (String prefix : prefixes) {
            result.put(prefix, new LinkedHashSet<>());
        }

        Pattern pattern = patternCache.computeIfAbsent(prefixes, Helper::compilePattern);
        Matcher matcher = pattern.matcher(input);

//        matcher.results().forEach(m ->
//                result.computeIfAbsent(m.group(1), k -> new LinkedHashSet<>())
//                        .add(m.group(2))
//        );

        while (matcher.find()) {
            String prefix = matcher.group(1); // $, $prev$, $_
            String variable = matcher.group(2); // name, id, profile, etc.

            result.get(prefix).add(variable);
        }

        return result;
    }

    private static Pattern compilePattern(Set<String> prefixes) {
        List<String> sorted = prefixes.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .map(Pattern::quote)
                .toList();

        // Match prefix + "." + variable name
        String prefixGroup = String.join("|", sorted);
//        String regex = "(" + prefixGroup + ")\\.([a-zA-Z0-9_]+)";
        String regex = "(" + prefixGroup + ")\\.([$a-zA-Z_][a-zA-Z0-9_]*)";

        return Pattern.compile(regex);
    }

    public static void addIfNonNull(Collection<String> target, String... values) {
        for (String val : values) {
            if (val != null && !val.isBlank()) { // optional: skip blanks too
                target.add(val);
            }
        }
    }

    public static List<String> parseCSV(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder currentPart = new StringBuilder();

        boolean withinQuotes = false;
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);

            if (ch == '"') {
                withinQuotes = !withinQuotes;
            } else if (ch == ',' && !withinQuotes) {
                parts.add(currentPart.toString().trim());
                currentPart.setLength(0); // reset StringBuilder
            } else {
                currentPart.append(ch);
            }
        }

        // Push the last part after the loop ends
        parts.add(currentPart.toString().trim());

        return parts;
    }


}
