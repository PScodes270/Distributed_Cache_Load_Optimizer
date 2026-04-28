package monitoring;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class SystemMonitor {

    private static final int MAX_EVENTS = 80;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Deque<SystemEvent> RECENT_EVENTS = new ArrayDeque<>();

    private SystemMonitor() {
    }

    public static synchronized void logEvent(String type, String message) {
        RECENT_EVENTS.addFirst(new SystemEvent(LocalTime.now().format(TIME_FORMATTER), type, message));

        while (RECENT_EVENTS.size() > MAX_EVENTS) {
            RECENT_EVENTS.removeLast();
        }
    }

    public static synchronized List<SystemEvent> getRecentEvents() {
        return new ArrayList<>(RECENT_EVENTS);
    }
}
