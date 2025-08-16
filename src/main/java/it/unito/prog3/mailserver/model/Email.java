package it.unito.prog3.mailserver.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Rappresenta un'email memorizzata sul server.
 * @param id identificativo univoco (incrementale)
 * @param from indirizzo del mittente
 * @param to lista di destinatari
 * @param subject oggetto del messaggio
 * @param body corpo del messaggio
 * @param timestamp data/ora di ricezione
 */
public record Email(
        long id,
        String from,
        List<String> to,
        String subject,
        String body,
        Instant sentAt) implements Serializable {}
