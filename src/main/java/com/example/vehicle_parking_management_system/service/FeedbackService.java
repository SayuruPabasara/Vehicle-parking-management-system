package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.Feedback;
import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.repository.FeedbackRepository;
import com.example.vehicle_parking_management_system.repository.UserRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FeedbackService — business logic for Component 06 (Feedback & Review Management).
 *
 * Polymorphism: uses RatingDisplay interface to render feedback differently
 * for drivers vs admins without changing the Feedback data.
 */
@Service
public class FeedbackService {

    private final FeedbackRepository    feedbackRepository;
    private final ReservationService    reservationService;
    private final ActivityLogger        activityLogger;
    private final UserRepository        userRepository;

    // Polymorphic display renderers
    private final Feedback.RatingDisplay driverDisplay = new Feedback.DriverRatingDisplay();
    private final Feedback.RatingDisplay adminDisplay  = new Feedback.AdminRatingDisplay();

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ReservationService reservationService,
                           ActivityLogger activityLogger,
                           UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.reservationService = reservationService;
        this.activityLogger     = activityLogger;
        this.userRepository     = userRepository;
    }

    // ── Submit feedback ───────────────────────────────────────────────────────
    public Feedback submitFeedback(String driverId, int rating,String category, String comment) {
        
        // Validate rating range
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        Feedback feedback = new Feedback(
                IdGenerator.next("FBK"),
                driverId,
                rating,
                category,
                comment   
        );
        feedbackRepository.save(feedback);
        activityLogger.log(driverId, "DRIVER", "FEEDBACK_SUBMITTED", "Rating: " + rating);
        return feedback;
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> getFeedbackByDriver(String driverId) {
        return feedbackRepository.findByDriverId(driverId);
    }

    /** Returns all feedback. */
    public List<Feedback> getApprovedFeedback() {
        return feedbackRepository.findAll();
    }

    public Optional<Feedback> findById(String feedbackId) {
        return feedbackRepository.findById(feedbackId);
    }

    /**
     * Calculate the average rating across all approved feedback.
     * Returns 0.0 if no approved feedback exists.
     */
    public double calculateAverageRating() {
        List<Feedback> approved = getAllFeedback();
        if (approved.isEmpty()) return 0.0;
        double sum = approved.stream().mapToInt(Feedback::getRating).sum();
        double avg = sum / approved.size();
        return Math.round(avg * 10.0) / 10.0; // 1 decimal place
    }

    /**
     * Return feedback grouped by category with average rating per category.
     * Useful for admin analytics panel.
     */
    public Map<String, Double> averageRatingByCategory() {
        return feedbackRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        f -> f.getCategory() != null && !f.getCategory().isBlank()
                                ? f.getCategory() : "GENERAL",
                        Collectors.averagingInt(Feedback::getRating)
                ));
    }

    /** Stats + table rows for admin feedback management page. */
    public Map<String, Object> getAdminFeedbackManagementData() {
        List<Feedback> all = new ArrayList<>(feedbackRepository.findAll());
        all.sort(Comparator.comparing(
                Feedback::getSubmittedAt,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate lastWeekStart = weekStart.minusWeeks(1);
        LocalDate lastWeekEnd = weekStart.minusDays(1);

        int thisWeek = 0;
        int lastWeek = 0;
        int flagged = 0;
        for (Feedback f : all) {
            if (f.getRating() <= 2) flagged++;
            LocalDate d = f.getSubmittedAt() != null ? f.getSubmittedAt().toLocalDate() : null;
            if (d == null) continue;
            if (!d.isBefore(weekStart)) thisWeek++;
            if (!d.isBefore(lastWeekStart) && !d.isAfter(lastWeekEnd)) lastWeek++;
        }

        int weekDelta = thisWeek - lastWeek;
        String weekSub;
        if (thisWeek == 0 && lastWeek == 0) {
            weekSub = "No submissions this week";
        } else if (weekDelta > 0) {
            weekSub = "↑ " + weekDelta + " from last week";
        } else if (weekDelta < 0) {
            weekSub = "↓ " + Math.abs(weekDelta) + " from last week";
        } else {
            weekSub = "Same as last week";
        }

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Feedback f : all) {
            String driverName = userRepository.findById(f.getDriverId())
                    .map(User::getFullName)
                    .orElse(f.getDriverId());
            String category = f.getCategory() != null ? f.getCategory().trim() : "GENERAL";
            boolean isFlagged = f.getRating() <= 2;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", f.getId());
            row.put("driverName", driverName);
            row.put("rating", f.getRating());
            row.put("ratingDisplay", renderForDriver(f));
            row.put("category", category);
            row.put("categoryLabel", formatCategoryLabel(category));
            row.put("comment", f.getComments() != null ? f.getComments() : "");
            row.put("date", f.getSubmittedAt() != null ? f.getSubmittedAt().format(dateFmt) : "—");
            row.put("submittedAt", f.getSubmittedAt() != null ? f.getSubmittedAt().toString() : "");
            row.put("status", isFlagged ? "FLAGGED" : "PUBLISHED");
            row.put("statusLabel", isFlagged ? "Flagged" : "Published");
            rows.add(row);
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("averageRating", calculateAverageRating());
        stats.put("totalReviews", all.size());
        stats.put("thisWeek", thisWeek);
        stats.put("weekSub", weekSub);
        stats.put("flagged", flagged);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stats", stats);
        out.put("rows", rows);
        return out;
    }

    private static String formatCategoryLabel(String category) {
        if (category == null || category.isBlank()) return "General";
        String lower = category.trim().toLowerCase(Locale.ENGLISH);
        if (lower.length() == 1) return lower.toUpperCase(Locale.ENGLISH);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    // ── Polymorphic display ───────────────────────────────────────────────────

    /** Render feedback using the driver-facing display format. */
    public String renderForDriver(Feedback feedback) {
        return driverDisplay.render(feedback.getRating());
    }

    /** Render feedback using the admin-facing display format. */
    public String renderForAdmin(Feedback feedback) {
        return adminDisplay.render(feedback.getRating());
    }

    // ── Admin moderation ──────────────────────────────────────────────────────

    /**
     * Admin updates feedback content or status.
     */
    public boolean updateFeedback(String feedbackId, String newComment,
                                   String adminId) {
        Optional<Feedback> opt = feedbackRepository.findById(feedbackId);
        if (opt.isEmpty()) return false;

        Feedback feedback = opt.get();
        if (newComment != null && !newComment.isBlank()) feedback.setComments(newComment);
        

        boolean updated = feedbackRepository.update(feedback);
        if (updated) {
            activityLogger.log(adminId, "ADMIN", "FEEDBACK_MODERATED",
                    "Feedback: " + feedbackId + " → " );
        }
        return updated;
    }

    /**
     * Admin deletes a feedback entry.
     */
    public boolean deleteFeedback(String feedbackId, String adminId) {
        boolean deleted = feedbackRepository.deleteById(feedbackId);
        if (deleted) {
            activityLogger.log(adminId, "ADMIN", "FEEDBACK_DELETED",
                    "Feedback: " + feedbackId);
        }
        return deleted;
    }
}
