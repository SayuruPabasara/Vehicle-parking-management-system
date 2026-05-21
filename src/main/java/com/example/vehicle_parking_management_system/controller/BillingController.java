package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final ReservationService reservationService;

    public BillingController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/details")
    public ResponseEntity<?> getBillingDetails(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Authentication required."));
        }
        return ResponseEntity.ok(reservationService.getDriverBillingSummary(userId));
    }


    @PostMapping("/confirm-cash")
    public ResponseEntity<?> confirmCashPayment(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required."));
        }
        return ResponseEntity.ok(reservationService.confirmDriverCashPayments(userId));
    }


    @PostMapping("/pay")
    public ResponseEntity<?> processPayment(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required."));
        }
        return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Online card payment is not enabled. Pay with cash and tap Confirm Payment, or ask an administrator for help."));
    }
}
