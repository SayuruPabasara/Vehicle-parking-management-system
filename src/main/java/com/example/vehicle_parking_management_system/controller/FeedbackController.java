package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.Feedback;
import com.example.vehicle_parking_management_system.service.FeedbackService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * FeedbackController — HTTP endpoints for Component 06 (Feedback & Review Management).
 *
 * GET  /feedback/submit                 Show submission form data (completed reservations)
 * POST /feedback/submit                 Submit new feedback
 * GET  /feedback/my                     Driver's own submitted feedback
 * GET  /admin/feedback                  Admin view: all feedback
 * POST /admin/feedback/update/{id}      Admin moderates a review
 * POST /admin/feedback/delete/{id}      Admin deletes a review
 */
@RestController
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    // ── Driver: submit feedback ───────────────────────────────────────────────

    /**
     * GET /feedback/submit
     * Returns system average rating — used to render the submission form header.
     */
    @GetMapping("/feedback/submit")
    public ResponseEntity<?> feedbackFormData(HttpSession session) {
        if (session.getAttribute("userId") == null) return unauthorised();
        return ResponseEntity.ok(Map.of(
                "averageRating", feedbackService.calculateAverageRating(),
                "categories",    List.of("CLEANLINESS", "SAFETY", "STAFF", "GENERAL")
        ));
    }

    /**
     * POST /feedback/submit
     * Body params: reservationId, rating (1-5), category, comment
     */
    @PostMapping("/submit-feedback")
    public ResponseEntity<?> submitFeedback(
                                            @RequestParam int rating,
                                            @RequestParam String category,
                                            @RequestParam String comment,
                                            HttpSession session) {
        
        String driverId = (String) session.getAttribute("userId");

        if (driverId == null) return unauthorised();

        
        try{

            feedbackService.submitFeedback(driverId, rating, category, comment);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Feedback submitted successfully."
            ));

        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }







    // ── Driver: view own feedback ─────────────────────────────────────────────

    /**
     * GET /feedback/my
     * Returns all feedback submitted by the logged-in driver.
     */
    





    // ── Admin: view all feedback ──────────────────────────────────────────────

    /**
     * GET /admin/feedback/data
     * Admin-only: stats and feedback rows for the management UI.
     */
    @GetMapping("/admin/feedback/data")
    public ResponseEntity<?> adminFeedbackData(HttpSession session) {
        if (!isAdmin(session)) return adminForbidden();
        return ResponseEntity.ok(feedbackService.getAdminFeedbackManagementData());
    }
    
    // ── Admin: moderate ───────────────────────────────────────────────────────

    /**
     * POST /admin/feedback/update/{id}
     * Body params: comment (required for moderation edit)
     */
    @PostMapping("/admin/feedback/update/{id}")
    public ResponseEntity<?> updateFeedback(@PathVariable String id,
                                            @RequestParam String comment,
                                            HttpSession session) {
        if (!isAdmin(session)) return adminForbidden();

        String adminId = (String) session.getAttribute("userId");
        if (comment == null || comment.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Comment cannot be empty."
            ));
        }

        boolean updated = feedbackService.updateFeedback(id, comment.trim(), adminId);
        return ResponseEntity.ok(Map.of(
                "success", updated,
                "message", updated ? "Feedback updated." : "Feedback not found."
        ));
    }




    /**
     * POST /admin/feedback/delete/{id}
     */
    @PostMapping("/admin/feedback/delete/{id}")
    public ResponseEntity<?> deleteFeedback(@PathVariable String id,
                                            HttpSession session) {
        if (!isAdmin(session)) return adminForbidden();

        String adminId = (String) session.getAttribute("userId");
        boolean deleted = feedbackService.deleteFeedback(id, adminId);
        return ResponseEntity.ok(Map.of(
                "success", deleted,
                "message", deleted ? "Feedback deleted." : "Feedback not found."
        ));
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("userRole"))
                && session.getAttribute("userId") != null;
    }

    private ResponseEntity<Map<String, Object>> unauthorised() {
        return ResponseEntity.status(401).body(Map.of(
                "success", false, "message", "Authentication required."));
    }

    private ResponseEntity<Map<String, Object>> adminForbidden() {
        return ResponseEntity.status(403).body(Map.of(
                "success", false, "message", "Admin access required."));
    }
}
