package com.benzourry.leap.utility;//package com.benzourry.leap.utility;
//
//import java.io.*;
//import java.util.*;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.awt.geom.Rectangle2D;
//import javax.imageio.ImageIO;
//import java.awt.font.FontRenderContext;
//import java.util.List;
//
//public class BoundingBoxProcessing {
//
//    public static float[][][] getAnchors(String anchorsPath, boolean tiny) throws IOException {
//        // Load anchors from a file
////        BufferedReader reader = new BufferedReader(new FileReader(anchorsPath));
//        String line = "12,16, 19,36, 40,28, 36,75, 76,55, 72,146, 142,110, 192,243, 459,401"
////        reader.close();
//
//        String[] anchorStrs = line.split(",");
//        float[][][] anchors = new float[3][3][2];
//        int idx = 0;
//        for (int i = 0; i < 3; i++) {
//            for (int j = 0; j < 3; j++) {
//                anchors[i][j][0] = Float.parseFloat(anchorStrs[idx++]);
//                anchors[i][j][1] = Float.parseFloat(anchorStrs[idx++]);
//            }
//        }
//        return anchors;
//    }
//
//    public static float[][] postprocessBbbox(float[][][][][] pred_bbox, float[][][] anchors, int[] strides, float[] xyScale) {
//        for (int i = 0; i < pred_bbox.length; i++) {
//            float[][][][] pred = pred_bbox[i];
//            int outputSize = pred.length;
//            float[][][] xy_grid = new float[outputSize][outputSize][2];
//
//            for (int y = 0; y < outputSize; y++) {
//                for (int x = 0; x < outputSize; x++) {
//                    xy_grid[y][x][0] = x;
//                    xy_grid[y][x][1] = y;
//                }
//            }
//
//            for (int y = 0; y < outputSize; y++) {
//                for (int x = 0; x < outputSize; x++) {
//                    for (int a = 0; a < 3; a++) {
//                        float[] dxdy = pred[y][x][a][0];
//                        float[] dwdh = pred[y][x][a][1];
//                        float[] xy = new float[2];
//                        xy[0] = ((sigmoid(dxdy[0]) * xyScale[i]) - 0.5f * (xyScale[i] - 1) + xy_grid[y][x][0]) * strides[i];
//                        xy[1] = ((sigmoid(dxdy[1]) * xyScale[i]) - 0.5f * (xyScale[i] - 1) + xy_grid[y][x][1]) * strides[i];
//                        float[] wh = new float[2];
//                        wh[0] = (float) Math.exp(dwdh[0]) * anchors[i][a][0];
//                        wh[1] = (float) Math.exp(dwdh[1]) * anchors[i][a][1];
//                        pred[y][x][a][0] = xy;
//                        pred[y][x][a][1] = wh;
//                    }
//                }
//            }
//        }
//
//        List<float[]> reshapedPredBbox = new ArrayList<>();
//        for (float[][][][] pred : pred_bbox) {
//            for (float[][][] p : pred) {
//                for (float[][] b : p) {
//                    reshapedPredBbox.add(b[0]);
//                }
//            }
//        }
//
//        float[][] finalPredBbox = new float[reshapedPredBbox.size()][];
//        reshapedPredBbox.toArray(finalPredBbox);
//        return finalPredBbox;
//    }
//
//    public static float sigmoid(float x) {
//        return (float) (1 / (1 + Math.exp(-x)));
//    }
//
//    public static void drawBbox(BufferedImage image, float[][] bboxes, Map<Integer, String> classes, boolean showLabel) {
//        Graphics2D g = image.createGraphics();
//        g.setStroke(new BasicStroke(2));
//        Random rand = new Random(0);
//
//        for (float[] bbox : bboxes) {
//            int xMin = (int) bbox[0];
//            int yMin = (int) bbox[1];
//            int xMax = (int) bbox[2];
//            int yMax = (int) bbox[3];
//            float score = bbox[4];
//            int classIdx = (int) bbox[5];
//
//            Color color = new Color(rand.nextInt(255), rand.nextInt(255), rand.nextInt(255));
//            g.setColor(color);
//            g.drawRect(xMin, yMin, xMax - xMin, yMax - yMin);
//
//            if (showLabel) {
//                String label = String.format("%s: %.2f", classes.get(classIdx), score);
//                FontMetrics fm = g.getFontMetrics();
//                Rectangle2D rect = fm.getStringBounds(label, g);
//                g.fillRect(xMin, yMin - fm.getHeight(), (int) rect.getWidth(), (int) rect.getHeight());
//                g.setColor(Color.BLACK);
//                g.drawString(label, xMin, yMin);
//            }
//        }
//
//        g.dispose();
//    }
//
//    public static void main(String[] args) throws IOException {
//        // Example usage of the above functions
//        float[][][] anchors = getAnchors("anchors.txt", false);
//        // Dummy values for test
//        float[][][][][] pred_bbox = new float[1][1][1][3][2]; // Shape: [batch, height, width, anchors, values]
//        int[] strides = {8, 16, 32};
//        float[] xyScale = {1.0f, 1.0f, 1.0f};
//
//        // Perform postprocessing of bounding boxes
//        float[][] processedBboxes = postprocessBbbox(pred_bbox, anchors, strides, xyScale);
//
//        // Draw bounding boxes on an image
//        BufferedImage image = ImageIO.read(new File("image.jpg"));
//        Map<Integer, String> classes = new HashMap<>();
//        classes.put(0, "class_name");  // Example class names
//        drawBbox(image, processedBboxes, classes, true);
//
//        // Save the processed image
//        ImageIO.write(image, "jpg", new File("output.jpg"));
//    }
//}
