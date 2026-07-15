package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistogramAnalyzerTest {

    @Test
    void elHistogramaTiene256NivelesYCuentaTodosLosPixeles() {
        WritableImage imagen = new WritableImage(2, 2);
        imagen.getPixelWriter().setColor(0, 0, Color.BLACK);
        imagen.getPixelWriter().setColor(1, 0, Color.WHITE);
        imagen.getPixelWriter().setColor(0, 1, Color.BLACK);
        imagen.getPixelWriter().setColor(1, 1, Color.gray(0.5));

        int[] histograma = new HistogramAnalyzer().calcularHistograma(imagen);
        int total = 0;
        for (int cantidad : histograma) {
            total += cantidad;
        }

        assertEquals(256, histograma.length);
        assertEquals(4, total);
        assertEquals(2, histograma[0]);
        assertEquals(1, histograma[255]);
    }
}
