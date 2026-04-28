package Routing;

import java.util.*;

public class dijikstra {

    static class Node implements Comparable<Node> {

        int server;
        int distance;

        public Node(int server, int distance) {
            this.server = server;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    public Map<Integer, Integer> shortestPath(graph g, int source) {

        Map<Integer, Integer> distance = new HashMap<>();

        for(int server : g.getServers()){
            distance.put(server, Integer.MAX_VALUE);
        }

        distance.put(source, 0);

        PriorityQueue<Node> pq = new PriorityQueue<>();
        pq.add(new Node(source,0));

        while(!pq.isEmpty()){

            Node current = pq.poll();
            int currentServer = current.server;
            int currentDistance = current.distance;

            for(graph.Edge edge : g.getNeighbors(currentServer)){

                int neighbor = edge.destination;
                int weight = edge.weight;

                int newDist = currentDistance + weight;

                if(newDist < distance.get(neighbor)){

                    distance.put(neighbor, newDist);
                    pq.add(new Node(neighbor,newDist));
                }
            }
        }

        return distance;
    }
}

