package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class HistogramAnalyzer {

    public int[] calculateHistogram(Image image) {
        if (image == null) {
            throw new IllegalArgumentException("La imagen no puede ser nula.");
        }

        int[] histogram = new int[256];
        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                int intensity = grayscaleValue(color);
                histogram[intensity]++;
            }
        }
        return histogram;
    }

    private int grayscaleValue(Color color) {
        double luminance = 0.299 * color.getRed()
                + 0.587 * color.getGreen()
                + 0.114 * color.getBlue();
        return (int) Math.round(luminance * 255);
    }
}
