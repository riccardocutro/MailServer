package it.unito.prog3.mailserver.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Avvia il server e rimane in ascolto per le connessioni client.
 * Usa un pool di thread scalabile (CachedThreadPool) e mostra
 * il numero di client connessi in tempo reale.
 */
public class ServerCore {

    private static final int PORT = 12345;
    private final ExecutorService pool;
    private final AtomicInteger connectedClients = new AtomicInteger(0);

    public ServerCore() {
        // Pool scalabile: crea nuovi thread al bisogno e li riusa
        pool = Executors.newCachedThreadPool();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("📡 Mail Server in ascolto sulla porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                int current = connectedClients.incrementAndGet();
                System.out.println("🔌 Nuovo client connesso. Totale: " + current);

                pool.execute(() -> {
                    try {
                        new RequestHandler(clientSocket).run();
                    } finally {
                        int remaining = connectedClients.decrementAndGet();
                        System.out.println("❎ Client disconnesso. Totale: " + remaining);
                    }
                });
            }
        } catch (IOException e) {
            System.err.println("❌ Errore avvio server: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ServerCore().start();
    }
}
