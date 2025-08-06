package it.unito.prog3.mailserver;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ServerApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ServerLogView.fxml"));
        stage.setScene(new Scene(loader.load(), 800, 600));
        stage.setTitle("Mail Server");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
