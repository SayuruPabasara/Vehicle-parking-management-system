package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.Admin;
import com.example.vehicle_parking_management_system.model.ParkingSlot;
import com.example.vehicle_parking_management_system.model.Reservation;
import com.example.vehicle_parking_management_system.repository.AdminRepository;
import com.example.vehicle_parking_management_system.repository.FeedbackRepository;
import com.example.vehicle_parking_management_system.repository.UserRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

/**
 * AdminService — business logic for Component 05 (Admin Management).
 *
 * Abstraction: exposes getSystemSummary() which aggregates data from
 * multiple repositories without the controller needing to know the details.
 */
@Service
public class AdminService {

    private final AdminRepository       adminRepository;
    private final UserRepository        userRepository;
    private final UserService           userService;
    private final VehicleService        vehicleService;
    private final SlotService           slotService;
    private final ReservationService    reservationService;
    private final FeedbackRepository    feedbackRepository;
    private final ActivityLogger        activityLogger;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AdminService(AdminRepository adminRepository,
                        UserRepository userRepository,
                        UserService userService,
                        VehicleService vehicleService,
                        SlotService slotService,
                        ReservationService reservationService,
                        FeedbackRepository feedbackRepository,
                        ActivityLogger activityLogger) {
        this.adminRepository    = adminRepository;
        this.userRepository     = userRepository;
        this.userService        = userService;
        this.vehicleService     = vehicleService;
        this.slotService        = slotService;
        this.reservationService = reservationService;
        this.feedbackRepository = feedbackRepository;
        this.activityLogger     = activityLogger;
    }

    // ── Admin account management ──────────────────────────────────────────────

    /**
     * Register a new admin account. Only SUPER admins should be able to call this.
     *
     * @param createdById  ID of the SUPER admin creating this account
     * @throws IllegalArgumentException if email already exists
     */
    public Admin registerAdmin(String fullName, String username, String email, String phone,String password,
                                Admin.AdminLevel level, String createdById) {
        if (adminRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Admin email already registered: " + email);
        }

        Admin admin = new Admin(
                IdGenerator.next("ADM"),
                fullName,
                username,
                email,
                phone,
                passwordEncoder.encode(password),
                "ADMIN",
                level,
                createdById
        );
        adminRepository.save(admin);
        activityLogger.log(createdById, "ADMIN", "ADMIN_CREATED",
                "New admin: " + email + " level: " + level);
        return admin;
    }

    /**
     * Authenticate an admin by email and raw password.
     */
    public Optional<Admin> login(String email, String rawPassword) {
        Optional<Admin> opt = adminRepository.findByEmail(email);
        if (opt.isEmpty()) return Optional.empty();

        Admin admin = opt.get();
        if (!passwordEncoder.matches(rawPassword, admin.getPassword())) {
            activityLogger.log(admin.getId(), "ADMIN", "ADMIN_LOGIN_FAILED",
                    "Bad password for " + email);
            return Optional.empty();
        }
        activityLogger.log(admin.getId(), "ADMIN", "ADMIN_LOGIN", email);
        return Optional.of(admin);
    }

    /**
     * Delete an admin account. SUPER admin cannot delete their own account.
     *
     * @param targetId   the admin ID to delete
     * @param requesterId the admin requesting the deletion
     */
    public boolean deleteAdmin(String targetId, String requesterId) {
        if (targetId.equals(requesterId)) {
            throw new IllegalArgumentException("An admin cannot delete their own account.");
        }
        boolean deleted = adminRepository.deleteById(targetId);
        if (deleted) {
            activityLogger.log(requesterId, "ADMIN", "ADMIN_DELETED", "Target: " + targetId);
        }
        return deleted;
    }

    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    public Optional<Admin> findById(String id) {
        return adminRepository.findById(id);
    }

    public Optional<Admin> findByEmail(String email) {
        return adminRepository.findByEmail(email);
    }

    // ── Dashboard aggregation ─────────────────────────────────────────────────

    /**
     * Produce a summary map for the admin dashboard.
     * Reads across all components without writing to their CSV files.
     */
    public Map<String, Object> getSystemSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        int totalDrivers = userRepository.countDrivers();
        int totalVehicles = vehicleService.getTotalVehicleCount();
        List<ParkingSlot> slots = slotService.getAllSlots();
        int totalSlots = slots.size();
        long occupiedSlots = slots.stream()
                .filter(s -> s.getStatus() == ParkingSlot.SlotStatus.OCCUPIED)
                .count();
        long availableSlots = slots.stream()
                .filter(s -> s.getStatus() == ParkingSlot.SlotStatus.AVAILABLE)
                .count();
        double freePct = totalSlots > 0 ? (availableSlots * 100.0 / totalSlots) : 0.0;

        YearMonth thisMonth = YearMonth.now();
        double revenueMonth = 0.0;
        for (Reservation r : reservationService.getAllReservations()) {
            if (r.getStatus() != Reservation.ReservationStatus.COMPLETED) continue;
            LocalDateTime end = r.getEndTime();
            if (end == null) continue;
            if (!YearMonth.from(end).equals(thisMonth)) continue;
            revenueMonth += r.getFee();
        }
        long revenueRounded = Math.round(revenueMonth);

        summary.put("totalDrivers", totalDrivers);
        summary.put("totalVehicles", totalVehicles);
        summary.put("occupiedSlots", (int) occupiedSlots);
        summary.put("availableSlots", (int) availableSlots);
        summary.put("totalSlots", totalSlots);
        summary.put("freePercent", Math.round(freePct * 10) / 10.0);
        summary.put("revenueMonthLkr", revenueRounded);
        summary.put("revenueMonthDisplay", formatRevenueShort(revenueRounded));

        summary.put("activeSessions", reservationService.getActiveSessionCount());
        summary.put("totalAdmins", adminRepository.findAll().size());
        summary.put("totalReservations", reservationService.getAllReservations().size());
        summary.put("totalFeedback", feedbackRepository.findAll().size());
        return summary;
    }

    private static String formatRevenueShort(long lkr) {
        if (lkr >= 1_000_000) {
            return String.format("%.1fM", lkr / 1_000_000.0);
        }
        if (lkr >= 1_000) {
            return String.format("%.0fK", lkr / 1_000.0);
        }
        return String.valueOf(lkr);
    }

    // ── Driver oversight ──────────────────────────────────────────────────────

    




    // ── Activity log reading ──────────────────────────────────────────────────

    /**
     * Read the system activity log file and return each line as a string.
     * The controller/view is responsible for display formatting.
     */
    public List<String> getActivityLog(String logFilePath) {
        List<String> lines = new ArrayList<>();
        java.io.File file = new java.io.File(logFilePath);
        if (!file.exists()) return lines;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank()) lines.add(line);
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read activity log: " + e.getMessage(), e);
        }
        // Return newest first
        Collections.reverse(lines);
        return lines;
    }
}
