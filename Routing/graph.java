package Routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class graph {

    static class Edge {

        int destination;
        int weight;

        public Edge(int destination, int weight) {
            this.destination = destination;
            this.weight = weight;
        }
    }

    private Map<Integer, List<Edge>> adjacencyList;

    public graph() {
        adjacencyList = new HashMap<>();
    }

    public int getSize() {
        return adjacencyList.size();
    }

    public void addServer(int serverID) {
        adjacencyList.putIfAbsent(serverID, new ArrayList<>());
    }

    public void addConnection(int source, int destination, int weight) {
        adjacencyList.get(source).add(new Edge(destination, weight));
        adjacencyList.get(destination).add(new Edge(source, weight));
    }

    public List<Edge> getNeighbors(int serverID) {
        return adjacencyList.getOrDefault(serverID, new ArrayList<>());
    }

    public Set<Integer> getServers() {
        return adjacencyList.keySet();
    }

    public List<int[]> getConnections() {
        List<int[]> connections = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (Map.Entry<Integer, List<Edge>> entry : adjacencyList.entrySet()) {
            int source = entry.getKey();

            for (Edge edge : entry.getValue()) {
                int from = Math.min(source, edge.destination);
                int to = Math.max(source, edge.destination);
                String key = from + "-" + to;

                if (visited.add(key)) {
                    connections.add(new int[] { from, to, edge.weight });
                }
            }
        }

        return connections;
    }

    public void displayGraph() {

        System.out.println("\nServer Network Topology:");

        for (int server : adjacencyList.keySet()) {
            System.out.print("Server " + server + " -> ");

            for (Edge edge : adjacencyList.get(server)) {
                System.out.print("(Server " + edge.destination + ", latency " + edge.weight + ") ");
            }

            System.out.println();
        }
    }
}
