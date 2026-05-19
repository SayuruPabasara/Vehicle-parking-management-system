package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.Driver;
import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.model.Vehicle;
import com.example.vehicle_parking_management_system.repository.UserRepository;
import com.example.vehicle_parking_management_system.repository.VehicleRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VehicleService {

    private static final DateTimeFormatter REGISTERED_FMT =
            DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final ActivityLogger activityLogger;

    @Value("${parknow.data.activity-log:default_activity.log}")
    private String activityLogPath;

    public VehicleService(VehicleRepository vehicleRepository,
                          UserRepository userRepository,
                          UserService userService,
                          ActivityLogger activityLogger) {
        this.vehicleRepository = vehicleRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.activityLogger = activityLogger;
    }

    public int getTotalVehicleCount() {
        return vehicleRepository.findAll().size();
    }

    public List<Vehicle> getVehiclesByOwner(String ownerId) {
        return vehicleRepository.findByDriverId(ownerId);
    }

    public boolean deleteVehicle(String vehicleId, String ownerId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isPresent() && vehicleOpt.get().getOwnerId().equals(ownerId)) {
            boolean deleted = vehicleRepository.deleteById(vehicleId);
            if (deleted) {
                activityLogger.log(ownerId, "DRIVER", "VEHICLE_REMOVED",
                        "Removed vehicle with plate " + vehicleOpt.get().getPlateNumber());
            }
            return deleted;
        }
        return false;
    }

    public Vehicle registerVehicle(String plate, String color, String vtype, HttpSession session,String ownerId) {
        String normalizedPlate = plate.toUpperCase().trim();

        if (vehicleRepository.findByPlateNumber(normalizedPlate).isPresent()) {
            throw new IllegalArgumentException("A vehicle with that plate already exists.");
        }

        // Create and save the vehicle
        Vehicle vehicle = new Vehicle(IdGenerator.next("VHCL"), normalizedPlate, vtype, color, ownerId);
        
        vehicleRepository.save(vehicle);

        // Log the activity
        activityLogger.log(vehicle.getOwnerId(), "DRIVER", "VEHICLE_REGISTERED",
                "Registered vehicle with plate " + normalizedPlate);
        

        return vehicle;
    }

    /**
     * Admin vehicle-management page: summary stats plus vehicle rows
     * (owner names from users.csv; registration dates from activity log when available).
     */
    public Map<String, Object> getVehicleManagementData() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        Map<String, LocalDateTime> registeredByPlate = loadVehicleRegistrationTimes();
        Map<String, User> usersById = new HashMap<>();
        for (User u : userRepository.findAll()) {
            usersById.put(u.getId(), u);
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        int active = 0;
        int flagged = 0;
        Set<String> typeSet = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Vehicle v : vehicles) {
            typeSet.add(v.getType() == null || v.getType().isBlank() ? "Unknown" : v.getType().trim());

            User owner = usersById.get(v.getOwnerId());
            boolean isDriverOwner = owner instanceof Driver;
            String status = isDriverOwner ? "ACTIVE" : "FLAGGED";
            if ("ACTIVE".equals(status)) active++;
            else flagged++;

            String ownerName = "—";
            if (owner != null) {
                ownerName = owner.getFullName() == null || owner.getFullName().isBlank()
                        ? owner.getUserName()
                        : owner.getFullName();
            } else if (v.getOwnerId() != null && !v.getOwnerId().isBlank()) {
                ownerName = "Unknown owner";
            }

            String plateKey = v.getPlateNumber() == null ? "" : v.getPlateNumber().trim();
            LocalDateTime registered = registeredByPlate.get(plateKey.toUpperCase(Locale.ROOT));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", v.getId());
            row.put("plateNumber", v.getPlateNumber());
            row.put("type", v.getType());
            row.put("color", v.getColor());
            row.put("ownerId", v.getOwnerId());
            row.put("ownerName", ownerName);
            row.put("registered", registered == null ? "—" : REGISTERED_FMT.format(registered));
            row.put("status", status);
            rows.add(row);
        }

        rows.sort(Comparator.comparing(
                (Map<String, Object> r) -> (String) r.get("plateNumber"),
                String.CASE_INSENSITIVE_ORDER));

        int total = vehicles.size();
        List<String> typeLabels = new ArrayList<>(typeSet);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("flagged", flagged);
        stats.put("typeCount", typeLabels.size());
        stats.put("typeLabels", typeLabels.isEmpty() ? "—" : String.join(", ", typeLabels));
        stats.put("typeLabelsList", typeLabels);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stats", stats);
        payload.put("vehicles", rows);
        return payload;
    }

    /** Admin removes any vehicle by id. */
    public boolean deleteVehicleByAdmin(String vehicleId, String adminId) {
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleId);
        if (vehicleOpt.isEmpty()) return false;

        Vehicle vehicle = vehicleOpt.get();
        boolean deleted = vehicleRepository.deleteById(vehicleId);
        if (deleted) {
            activityLogger.log(adminId, "ADMIN", "VEHICLE_DELETED",
                    "Removed vehicle " + vehicle.getPlateNumber() + " (" + vehicleId + ")");
        }
        return deleted;
    }

    private Map<String, LocalDateTime> loadVehicleRegistrationTimes() {
        Map<String, LocalDateTime> times = new HashMap<>();
        File logFile = new File(activityLogPath);
        if (!logFile.exists()) return times;

        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",", 5);
                if (parts.length < 5) continue;
                if (!"VEHICLE_REGISTERED".equals(parts[3].trim())) continue;

                LocalDateTime ts;
                try {
                    ts = LocalDateTime.parse(parts[0].trim());
                } catch (Exception e) {
                    continue;
                }

                String detail = parts[4].trim();
                if (detail.startsWith("\"") && detail.endsWith("\"")) {
                    detail = detail.substring(1, detail.length() - 1);
                }
                String prefix = "Registered vehicle with plate ";
                if (!detail.regionMatches(true, 0, prefix, 0, prefix.length())) continue;

                String plate = detail.substring(prefix.length()).trim().toUpperCase(Locale.ROOT);
                if (!plate.isEmpty()) {
                    times.merge(plate, ts, (a, b) -> a.isBefore(b) ? a : b);
                }
            }
        } catch (IOException e) {
            System.err.println("[VehicleService] Failed to read activity log: " + e.getMessage());
        }
        return times;
    }
}
