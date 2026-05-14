package com.example.vehicle_parking_management_system.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

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
}
