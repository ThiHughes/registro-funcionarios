package br.edu.registrofuncionarios;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage palco) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("funcionarios-view.fxml"));
            Parent raiz = loader.load();
            palco.setTitle("Registro de Funcionários");
            palco.setScene(new Scene(raiz));
            palco.setHeight(720);
            palco.setWidth(1080);
            palco.show();
        } catch (Exception e) {
            Throwable causa = e;
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }
            Alert alerta = new Alert(Alert.AlertType.ERROR);
            alerta.setTitle("Erro");
            alerta.setHeaderText("Não foi possível iniciar o sistema.");
            alerta.setContentText(String.valueOf(causa.getMessage()));
            alerta.showAndWait();
            Platform.exit();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
