package it.unito.prog3.mailserver.controller;

import it.unito.prog3.mailserver.net.ServerCore;
import it.unito.prog3.mailserver.store.MailStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.shape.Circle;

/**
 * Controller della GUI del Mail Server.
 * Avvia/arresta il core del server, gestisce il log e
 * aggiorna lo stato visualizzato nella dashboard.
 */
public class ServerController {

    //RIFERIMENTI UI
    @FXML private TextArea logArea;
    @FXML private Label statusLabel;
    @FXML private Circle statusDot;
    @FXML private Label connCountLabel;
    @FXML private Label lastEventLabel;

    //COMPONENTI CORE
    private ServerCore core;
    private MailStore store;

    /**
     * Inizializzazione chiamata dopo il caricamento dell'FXML.
     */
    @FXML
    public void initialize() {
        appendLog("GUI server pronta.");
        this.store = MailStore.getInstance(this::appendLog);
        this.core = new ServerCore(5555, store, this::appendLog);
        updateStatus(false);
    }

    /** Appende una riga di log nella TextArea in modo thread-safe. */
    public void appendLog(String message) {
        if (message == null) return;
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(message + System.lineSeparator());
                lastEventLabel.setText(message);
            }
        });
    }

    /** Pulisce l'area di log. */
    @FXML
    private void onClearLog() {
        if (logArea != null) {
            logArea.clear();
            appendLog("Log pulito.");
        }
    }

    /** Avvia il server core. */
    @FXML
    private void onStart() {
        if (core == null) core = new ServerCore(5555, store, this::appendLog);
        core.start();
        appendLog("Server avviato manualmente.");
        updateStatus(true);
    }

    /** Ferma il server core. */
    @FXML
    private void onStop() {
        if (core != null) {
            core.stop();
            appendLog("Server fermato manualmente.");
            updateStatus(false);
        }
    }

    /** Arresta il core quando la finestra viene chiusa. */
    public void shutdown() {
        if (core != null) core.stop();
        appendLog("Shutdown richiesto. Bye.");
        updateStatus(false);
    }

    /** Aggiorna stato (Online/Offline). */
    private void updateStatus(boolean online) {
        Platform.runLater(() -> {
            statusLabel.setText(online ? "Online" : "Offline");
            statusDot.getStyleClass().setAll(online ? "online" : "offline");
        });
    }
}
