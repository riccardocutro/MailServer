package shared;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Rappresenta un'email.
 * Compatibile con la persistenza su file e con il formato testuale
 * usato nella comunicazione Client-Server.
 */
public class Email implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private int id;
    private String from;
    private List<String> to;  // supporto a più destinatari
    private String subject;
    private String body;
    private LocalDateTime date;

    public Email(int id, String from, String to, String subject, String body, LocalDateTime date) {
        this.id = id;
        this.from = from;
        this.to = Arrays.asList(to.split(",")); // converte la stringa in lista
        this.subject = subject;
        this.body = body;
        this.date = date;
    }

    public Email(int id, String from, List<String> to, String subject, String body, LocalDateTime date) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.date = date;
    }

    // Getter
    public int getId() { return id; }
    public String getFrom() { return from; }
    public List<String> getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public LocalDateTime getDate() { return date; }

    // Conversione in stringa per il protocollo socket
    @Override
    public String toString() {
        String toStr = String.join(",", to);
        return id + ";" + from + ";" + toStr + ";" + subject + ";" + body + ";" + date.format(DATE_FMT);
    }

    // Parsing da stringa ricevuta via socket
    public static Email fromString(String s) {
        try {
            String[] parts = s.split(";", 6);
            int id = Integer.parseInt(parts[0]);
            String from = parts[1];
            String to = parts[2];
            String subject = parts[3];
            String body = parts[4];
            LocalDateTime date = LocalDateTime.parse(parts[5], DATE_FMT);
            return new Email(id, from, to, subject, body, date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Formato Email non valido: " + s);
        }
    }
}
