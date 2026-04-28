package App;

import LoadBalancer.loadbalancer;
import Metrics.metrics;
import Routing.graph;
import Simulation.SimulationController;
import model.request;
import model.server;
import monitoring.DashboardSnapshot;
import monitoring.SystemMonitor;
import queue.RequestQueue;
import ui.DashBoard;

import javax.swing.SwingUtilities;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    public static void main(String[] args) {

        System.out.println("===== Distributed Cache & Intelligent Load Optimizer =====\n");
        SystemMonitor.logEvent("SYSTEM", "Distributed Cache Load Optimizer boot sequence started");

        graph network = new graph();

        for (int i = 0; i < 10; i++) {
            network.addServer(i);
        }

        network.addConnection(0, 1, 4);
        network.addConnection(0, 2, 2);
        network.addConnection(1, 2, 1);
        network.addConnection(1, 3, 5);
        network.addConnection(2, 3, 8);

        network.addConnection(2, 4, 3);
        network.addConnection(3, 5, 2);
        network.addConnection(4, 5, 4);
        network.addConnection(4, 6, 6);
        network.addConnection(5, 7, 3);

        network.addConnection(6, 7, 1);
        network.addConnection(6, 8, 7);
        network.addConnection(7, 8, 2);
        network.addConnection(8, 9, 3);

        network.addConnection(0, 9, 10);
        network.addConnection(1, 7, 6);
        network.addConnection(2, 8, 5);
        network.addConnection(3, 9, 4);
        network.addConnection(5, 9, 2);

        network.displayGraph();

        RequestQueue requestQueue = new RequestQueue();
        loadbalancer lb = new loadbalancer(network, requestQueue);
        SimulationController simulation = new SimulationController(requestQueue);

        server s1 = new server(1);
        server s2 = new server(2);
        server s3 = new server(3);

        lb.addServer(s1);
        lb.addServer(s2);
        lb.addServer(s3);

        System.out.println("\nServers initialized.\n");
        SystemMonitor.logEvent("SYSTEM", "Three application servers were registered with the balancer");

        final DashBoard[] dashboard = new DashBoard[1];
        SwingUtilities.invokeLater(() -> dashboard[0] = new DashBoard(network, simulation));

        simulation.startSimulation();

        ExecutorService executor = Executors.newFixedThreadPool(6);

        for (int i = 0; i < 6; i++) {
            executor.submit(() -> {

                while (true) {
                    try {
                        if (!requestQueue.isEmpty()) {
                            request req = requestQueue.getNextRequest();

                            if (req != null) {
                                lb.handleRequest(req);
                                System.out.println("---------------");
                            }
                        }

                        Thread.sleep(300);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        Thread metricsThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000);
                    metrics.printStats();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "metrics-printer");
        metricsThread.setDaemon(true);
        metricsThread.start();

        Thread dashboardThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);

                    if (dashboard[0] != null) {
                        dashboard[0].update(new DashboardSnapshot(
                                requestQueue.size(),
                                metrics.getTotalRequests(),
                                metrics.getCacheHits(),
                                metrics.getCacheMisses(),
                                metrics.getAverageLatency(),
                                metrics.getHitRatio(),
                                simulation.isPaused(),
                                simulation.getRequestsPerSecond(),
                                lb.getServerSnapshots(),
                                SystemMonitor.getRecentEvents(),
                                System.currentTimeMillis()
                        ));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "dashboard-refresh");
        dashboardThread.setDaemon(true);
        dashboardThread.start();
    }
}
