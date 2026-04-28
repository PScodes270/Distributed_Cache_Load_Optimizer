package model;

public class request implements Comparable<request> {

    private int requestID;
    private int priority;

    public request(int requestID, int priority) {
        this.requestID = requestID;
        this.priority = priority;
    }

    public int getRequestID() {
        return requestID;
    }

    public int getPriority() {
        return priority;
    }
 
    private int retryCount = 0;

    public int getRetryCount() {
        return retryCount;
    }

    public void incrementRetry() {
        retryCount++;
    }
    @Override
    public int compareTo(request other) {
       
        return this.priority - other.priority;
    }

    @Override
    public String toString() {
        return "Request " + requestID + " [Priority: " + priority + "]";
    }
}
