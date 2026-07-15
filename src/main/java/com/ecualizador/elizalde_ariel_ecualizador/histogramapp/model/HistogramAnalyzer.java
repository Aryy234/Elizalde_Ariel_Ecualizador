package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * Analiza la distribución de intensidades de una imagen.
 * Un histograma posee 256 posiciones: 0 representa negro, 255 representa
 * blanco y los valores intermedios representan diferentes niveles de gris.
 */
public class HistogramAnalyzer {

    /**
     * Cuenta cuántos píxeles pertenecen a cada nivel de intensidad.
     * Aunque la imagen tenga colores, cada píxel se convierte temporalmente
     * a una intensidad mediante luminancia; la imagen original no se modifica.
     *
     * @param imagen imagen que se desea analizar
     * @return arreglo de 256 contadores, uno para cada intensidad
     */
    public int[] calcularHistograma(Image imagen) {
        if (imagen == null || imagen.isError() || imagen.getPixelReader() == null) {
            throw new IllegalArgumentException("La imagen no es válida.");
        }

        int[] histograma = new int[256];
        PixelReader lector = imagen.getPixelReader();
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();

        // Se recorre la imagen fila por fila y columna por columna.
        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                Color color = lector.getColor(x, y);
                int intensidad = calcularIntensidad(color);
                histograma[intensidad]++;
            }
        }
        return histograma;
    }

    /**
     * Calcula la luminancia perceptual. El verde tiene mayor peso porque
     * el ojo humano es más sensible a ese canal que al rojo o al azul.
     */
    private int calcularIntensidad(Color color) {
        double luminancia = 0.299 * color.getRed()
                + 0.587 * color.getGreen()
                + 0.114 * color.getBlue();
        return (int) Math.round(luminancia * 255);
    }
}
