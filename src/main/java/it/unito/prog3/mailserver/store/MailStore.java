package it.unito.prog3.mailserver.store;

import it.unito.prog3.mailserver.model.Email;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Classe che gestisce la memorizzazione e la gestione delle caselle email.
 * <p>
 * Ogni account ha una inbox rappresentata da una lista di {@link Email}.
 * L'accesso concorrente è gestito tramite strutture thread-safe.
 * </p>
 */
public class MailStore {

    /** Insieme degli account registrati sul server */
    private final Set<String> accounts = new HashSet<>();
    /** Mappa indirizzo → inbox (lista dei messaggi) */
    private final Map<String, List<Email>> boxes = new ConcurrentHashMap<>();
    /** Generatore atomico di ID univoci per le email */
    private final AtomicLong idGen = new AtomicLong(0);
    /** Funzione di log per inviare messaggi alla GUI */
    private final Consumer<String> log;

    /**
     * Crea un nuovo MailStore e carica gli account di base.
     *
     * @param log funzione di logging (accetta stringhe)
     */
    public MailStore(Consumer<String> log) {
        this.log = log;

        List<String> base = List.of(
                "riccardo@mail.com",
                "davide@mail.com",
                "orlando@mail.it"
        );

        for (String a : base) {
            accounts.add(a);
            boxes.put(a, Collections.synchronizedList(new ArrayList<>()));
        }

        log.accept("Account caricati: " + accounts);
    }

    /**
     * Verifica se un account esiste.
     *
     * @param email indirizzo email
     * @return true se esiste, false altrimenti
     */
    public boolean exists(String email) {
        return accounts.contains(email);
    }

    /**
     * Genera un nuovo ID univoco per un'email.
     *
     * @return ID univoco
     */
    public long nextId() {
        return idGen.incrementAndGet();
    }

    /**
     * Consegna un'email a tutti i destinatari.
     *
     * @param e email da consegnare
     * @throws IllegalArgumentException se uno dei destinatari non esiste
     */
    public void deliver(Email e) {
        for (String to : e.to()) {
            if (!exists(to)) {
                throw new IllegalArgumentException("Unknown recipient: " + to);
            }
            boxes.get(to).add(e);
        }
    }

    /**
     * Restituisce tutti i messaggi ricevuti dopo un certo ID.
     *
     * @param email   account destinatario
     * @param afterId ID dopo il quale restituire i messaggi
     * @return lista di nuovi messaggi
     */
    public List<Email> getNew(String email, long afterId) {
        if (!exists(email)) return List.of();
        List<Email> all = boxes.get(email);
        List<Email> res = new ArrayList<>();
        synchronized (all) {
            for (Email e : all) {
                if (e.id() > afterId) res.add(e);
            }
        }
        return res;
    }

    /**
     * Elimina un messaggio dalla inbox di un utente.
     *
     * @param email account proprietario della inbox
     * @param id    ID del messaggio da eliminare
     * @return true se il messaggio è stato rimosso, false altrimenti
     */
    public boolean delete(String email, long id) {
        if (!exists(email)) return false;
        List<Email> all = boxes.get(email);
        synchronized (all) {
            return all.removeIf(e -> e.id() == id);
        }
    }

    /**
     * Costruisce una nuova {@link Email}.
     *
     * @param from    mittente
     * @param to      destinatari
     * @param subject oggetto
     * @param body    corpo del messaggio
     * @return nuova email
     */
    public Email buildEmail(String from, List<String> to, String subject, String body) {
        return new Email(nextId(), from, to, subject, body, Instant.now());
    }
}
