package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.controller;

import com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model.HistogramAnalyzer;
import com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model.ImageProcessor;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Conecta la interfaz FXML con los algoritmos del modelo.
 * Conserva tres imágenes separadas: la original, una copia gris utilizada por
 * el slice y el último resultado procesado que puede mostrarse o guardarse.
 */
public class MainController {

    @FXML private ImageView vistaImagen;
    @FXML private Label etiquetaVista;
    @FXML private Label etiquetaValorMinimo;
    @FXML private Label etiquetaValorMaximo;
    @FXML private Label etiquetaValorBrillo;
    @FXML private Slider deslizadorMinimo;
    @FXML private Slider deslizadorMaximo;
    @FXML private Slider deslizadorBrillo;
    @FXML private CheckBox casillaInvertir;
    @FXML private Button botonGuardar;
    @FXML private Button botonRestaurar;
    @FXML private Button botonEscalaGrises;
    @FXML private Button botonEcualizar;
    @FXML private Button botonBrillo;
    @FXML private Button botonSlice;
    @FXML private Button botonAlternarVista;
    @FXML private AreaChart<Number, Number> graficoHistogramaOriginal;
    @FXML private AreaChart<Number, Number> graficoHistogramaProcesado;

    private final ImageProcessor procesadorImagen = new ImageProcessor();
    private final HistogramAnalyzer analizadorHistograma = new HistogramAnalyzer();

    private Image imagenOriginal;
    private Image imagenGris;
    private Image imagenProcesada;
    private boolean mostrandoOriginal = true;

    @FXML
    private void initialize() {
        // Los listeners impiden que el límite mínimo supere al máximo.
        deslizadorMinimo.valueProperty().addListener((observable, valorAnterior, valorNuevo) -> {
            if (valorNuevo.doubleValue() > deslizadorMaximo.getValue()) {
                deslizadorMinimo.setValue(deslizadorMaximo.getValue());
            }
            actualizarEtiquetasRango();
        });
        deslizadorMaximo.valueProperty().addListener((observable, valorAnterior, valorNuevo) -> {
            if (valorNuevo.doubleValue() < deslizadorMinimo.getValue()) {
                deslizadorMaximo.setValue(deslizadorMinimo.getValue());
            }
            actualizarEtiquetasRango();
        });
        deslizadorBrillo.valueProperty().addListener((observable, valorAnterior, valorNuevo) ->
                actualizarEtiquetaBrillo());
        actualizarEtiquetasRango();
        actualizarEtiquetaBrillo();
        deshabilitarControles(true);
        configurarGrafico(graficoHistogramaOriginal);
        configurarGrafico(graficoHistogramaProcesado);
    }

