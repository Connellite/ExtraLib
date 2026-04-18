package io.github.connellite.image;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Reduces noise by combining several aligned images of the same scene: for each pixel and
 * each RGB band, samples near the median (within {@code difference}) are averaged; if too few
 * samples fall in that window, the mean of all samples is used instead.
 */
public class Denoise {

    private static final int RGB_BANDS = 3;

    private final File[] inputFiles;
    private final File outputFile;
    private final int difference;

    /**
     * @param inputFiles one or more aligned input images (same dimensions)
     * @param outputFile destination image file (format from extension, see {@link ImageHelper#drawImg})
     * @param difference half-width of the median band (inclusive) per sample value
     */
    public Denoise(File[] inputFiles, File outputFile, int difference) {
        this.inputFiles = inputFiles;
        this.outputFile = outputFile;
        this.difference = difference;
    }

    /**
     * Same as {@link #Denoise(File[], File, int)} with a single source file.
     *
     * @param inputFile  source image
     * @param outputFile destination image file
     * @param difference half-width of the median band (inclusive)
     */
    public Denoise(File inputFile, File outputFile, int difference) {
        this.inputFiles = new File[]{inputFile};
        this.outputFile = outputFile;
        this.difference = difference;
    }

    /**
     * Loads all inputs, builds a denoised RGB raster, and writes {@code outputFile}.
     *
     * @throws IOException if a stream cannot be opened, no reader exists, dimensions mismatch, or write fails
     */
    public void go() throws IOException {
        Raster[] rasters = new Raster[inputFiles.length];

        for (int i = 0; i < inputFiles.length; i++) {
            try (ImageInputStream is = ImageIO.createImageInputStream(inputFiles[i])) {
                if (is == null) {
                    throw new IOException("Cannot open image stream: " + inputFiles[i]);
                }
                Iterator<ImageReader> readers = ImageIO.getImageReaders(is);
                if (!readers.hasNext()) {
                    throw new IOException("No ImageIO reader for: " + inputFiles[i]);
                }
                ImageReader reader = readers.next();
                try {
                    reader.setInput(is);
                    if (reader.canReadRaster()) {
                        rasters[i] = reader.readRaster(0, null);
                    } else {
                        rasters[i] = reader.readAsRenderedImage(0, null).getData();
                    }
                } finally {
                    reader.dispose();
                }
            }
        }

        int width = rasters[0].getWidth();
        int height = rasters[0].getHeight();
        WritableRaster outputRaster = rasters[0].createCompatibleWritableRaster();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int[] color = new int[RGB_BANDS];
                for (int band = 0; band < RGB_BANDS; band++) {
                    int[] data = new int[rasters.length];
                    for (int imageNum = 0; imageNum < rasters.length; imageNum++) {
                        try {
                            data[imageNum] = rasters[imageNum].getSample(x, y, band);
                        } catch (ArrayIndexOutOfBoundsException ignored) {
                            // fewer bands than RGB on this raster
                        }
                    }
                    color[band] = average(data, difference);
                }
                outputRaster.setPixel(x, y, color);
            }
        }

        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        output.setData(outputRaster);
        ImageHelper.drawImg(output, outputFile);
    }

    /**
     * Median-centered trimmed mean: average samples in {@code [median - difference, median + difference]};
     * if at most one sample lies in that range, returns the arithmetic mean of all values.
     */
    private static int average(int[] data, int difference) {
        int n = data.length;
        Arrays.sort(data);
        int median;
        if (n % 2 == 0) {
            median = (data[n / 2 - 1] + data[n / 2]) / 2;
        } else {
            median = data[n / 2];
        }
        int min = median - difference;
        int max = median + difference;
        int sum = 0;
        int count = 0;
        for (int v : data) {
            if (v >= min && v <= max) {
                sum += v;
                count++;
            }
        }
        if (count <= 1) {
            sum = 0;
            for (int v : data) {
                sum += v;
            }
            return sum / n;
        }
        return sum / count;
    }
}
