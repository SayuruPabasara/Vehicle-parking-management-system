package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.Driver;
import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.repository.UserRepository;
import com.example.vehicle_parking_management_system.repository.VehicleRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * UserService — business logic for User Management
 *
 * Abstraction: exposes high-level operations; callers don't need to know
 * how passwords are hashed or how the CSV is structured.
 */
@Service
public class UserService {

    private final UserRepository    userRepository;
    private final VehicleRepository vehicleRepository;
    private final ActivityLogger    activityLogger;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${parknow.data.activity-log:default_activity.log}")
    private String activityLogPath;

    private static final DateTimeFormatter JOINED_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    public UserService(UserRepository userRepository,
                       VehicleRepository vehicleRepository,
                       ActivityLogger activityLogger) {
        this.userRepository  = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.activityLogger  = activityLogger;
    }

    // register a driver
    public Driver register(String fullName, String userName, String email, String phone, String password) {
        
        //check if already registered
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        Driver driver = new Driver(
                IdGenerator.next("USR"),
                fullName,
                userName,
                email,
                phone,
                passwordEncoder.encode(password),
                "LICENSE-" + IdGenerator.next("LIC"),
                0
        );

        userRepository.save(driver);

        activityLogger.log(driver.getId(), "DRIVER", "USER_REGISTERED",
                "New driver: " + email);

        return driver;
    }

    // authenticate user by email and password
    public Optional<User> login(String email,String password){

        Optional<User> userOpt = userRepository.findByEmail(email);

        //not found
        if (userOpt.isEmpty()) 
            return Optional.empty();

        User user = userOpt.get();

        //password doesn't match
        if (!passwordEncoder.matches(password, user.getPassword())) {

            activityLogger.log(user.getId(), user.getRole(), "LOGIN_FAILED","Bad password for " + email);

            return Optional.empty();
        }
        
        activityLogger.log(user.getId(), user.getRole(), "USER_LOGIN", email);
        
        return Optional.of(user);
    }

    /** Increment vehicle count for a driver (called by VehicleService). */
    public void incrementVehicleCount(String driverId) {
        userRepository.findById(driverId).ifPresent(user -> {
            if (user instanceof Driver d) {
                d.setVehicleCount(d.getVehicleCount() + 1);
                userRepository.update(d);
            }
        });
    }

    /**
     * Admin driver-management page: summary stats plus driver rows
     * (vehicle counts from vehicles.csv; join dates from activity log when available).
     */
    public Map<String, Object> getDriverManagementData() {
        List<Driver> drivers = userRepository.findAllDrivers();
        Map<String, Integer> vehicleCounts = vehicleRepository.countByOwnerId();
        Map<String, LocalDateTime> registeredAt = loadDriverRegistrationTimes();

        List<Map<String, Object>> rows = new ArrayList<>();
        int withVehicles = 0;
        for (Driver d : drivers) {
            int vehicles = vehicleCounts.getOrDefault(d.getId(), 0);
            if (vehicles > 0) withVehicles++;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", d.getId());
            row.put("fullName", d.getFullName());
            row.put("userName", d.getUserName());
            row.put("email", d.getEmail());
            row.put("phone", d.getPhone() == null || d.getPhone().isBlank() ? "—" : d.getPhone());
            row.put("vehicleCount", vehicles);
            row.put("licenseNumber", d.getLicenseNumber());
            row.put("status", "ACTIVE");
            LocalDateTime joined = registeredAt.get(d.getId());
            row.put("joined", joined == null ? "—" : JOINED_FMT.format(joined));
            row.put("joinedIso", joined == null ? null : joined.toString());
            rows.add(row);
        }

        rows.sort(Comparator.comparing(
                (Map<String, Object> r) -> (String) r.get("fullName"),
                String.CASE_INSENSITIVE_ORDER));

        int total = drivers.size();
        int suspended = 0;
        int active = total - suspended;
        int newThisWeek = countRegistrationsSince(registeredAt, 7);
        double activeRate = total > 0 ? (active * 100.0 / total) : 0.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("suspended", suspended);
        stats.put("newThisWeek", newThisWeek);
        stats.put("withVehicles", withVehicles);
        stats.put("activeRatePercent", String.format("%.1f%%", activeRate));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stats", stats);
        payload.put("drivers", rows);
        return payload;
    }

    private Map<String, LocalDateTime> loadDriverRegistrationTimes() {
        Map<String, LocalDateTime> times = new HashMap<>();
        File logFile = new File(activityLogPath);
        if (!logFile.exists()) return times;

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 5);
                if (parts.length < 4) continue;
                if (!"USER_REGISTERED".equals(parts[3].trim())) continue;
                if (!"DRIVER".equals(parts[2].trim())) continue;

                String driverId = parts[1].trim();
                LocalDateTime ts;
                try {
                    ts = LocalDateTime.parse(parts[0].trim());
                } catch (Exception e) {
                    continue;
                }
                times.merge(driverId, ts, (a, b) -> a.isBefore(b) ? a : b);
            }
        } catch (IOException e) {
            System.err.println("[UserService] Failed to read activity log: " + e.getMessage());
        }
        return times;
    }

    private int countRegistrationsSince(Map<String, LocalDateTime> registeredAt, int days) {
        LocalDate cutoff = LocalDate.now().minus(days, ChronoUnit.DAYS);
        int count = 0;
        for (LocalDateTime ts : registeredAt.values()) {
            if (!ts.toLocalDate().isBefore(cutoff)) count++;
        }
        return count;
    }

}