    @FXML
    private void cargarImagen() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Seleccionar imagen");
        selector.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
        );
        File archivo = selector.showOpenDialog(vistaImagen.getScene().getWindow());
        if (archivo == null) {
            return;
        }

        try {
            Image imagenCargada = new Image(archivo.toURI().toString(), false);
            if (imagenCargada.isError() || imagenCargada.getPixelReader() == null) {
                throw new IllegalArgumentException("El archivo no contiene una imagen válida.");
            }
            imagenOriginal = imagenCargada;
            imagenGris = procesadorImagen.convertirAEscalaGrises(imagenOriginal);
            imagenProcesada = imagenGris;
            deslizadorBrillo.setValue(0);
            mostrandoOriginal = true;
            actualizarVistaImagen();
            actualizarHistograma(graficoHistogramaOriginal, imagenGris);
            actualizarHistograma(graficoHistogramaProcesado, imagenProcesada);
            deshabilitarControles(false);
        } catch (RuntimeException excepcion) {
            mostrarError("No se pudo abrir la imagen", excepcion.getMessage());
        }
    }

    @FXML
    private void convertirAEscalaGrises() {
        imagenProcesada = procesadorImagen.convertirAEscalaGrises(imagenOriginal);
        mostrarResultadoProcesado();
    }

    @FXML
    private void ecualizarImagen() {
        imagenProcesada = procesadorImagen.ecualizar(imagenOriginal);
        mostrarResultadoProcesado();
    }

    @FXML
    private void ajustarBrillo() {
        int cantidad = (int) Math.round(deslizadorBrillo.getValue());
        imagenProcesada = procesadorImagen.ajustarBrillo(imagenOriginal, cantidad);
        mostrarResultadoProcesado();
    }

    @FXML
    private void aplicarSlice() {
        int minimo = (int) Math.round(deslizadorMinimo.getValue());
        int maximo = (int) Math.round(deslizadorMaximo.getValue());
        imagenProcesada = procesadorImagen.aplicarSliceIntensidad(
                imagenGris, minimo, maximo, casillaInvertir.isSelected());
        mostrarResultadoProcesado();
    }

    @FXML
    private void restaurarImagen() {
        imagenProcesada = imagenGris;
        deslizadorBrillo.setValue(0);
        mostrandoOriginal = true;
        actualizarVistaImagen();
        actualizarHistograma(graficoHistogramaProcesado, imagenProcesada);
    }

    @FXML
    private void alternarVista() {
        mostrandoOriginal = !mostrandoOriginal;
        actualizarVistaImagen();
    }

    @FXML
    private void guardarImagen() {
        if (imagenProcesada == null) {
            return;
        }
        FileChooser selector = new FileChooser();
        selector.setTitle("Guardar imagen procesada");
        selector.setInitialFileName("imagen-procesada.png");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File archivo = selector.showSaveDialog(vistaImagen.getScene().getWindow());
        if (archivo == null) {
            return;
        }
        if (!archivo.getName().toLowerCase().endsWith(".png")) {
            archivo = new File(archivo.getAbsolutePath() + ".png");
        }

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(imagenProcesada, null), "png", archivo);
        } catch (IOException excepcion) {
            mostrarError("No se pudo guardar la imagen", excepcion.getMessage());
        }
    }

    private void mostrarResultadoProcesado() {
        // Cada operación cambia automáticamente a la vista del resultado.
        mostrandoOriginal = false;
        actualizarVistaImagen();
        actualizarHistograma(graficoHistogramaProcesado, imagenProcesada);
    }

    private void actualizarVistaImagen() {
        vistaImagen.setImage(mostrandoOriginal ? imagenOriginal : imagenProcesada);
        etiquetaVista.setText(mostrandoOriginal ? "Vista: imagen original" : "Vista: imagen procesada");
        botonAlternarVista.setText(mostrandoOriginal ? "Ver procesada" : "Ver original");
    }

    private void actualizarHistograma(AreaChart<Number, Number> grafico, Image imagen) {
        // Cada dato usa la intensidad en X y la cantidad de píxeles en Y.
        int[] histograma = analizadorHistograma.calcularHistograma(imagen);
        XYChart.Series<Number, Number> serie = new XYChart.Series<>();
        for (int intensidad = 0; intensidad < histograma.length; intensidad++) {
            serie.getData().add(new XYChart.Data<>(intensidad, histograma[intensidad]));
        }
        grafico.getData().clear();
        grafico.getData().add(serie);
    }

    private void configurarGrafico(AreaChart<Number, Number> grafico) {
        grafico.setAnimated(false);
        grafico.setLegendVisible(false);
        grafico.setCreateSymbols(false);
    }

    private void actualizarEtiquetasRango() {
        etiquetaValorMinimo.setText(String.valueOf((int) Math.round(deslizadorMinimo.getValue())));
        etiquetaValorMaximo.setText(String.valueOf((int) Math.round(deslizadorMaximo.getValue())));
    }

    private void actualizarEtiquetaBrillo() {
        int valor = (int) Math.round(deslizadorBrillo.getValue());
        etiquetaValorBrillo.setText(valor > 0 ? "+" + valor : String.valueOf(valor));
    }

    private void deshabilitarControles(boolean deshabilitados) {
        botonGuardar.setDisable(deshabilitados);
        botonRestaurar.setDisable(deshabilitados);
        botonEscalaGrises.setDisable(deshabilitados);
        botonEcualizar.setDisable(deshabilitados);
        botonBrillo.setDisable(deshabilitados);
        botonSlice.setDisable(deshabilitados);
        botonAlternarVista.setDisable(deshabilitados);
        deslizadorMinimo.setDisable(deshabilitados);
        deslizadorMaximo.setDisable(deshabilitados);
        deslizadorBrillo.setDisable(deshabilitados);
        casillaInvertir.setDisable(deshabilitados);
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(titulo);
        alerta.setContentText(mensaje == null ? "Ocurrió un error inesperado." : mensaje);
        alerta.showAndWait();
    }
}
