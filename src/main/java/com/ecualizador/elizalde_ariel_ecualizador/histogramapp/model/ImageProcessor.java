package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class ImageProcessor {

    private final HistogramAnalyzer histogramAnalyzer = new HistogramAnalyzer();

    public Image convertToGrayscale(Image image) {
        validateImage(image);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage result = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                double gray = 0.299 * color.getRed()
                        + 0.587 * color.getGreen()
                        + 0.114 * color.getBlue();
                writer.setColor(x, y, new Color(gray, gray, gray, color.getOpacity()));
            }
        }
        return result;
    }

    public Image equalize(Image image) {
        validateImage(image);
        Image grayImage = convertToGrayscale(image);
        int[] histogram = histogramAnalyzer.calculateHistogram(grayImage);
        int totalPixels = (int) (grayImage.getWidth() * grayImage.getHeight());

        int[] cumulative = new int[256];
        cumulative[0] = histogram[0];
        for (int i = 1; i < cumulative.length; i++) {
            cumulative[i] = cumulative[i - 1] + histogram[i];
        }

        int firstNonZero = 0;
        while (firstNonZero < 256 && histogram[firstNonZero] == 0) {
            firstNonZero++;
        }
        if (firstNonZero == 256 || totalPixels == cumulative[firstNonZero]) {
            return copyImage(grayImage);
        }

        int width = (int) grayImage.getWidth();
        int height = (int) grayImage.getHeight();
        PixelReader reader = grayImage.getPixelReader();
        WritableImage result = new WritableImage(width, height);
        PixelWriter writer = result.getPixelWriter();
        int minimumCumulative = cumulative[firstNonZero];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int oldValue = (int) Math.round(reader.getColor(x, y).getRed() * 255);
                double normalized = (double) (cumulative[oldValue] - minimumCumulative)
                        / (totalPixels - minimumCumulative);
                double newValue = Math.max(0, Math.min(1, normalized));
                writer.setColor(x, y, Color.gray(newValue));
            }
        }
        return result;
    }

    public Image applyIntensitySlice(Image image, int minimum, int maximum, boolean inverted) {
        validateImage(image);
        if (minimum < 0 || maximum > 255 || minimum > maximum) {
            throw new IllegalArgumentException("El rango debe estar entre 0 y 255.");
        }

        Image grayImage = convertToGrayscale(image);
        int width = (int) grayImage.getWidth();
        int height = (int) grayImage.getHeight();
        PixelReader reader = grayImage.getPixelReader();
        WritableImage result = new WritableImage(width, height);
        PixelWriter writer = result.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int intensity = (int) Math.round(reader.getColor(x, y).getRed() * 255);
                boolean insideRange = intensity >= minimum && intensity <= maximum;
                boolean whitePixel = inverted ? !insideRange : insideRange;
                writer.setColor(x, y, whitePixel ? Color.WHITE : Color.BLACK);
            }
        }
        return result;
    }

    private Image copyImage(Image image) {
        return new WritableImage(image.getPixelReader(), (int) image.getWidth(), (int) image.getHeight());
    }

    private void validateImage(Image image) {
        if (image == null || image.isError() || image.getPixelReader() == null) {
            throw new IllegalArgumentException("La imagen no es válida.");
        }
    }
}
