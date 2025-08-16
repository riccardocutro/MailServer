package it.unito.prog3.mailserver.net;

import it.unito.prog3.mailserver.store.MailStore;
import shared.Email;
import shared.Protocol;
import shared.Wire;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Gestisce una singola connessione client.
 * <p>Legge un comando testuale, lo interpreta e invia la risposta.</p>
 */
public class RequestHandler implements Runnable {

    private final Socket socket;
    private final MailStore store;
    private final Consumer<String> log;

    /**
     * @param socket connessione accettata dal server
     * @param store  archivio dati condiviso
     * @param log    callback per log eventi (usare s -> {} per disabilitare)
     */
    public RequestHandler(Socket socket, MailStore store, Consumer<String> log) {
        this.socket = socket;
        this.store = store;
        this.log = (log == null) ? s -> {} : log;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true)) {

            String line = in.readLine();
            if (line == null) return;

            String[] p = line.split(";", -1); // non perde campi vuoti
            String cmd = p[0];

            switch (cmd) {
                case Protocol.CMD_LOGIN  -> handleLogin(p, out);
                case Protocol.CMD_SEND   -> handleSend(p, out);
                case Protocol.CMD_GET    -> handleGet(p, out);
                case Protocol.CMD_DELETE -> handleDelete(p, out);
                default -> out.println(Protocol.RESP_ERROR + ";UnknownCommand");
            }
        } catch (SocketException se) {
            log.accept("Connessione interrotta: " + se.getMessage());
        } catch (IOException ioe) {
            log.accept("Errore I/O handler: " + ioe.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** LOGIN;email */
    private void handleLogin(String[] p, PrintWriter out) {
        if (p.length < 2) { out.println(Protocol.RESP_ERROR + ";BadRequest"); return; }
        String email = p[1];
        out.println(store.userExists(email) ? Protocol.RESP_OK : Protocol.RESP_ERROR + ";UserNotFound");
    }

    /** SEND;from;toCsv;base64(subject);base64(body) */
    private void handleSend(String[] p, PrintWriter out) {
        if (p.length < 5) { out.println(Protocol.RESP_ERROR + ";BadRequest"); return; }

        String from = p[1];
        List<String> to = Arrays.stream(p[2].split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        String subject = Wire.unb64(p[3]);
        String body    = Wire.unb64(p[4]);

        // Valida destinatari
        for (String r : to) {
            if (!store.userExists(r)) {
                out.println(Protocol.RESP_ERROR + ";InvalidRecipient;" + r);
                return;
            }
        }

        // Consegna (copia singola in inbox del destinatario)
        for (String r : to) {
            Email email = new Email(
                    store.getNextEmailId(), // se preferisci, rinomina in nextId()
                    from,
                    List.of(r),
                    subject,
                    body,
                    LocalDateTime.now()
            );
            store.addEmail(r, email);
        }

        out.println(Protocol.RESP_OK);
        log.accept("SEND da " + from + " a " + String.join(",", to));
    }

    /** GET;user;lastId  → stream di: MSG;id;from;toCsv;base64(subject);base64(body);epochSeconds ... poi END */
    private void handleGet(String[] p, PrintWriter out) {
        if (p.length < 3) { out.println(Protocol.RESP_ERROR + ";BadRequest"); return; }

        String user = p[1];
        int lastId;
        try { lastId = Integer.parseInt(p[2]); }
        catch (NumberFormatException e) { out.println(Protocol.RESP_ERROR + ";InvalidId"); return; }

        if (!store.userExists(user)) { out.println(Protocol.RESP_ERROR + ";UserNotFound"); return; }

        List<Email> list = store.getEmailsAfter(user, lastId);
        for (Email e : list) {
            String toCsv = e.getTo().stream().collect(Collectors.joining(","));
            long epoch = e.getSentAt().toEpochSecond(ZoneOffset.UTC);
            out.println(String.join(";",
                    "MSG",
                    String.valueOf(e.getId()),
                    e.getFrom(),
                    toCsv,
                    Wire.b64(e.getSubject()),
                    Wire.b64(e.getBody()),
                    String.valueOf(epoch)
            ));
        }
        out.println("END");
        log.accept("GET per " + user + " -> " + list.size() + " nuovi");
    }

    /** DELETE;user;msgId */
    private void handleDelete(String[] p, PrintWriter out) {
        if (p.length < 3) { out.println(Protocol.RESP_ERROR + ";BadRequest"); return; }

        String user = p[1];
        int msgId;
        try { msgId = Integer.parseInt(p[2]); }
        catch (NumberFormatException e) { out.println(Protocol.RESP_ERROR + ";InvalidId"); return; }

        if (!store.userExists(user)) { out.println(Protocol.RESP_ERROR + ";UserNotFound"); return; }

        boolean ok = store.deleteEmail(user, msgId);
        out.println(ok ? Protocol.RESP_OK : Protocol.RESP_ERROR + ";MessageNotFound");
        if (ok) log.accept("DELETE id=" + msgId + " per " + user);
    }
}
