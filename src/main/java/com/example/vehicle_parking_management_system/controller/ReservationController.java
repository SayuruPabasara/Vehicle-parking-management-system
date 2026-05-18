package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.Reservation;
import com.example.vehicle_parking_management_system.service.ReservationService;
import com.example.vehicle_parking_management_system.service.SlotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ReservationController — HTTP endpoints for Component 04 (Reservation & Booking).
 *
 * GET  /booking                   Booking form data (available slots)
 * POST /booking/create            Create new reservation
 * GET  /booking/active            Active reservations for logged-in driver
 * POST /booking/checkout/{id}     Check out and compute fee
 * GET  /admin/reservations        Admin view of all reservations
 */
@RestController
public class ReservationController {

    private final ReservationService reservationService;
    private final SlotService        slotService;

    public ReservationController(ReservationService reservationService,
                                 SlotService slotService) {
        this.reservationService = reservationService;
        this.slotService        = slotService;
    }

    // ── Booking form data ─────────────────────────────────────────────────────

    /**
     * GET /booking
     * Returns available slots so the booking form can populate a dropdown.
     */
    @GetMapping("/booking")
    public ResponseEntity<?> getBookingFormData(HttpSession session) {
        String driverId = requireDriver(session);
        if (driverId == null) return unauthorised();

        return ResponseEntity.ok(Map.of(
                "availableSlots", slotService.getAvailableSlots().stream()
                        .map(s -> Map.of(
                                "id",         s.getId(),
                                "slotNumber", s.getSlotNumber(),
                                "status",       s.getStatus().name(),
                                "hourlyRate", s.getHourlyRate()
                        )).toList()
        ));
    }

    // ── Create booking ────────────────────────────────────────────────────────

    /**
     * POST /booking/create
     * Body params: slotId, vehicleId, startTime, endTime
     */
    @PostMapping("/booking/create")
    public ResponseEntity<?> createBooking(@RequestParam String slotId,
                                           @RequestParam String vehicleId,
                                           @RequestParam String startTime,
                                           @RequestParam String endTime,
                                           HttpSession session) {
        String driverId = requireDriver(session);
        if (driverId == null) return unauthorised();

        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end   = LocalDateTime.parse(endTime);

            Reservation reservation = reservationService.createBooking(
                    driverId, slotId, vehicleId, start, end);
            return ResponseEntity.ok(Map.of(
                    "success",       true,
                    "message",       "Booking confirmed.",
                    "reservationId", reservation.getId(),
                    "startTime",     reservation.getStartTime().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    // ── Active sessions ───────────────────────────────────────────────────────

    /**
     * GET /booking/active
     * Returns all ACTIVE reservations for the logged-in driver.
     */
    @GetMapping("/booking/active")
    public ResponseEntity<?> getActiveSessions(HttpSession session) {
        String driverId = requireDriver(session);
        if (driverId == null) return unauthorised();

        List<Reservation> active = reservationService.getActiveSessions(driverId);
        return ResponseEntity.ok(active.stream().map(r -> Map.of(
                "id",          r.getId(),
                "slotId",      r.getSlotId(),
                "vehicleId",   r.getVehicleId(),
                "startTime",   r.getStartTime().toString(),
                "runningFee",  reservationService.getRunningFee(r.getId()),
                "status",      r.getStatus().name()
        )).toList());
    }

    // ── Checkout ──────────────────────────────────────────────────────────────

    /**
     * POST /booking/checkout/{id}
     * Completes the reservation, computes fee, releases the slot.
     */
    @PostMapping("/booking/checkout/{id}")
    public ResponseEntity<?> checkout(@PathVariable String id,
                                      HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return unauthorised();

        String role     = (String) session.getAttribute("userRole");
        String driverId = "ADMIN".equals(role) ? null : userId;

        try {
            Reservation completed = reservationService.checkOut(id, driverId);
            return ResponseEntity.ok(Map.of(
                    "success",   true,
                    "message",   "Checkout successful.",
                    "fee",       completed.getFee(),
                    "startTime", completed.getStartTime().toString(),
                    "endTime",   completed.getEndTime().toString()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", e.getMessage()));
        }
    }

    // ── Driver history ────────────────────────────────────────────────────────

    /**
     * GET /booking/history
     * Returns complete booking history for the logged-in driver.
     */
    @GetMapping("/booking/history")
    public ResponseEntity<?> getHistory(HttpSession session) {
        String driverId = requireDriver(session);
        if (driverId == null) return unauthorised();

        return ResponseEntity.ok(
                reservationService.getDriverHistory(driverId).stream()
                        .map(this::toMap).toList());
    }

    // ── Admin view ────────────────────────────────────────────────────────────

    /**
     * GET /admin/reservations/data
     * Admin-only: reservations with driver/plate/slot labels for the management UI.
     */
    @GetMapping("/admin/reservations/data")
    public ResponseEntity<?> adminReservationsData(HttpSession session) {
        if (!isAdmin(session)) return adminForbidden();
        return ResponseEntity.ok(reservationService.getAdminReservationRows());
    }

    /**
     * POST /admin/reservations/confirm-payment
     * Marks selected reservations as PAID (admin confirms payment received).
     */
    @PostMapping("/admin/reservations/confirm-payment")
    public ResponseEntity<?> confirmReservationPayments(
            @RequestParam(value = "reservationIds", required = false) List<String> reservationIds,
            HttpSession session) {
        if (!isAdmin(session)) return adminForbidden();
        if (reservationIds == null || reservationIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "No reservations selected."));
        }
        String adminId = (String) session.getAttribute("userId");
        int updated = reservationService.confirmPaymentsPaid(reservationIds, adminId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updated", updated,
                "message", updated + " reservation(s) marked as paid."));
    }

    /**
     * POST /admin/reservations/mark-unpaid
     * Marks selected completed reservations as UNPAID (admin correction).
     */
    @PostMapping("/admin/reservations/mark-unpaid")
    public ResponseEntity<?> markReservationPaymentsUnpaid(
            @RequestParam(value = "reservationIds", required = false) List<String> reservationIds,
            HttpSession session) {
        if (!isAdmin(session)) return adminForbidden();
        if (reservationIds == null || reservationIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", "No reservations selected."));
        }
        String adminId = (String) session.getAttribute("userId");
        int updated = reservationService.markPaymentsUnpaid(reservationIds, adminId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "updated", updated,
                "message", updated + " reservation(s) marked as unpaid."));
    }

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("userRole"))
                && session.getAttribute("userId") != null;
    }

    private ResponseEntity<Map<String, Object>> adminForbidden() {
        return ResponseEntity.status(403).body(new LinkedHashMap<>(Map.of(
                "success", false,
                "message", "Admin access required.")));
    }


    

    // ── Internal helpers ──────────────────────────────────────────────────────

    private String requireDriver(HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) return null;
        return (String) userId;
    }

    private ResponseEntity<Map<String, Object>> unauthorised() {
        return ResponseEntity.status(401).body(Map.of(
                "success", false, "message", "Authentication required."));
    }

    private Map<String, Object> toMap(Reservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("driverId", r.getDriverId());
        m.put("slotId", r.getSlotId());
        m.put("vehicleId", r.getVehicleId());
        m.put("startTime", r.getStartTime() != null ? r.getStartTime().toString() : "");
        m.put("endTime", r.getEndTime() == null ? "" : r.getEndTime().toString());
        m.put("status", r.getStatus().name());
        m.put("fee", r.getFee());
        m.put("paymentStatus", r.getPaymentStatus().name());
        return m;
    }
}
