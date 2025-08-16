package it.unito.prog3.mailserver.controller;

import it.unito.prog3.mailserver.net.ServerCore;
import it.unito.prog3.mailserver.store.MailStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

/**
 * Controller della GUI del Mail Server.
 * <p>
 * Avvia e arresta il core del server (socket listener) e
 * gestisce il log degli eventi mostrato nella TextArea.
 * </p>
 */
public class ServerController {

    /** Area di testo su cui mostrare i log di server e connessioni. */
    @FXML
    private TextArea logArea;

    /** Componente core che accetta connessioni e delega le richieste. */
    private ServerCore core;

    /** Store in-memory (o con persistenza) delle caselle di posta. */
    private MailStore store;

    /**
     * Inizializzazione chiamata da JavaFX dopo il caricamento dell'FXML.
     * Crea {@link MailStore}, avvia {@link ServerCore} e scrive un messaggio di benvenuto.
     */
    @FXML
    public void initialize() {
        appendLog("GUI server pronta.");
        // MailStore con callback di log verso la GUI
        this.store = MailStore.getInstance(this::appendLog);
        this.core = new ServerCore(5555, store, this::appendLog);
        this.core.start();
    }

    /**
     * Appende una riga di log nella TextArea in modo thread-safe.
     *
     * @param message messaggio da mostrare
     */
    public void appendLog(String message) {
        if (message == null) return;
        Platform.runLater(() -> {
            if (logArea != null) {
                logArea.appendText(message + System.lineSeparator());
            }
        });
    }

    /**
     * Arresta in modo ordinato il core del server (chiamata alla chiusura finestra).
     * Da invocare in {@code ServerApp} su evento {@code setOnCloseRequest}.
     */
    public void shutdown() {
        if (core != null) {
            core.stop();
        }
        appendLog("Shutdown richiesto. Bye.");
    }

    // (Opzionali, se in futuro aggiungi pulsanti Start/Stop nell'FXML)
    /** Avvia il server core se non già in esecuzione. */
    @FXML
    private void onStart() {
        if (core == null) core = new ServerCore(5555, store, this::appendLog);
        core.start();
        appendLog("Server avviato manualmente.");
    }

    /** Ferma il server core se in esecuzione. */
    @FXML
    private void onStop() {
        if (core != null) {
            core.stop();
            appendLog("Server fermato manualmente.");
        }
    }
}
