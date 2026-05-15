package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.Feedback;
import com.example.vehicle_parking_management_system.model.Reservation;
import com.example.vehicle_parking_management_system.repository.FeedbackRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

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

    // Polymorphic display renderers
    private final Feedback.RatingDisplay driverDisplay = new Feedback.DriverRatingDisplay();
    private final Feedback.RatingDisplay adminDisplay  = new Feedback.AdminRatingDisplay();

    public FeedbackService(FeedbackRepository feedbackRepository,
                           ReservationService reservationService,
                           ActivityLogger activityLogger) {
        this.feedbackRepository = feedbackRepository;
        this.reservationService = reservationService;
        this.activityLogger     = activityLogger;
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
        // Grouping by a default category as 'category' field is removed from simplified model
        return feedbackRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        f -> "General",
                        Collectors.averagingInt(Feedback::getRating)
                ));
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
