module com.ecualizador.elizalde_ariel_ecualizador {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;

    exports com.ecualizador.elizalde_ariel_ecualizador.histogramapp;
    opens com.ecualizador.elizalde_ariel_ecualizador.histogramapp.controller to javafx.fxml;
}
