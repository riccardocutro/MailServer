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
import java.util.function.Consumer;

/**
 * Core del Mail Server.
 * <p>
 * Accetta connessioni client su una porta TCP, delega le richieste a
 * {@link RequestHandler} e gestisce ciclo di vita (start/stop).
 * </p>
 */
public class ServerCore {

    private final int port;
    private final MailStore store;
    private final Consumer<String> log;

    private volatile boolean running = false;
    private Thread acceptorThread;
    private ServerSocket serverSocket;
    private ExecutorService pool;

    /**
     * @param port  porta TCP di ascolto
     * @param store archivio dati condiviso
     * @param log   callback per log eventi
     */
    public ServerCore(int port, MailStore store, Consumer<String> log) {
        this.port = port;
        this.store = Objects.requireNonNull(store);
        this.log = Objects.requireNonNull(log);
    }

    /** Avvia il server se non già attivo. */
    public synchronized void start() {
        if (running) return;
        running = true;

        if (pool == null || pool.isShutdown() || pool.isTerminated()) {
            ThreadFactory tf = r -> {
                Thread t = new Thread(r, "server-worker");
                t.setDaemon(true);
                return t;
            };
            pool = Executors.newCachedThreadPool(tf);
        }

        acceptorThread = new Thread(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                serverSocket = ss;
                log.accept("Server in ascolto su porta " + port); 
                while (running) {
                    try {
                        Socket client = ss.accept();
                        log.accept("🔌 Connessione da " + client.getRemoteSocketAddress());
                        pool.submit(new RequestHandler(client, store, log));
                    } catch (SocketException se) {
                        if (running) log.accept("⚠️ Errore socket: " + se.getMessage());
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
                log.accept("👂 Listener terminato.");
            }
        }, "server-acceptor");

        acceptorThread.setDaemon(true);
        acceptorThread.start();
    }

    /** Ferma il server e libera le risorse. */
    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException ignored) {}
        if (pool != null) pool.shutdownNow();
        if (acceptorThread != null && acceptorThread.isAlive()) {
            try { acceptorThread.join(1500); } catch (InterruptedException ignored) {}
        }
        log.accept("Server arrestato.");
    }

    /** @return true se il server è in esecuzione. */
    public boolean isRunning() {
        return running;
    }
}
