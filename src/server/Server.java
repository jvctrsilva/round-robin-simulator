package server;

import core.Scheduler;

import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.*;

public class Server implements AutoCloseable {
    private final int port;
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private volatile boolean running = false;
    private volatile Scheduler scheduler;             // será setado depois
    private final Queue<ProcessRequest> buffer = new ConcurrentLinkedQueue<>(); // buffer antes do scheduler
    private ServerSocket server;

    public Server(int port) {
        this.port = port;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
        // drena o buffer pendente (se já recebeu antes de setar)
        ProcessRequest req;
        while ((req = buffer.poll()) != null) {
            scheduler.submit(req.entryTime, req.executeTime, req.blocked, req.blockedTime);
        }
    }

    public void start() {
        pool.execute(() -> {
            try (ServerSocket ss = new ServerSocket(port)) {
                this.server = ss;
                this.running = true;
                System.out.println("[server] escutando na porta: " + port);
                while (running) {
                    Socket client = ss.accept();
                    pool.execute(() -> handleClient(client));
                }
            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        });
    }

    private void handleClient(Socket client) {
        try (Socket c = client; ObjectInputStream in = new ObjectInputStream(c.getInputStream())) {
            Object o = in.readObject();
            if (o instanceof ProcessRequest pr) {
                System.out.println("[server] received: " + pr);
                Scheduler s = this.scheduler;
                if (s != null) {
                    s.submit(pr.entryTime, pr.executeTime, pr.blocked, pr.blockedTime);
                } else {
                    buffer.add(pr); // ainda não temos scheduler: guarda pra depois
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override public void close() {
        running = false;
        try { if (server != null) server.close(); } catch (Exception ignore) {}
        pool.shutdownNow();
    }
}
