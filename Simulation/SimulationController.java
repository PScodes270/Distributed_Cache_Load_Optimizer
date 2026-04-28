package Simulation;

import model.request;
import monitoring.SystemMonitor;
import queue.RequestQueue;

import java.util.Random;

public class SimulationController {

    private static final int MIN_RATE = 1;
    private static final int MAX_RATE = 25;

    private final RequestQueue queue;
    private final Random rand;
    private final Object pauseLock;

    private volatile boolean paused;
    private volatile boolean started;
    private volatile int requestsPerSecond;

    public SimulationController(RequestQueue queue) {
        this.queue = queue;
        this.rand = new Random();
        this.pauseLock = new Object();
        this.requestsPerSecond = 10;
    }

    public void startSimulation() {
        if (started) {
            return;
        }

        started = true;

        Thread simulationThread = new Thread(() -> {
            while (true) {
                try {
                    waitIfPaused();

                    int id = rand.nextInt(20) + 1;
                    int priority = rand.nextInt(5) + 1;

                    request req = new request(id, priority);
                    queue.addRequest(req);

                    SystemMonitor.logEvent("REQUEST", "Queued Request " + id + " with priority " + priority);
                    System.out.println("Generated Request -> ID: " + id + " Priority: " + priority);

                    Thread.sleep(Math.max(40, 1000 / requestsPerSecond));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "simulation-generator");

        simulationThread.setDaemon(true);
        simulationThread.start();
        SystemMonitor.logEvent("SYSTEM", "Simulation started at " + requestsPerSecond + " req/sec");
    }

    private void waitIfPaused() throws InterruptedException {
        synchronized (pauseLock) {
            while (paused) {
                pauseLock.wait();
            }
        }
    }

    public void pauseSimulation() {
        paused = true;
        SystemMonitor.logEvent("SYSTEM", "Simulation paused");
    }

    public void resumeSimulation() {
        synchronized (pauseLock) {
            paused = false;
            pauseLock.notifyAll();
        }
        SystemMonitor.logEvent("SYSTEM", "Simulation resumed");
    }

    public void toggleSimulation() {
        if (paused) {
            resumeSimulation();
        } else {
            pauseSimulation();
        }
    }

    public boolean isPaused() {
        return paused;
    }

    public int getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(int requestsPerSecond) {
        this.requestsPerSecond = Math.max(MIN_RATE, Math.min(MAX_RATE, requestsPerSecond));
        SystemMonitor.logEvent("SYSTEM", "Simulation rate adjusted to " + this.requestsPerSecond + " req/sec");
    }
}
