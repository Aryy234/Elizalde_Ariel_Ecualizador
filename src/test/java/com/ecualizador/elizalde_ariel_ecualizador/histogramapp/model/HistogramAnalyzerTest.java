package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistogramAnalyzerTest {

    @Test
    void histogramHas256LevelsAndCountsEveryPixel() {
        WritableImage image = new WritableImage(2, 2);
        image.getPixelWriter().setColor(0, 0, Color.BLACK);
        image.getPixelWriter().setColor(1, 0, Color.WHITE);
        image.getPixelWriter().setColor(0, 1, Color.BLACK);
        image.getPixelWriter().setColor(1, 1, Color.gray(0.5));

        int[] histogram = new HistogramAnalyzer().calculateHistogram(image);
        int total = 0;
        for (int count : histogram) {
            total += count;
        }

        assertEquals(256, histogram.length);
        assertEquals(4, total);
        assertEquals(2, histogram[0]);
        assertEquals(1, histogram[255]);
    }
}
