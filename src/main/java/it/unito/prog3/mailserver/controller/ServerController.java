package it.unito.prog3.mailserver.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

public class ServerController {

    @FXML
    private TextArea logArea;

    @FXML
    public void initialize() {
        logArea.appendText("Server avviato...\n");
    }
}
