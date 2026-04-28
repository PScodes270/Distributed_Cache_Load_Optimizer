package LoadBalancer;

import model.server;
import model.request;
import Routing.dijikstra;
import Routing.graph;
import monitoring.ServerSnapshot;
import monitoring.SystemMonitor;
import queue.RequestQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class loadbalancer {

    private PriorityQueue<server> serverQueue;
    private List<server> serverList;

    private graph network;
    private dijikstra router;

    private RequestQueue requestQueue;

    public loadbalancer(graph network, RequestQueue queue) {

        serverQueue = new PriorityQueue<>();
        serverList = new ArrayList<>();

        this.network = network;
        this.requestQueue = queue;

        router = new dijikstra();
    }

    public synchronized void addServer(server server) {
        serverQueue.add(server);
        serverList.add(server);
    }

    public void handleRequest(request req) {

        server chosenServer = null;
        int bestScore = Integer.MAX_VALUE;

        synchronized (this) {

            if (serverList.isEmpty()) {
                System.out.println("No servers available!");
                return;
            }

            int source = req.getRequestID() % network.getSize();
            Map<Integer, Integer> distances = router.shortestPath(network, source);

            for (server s : serverList) {
                if (!s.isActive()) {
                    continue;
                }

                int serverID = s.getserverID();
                int latency = distances.getOrDefault(serverID, Integer.MAX_VALUE);
                int load = s.getcurrentLoad();
                int score = latency + (load * 5);

                if (score < bestScore) {
                    bestScore = score;
                    chosenServer = s;
                }
            }
        }

        if (chosenServer == null) {
            System.out.println("No active server, retrying request: " + req);
            SystemMonitor.logEvent("SERVER", "No active server available for " + req);

            if (req.getRetryCount() < 3) {
                req.incrementRetry();

                try {
                    Thread.sleep(200);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                requestQueue.addRequest(req);
            } else {
                System.out.println("Dropping request after max retries: " + req);
                SystemMonitor.logEvent("SERVER", "Dropped " + req + " after max retries");
            }

            return;
        }

        System.out.println(req + " routed to Server " + chosenServer.getserverID());
        SystemMonitor.logEvent(
                "ROUTE",
                req + " routed to Server " + chosenServer.getserverID() + " with score " + bestScore);

        chosenServer.increaseLoad();

        try {
            String result = chosenServer.processRequest(req.getRequestID());
            System.out.println(result);

            if (Math.random() < 0.01) {
                chosenServer.failServer();
                System.out.println("Server " + chosenServer.getserverID() + " FAILED!");
                SystemMonitor.logEvent("SERVER",
                        "Server " + chosenServer.getserverID() + " failed and entered recovery");
            }
        } finally {
            chosenServer.decreaseLoad();
        }

        synchronized (this) {
            serverQueue.remove(chosenServer);
            serverQueue.add(chosenServer);
        }
    }

    public synchronized void displayServers() {

        System.out.println("\nServer Status:");

        for (server s : serverQueue) {
            System.out.println(s);
        }
    }

    public synchronized List<ServerSnapshot> getServerSnapshots() {
        List<ServerSnapshot> snapshots = new ArrayList<>();

        for (server currentServer : serverList) {
            snapshots.add(new ServerSnapshot(
                    currentServer.getserverID(),
                    currentServer.getcurrentLoad(),
                    currentServer.getProcessedRequests(),
                    currentServer.getServerCacheHits(),
                    currentServer.getServerCacheMisses(),
                    currentServer.getLastLatencyMillis(),
                    currentServer.getCacheOccupancy(),
                    currentServer.getCacheCapacity(),
                    currentServer.isActive()));
        }

        return snapshots;
    }
}
