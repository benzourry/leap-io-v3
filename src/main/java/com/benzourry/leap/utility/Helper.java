package com.benzourry.leap.utility;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.benzourry.leap.config.Constant;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.itextpdf.styledxmlparser.jsoup.Jsoup;
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
//import org.jsoup.Jsoup;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

//import static org.bytedeco.leptonica.global.lept.pixDestroy;
//import static org.bytedeco.leptonica.global.lept.pixRead;

import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixRead;

public class Helper {


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
        if (org.springframework.util.StringUtils.hasText(bearerToken) && bearerToken.startsWith("ApiKey ")) {
            return bearerToken.substring(7);
        }else if(request.getParameter("api_key")!=null){ //  && "local".equals(request.getParameter("provider")) && request.getParameter("provider")==null && "local".equals(request.getParameter("provider"))
            return request.getParameter("api_key");
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
}
