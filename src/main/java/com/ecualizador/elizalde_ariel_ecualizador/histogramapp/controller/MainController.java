package com.ecualizador.elizalde_ariel_ecualizador.histogramapp.controller;

import com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model.HistogramAnalyzer;
import com.ecualizador.elizalde_ariel_ecualizador.histogramapp.model.ImageProcessor;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
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

public class MainController {

    @FXML private ImageView imageView;
    @FXML private Label viewLabel;
    @FXML private Label minimumValueLabel;
    @FXML private Label maximumValueLabel;
    @FXML private Slider minimumSlider;
    @FXML private Slider maximumSlider;
    @FXML private CheckBox invertedCheckBox;
    @FXML private Button saveButton;
    @FXML private Button restoreButton;
    @FXML private Button grayscaleButton;
    @FXML private Button equalizeButton;
    @FXML private Button sliceButton;
    @FXML private Button toggleViewButton;
    @FXML private BarChart<String, Number> originalHistogramChart;
    @FXML private BarChart<String, Number> processedHistogramChart;

    private final ImageProcessor imageProcessor = new ImageProcessor();
    private final HistogramAnalyzer histogramAnalyzer = new HistogramAnalyzer();

    private Image originalImage;
    private Image grayscaleImage;
    private Image processedImage;
    private boolean showingOriginal = true;

    @FXML
    private void initialize() {
        minimumSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() > maximumSlider.getValue()) {
                minimumSlider.setValue(maximumSlider.getValue());
            }
            updateSliderLabels();
        });
        maximumSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() < minimumSlider.getValue()) {
                maximumSlider.setValue(minimumSlider.getValue());
            }
            updateSliderLabels();
        });
        updateSliderLabels();
        setControlsDisabled(true);
        configureChart(originalHistogramChart);
        configureChart(processedHistogramChart);
    }

    @FXML
    private void loadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar imagen");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg")
        );
        File file = chooser.showOpenDialog(imageView.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            Image loadedImage = new Image(file.toURI().toString(), false);
            if (loadedImage.isError() || loadedImage.getPixelReader() == null) {
                throw new IllegalArgumentException("El archivo no contiene una imagen válida.");
            }
            originalImage = loadedImage;
            grayscaleImage = imageProcessor.convertToGrayscale(originalImage);
            processedImage = grayscaleImage;
            showingOriginal = true;
            updateImageView();
            updateHistogram(originalHistogramChart, grayscaleImage);
            updateHistogram(processedHistogramChart, processedImage);
            setControlsDisabled(false);
        } catch (RuntimeException exception) {
            showError("No se pudo abrir la imagen", exception.getMessage());
        }
    }

    @FXML
    private void convertToGrayscale() {
        processedImage = imageProcessor.convertToGrayscale(originalImage);
        showProcessedResult();
    }

    @FXML
    private void equalizeImage() {
        processedImage = imageProcessor.equalize(grayscaleImage);
        showProcessedResult();
    }

    @FXML
    private void applySlice() {
        int minimum = (int) Math.round(minimumSlider.getValue());
        int maximum = (int) Math.round(maximumSlider.getValue());
        processedImage = imageProcessor.applyIntensitySlice(
                grayscaleImage, minimum, maximum, invertedCheckBox.isSelected());
        showProcessedResult();
    }

    @FXML
    private void restoreImage() {
        processedImage = grayscaleImage;
        showingOriginal = true;
        updateImageView();
        updateHistogram(processedHistogramChart, processedImage);
    }

    @FXML
    private void toggleView() {
        showingOriginal = !showingOriginal;
        updateImageView();
    }

    @FXML
    private void saveImage() {
        if (processedImage == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar imagen procesada");
        chooser.setInitialFileName("imagen-procesada.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        File file = chooser.showSaveDialog(imageView.getScene().getWindow());
        if (file == null) {
            return;
        }
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new File(file.getAbsolutePath() + ".png");
        }

        try {
            ImageIO.write(SwingFXUtils.fromFXImage(processedImage, null), "png", file);
        } catch (IOException exception) {
            showError("No se pudo guardar la imagen", exception.getMessage());
        }
    }

    private void showProcessedResult() {
        showingOriginal = false;
        updateImageView();
        updateHistogram(processedHistogramChart, processedImage);
    }

    private void updateImageView() {
        imageView.setImage(showingOriginal ? originalImage : processedImage);
        viewLabel.setText(showingOriginal ? "Vista: imagen original" : "Vista: imagen procesada");
        toggleViewButton.setText(showingOriginal ? "Ver procesada" : "Ver original");
    }

    private void updateHistogram(BarChart<String, Number> chart, Image image) {
        int[] histogram = histogramAnalyzer.calculateHistogram(image);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (int intensity = 0; intensity < histogram.length; intensity++) {
            series.getData().add(new XYChart.Data<>(String.valueOf(intensity), histogram[intensity]));
        }
        chart.getData().setAll(series);
    }

    private void configureChart(BarChart<String, Number> chart) {
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setCategoryGap(0);
        chart.setBarGap(0);
    }

    private void updateSliderLabels() {
        minimumValueLabel.setText(String.valueOf((int) Math.round(minimumSlider.getValue())));
        maximumValueLabel.setText(String.valueOf((int) Math.round(maximumSlider.getValue())));
    }

    private void setControlsDisabled(boolean disabled) {
        saveButton.setDisable(disabled);
        restoreButton.setDisable(disabled);
        grayscaleButton.setDisable(disabled);
        equalizeButton.setDisable(disabled);
        sliceButton.setDisable(disabled);
        toggleViewButton.setDisable(disabled);
        minimumSlider.setDisable(disabled);
        maximumSlider.setDisable(disabled);
        invertedCheckBox.setDisable(disabled);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText(message == null ? "Ocurrió un error inesperado." : message);
        alert.showAndWait();
    }
}
