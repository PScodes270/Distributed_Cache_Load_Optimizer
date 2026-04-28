package Cache;

import java.util.HashMap;

public class LRUCache {

    class Node {
        int key;
        int value;
        Node prev;
        Node next;

        Node(int k, int v) {
            key = k;
            value = v;
        }
    }

    private int capacity;
    private HashMap<Integer, Node> map;

    private Node head;
    private Node tail;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        map = new HashMap<>();

        head = new Node(0, 0);
        tail = new Node(0, 0);

        head.next = tail;
        tail.prev = head;
    }

    private void remove(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    private void insert(Node node) {
        node.next = head.next;
        node.prev = head;

        head.next.prev = node;
        head.next = node;
    }


    public synchronized int get(int key) {

        if (!map.containsKey(key)) {
            return -1;
        }

        Node node = map.get(key);

        remove(node);
        insert(node);

        return node.value;
    }


    public synchronized void put(int key, int value) {

        if (capacity == 0) return;

        if (map.containsKey(key)) {

            Node node = map.get(key);
            node.value = value;

            remove(node);
            insert(node);

        } else {

            if (map.size() >= capacity) {

                Node lru = tail.prev;

        
                if (lru != null && lru != head) {
                    remove(lru);
                    map.remove(lru.key);
                }
            }

            Node newNode = new Node(key, value);

            map.put(key, newNode);
            insert(newNode);
        }
    }
}
