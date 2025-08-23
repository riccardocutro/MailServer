package shared;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Rappresenta un'email con mittente, destinatari, oggetto e corpo.
 * <p>Compatibile con persistenza e con il protocollo testuale client-server.</p>
 */
public class Email implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final int id;
    private final String from;
    private final List<String> to;   // supporta più destinatari
    private final String subject;
    private final String body;
    private final LocalDateTime date;

    /** Costruisce un'email a partire da una stringa CSV dei destinatari. */
    public Email(int id, String from, String to, String subject, String body, LocalDateTime date) {
        this(id, from, Arrays.asList(to.split(",")), subject, body, date);
    }

    /** Costruisce un'email con lista di destinatari. */
    public Email(int id, String from, List<String> to, String subject, String body, LocalDateTime date) {
        this.id = id;
        this.from = from;
        this.to = List.copyOf(to); // difensivo: lista immutabile
        this.subject = subject;
        this.body = body;
        this.date = date;
    }

    /** @return id univoco */
    public int getId() { return id; }
    /** @return mittente */
    public String getFrom() { return from; }
    /** @return lista destinatari */
    public List<String> getTo() { return to; }
    /** @return oggetto del messaggio */
    public String getSubject() { return subject; }
    /** @return corpo del messaggio */
    public String getBody() { return body; }
    /** @return timestamp di invio */
    public LocalDateTime getDate() { return date; }

    /** Serializza l'email in formato testuale per il protocollo socket. */
    @Override
    public String toString() {
        String toStr = String.join(",", to);
        return id + ";" + from + ";" + toStr + ";" + subject + ";" + body + ";" + date.format(DATE_FMT);
    }

    /** @return timestamp di invio (alias di getDate) */
    public LocalDateTime getSentAt() {
        return date;
    }
}
