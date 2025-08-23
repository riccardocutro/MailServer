package it.unito.prog3.mailserver;

import it.unito.prog3.mailserver.controller.ServerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerApp extends Application {

    /**
     * Entry point JavaFX per il Mail Server.
     * Carica l'interfaccia grafica ServerLogView.fxml, inizializza
     * il ServerController e mostra la finestra principale.
     */
    @Override
    public void start(Stage stage) throws Exception {
        var loader = new FXMLLoader(getClass().getResource("/ServerLogView.fxml"));
        Parent root = loader.load();
        ServerController controller = loader.getController();
        stage.setScene(new Scene(root, 800, 600));
        stage.setOnCloseRequest(e -> controller.shutdown());
        stage.setTitle("Mail Server");
        stage.show();
    }

    //Avvio applicazione
    public static void main(String[] args) {
        launch(args);
    }
}
