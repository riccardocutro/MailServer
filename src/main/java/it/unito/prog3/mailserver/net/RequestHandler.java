package it.unito.prog3.mailserver.net;

import it.unito.prog3.mailserver.model.Email;
import it.unito.prog3.mailserver.store.MailStore;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestisce una singola connessione client-server.
 * <p>
 * Questa classe viene creata dal {@link ServerCore} ogni volta che un
 * nuovo client si connette. Il costruttore riceve il {@link Socket}
 * e viene eseguito in un thread separato per non bloccare il server principale.
 * </p>
 */
public class RequestHandler implements Runnable {

    /** Socket della connessione con il client */
    private final Socket socket;

    /** Riferimento al MailStore per la gestione dei dati */
    private final MailStore store;

    /** Funzione di logging verso la GUI */
    private final Consumer<String> log;

    /**
     * Crea un nuovo gestore richieste per un client.
     *
     * @param socket connessione socket con il client
     * @param store  oggetto di archiviazione email
     * @param log    funzione di logging
     */
    public RequestHandler(Socket socket, MailStore store, Consumer<String> log) {
        this.socket = socket;
        this.store = store;
        this.log = log;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true)
        ) {
            log.accept("Connessione da " + socket.getInetAddress());

            // Leggi il comando dal client
            String line = in.readLine();
            if (line == null) return;

            // Parsing semplice del comando
            String[] parts = line.split(" ", 2);
            String command = parts[0].toUpperCase();

            switch (command) {
                case "PING" -> {
                    out.println("PONG");
                    log.accept("Risposto a PING");
                }
                case "CHECK" -> {
                    if (parts.length < 2) {
                        out.println("ERROR Mancano parametri");
                        break;
                    }
                    String email = parts[1];
                    if (store.exists(email)) {
                        out.println("OK");
                        log.accept("Verifica esistenza: " + email + " → OK");
                    } else {
                        out.println("NOT_FOUND");
                        log.accept("Verifica esistenza: " + email + " → NON TROVATO");
                    }
                }
                case "GET" -> {
                    if (parts.length < 2) {
                        out.println("ERROR Mancano parametri");
                        break;
                    }
                    String[] getArgs = parts[1].split(" ");
                    if (getArgs.length != 2) {
                        out.println("ERROR Parametri errati");
                        break;
                    }
                    String email = getArgs[0];
                    long afterId = Long.parseLong(getArgs[1]);
                    List<Email> newEmails = store.getNew(email, afterId);
                    out.println("MESSAGES " + newEmails.size());
                    for (Email e : newEmails) {
                        // Serializzazione semplificata (per test)
                        out.println(e.id() + "|" + e.from() + "|" + e.to() + "|" + e.subject() + "|" + e.body());
                    }
                    log.accept("Inviati " + newEmails.size() + " messaggi a " + email);
                }
                default -> {
                    out.println("ERROR Comando sconosciuto");
                    log.accept("Comando sconosciuto ricevuto: " + command);
                }
            }

        } catch (IOException e) {
            log.accept("Errore connessione: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            log.accept("Connessione chiusa con " + socket.getInetAddress());
        }
    }
}
