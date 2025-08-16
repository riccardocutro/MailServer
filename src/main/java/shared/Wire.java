package shared;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Classe di utilità per la comunicazione testuale su socket
 * tra MailClient e MailServer.
 * <p>
 * Incapsula le operazioni di invio/ricezione di messaggi
 * secondo il protocollo definito in {@link Protocol}.
 * Offre inoltre metodi statici per codifica/decodifica Base64.
 * </p>
 */
public class Wire implements Closeable {

    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    /**
     * Crea un nuovo canale di comunicazione con il server.
     *
     * @param host indirizzo del server (es. "localhost")
     * @param port porta del server (es. 5555)
     * @throws IOException se la connessione fallisce
     */
    public Wire(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Invia un messaggio (riga testuale) al server.
     *
     * @param msg stringa da inviare
     */
    public void send(String msg) {
        out.println(msg);
    }

    /**
     * Legge una singola riga di risposta dal server.
     *
     * @return risposta del server oppure {@code null} se la connessione è chiusa
     * @throws IOException se la lettura fallisce
     */
    public String receive() throws IOException {
        return in.readLine();
    }

    /**
     * Legge tutte le righe fino a un marcatore "END".
     * <p>Usato, ad esempio, per ricevere una lista di email.</p>
     *
     * @return lista di stringhe ricevute (senza "END")
     * @throws IOException se la lettura fallisce
     */
    public List<String> receiveUntilEnd() throws IOException {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            if ("END".equals(line)) break;
            lines.add(line);
        }
        return lines;
    }

    /**
     * Codifica una stringa in Base64 (utile per subject/body email).
     *
     * @param s stringa da codificare
     * @return stringa codificata in Base64
     */
    public static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

    /**
     * Decodifica una stringa Base64 in testo.
     *
     * @param s stringa codificata in Base64
     * @return stringa decodificata
     */
    public static String unb64(String s) {
        return new String(Base64.getDecoder().decode(s));
    }

    /**
     * Chiude il canale e le risorse associate.
     */
    @Override
    public void close() {
        try { in.close(); } catch (IOException ignored) {}
        out.close();
        try { socket.close(); } catch (IOException ignored) {}
    }

    /**
     * Ritorna se il socket è ancora attivo.
     */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
