package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ImageProcessorTest {

    private final ImageProcessor procesador = new ImageProcessor();

    @Test
    void escalaGrisesCreaImagenNuevaConCanalesIguales() {
        WritableImage origen = crearFila(Color.RED, Color.BLUE);

        Image resultado = procesador.convertirAEscalaGrises(origen);
        Color pixel = resultado.getPixelReader().getColor(0, 0);

        assertNotSame(origen, resultado);
        assertEquals(pixel.getRed(), pixel.getGreen(), 0.001);
        assertEquals(pixel.getGreen(), pixel.getBlue(), 0.001);
    }

    @Test
    void sliceVuelveBlancoElRangoSeleccionadoYNegroElResto() {
        WritableImage origen = crearFila(Color.gray(0.25), Color.gray(0.75));

        Image resultado = procesador.aplicarSliceIntensidad(origen, 0, 127, false);

        assertEquals(Color.WHITE, resultado.getPixelReader().getColor(0, 0));
        assertEquals(Color.BLACK, resultado.getPixelReader().getColor(1, 0));
    }

    @Test
    void sliceInvertidoIntercambiaNegroYBlanco() {
        WritableImage origen = crearFila(Color.gray(0.25), Color.gray(0.75));

        Image resultado = procesador.aplicarSliceIntensidad(origen, 0, 127, true);

        assertEquals(Color.BLACK, resultado.getPixelReader().getColor(0, 0));
        assertEquals(Color.WHITE, resultado.getPixelReader().getColor(1, 0));
    }

    @Test
    void ecualizacionExtiendeDosIntensidadesHastaNegroYBlanco() {
        WritableImage origen = crearFila(Color.gray(0.2), Color.gray(0.4));

        Image resultado = procesador.ecualizar(origen);

        assertEquals(0.0, resultado.getPixelReader().getColor(0, 0).getRed(), 0.01);
        assertEquals(1.0, resultado.getPixelReader().getColor(1, 0).getRed(), 0.01);
    }

    @Test
    void ecualizacionConservaTonoYSaturacion() {
        Color rojoOscuro = Color.hsb(0, 0.8, 0.2);
        Color azulClaro = Color.hsb(220, 0.7, 0.6);
        WritableImage origen = crearFila(rojoOscuro, azulClaro);

        Image resultado = procesador.ecualizar(origen);
        Color azulResultado = resultado.getPixelReader().getColor(1, 0);

        assertEquals(azulClaro.getHue(), azulResultado.getHue(), 0.5);
        assertEquals(azulClaro.getSaturation(), azulResultado.getSaturation(), 0.01);
    }

    @Test
    void brilloPositivoAclaraYConservaDiferenciasDeColor() {
        WritableImage origen = crearFila(Color.color(0.1, 0.2, 0.3), Color.BLACK);

        Image resultado = procesador.ajustarBrillo(origen, 30);
        Color pixel = resultado.getPixelReader().getColor(0, 0);

        assertEquals(0.4, pixel.getRed(), 0.01);
        assertEquals(0.5, pixel.getGreen(), 0.01);
        assertEquals(0.6, pixel.getBlue(), 0.01);
    }

    @Test
    void brilloSeLimitaAlRangoValido() {
        WritableImage origen = crearFila(Color.gray(0.9), Color.gray(0.1));

        Image masClara = procesador.ajustarBrillo(origen, 50);
        Image masOscura = procesador.ajustarBrillo(origen, -50);

        assertEquals(1.0, masClara.getPixelReader().getColor(0, 0).getRed(), 0.01);
        assertEquals(0.0, masOscura.getPixelReader().getColor(1, 0).getRed(), 0.01);
    }

    private WritableImage crearFila(Color primero, Color segundo) {
        WritableImage imagen = new WritableImage(2, 1);
        imagen.getPixelWriter().setColor(0, 0, primero);
        imagen.getPixelWriter().setColor(1, 0, segundo);
        return imagen;
    }
}
