package it.unito.prog3.mailserver.controller;

import it.unito.prog3.mailserver.net.ServerCore;
import it.unito.prog3.mailserver.store.MailStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField; // necessario per portField
import javafx.scene.shape.Circle;

public class ServerController {

    // RIFERIMENTI UI
    @FXML private TextArea logArea;
    @FXML private Label statusLabel;
    @FXML private Circle statusDot;
    @FXML private Label connCountLabel;
    @FXML private Label lastEventLabel;
    @FXML private TextField portField; // campo porta in GUI (solo informativo)

    // COMPONENTI CORE
    private ServerCore core;
    private MailStore store;

    @FXML
    public void initialize() {
        appendLog("GUI server pronta.");

        // inizializza store e core (porta fissa 5555); passa callback per aggiornare il contatore connessioni
        this.store = MailStore.getInstance(this::appendLog);
        this.core  = new ServerCore(5555, store, this::appendLog, this::updateConnectionCount);

        // porta in UI non modificabile (scelta progettuale per consegna)
        if (portField != null) {
            portField.setText("5555");
            portField.setEditable(false);
            portField.setFocusTraversable(false);
        }

        updateStatus(false);
    }

    // Appende una riga di log nella TextArea in modo thread-safe
    public void appendLog(String message) {
        if (message == null) return;
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(message + System.lineSeparator());
                if (lastEventLabel != null) lastEventLabel.setText(message);
            }
        });
    }

    @FXML
    private void onClearLog() {
        if (logArea != null) {
            logArea.clear();
            appendLog("Log pulito.");
        }
    }

    @FXML
    private void onStart() {
        // se per qualche motivo core è null, ricrea con callback conteggio
        if (core == null) core = new ServerCore(5555, store, this::appendLog, this::updateConnectionCount);
        core.start();
        appendLog("Server avviato manualmente.");
        updateStatus(true);
    }

    @FXML
    private void onStop() {
        if (core != null) {
            core.stop();
            appendLog("Server fermato manualmente.");
            updateStatus(false);
            updateConnectionCount(0); // azzera contatore in UI dopo stop
        }
    }

    // Arresta il core quando la finestra viene chiusa
    public void shutdown() {
        if (core != null) core.stop();
        appendLog("Shutdown richiesto. Bye.");
        updateStatus(false);
        updateConnectionCount(0);
    }

    // Aggiorna stato (Online/Offline) e il "dot" colorato
    private void updateStatus(boolean online) {
        Platform.runLater(() -> {
            if (statusLabel != null) statusLabel.setText(online ? "Online" : "Offline");
            if (statusDot != null) {
                // rimpiazza le classi per attivare lo stile CSS .online/.offline
                statusDot.getStyleClass().setAll(online ? "online" : "offline");
            }
        });
    }

    // Callback usata dal ServerCore per aggiornare il numero di connessioni attive
    public void updateConnectionCount(int count) {
        Platform.runLater(() -> {
            if (connCountLabel != null) connCountLabel.setText(String.valueOf(count));
        });
    }
}
