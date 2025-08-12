package it.unito.prog3.mailserver.model;

import java.time.Instant;
import java.util.List;

public record Email(long id, String from, List<String> to, String subject, String body, Instant sentAt) {}
