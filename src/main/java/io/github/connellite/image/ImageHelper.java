package io.github.connellite.image;

import lombok.experimental.UtilityClass;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.LookupOp;
import java.awt.image.ShortLookupTable;
import java.awt.image.WritableRaster;

@UtilityClass
public class ImageHelper {
    private static final short[] invertTable;

    static {
        invertTable = new short[256];
        for (int i = 0; i < 256; i++) {
            invertTable[i] = (short) (255 - i);
        }
    }

    /**
     * Reads a {@link BufferedImage} from the given file.
     *
     * @param file image file
     * @return loaded image, or {@code null} if no suitable reader is found
     * @throws IOException if an error occurs during reading
     */
    public static BufferedImage readImage(File file) throws IOException {
        return ImageIO.read(file);
    }

    /**
     * Returns the percentage of identical pixels between two images of the same width and height.
     * If dimensions differ, returns {@code 0.0}.
     *
     * @param bufImg1 first image
     * @param bufImg2 second image
     * @return similarity in percent ({@code 0.0}–{@code 100.0}), or {@code 0.0} when sizes differ
     */
    public static double getSimilarImage(BufferedImage bufImg1, BufferedImage bufImg2) {
        double result = 0.0;
        int w1 = bufImg1.getWidth();
        int h1 = bufImg1.getHeight();
        int w2 = bufImg2.getWidth();
        int h2 = bufImg2.getHeight();
        if (w1 == w2 && h1 == h2) {
            int[] p1 = bufImg1.getRGB(0, 0, w1, h1, null, 0, w1);
            int[] p2 = bufImg2.getRGB(0, 0, w1, h1, null, 0, w1);
            int len = p1.length;
            if (len == 0) {
                return 0.0;
            }
            for (int i = 0; i < len; i++) {
                if (p1[i] == p2[i]) {
                    result++;
                }
            }
            return result / len * 100.0;
        }
        return result;
    }

    /**
     * A simple method to convert an image to binary or B/W image.
     *
     * @param image input image
     * @return a monochrome image
     */
    public static BufferedImage convertImageToBinary(BufferedImage image) {
        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2 = tmp.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return tmp;
    }

    /**
     * Removes alpha channel from image
     *
     * @param image input image
     * @return image with alpha channel removed
     */
    public static BufferedImage removeAlphaChannel(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }

        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = tmp.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, tmp.getWidth(), tmp.getHeight());
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return tmp;
    }

    /**
     * A simple method to convert an image to gray scale.
     *
     * @param image input image
     * @return a monochrome image
     */
    public static BufferedImage convertImageToGrayscale(BufferedImage image) {
        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2 = tmp.createGraphics();
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return tmp;
    }

    /**
     * Inverts image color.
     *
     * @param image input image
     * @return an inverted-color image
     */
    public static BufferedImage invertImageColor(BufferedImage image) {
        BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        BufferedImageOp invertOp = new LookupOp(new ShortLookupTable(0, invertTable), null);
        return invertOp.filter(image, tmp);
    }

    /**
     * Rotates an image.
     *
     * @param image the original image
     * @param angle the degree of rotation
     * @return a rotated image
     */
    public static BufferedImage rotateImage(BufferedImage image, double angle) {
        double theta = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(theta));
        double cos = Math.abs(Math.cos(theta));
        int w = image.getWidth();
        int h = image.getHeight();
        int newW = (int) Math.floor(w * cos + h * sin);
        int newH = (int) Math.floor(h * cos + w * sin);

        BufferedImage tmp = new BufferedImage(newW, newH, image.getType());
        Graphics2D g2d = tmp.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.translate((newW - w) / 2, (newH - h) / 2);
        g2d.rotate(theta, (double) w / 2, (double) h / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return tmp;
    }

    /**
     * Clones an image.
     * <a href="http://stackoverflow.com/questions/3514158/how-do-you-clone-a-bufferedimage">Clones an image</a>
     *
     * @param bi input image
     * @return cloned image
     */
    public static BufferedImage cloneImage(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    /**
     * Create a new buffered image with the same characteristics (color model,
     * raster type, properties...) than the specified one.
     *
     * @param width the width
     * @param height the height
     * @param image an image with the same characteristics than the one which
     * will be created.
     */
    public static BufferedImage createBufferedImage(int width, int height, BufferedImage image) {
        Hashtable<String, Object> properties = null;
        String[] propertyNames = image.getPropertyNames();
        if (propertyNames != null) {
            properties = new Hashtable<>(propertyNames.length);
            for (String propertyName : propertyNames) {
                properties.put(propertyName, image.getProperty(propertyName));
            }
        }
        return new BufferedImage(
                image.getColorModel(),
                image.getRaster().createCompatibleWritableRaster(width, height),
                image.isAlphaPremultiplied(),
                properties);
    }

    /**
     * Writes {@code image} to {@code file} using an {@link ImageWriter} chosen from the file extension.
     * When the writer supports compression, uses explicit mode with quality {@code 0.1}.
     * Does nothing if the extension is missing, no writer is found, or an image output stream cannot be created.
     *
     * @param image image to write
     * @param file  destination file (format is inferred from the filename extension)
     * @throws IOException if an error occurs during writing
     */
    public static void drawImg(BufferedImage image, File file) throws IOException {
        String formatName = formatNameFromFile(file);
        if (formatName.isEmpty()) {
            return;
        }
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        Iterator<ImageWriter> iter = ImageIO.getImageWriters(type, formatName);
        if (!iter.hasNext()) {
            return;
        }
        ImageWriter writer = iter.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(file)) {
            if (outputStream == null) {
                return;
            }
            IIOImage iioImage = new IIOImage(image, null, null);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.1f);
            }
            writer.setOutput(outputStream);
            writer.write(null, iioImage, param);
        } finally {
            writer.dispose();
        }
    }

    private static String formatNameFromFile(File file) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.ROOT);
        if ("jpg".equals(ext)) {
            return "jpeg";
        }
        return ext;
    }
}
