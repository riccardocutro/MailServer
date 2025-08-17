package it.unito.prog3.mailserver.store;

import shared.Email;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Archivio centrale delle caselle di posta.
 * <p>Gestisce account, inbox e persistenza su file, con metodi thread-safe.</p>
 */
public class MailStore {

    private static MailStore instance;

    private final Set<String> accounts = new HashSet<>();
    private final Map<String, List<Email>> boxes = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(0);
    private final Consumer<String> log;
    private static final String STORE_FILE = "mailstore.dat";

    private MailStore(Consumer<String> log) {
        this.log = (log == null) ? s -> {} : log;
        loadData();
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
        log.accept("📩 Nuova email per " + r + " [id=" + email.getId() + "]");
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

    /** Costruisce una nuova Email pronta per essere salvata. */
    public Email buildEmail(String from, List<String> to, String subject, String body) {
        return new Email(
                getNextEmailId(),
                norm(from),
                to.stream().map(this::norm).toList(),
                subject,
                body,
                LocalDateTime.now()
        );
    }

    /** Carica account e inbox da file, o crea dati iniziali. */
    @SuppressWarnings("unchecked")
    private void loadData() {
        File f = new File(STORE_FILE);
        if (!f.exists()) {
            log.accept("ℹ️ Nessun datastore: creo account di default");

            for (String a : List.of("riccardo@mail.com","davide@mail.com","orlando@mail.it")) {
                String n = norm(a);
                accounts.add(n);
                boxes.put(n, Collections.synchronizedList(new ArrayList<>()));
            }

            idGen.set(0);
            saveData();
            log.accept("Account iniziali creati.");
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            accounts.clear();
            boxes.clear();
            accounts.addAll((Set<String>) ois.readObject());
            boxes.putAll((Map<String, List<Email>>) ois.readObject());
            idGen.set(ois.readInt());
            log.accept("✅ Dati caricati da file. Account: " + accounts);
        } catch (IOException | ClassNotFoundException e) {
            log.accept("⚠️ Errore caricamento dati: " + e.getMessage());
        }
    }

    /** Salva lo stato su file. */
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STORE_FILE))) {
            oos.writeObject(accounts);
            oos.writeObject(boxes);
            oos.writeInt(idGen.get());
        } catch (IOException e) {
            log.accept("⚠️ Errore salvataggio dati: " + e.getMessage());
        }
    }

    /** Normalizza indirizzo */
    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

    /** Log degli account presenti */
    public Set<String> listAccounts() {
        return Collections.unmodifiableSet(accounts);
    }


}
