package queue;

import model.request;
import java.util.PriorityQueue;

public class RequestQueue {

    private PriorityQueue<request> queue;

    public RequestQueue() {
        queue = new PriorityQueue<>();
    }

    public synchronized void addRequest(request req) {
        queue.add(req);
    }

    public synchronized request getNextRequest() {
        return queue.poll();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }

    public synchronized int size() {
        return queue.size();
    }
}
