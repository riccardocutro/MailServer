package it.unito.prog3.mailserver.net;

import shared.Email;
import shared.Protocol;
import it.unito.prog3.mailserver.store.MailStore;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Gestisce una singola connessione con il client.
 * Riceve un comando testuale, lo interpreta e risponde.
 */
public class RequestHandler implements Runnable {

    private final Socket socket;
    private final MailStore mailStore; // Accesso ai dati persistenti

    public RequestHandler(Socket socket) {
        this.socket = socket;
        this.mailStore = MailStore.getInstance(); // Singleton per accesso unico
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String request = in.readLine();
            System.out.println("📩 Richiesta ricevuta: " + request);

            if (request != null) {
                String[] parts = request.split(";");
                String command = parts[0];

                switch (command) {
                    case Protocol.CMD_LOGIN -> handleLogin(parts, out);
                    case Protocol.CMD_SEND -> handleSend(parts, out);
                    case Protocol.CMD_GET -> handleGet(parts, out);
                    case Protocol.CMD_DELETE -> handleDelete(parts, out);
                    default -> out.println(Protocol.RESP_ERROR + ";Comando sconosciuto");
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Errore nella connessione client: " + e.getMessage());
        }
    }

    private void handleLogin(String[] parts, PrintWriter out) {
        if (parts.length < 2) {
            out.println(Protocol.RESP_ERROR + ";BadRequest");
            return;
        }
        String email = parts[1];
        if (mailStore.userExists(email)) {
            out.println(Protocol.RESP_OK);
        } else {
            out.println(Protocol.RESP_ERROR + ";UserNotFound");
        }
    }

    private void handleSend(String[] parts, PrintWriter out) {
        if (parts.length < 5) {
            out.println(Protocol.RESP_ERROR + ";BadRequest");
            return;
        }
        String from = parts[1];
        String toList = parts[2];
        String subject = parts[3];
        String body = parts[4];

        String[] recipients = toList.split(",");

        for (String recipient : recipients) {
            if (!mailStore.userExists(recipient)) {
                out.println(Protocol.RESP_ERROR + ";InvalidRecipient:" + recipient);
                return;
            }
        }

        for (String recipient : recipients) {
            Email email = new Email(
                    mailStore.getNextEmailId(),
                    from,
                    recipient,
                    subject,
                    body,
                    LocalDateTime.now()
            );
            mailStore.addEmail(recipient, email);
        }

        out.println(Protocol.RESP_OK);
    }

    private void handleGet(String[] parts, PrintWriter out) {
        if (parts.length < 3) {
            out.println(Protocol.RESP_ERROR + ";BadRequest");
            return;
        }
        String user = parts[1];
        int lastId;
        try {
            lastId = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println(Protocol.RESP_ERROR + ";InvalidId");
            return;
        }

        if (!mailStore.userExists(user)) {
            out.println(Protocol.RESP_ERROR + ";UserNotFound");
            return;
        }

        List<Email> newMessages = mailStore.getEmailsAfter(user, lastId);
        for (Email email : newMessages) {
            out.println(email.toString());
        }
        out.println("END"); // Segnale di fine lista
    }

    private void handleDelete(String[] parts, PrintWriter out) {
        if (parts.length < 3) {
            out.println(Protocol.RESP_ERROR + ";BadRequest");
            return;
        }
        String user = parts[1];
        int messageId;
        try {
            messageId = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            out.println(Protocol.RESP_ERROR + ";InvalidId");
            return;
        }

        if (!mailStore.userExists(user)) {
            out.println(Protocol.RESP_ERROR + ";UserNotFound");
            return;
        }

        boolean deleted = mailStore.deleteEmail(user, messageId);
        if (deleted) {
            out.println(Protocol.RESP_OK);
        } else {
            out.println(Protocol.RESP_ERROR + ";MessageNotFound");
        }
    }
}
