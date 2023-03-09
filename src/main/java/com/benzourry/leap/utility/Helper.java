package com.benzourry.leap.utility;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageConfig;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.hibernate.internal.util.SerializationHelper;
import org.springframework.http.MediaType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

public class Helper {


    public static byte[] generateQRCode(String text, int width, int height) throws WriterException, IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BitMatrix matrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, width, height,Map.of(EncodeHintType.MARGIN,2));
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
}
