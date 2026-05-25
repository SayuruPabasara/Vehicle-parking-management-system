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










    @PostMapping("/admin/admins/update")
    public ResponseEntity<?> updateAdmin(@RequestParam String id,
                                         @RequestParam String fullName,
                                         @RequestParam String userName,
                                         @RequestParam String email,
                                         @RequestParam(required = false, defaultValue = "") String phone,
                                         @RequestParam String adminLevel,
                                         @RequestParam(required = false) String password,
                                         HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        try {
            Admin.AdminLevel level = Admin.AdminLevel.valueOf(adminLevel.trim().toUpperCase());
            String phoneVal = phone == null || phone.isBlank() ? "-" : phone.trim();
            Admin admin = adminService.updateAdmin(
                    id.trim(),
                    fullName,
                    userName,
                    email,
                    phoneVal,
                    level,
                    password,
                    adminId(session));

            String currentId = adminId(session);
            if (currentId != null && currentId.equals(admin.getId())) {
                session.setAttribute("userName", admin.getUserName());
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Administrator updated."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }


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


    @GetMapping("/admin/drivers/data")
    public ResponseEntity<?> driverManagementData(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(userService.getDriverManagementData());
    }


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


    @GetMapping("/admin/slots/data")
    public ResponseEntity<?> slotManagementData(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(slotService.getSlotManagementData());
    }


    @PostMapping("/admin/slots/update")
    public ResponseEntity<?> updateSlot(@RequestParam String slotId,
                                        @RequestParam String status,
                                        HttpSession session) {
        if (!isAdmin(session)) return forbidden();

        try {
            ParkingSlot.SlotStatus newStatus =
                    ParkingSlot.SlotStatus.valueOf(status.trim().toUpperCase());
            String message = slotService.updateSlot(slotId, newStatus, adminId(session));
            return ResponseEntity.ok(Map.of(
                    "success", message != null,
                    "message", message != null ? message : "Slot not found."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid status. Use AVAILABLE, OCCUPIED, or MAINTENANCE."));
        }
    }


    @GetMapping("/admin/vehicles/data")
    public ResponseEntity<?> vehicleManagementData(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        return ResponseEntity.ok(vehicleService.getVehicleManagementData());
    }


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


    @GetMapping("/admin/logs/data")
    public ResponseEntity<?> getActivityLogs(HttpSession session) {
        if (!isAdmin(session)) return forbidden();
        
        return ResponseEntity.ok(adminService.getRecentActivityForDashboard(20));
    }
}
