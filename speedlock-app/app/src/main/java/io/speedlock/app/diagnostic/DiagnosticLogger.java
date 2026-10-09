package io.speedlock.app.diagnostic;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Structured in-memory and exportable logger for diagnostic events.
 */
public class DiagnosticLogger {

    public enum Level {
        DEBUG, INFO, WARN, ERROR
    }

    public static class LogEntry {
        public final long timestamp;
        public final Level level;
        public final String tag;
        public final String message;

        public LogEntry(Level level, String tag, String message) {
            this.timestamp = System.currentTimeMillis();
            this.level = level;
            this.tag = tag;
            this.message = message;
        }

        @Override
        public String toString() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            return String.format("[%s] [%s] [%s]: %s", sdf.format(new Date(timestamp)), level, tag, message);
        }
    }

    private final List<LogEntry> entries = new ArrayList<>();

    public synchronized void log(Level level, String tag, String message) {
        entries.add(new LogEntry(level, tag, message));
    }

    public synchronized void info(String tag, String message) {
        log(Level.INFO, tag, message);
    }

    public synchronized void warn(String tag, String message) {
        log(Level.WARN, tag, message);
    }

    public synchronized void error(String tag, String message) {
        log(Level.ERROR, tag, message);
    }

    public synchronized void debug(String tag, String message) {
        log(Level.DEBUG, tag, message);
    }

    public synchronized List<LogEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    public synchronized String getFormattedLog() {
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : entries) {
            sb.append(e.toString()).append("\n");
        }
        return sb.toString();
    }
}
