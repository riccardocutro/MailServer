package it.unito.prog3.mailserver.store;

import shared.Email;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Archivio centrale delle caselle di posta.
 * Gestisce account, inbox e persistenza su file, con metodi thread-safe.
 * accounts.txt (un indirizzo per ogni riga)
 * mails.txt (id;from;toCsv;base64(subject);base64(body);ISO_LOCAL_DATE_TIME)
 */
public class MailStore {

    private static final String ACCOUNTS_FILE = "accounts.txt";
    private static final String MAILS_FILE    = "mails.txt";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static MailStore instance;

    private final Set<String> accounts = new HashSet<>();
    private final Map<String, List<Email>> boxes = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(0);
    private final Consumer<String> log;

    private MailStore(Consumer<String> log) {
        this.log = (log == null) ? s -> {} : log;
        try {
            log.accept("Working dir: " + System.getProperty("user.dir"));
            loadAccounts();
            loadMails();
        } catch (IOException e) {
            this.log.accept("Errore caricamento dati: " + e.getMessage());
        }
    }

    /** Singleton globale. */
    public static synchronized MailStore getInstance(Consumer<String> log) {
        if (instance == null) instance = new MailStore(log);
        return instance;
    }

    /** @return true se l'account esiste. */
    public boolean userExists(String email) {
        return accounts.contains(norm(email));
    }

    /** Genera un nuovo ID email. */
    public int getNextEmailId() {
        return idGen.incrementAndGet();
    }

    /** Inserisce un messaggio nella inbox del destinatario. */
    public void addEmail(String recipient, Email email) {
        String r = norm(recipient);
        if (!userExists(r)) throw new IllegalArgumentException("Unknown recipient: " + recipient);
        boxes.get(r).add(email);
        saveData();
        log.accept("Nuova email per " + r + " [id=" + email.getId() + "]");
    }

    /** Restituisce i messaggi con id > lastId. */
    public List<Email> getEmailsAfter(String user, int lastId) {
        String u = norm(user);
        if (!userExists(u)) return List.of();
        List<Email> inbox = boxes.get(u);
        synchronized (inbox) {
            List<Email> res = new ArrayList<>();
            for (Email e : inbox) if (e.getId() > lastId) res.add(e);
            return res;
        }
    }

    /** Cancella un messaggio dalla inbox. */
    public boolean deleteEmail(String user, int id) {
        String u = norm(user);
        if (!userExists(u)) return false;
        List<Email> inbox = boxes.get(u);
        boolean removed;
        synchronized (inbox) {
            removed = inbox.removeIf(e -> e.getId() == id);
        }
        if (removed) saveData();
        return removed;
    }

    /** Salva lo stato su file. */
    private void saveData() {
        try {
            saveAccounts();
            saveMails();
        } catch (IOException e) {
            log.accept("Errore salvataggio dati: " + e.getMessage());
        }
    }

    private void loadAccounts() throws IOException {
        Path path = Paths.get(ACCOUNTS_FILE);
        accounts.clear();
        boxes.clear();

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                String email = norm(line);
                if (!email.isEmpty()) {
                    accounts.add(email);
                    boxes.put(email, Collections.synchronizedList(new ArrayList<>()));
                }
            }
        }
        if (accounts.isEmpty()) {
            log.accept("Nessun account presente in " + ACCOUNTS_FILE);
        }
    }

    //solo per modifiche future
    private void saveAccounts() throws IOException {
        Path tmp = Paths.get(ACCOUNTS_FILE + ".tmp");
        try (BufferedWriter bw = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String acc : accounts) {
                bw.write(acc);
                bw.newLine();
            }
        }
        Files.move(tmp, Paths.get(ACCOUNTS_FILE), REPLACE_EXISTING);
    }

    private void loadMails() throws IOException {
        Path path = Paths.get(MAILS_FILE);
        if (!Files.exists(path)) {
            log.accept("Nessun file mails.txt, inbox vuote.");
            return;
        }
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = br.readLine()) != null) {
                Email e = Email.fromString(line);
                for (String r : e.getTo()) {
                    if (accounts.contains(r)) {
                        boxes.get(r).add(e);
                        idGen.set(Math.max(idGen.get(), e.getId()));
                    }
                }
            }
            log.accept("Email caricate da file.");
        } catch (Exception e) {
            log.accept("⚠️ Errore caricamento mail: " + e.getMessage());
        }
    }

    private void saveMails() throws IOException {
        Path tmp = Paths.get(MAILS_FILE + ".tmp");
        try (BufferedWriter bw = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (var entry : boxes.entrySet()) {
                List<Email> inbox = entry.getValue();
                synchronized (inbox) {
                    for (Email e : inbox) {
                        String id     = String.valueOf(e.getId());
                        String from   = e.getFrom();
                        String toCsv  = String.join(",", e.getTo()); // qui è una lista con un solo destinatario
                        String subj64 = Base64.getEncoder().encodeToString(e.getSubject().getBytes(StandardCharsets.UTF_8));
                        String body64 = Base64.getEncoder().encodeToString(e.getBody().getBytes(StandardCharsets.UTF_8));
                        String date   = e.getDate().format(DATE_FMT);

                        bw.write(String.join(";", id, from, toCsv, subj64, body64, date));
                        bw.newLine();
                    }
                }
            }
        }
        Files.move(tmp, Paths.get(MAILS_FILE), REPLACE_EXISTING);
    }


    /** Normalizza indirizzo */
    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
