package com.example.vehicle_parking_management_system.repository;

import com.example.vehicle_parking_management_system.model.Feedback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * FeedbackRepository — reads and writes feedback.csv.
 *
 * CSV format: id,driverId,reservationId,rating,category,comment,submittedAt,status
 * Note: comment is wrapped in double-quotes to handle embedded commas.
 */
@Repository
public class FeedbackRepository {

    @Value("${parknow.data.feedback}")
    private String filePath;

    // ── Read operations ───────────────────────────────────────────────────────

    public List<Feedback> findAll() {
        List<Feedback> feedbacks = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return feedbacks;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Feedback f = parseLine(line);
                if (f != null) feedbacks.add(f);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read feedback.csv: " + e.getMessage(), e);
        }
        return feedbacks;
    }

    public Optional<Feedback> findById(String id) {
        return findAll().stream().filter(f -> f.getId().equals(id)).findFirst();
    }

    public List<Feedback> findByDriverId(String driverId) {
        List<Feedback> result = new ArrayList<>();
        for (Feedback f : findAll()) {
            if (f.getDriverId().equals(driverId)) result.add(f);
        }
        return result;
    }

    

    // ── Write operations ──────────────────────────────────────────────────────

    //save new feedback
    public void save(Feedback feedback) {
        ensureFileExists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(feedback.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to feedback.csv: " + e.getMessage(), e);
        }
    }

    // Update an existing feedback entry. Returns true if successful, false if not found.
    public boolean update(Feedback updated) {
        List<Feedback> all = findAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(updated.getId())) {
                all.set(i, updated);
                found = true;
                break;
            }
        }
        if (found) rewriteAll(all);
        return found;
    }

    // Delete a feedback entry by ID. Returns true if successful, false if not found.
    public boolean deleteById(String id) {
        List<Feedback> all = findAll();
        boolean removed = all.removeIf(f -> f.getId().equals(id));
        if (removed) rewriteAll(all);
        return removed;
    }

    /** Remove all feedback submitted by a driver. Returns number of rows removed. */
    public int deleteByDriverId(String driverId) {
        List<Feedback> all = findAll();
        int before = all.size();
        all.removeIf(f -> driverId != null && driverId.equals(f.getDriverId()));
        int removed = before - all.size();
        if (removed > 0) rewriteAll(all);
        return removed;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void rewriteAll(List<Feedback> feedbacks) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (Feedback f : feedbacks) pw.println(f.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite feedback.csv: " + e.getMessage(), e);
        }
    }

    private void ensureFileExists() {
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try { f.createNewFile(); } catch (IOException e) {
                throw new RuntimeException("Cannot create feedback.csv", e);
            }
        }
    }

    /**
     * Parse: id,driverId,reservationId,rating,category,"comment",submittedAt,status
     * Uses a simple quoted-field parser for the comment column.
     */
    private Feedback parseLine(String line) {
        // Split respecting quoted comment field
        List<String> parts = splitCsvLine(line);
        // id,driverId,rating,category,"comment",submittedAt
        if (parts.size() < 6) return null;
        
        Feedback f = new Feedback();
        f.setId(parts.get(0).trim());
        f.setDriverId(parts.get(1).trim());
        f.setRating(parseIntSafe(parts.get(2).trim()));
        f.setCategory(parts.get(3).trim());
        // Strip surrounding quotes from comment
        String comment = parts.get(4).trim();
        if (comment.startsWith("\"") && comment.endsWith("\""))
            comment = comment.substring(1, comment.length() - 1);
        comment = comment.replace("\"\"", "\"");
        f.setComments(comment);
        try { f.setSubmittedAt(LocalDateTime.parse(parts.get(5).trim())); }
        catch (Exception e) { f.setSubmittedAt(LocalDateTime.now()); }
        return f;
    }

    /** Simple CSV splitter that respects double-quoted fields. */
    private List<String> splitCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
                sb.append(c);
            } else if (c == ',' && !inQuotes) {
                result.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        result.add(sb.toString());
        return result;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
}
