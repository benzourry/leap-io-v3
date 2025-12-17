package com.benzourry.leap.utility;

import com.benzourry.leap.config.Constant;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
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
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.hibernate.internal.util.SerializationHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixRead;

public class Helper {

    private static final Logger logger = LoggerFactory.getLogger(Helper.class);

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> void writeWithCsvBeanWriter(
            Writer writer,
            List<T> list,
            CellProcessor[] processors,
            String[] headers
    ) throws IOException {

        try (ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, CsvPreference.STANDARD_PREFERENCE)) {

            // Write the header
            beanWriter.writeHeader(headers);

            // Write the beans
            for (T bean : list) {
                beanWriter.write(bean, headers, processors);
            }
        }
    }


    private static final FieldRenderer FIELD_RENDERER = new FieldRenderer();

    public static String compileTpl(String text, Map<String, Object> obj) {
        String escapedText = escapeJsonBraces(text);

        ST content = new ST(rewriteTemplate(escapedText), '{', '}');
        obj.forEach(content::add);
        content.groupThatCreatedThisInstance.registerRenderer(Object.class, FIELD_RENDERER);

        String rendered = content.render();
        return unescapeJsonBraces(rendered);
    }

    private static String escapeJsonBraces(String text) {
        // Escape single { or } that are NOT part of {{ or }}
        // ⦃ and ⦄ are temporary placeholders that won't interfere with ST
        return text
                .replaceAll("(?<!\\{)\\{(?!\\{)", "⦃")  // Replace { not followed by another {
                .replaceAll("(?<!\\})\\}(?!\\})", "⦄"); // Replace } not preceded by another }
    }

    private static String unescapeJsonBraces(String text) {
        return text.replace("⦃", "{").replace("⦄", "}");
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

//    public static String rewriteTemplate(String str) {
//        if (str != null) {
//            str = str.replace("$$_", "approval_");
//            str = str.replace("$$", "approval");
//            str = str.replace("$uiUri$", "uiUri");
//            str = str.replace("$approval$", "approval");
//            str = str.replace("$viewUri$", "viewUri");
//            str = str.replace("$editUri$", "editUri");
//            str = str.replace("$tier$", "tier");
//            str = str.replace("$prev$.$code", "prev_code");
//            str = str.replace("$prev$.$id", "prev_id");
//            str = str.replace("$prev$.$counter", "prev_counter");
//            str = str.replace("$conf$", "conf"); // just to allow presetFilter with $conf$ dont throw error because of succcessive replace of '$'. Normally it will become $$confdata.category$
//            str = str.replace("$prev$", "prev");
//            str = str.replace("$user$", "user");
//            str = str.replace("$param$", "param");
//            str = str.replace("$_", "_");
//            str = str.replace("$.$code", "code");
//            str = str.replace("$.$id", "id");
//            str = str.replace("$.$counter", "counter");
//            str = str.replace("$.", "data.");
//            str = str.replace("{{", "{");
//            str = str.replace("}}", "}");
//        }
//        return str;
//    }

    /// Higher performance to rewrite template
    public static String rewriteTemplate(String str) {
        if (str == null || str.isEmpty()) return str;

        // Ordered replacements
        String[][] rules = {
                {"$$_",        "approval_"},
                {"$$",         "approval"},
                {"$uiUri$",    "uiUri"},
                {"$approval$", "approval"},
                {"$viewUri$",  "viewUri"},
                {"$editUri$",  "editUri"},
                {"$tier$",     "tier"},
                {"$prev$.$code",    "prev_code"},
                {"$prev$.$id",      "prev_id"},
                {"$prev$.$counter", "prev_counter"},
                {"$conf$",     "conf"},
                {"$prev$",     "prev"},
                {"$user$",     "user"},
                {"$param$",    "param"},
                {"$_",         "_"},
                {"$.$code",    "code"},
                {"$.$id",      "id"},
                {"$.$counter", "counter"},
                {"$.",         "data."},
                {"{{",         "{"},
                {"}}",         "}"}
        };

        StringBuilder sb = new StringBuilder(str.length());

        outer:
        for (int i = 0; i < str.length();) {

            for (String[] rule : rules) {
                String key = rule[0];
                String value = rule[1];

                if (str.startsWith(key, i)) {
                    sb.append(value);
                    i += key.length();
                    continue outer;
                }
            }

            // No rule matched → copy char
            sb.append(str.charAt(i++));
        }

        return sb.toString();
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
                }

                // Open input image with leptonica library
                PIX image = pixRead(filePath);
                api.SetImage(image);
                // Get OCR result
                outText = api.GetUTF8Text();
                if (outText!=null) {
                    rText = outText.getString();
                }
                logger.info("OCR output:\n" + rText);

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

    public static boolean dateBetween(Date d1, Date from, Date to) {
        if (from != null && to != null) {
            return d1.after(from) && d1.before(to);
        }
        if (from != null) {
            return d1.after(from);
        }
        if (to != null) {
            return d1.before(to);
        }
        return true;
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


    public static List<String> extractURLFromText(String text) {
        List<String> urls = new ArrayList<>();
        String regex = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)";
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    public static String escapePlaceholders(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // Replace all {{ ... }} with {{ '{{...}}' }}
        return input.replaceAll("\\{\\{([^}]+)\\}\\}", "{{ '{{$1}}' }}");
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
            BufferedImage image = ImageIO.read(new File(imagePath));

            // Pad to required YOLO size
            BufferedImage resizedImage = pad(image, w, h, Color.WHITE);

            // Ensure output type matches YOLO/your model
            BufferedImage scaledImage = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = scaledImage.createGraphics();
            g.drawImage(resizedImage, 0, 0, null);
            g.dispose();

            // No file writing
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

            JsonNode schemaNode = MAPPER.readTree(schemaString);
            JsonSchemaFactory factory = JsonSchemaFactory
                    .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012))
                    .objectMapper(MAPPER)
                    .build();

            JsonSchema schema = factory.getSchema(schemaNode,schemaValidatorsConfig);
            Set<ValidationMessage> errors = schema.validate(jsonNode);

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
    private static final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public static Map<String, Set<String>> extractVariables(Set<String> prefixes, String input) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (prefixes == null || prefixes.isEmpty() || input == null || input.isEmpty()) {
            return result;
        }

        // Prepare result with empty hashset
        for (String prefix : prefixes) {
            result.put(prefix, new LinkedHashSet<>());
        }

        List<String> sorted = prefixes.stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .map(Pattern::quote) // since this will be passed to compilePattern
                .toList();

        // Create deterministic string key (sorted to ensure consistency)
        String cacheKey = String.join("|", sorted);

        Pattern pattern = patternCache.computeIfAbsent(cacheKey, key -> compilePattern(sorted));
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String prefix = matcher.group(1); // $, $prev$, $_
            String variable = matcher.group(2); // name, id, profile, etc.

            result.get(prefix).add(variable);
        }

        return result;
    }

    private static Pattern compilePattern(List<String> sorted) {
        String prefixGroup = String.join("|", sorted);
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

    public static JsonNode parseJson(String json) {
        if (json == null) return null;
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static <K, V> Map<K, V> parseJsonMap(String json, Class<K> keyClass, Class<V> valueClass) {
        if (json == null) return Map.of();
        try {
            JavaType type = MAPPER.getTypeFactory().constructMapType(Map.class, keyClass, valueClass);
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
