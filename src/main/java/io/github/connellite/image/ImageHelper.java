package io.github.connellite.image;

import lombok.experimental.UtilityClass;

import java.awt.*;
import java.awt.image.BufferedImage;
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
}
