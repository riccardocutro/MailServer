package it.unito.prog3.mailserver.net;

import it.unito.prog3.mailserver.store.MailStore;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ServerCore {
    private final int port;
    private final Consumer<String> log;
    private final MailStore store;
    private volatile boolean running;
    private Thread acceptor;
    private final ExecutorService pool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public ServerCore(int port, MailStore store, Consumer<String> log) {
        this.port = port;
        this.store = store;
        this.log = log;
    }

    public void start() {
        if (running) return;
        running = true;
        acceptor = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                log.accept("Server in ascolto sulla porta " + port);
                while (running) {
                    Socket s = ss.accept();
                    pool.submit(new RequestHandler(s, store, log));
                }
            } catch (Exception e) {
                log.accept("Errore server: " + e.getMessage());
            }
        }, "server-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    public void stop() {
        running = false;
        pool.shutdownNow();
        log.accept("Server arrestato.");
    }
}
