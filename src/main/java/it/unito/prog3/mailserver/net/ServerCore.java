package it.unito.prog3.mailserver.net;

import it.unito.prog3.mailserver.store.MailStore;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class ServerCore {

    // porta di ascolto, archivio, logger
    private final int port;
    private final MailStore store;
    private final Consumer<String> log;

    // callback verso la GUI per aggiornare il contatore connessioni (no-op se null)
    private final IntConsumer onConnCount;

    // stato interno
    private volatile boolean running = false;
    private Thread acceptorThread;
    private ServerSocket serverSocket;
    private ExecutorService pool;

    // contatore connessioni attive (incrementa su accept, decrementa quando l'handler termina)
    private final AtomicInteger activeConns = new AtomicInteger(0);

    // costruttore “esteso”: permette di passare la callback per la GUI
    public ServerCore(int port, MailStore store, Consumer<String> log, IntConsumer onConnCount) {
        this.port = port;
        this.store = Objects.requireNonNull(store);
        this.log = Objects.requireNonNull(log);
        this.onConnCount = onConnCount != null ? onConnCount : (n -> {});
    }

    public ServerCore(int port, MailStore store, Consumer<String> log) {
        this(port, store, log, null);
    }

    // Avvia il server se non già attivo
    public synchronized void start() {
        if (running) return;
        running = true;

        // crea (o ricrea) il thread pool per i worker
        if (pool == null || pool.isShutdown() || pool.isTerminated()) {
            ThreadFactory tf = r -> {
                Thread t = new Thread(r, "server-worker");
                t.setDaemon(true);
                return t;
            };
            pool = Executors.newCachedThreadPool(tf);
        }

        // thread acceptor che accetta connessioni e le delega al pool
        acceptorThread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                log.accept("Server in ascolto su porta " + port);

                while (running) {
                    try {
                        Socket client = ss.accept();

                        // appena accetti, incrementa e notifica la GUI
                        int now = activeConns.incrementAndGet();
                        onConnCount.accept(now);
                        log.accept("Connessione da " + client.getRemoteSocketAddress() + " (attive: " + now + ")");

                        // callback da invocare quando la richiesta è finita
                        Runnable onClose = () -> {
                            int left = activeConns.decrementAndGet();
                            onConnCount.accept(left);
                        };

                        // delega al worker
                        pool.submit(new RequestHandler(client, store, log, onClose));

                    } catch (SocketException se) {
                        // se running è ancora true, logga l'errore; altrimenti è una chiusura attesa
                        if (running) log.accept("Errore socket: " + se.getMessage());
                        break;
                    }
                }
            } catch (BindException be) {
                log.accept("Porta " + port + " occupata: " + be.getMessage());
            } catch (IOException ioe) {
                if (running) log.accept("Errore server: " + ioe.getMessage());
            } finally {
                running = false;
                serverSocket = null;
                log.accept("Listener terminato.");
            }
        }, "server-acceptor");

        acceptorThread.setDaemon(true);
        acceptorThread.start();
    }

    // Ferma il server e libera le risorse
    public synchronized void stop() {
        running = false;

        // chiude il socket di ascolto per sbloccare l'accept
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}

        // interrompe i worker correnti
        if (pool != null) pool.shutdownNow();

        // aspetta (poco) la fine dell'acceptor
        if (acceptorThread != null && acceptorThread.isAlive()) {
            try { acceptorThread.join(1500); } catch (InterruptedException ignored) {}
        }

        log.accept("Server arrestato.");

        // opzionale: forza refresh GUI a 0 (in caso di chiusure “brusche”)
        int left = Math.max(0, activeConns.get());
        if (left != 0) {
            activeConns.set(0);
            onConnCount.accept(0);
        }
    }

    // stato running (true se il server è attivo)
    public boolean isRunning() {
        return running;
    }
}
