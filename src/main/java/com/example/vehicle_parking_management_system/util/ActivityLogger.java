package com.example.vehicle_parking_management_system.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ActivityLogger — shared utility for system-wide event logging.
 *
 * Appends one CSV row per event to activity-log.csv.
 * Any component can call ActivityLogger.log() to record an action.
 *
 * CSV format: timestamp,actorId,actorRole,action,detail
 */
@Component
public class ActivityLogger {

    @Value("${parknow.data.activity-log:default_activity.log}")
    private String logPath;

    /**
     * Log a system event.
     *
     * @param actorId   ID of the user performing the action (or "SYSTEM")
     * @param actorRole Role of the actor (DRIVER / ADMIN / SYSTEM)
     * @param action    Short action name, e.g. "USER_LOGIN", "SLOT_ALLOCATED"
     * @param detail    Optional human-readable detail string
     */
    public synchronized void log(String actorId, String actorRole,
                                 String action, String detail) {
        String row = String.join(",",
                LocalDateTime.now().toString(),
                actorId    == null ? "SYSTEM" : actorId,
                actorRole  == null ? "SYSTEM" : actorRole,
                action,
                "\"" + (detail == null ? "" : detail.replace("\"", "'")) + "\"");

        try (PrintWriter pw = new PrintWriter(new FileWriter(logPath, true))) {
            pw.println(row);
        } catch (IOException e) {
            // Logger failure must not break the calling operation
            System.err.println("[ActivityLogger] Failed to write log entry: " + e.getMessage());
        }
    }

    /** Convenience overload with no detail. */
    public void log(String actorId, String actorRole, String action) {
        log(actorId, actorRole, action, null);
    }
    
    /** One parsed row from the activity log file. */
    public record LogEntry(String timestamp, String actorId, String actorRole, String action, String detail) {}

    /**
     * Read the most recent log entries (newest first).
     */
    public List<LogEntry> readRecent(int limit) {
        List<LogEntry> all = readAll();
        Collections.reverse(all);
        if (limit <= 0 || all.size() <= limit) return all;
        return new ArrayList<>(all.subList(0, limit));
    }

    /** Read all entries in file order (oldest first). */
    public List<LogEntry> readAll() {
        List<LogEntry> entries = new ArrayList<>();
        File file = new File(logPath);
        if (!file.exists()) return entries;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                LogEntry entry = parseLine(line);
                if (entry != null) entries.add(entry);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read activity log: " + e.getMessage(), e);
        }
        return entries;
    }

    /**
     * Parse: timestamp,actorId,actorRole,action,"detail"
     */
    static LogEntry parseLine(String line) {
        String[] parts = line.split(",", 5);
        if (parts.length < 4) return null;
        String detail = parts.length > 4 ? unquote(parts[4].trim()) : "";
        return new LogEntry(
                parts[0].trim(),
                parts[1].trim(),
                parts[2].trim(),
                parts[3].trim(),
                detail);
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
