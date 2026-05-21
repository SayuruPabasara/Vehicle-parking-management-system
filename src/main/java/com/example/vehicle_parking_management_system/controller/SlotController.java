package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.ParkingSlot;
import com.example.vehicle_parking_management_system.service.ReservationService;
import com.example.vehicle_parking_management_system.service.SlotService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RestController
public class SlotController {

    private final SlotService slotService;
    private final ReservationService reservationService;

    public SlotController(SlotService slotService, ReservationService reservationService) {
        this.slotService = slotService;
        this.reservationService = reservationService;
    }


    @GetMapping("/api/slots/map")
    public ResponseEntity<Map<String, List<Map<String, Object>>>> getSlotsForMap() {
        return ResponseEntity.ok(groupSlots(slotService.getAllSlots()));
    }


    @GetMapping("/slots")
    public ResponseEntity<?> getAllSlots(HttpSession session) {
        if (session.getAttribute("userId") == null) return unauthorised();

        return ResponseEntity.ok(groupSlots(slotService.getAllSlots()));
    }


    @GetMapping("/slots/available")
    public ResponseEntity<?> getAvailableSlots(HttpSession session) {
        if (session.getAttribute("userId") == null) return unauthorised();

        reservationService.expireOverdueActiveSessions();
        List<ParkingSlot> available = slotService.getAvailableSlots();
        return ResponseEntity.ok(Map.of(
                "count",  available.size(),
                "slots",  available.stream().map(this::toMap).toList()
        ));
    }


    @PostMapping("/slots/allocate/{slotId}")
    public ResponseEntity<?> allocateSlot(@PathVariable String slotId,
                                          @RequestParam String vehicleId,
                                          HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return unauthorised();

        try {
            ParkingSlot slot = slotService.allocateSlot(slotId, vehicleId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Slot " + slot.getSlotNumber() + " allocated.",
                    "slot",    toMap(slot)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/slots/release/{slotId}")
    public ResponseEntity<?> releaseSlot(@PathVariable String slotId,
                                         HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        if (userId == null) return unauthorised();

        try {
            ParkingSlot slot = slotService.releaseSlot(slotId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Slot " + slot.getSlotNumber() + " is now available.",
                    "slot",    toMap(slot)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/slots/edit/{slotId}")
    public ResponseEntity<?> editSlot(@PathVariable String slotId,
                                      @RequestParam String status,
                                      @RequestParam double hourlyRate,
                                      HttpSession session) {
        if (!"ADMIN".equals(session.getAttribute("userRole"))) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Admin access required."));
        }

        String adminId = (String) session.getAttribute("userId");

        try {
            ParkingSlot.SlotStatus newStatus = ParkingSlot.SlotStatus.valueOf(status.toUpperCase());
            boolean updated = slotService.updateSlot(slotId, newStatus, hourlyRate, adminId);
            return ResponseEntity.ok(Map.of(
                    "success", updated,
                    "message", updated ? "Slot updated." : "Slot not found."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid status value: " + status));
        }
    }



    private ResponseEntity<Map<String, Object>> unauthorised() {
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Authentication required."));
    }

    private Map<String, Object> toMap(ParkingSlot slot) {
        return Map.of(
                "id",           slot.getId(),
                "slotNumber",   slot.getSlotNumber(),
                "status",       slot.getStatus().name(),
                "hourlyRate",   slot.getHourlyRate(),
                "occupant",     slot.getCurrentVehicleId() == null
                                    ? "" : slot.getCurrentVehicleId()
        );
    }


    private Map<String, List<Map<String, Object>>> groupSlots(List<ParkingSlot> all) {
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        grouped.put("Section A", new ArrayList<>());
        grouped.put("Section B", new ArrayList<>());
        grouped.put("Section C", new ArrayList<>());

        for (ParkingSlot s : all) {
            String key = mapSectionKey(s);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(toMap(s));
        }

        Comparator<Map<String, Object>> bySlotNumber =
                Comparator.comparing(m -> (String) m.get("slotNumber"));
        for (List<Map<String, Object>> section : grouped.values()) {
            section.sort(bySlotNumber);
        }
        return grouped;
    }

    private static String mapSectionKey(ParkingSlot s) {
        String sn = s.getSlotNumber();
        if (sn != null && sn.length() >= 3 && sn.charAt(1) == '-') {
            char c = Character.toUpperCase(sn.charAt(0));
            if (c == 'A' || c == 'B' || c == 'C') {
                return "Section " + c;
            }
        }
        int ord = ordinalFromSlot(s);
        if (ord >= 1 && ord <= 10) return "Section A";
        if (ord <= 20) return "Section B";
        if (ord <= 30) return "Section C";
        return "Section C";
    }

    private static int ordinalFromSlot(ParkingSlot s) {
        int fromId = parseSltOrdinal(s.getId());
        if (fromId > 0) return fromId;
        return parseSltOrdinal(s.getSlotNumber());
    }

    private static int parseSltOrdinal(String raw) {
        if (raw == null) return -1;
        String t = raw.trim();
        if (t.length() < 5 || !t.regionMatches(true, 0, "SLT-", 0, 4)) return -1;
        try {
            return Integer.parseInt(t.substring(4).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
