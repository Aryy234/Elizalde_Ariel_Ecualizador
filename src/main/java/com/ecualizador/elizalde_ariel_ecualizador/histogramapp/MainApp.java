package com.ecualizador.elizalde_ariel_ecualizador.histogramapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    @Override
    public void start(Stage escenario) throws IOException {
        // FXMLLoader crea los controles declarados en el archivo FXML y conecta el controlador.
        FXMLLoader cargador = new FXMLLoader(MainApp.class.getResource("main-view.fxml"));
        Scene escena = new Scene(cargador.load(), 1000, 700);

        escenario.setTitle("Ecualizador de Histogramas");
        escenario.setMinWidth(900);
        escenario.setMinHeight(650);
        escenario.setScene(escena);
        escenario.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
