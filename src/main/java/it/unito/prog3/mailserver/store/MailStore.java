package it.unito.prog3.mailserver.store;

import shared.Email;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Gestisce la memorizzazione e persistenza delle caselle email.
 * Thread-safe per gestire più client contemporaneamente.
 */
public class MailStore {

    private static MailStore instance;

    /** Insieme degli account registrati sul server */
    private final Set<String> accounts = new HashSet<>();
    /** Mappa indirizzo → inbox (lista dei messaggi) */
    private final Map<String, List<Email>> boxes = new ConcurrentHashMap<>();
    /** Generatore atomico di ID univoci per le email */
    private final AtomicInteger idGen = new AtomicInteger(0);
    /** Funzione di log per inviare messaggi alla GUI */
    private final Consumer<String> log;
    /** File di persistenza */
    private static final String STORE_FILE = "mailstore.dat";

    private MailStore(Consumer<String> log) {
        this.log = log;
        loadData();
    }

    /** Singleton */
    public static synchronized MailStore getInstance(Consumer<String> log) {
        if (instance == null) {
            instance = new MailStore(log);
        }
        return instance;
    }

    /** Verifica se un account esiste */
    public boolean userExists(String email) {
        return accounts.contains(email);
    }

    /** Genera un nuovo ID univoco */
    public int getNextEmailId() {
        return idGen.incrementAndGet();
    }

    /** Aggiunge un'email nella inbox di un utente */
    public void addEmail(String recipient, Email email) {
        if (!userExists(recipient)) {
            throw new IllegalArgumentException("Unknown recipient: " + recipient);
        }
        boxes.get(recipient).add(email);
        saveData();
        log.accept("Nuova email per " + recipient + ": " + email);
    }

    /** Restituisce i messaggi ricevuti dopo un certo ID */
    public List<Email> getEmailsAfter(String user, int lastId) {
        if (!userExists(user)) return List.of();
        List<Email> inbox = boxes.get(user);
        synchronized (inbox) {
            List<Email> res = new ArrayList<>();
            for (Email e : inbox) {
                if (e.getId() > lastId) res.add(e);
            }
            return res;
        }
    }

    /** Cancella un messaggio */
    public boolean deleteEmail(String user, int id) {
        if (!userExists(user)) return false;
        List<Email> inbox = boxes.get(user);
        boolean removed;
        synchronized (inbox) {
            removed = inbox.removeIf(e -> e.getId() == id);
        }
        if (removed) saveData();
        return removed;
    }

    /** Costruisce una nuova Email */
    public Email buildEmail(String from, String to, String subject, String body) {
        return new Email(
                getNextEmailId(),
                from,
                to,
                subject,
                body,
                LocalDateTime.now()
        );
    }

    /** Carica dati da file o crea gli account predefiniti */
    @SuppressWarnings("unchecked")
    private void loadData() {
        File f = new File(STORE_FILE);
        if (!f.exists()) {
            log.accept("Nessun file di persistenza trovato. Creazione account predefiniti...");

            List<String> base = List.of(
                    "riccardo@mail.com",
                    "davide@mail.com",
                    "orlando@mail.it"
            );

            for (String a : base) {
                accounts.add(a);
                boxes.put(a, Collections.synchronizedList(new ArrayList<>()));
            }

            idGen.set(0); // reset contatore ID
            saveData();   // salva subito la struttura di base
            log.accept("Account iniziali creati e salvati su file.");
            return;
        }

        // Se il file esiste, carica i dati
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            accounts.clear();
            boxes.clear();
            accounts.addAll((Set<String>) ois.readObject());
            boxes.putAll((Map<String, List<Email>>) ois.readObject());
            idGen.set(ois.readInt());
            log.accept("Dati caricati da file. Account: " + accounts);
        } catch (IOException | ClassNotFoundException e) {
            log.accept("Errore nel caricamento dati: " + e.getMessage());
        }
    }

    /** Salva dati su file */
    private void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(STORE_FILE))) {
            oos.writeObject(accounts);
            oos.writeObject(boxes);
            oos.writeInt(idGen.get());
        } catch (IOException e) {
            log.accept("Errore nel salvataggio dati: " + e.getMessage());
        }
    }
}
