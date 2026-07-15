package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Contiene los algoritmos de procesamiento de imágenes.
 * Cada operación crea y devuelve una imagen nueva para mantener intacta
 * la imagen recibida como parámetro.
 */
public class ImageProcessor {

    /** Convierte una imagen a gris usando la fórmula de luminancia perceptual. */
    public Image convertirAEscalaGrises(Image imagen) {
        validarImagen(imagen);
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        WritableImage resultado = new WritableImage(ancho, alto);
        PixelReader lector = imagen.getPixelReader();
        PixelWriter escritor = resultado.getPixelWriter();

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                Color color = lector.getColor(x, y);
                double gris = 0.299 * color.getRed()
                        + 0.587 * color.getGreen()
                        + 0.114 * color.getBlue();

                // Los tres canales reciben el mismo valor para producir gris.
                escritor.setColor(x, y, new Color(gris, gris, gris, color.getOpacity()));
            }
        }
        return resultado;
    }

    /**
     * Ecualiza el brillo sin eliminar los colores.
     * Se conserva el tono y la saturación del modelo HSB, mientras el brillo
     * se redistribuye mediante el histograma acumulado.
     */
    public Image ecualizar(Image imagen) {
        validarImagen(imagen);
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        int totalPixeles = ancho * alto;
        int[] histograma = calcularHistogramaBrillo(imagen);

        // La posición i contiene la cantidad de píxeles entre 0 e i.
        int[] acumulado = new int[256];
        acumulado[0] = histograma[0];
        for (int i = 1; i < acumulado.length; i++) {
            acumulado[i] = acumulado[i - 1] + histograma[i];
        }

        // Se busca la primera intensidad utilizada para normalizar desde cero.
        int primerNivelUsado = 0;
        while (primerNivelUsado < 256 && histograma[primerNivelUsado] == 0) {
            primerNivelUsado++;
        }

        // Una imagen de un solo nivel no puede ampliar su contraste.
        if (primerNivelUsado == 256 || totalPixeles == acumulado[primerNivelUsado]) {
            return copiarImagen(imagen);
        }

        PixelReader lector = imagen.getPixelReader();
        WritableImage resultado = new WritableImage(ancho, alto);
        PixelWriter escritor = resultado.getPixelWriter();
        int acumuladoMinimo = acumulado[primerNivelUsado];

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                Color colorOriginal = lector.getColor(x, y);
                int brilloAnterior = (int) Math.round(colorOriginal.getBrightness() * 255);

                // Esta fórmula lleva la distribución acumulada al intervalo 0..1.
                double brilloNuevo = (double) (acumulado[brilloAnterior] - acumuladoMinimo)
                        / (totalPixeles - acumuladoMinimo);
                brilloNuevo = limitarValorColor(brilloNuevo);

                Color colorEcualizado = Color.hsb(
                        colorOriginal.getHue(),
                        colorOriginal.getSaturation(),
                        brilloNuevo,
                        colorOriginal.getOpacity()
                );
                escritor.setColor(x, y, colorEcualizado);
            }
        }
        return resultado;
    }

    /** Construye el histograma del componente brillo utilizado al ecualizar. */
    private int[] calcularHistogramaBrillo(Image imagen) {
        int[] histograma = new int[256];
        PixelReader lector = imagen.getPixelReader();
        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int brillo = (int) Math.round(lector.getColor(x, y).getBrightness() * 255);
                histograma[brillo]++;
            }
        }
        return histograma;
    }

    /**
     * Realiza un slice binario. Los píxeles dentro del rango inclusivo quedan
     * blancos y los demás negros; invertir intercambia ambos resultados.
     */
    public Image aplicarSliceIntensidad(Image imagen, int minimo, int maximo, boolean invertir) {
        validarImagen(imagen);
        if (minimo < 0 || maximo > 255 || minimo > maximo) {
            throw new IllegalArgumentException("El rango debe estar entre 0 y 255.");
        }

        Image imagenGris = convertirAEscalaGrises(imagen);
        int ancho = (int) imagenGris.getWidth();
        int alto = (int) imagenGris.getHeight();
        PixelReader lector = imagenGris.getPixelReader();
        WritableImage resultado = new WritableImage(ancho, alto);
        PixelWriter escritor = resultado.getPixelWriter();

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                int intensidad = (int) Math.round(lector.getColor(x, y).getRed() * 255);
                boolean estaEnRango = intensidad >= minimo && intensidad <= maximo;
                boolean debeSerBlanco = invertir ? !estaEnRango : estaEnRango;
                escritor.setColor(x, y, debeSerBlanco ? Color.WHITE : Color.BLACK);
            }
        }
        return resultado;
    }

    /**
     * Suma una cantidad a los tres canales RGB. Un valor positivo aclara y uno
     * negativo oscurece. Los valores se limitan al intervalo válido 0..1.
     */
    public Image ajustarBrillo(Image imagen, int cantidad) {
        validarImagen(imagen);
        if (cantidad < -100 || cantidad > 100) {
            throw new IllegalArgumentException("El brillo debe estar entre -100 y 100.");
        }

        int ancho = (int) imagen.getWidth();
        int alto = (int) imagen.getHeight();
        PixelReader lector = imagen.getPixelReader();
        WritableImage resultado = new WritableImage(ancho, alto);
        PixelWriter escritor = resultado.getPixelWriter();
        double cambio = cantidad / 100.0;

        for (int y = 0; y < alto; y++) {
            for (int x = 0; x < ancho; x++) {
                Color color = lector.getColor(x, y);
                double rojo = limitarValorColor(color.getRed() + cambio);
                double verde = limitarValorColor(color.getGreen() + cambio);
                double azul = limitarValorColor(color.getBlue() + cambio);
                escritor.setColor(x, y, new Color(rojo, verde, azul, color.getOpacity()));
            }
        }
        return resultado;
    }

    private double limitarValorColor(double valor) {
        return Math.max(0, Math.min(1, valor));
    }

    private Image copiarImagen(Image imagen) {
        return new WritableImage(
                imagen.getPixelReader(), (int) imagen.getWidth(), (int) imagen.getHeight());
    }

    private void validarImagen(Image imagen) {
        if (imagen == null || imagen.isError() || imagen.getPixelReader() == null) {
            throw new IllegalArgumentException("La imagen no es válida.");
        }
    }
}
