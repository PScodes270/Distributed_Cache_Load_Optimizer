package monitoring;

public class SystemEvent {

    private final String time;
    private final String type;
    private final String message;

    public SystemEvent(String time, String type, String message) {
        this.time = time;
        this.type = type;
        this.message = message;
    }

    public String getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
