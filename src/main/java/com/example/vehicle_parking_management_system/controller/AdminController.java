package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.Admin;
import com.example.vehicle_parking_management_system.model.ParkingSlot;
import com.example.vehicle_parking_management_system.service.AdminService;
import com.example.vehicle_parking_management_system.service.SlotService;
import com.example.vehicle_parking_management_system.service.UserService;
import com.example.vehicle_parking_management_system.service.VehicleService;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminController — HTTP endpoints for Component 05 (Admin Management).
 *
 * GET  /admin/dashboard          System summary stats
 * GET  /admin/admins             List all admin accounts
 * POST /admin/admins/add         Create a new admin
 * POST /admin/admins/delete/{id} Delete an admin account
 * GET  /admin/users              View all driver accounts
 * GET  /admin/logs               View system activity log
 *
 * All /admin/** endpoints enforce ADMIN role check.
 */
@RestController
public class AdminController {

    private final AdminService adminService;
    private final UserService userService;
    private final VehicleService vehicleService;
    private final SlotService slotService;

    @Value("${parknow.data.activity-log}")
    private String activityLogPath;

    public AdminController(AdminService adminService,
                           UserService userService,
                           VehicleService vehicleService,
                           SlotService slotService) {
        this.adminService = adminService;
        this.userService = userService;
        this.vehicleService = vehicleService;
        this.slotService = slotService;
    }

    // ── Auth helpers ──────────────────────────────────────────────────────────

    private boolean isAdmin(HttpSession session) {
        return "ADMIN".equals(session.getAttribute("userRole"))
                && session.getAttribute("userId") != null;
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(403).body(Map.of(
                "success", false, "message", "Admin access required."));
    }

    private String adminId(HttpSession session) {
        return (String) session.getAttribute("userId");
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    // ── Admin account management ──────────────────────────────────────────────



    /**
     * GET /admin/admins/list
     * JSON list of admins (no password hashes).
     */
    @GetMapping("/admin/admins/list")
    public ResponseEntity<?> listAdmins(HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (Admin a : adminService.getAllAdmins()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("fullName", a.getFullName());
            m.put("userName", a.getUserName());
            m.put("email", a.getEmail());
            m.put("phone", a.getPhone());
            m.put("adminLevel", a.getAdminLevel().name());
            m.put("createdBy", a.getCreatedBy());
            rows.add(m);
        }
        return ResponseEntity.ok(rows);
    }

    /**
     * POST /admin/admins/add
     * Form params: fullName, userName, email, phone (optional), password, adminLevel (SUPER|PARKING|FINANCE|READONLY)
     */
    @PostMapping("/admin/admins/add")
    public ResponseEntity<?> addAdmin(@RequestParam String fullName,
                                      @RequestParam String userName,
                                      @RequestParam String email,
                                      @RequestParam(required = false, defaultValue = "") String phone,
                                      @RequestParam String password,
                                      @RequestParam String adminLevel,
                                      HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        try {
            Admin.AdminLevel level = Admin.AdminLevel.valueOf(adminLevel.trim().toUpperCase());
            String phoneVal = phone == null || phone.isBlank() ? "-" : phone.trim();
            Admin admin = adminService.registerAdmin(
                    fullName.trim(),
                    userName.trim(),
                    email.trim(),
                    phoneVal,
                    password,
                    level,
                    adminId(session));
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Administrator saved.",
                    "id", admin.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }










    /**
     * POST /admin/admins/delete/{id}
     * Soft-delete: removes admin from admins.csv.
     */
    @PostMapping("/admin/admins/delete/{id}")
    public ResponseEntity<?> deleteAdmin(@PathVariable String id,
                                         HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        try {
            boolean deleted = adminService.deleteAdmin(id, adminId(session));
            return ResponseEntity.ok(Map.of(
                    "success", deleted,
                    "message", deleted ? "Admin deleted." : "Admin not found."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false, "message", e.getMessage()));
        }
    }

    // ── Driver oversight ──────────────────────────────────────────────────────

    /**
     * GET /admin/drivers/data
     * Returns driver list and summary stats for the admin driver-management UI.
     */
    @GetMapping("/admin/drivers/data")
    public ResponseEntity<?> driverManagementData(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(userService.getDriverManagementData());
    }

    /**
     * POST /admin/drivers/delete/{id}
     * Removes driver from users.csv and cascades related vehicles, reservations, and feedback.
     */
    @PostMapping("/admin/drivers/delete/{id}")
    public ResponseEntity<?> deleteDriver(@PathVariable String id,
                                          HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        boolean deleted = userService.deleteDriver(id, adminId(session));
        return ResponseEntity.ok(Map.of(
                "success", deleted,
                "message", deleted ? "Driver deleted." : "Driver not found."
        ));
    }

    // ── Slot oversight ──────────────────────────────────────────────────────

    /**
     * GET /admin/slots/data
     * Returns slot list and summary stats for the admin slot-management UI.
     */
    @GetMapping("/admin/slots/data")
    public ResponseEntity<?> slotManagementData(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(slotService.getSlotManagementData());
    }

    /**
     * POST /admin/slots/update
     * Updates slot status and hourly rate in slots.csv.
     */
    @PostMapping("/admin/slots/update")
    public ResponseEntity<?> updateSlot(@RequestParam String slotId,
                                        @RequestParam String status,
                                        @RequestParam double hourlyRate,
                                        HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        try {
            ParkingSlot.SlotStatus newStatus =
                    ParkingSlot.SlotStatus.valueOf(status.trim().toUpperCase());
            boolean updated = slotService.updateSlot(
                    slotId, newStatus, hourlyRate, adminId(session));
            return ResponseEntity.ok(Map.of(
                    "success", updated,
                    "message", updated ? "Slot updated." : "Slot not found."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid status. Use AVAILABLE, OCCUPIED, or MAINTENANCE."));
        }
    }

    // ── Vehicle oversight ─────────────────────────────────────────────────────

    /**
     * GET /admin/vehicles/data
     * Returns vehicle list and summary stats for the admin vehicle-management UI.
     */
    @GetMapping("/admin/vehicles/data")
    public ResponseEntity<?> vehicleManagementData(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(vehicleService.getVehicleManagementData());
    }

    /**
     * POST /admin/vehicles/delete/{id}
     * Removes a vehicle from vehicles.csv.
     */
    @PostMapping("/admin/vehicles/delete/{id}")
    public ResponseEntity<?> deleteVehicle(@PathVariable String id,
                                           HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        boolean deleted = vehicleService.deleteVehicleByAdmin(id, adminId(session));
        return ResponseEntity.ok(Map.of(
                "success", deleted,
                "message", deleted ? "Vehicle deleted." : "Vehicle not found."
        ));
    }

    // ── Activity log ──────────────────────────────────────────────────────────

    /**
     * GET /admin/logs
     * Returns the system activity log lines (newest first).
     */
    @GetMapping("/admin/logs/data")
    public ResponseEntity<?> getActivityLogs(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        
        return ResponseEntity.ok(adminService.getRecentActivityForDashboard(20));
    }
}
