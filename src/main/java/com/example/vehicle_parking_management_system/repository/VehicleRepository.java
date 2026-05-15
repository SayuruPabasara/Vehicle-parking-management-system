package com.example.vehicle_parking_management_system.repository;

import com.example.vehicle_parking_management_system.model.Vehicle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.util.*;

@Repository
public class VehicleRepository {

    @Value("${parknow.data.vehicles}")
    private String filePath;

    // ── Read operations ───────────────────────────────────────────────────────

    public List<Vehicle> findAll() {
        List<Vehicle> vehicles = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return vehicles;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Vehicle v = parseLine(line);
                if (v != null) vehicles.add(v);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read vehicles.csv: " + e.getMessage(), e);
        }
        return vehicles;
    }

    public Optional<Vehicle> findById(String id) {
        return findAll().stream().filter(v -> v.getId().equals(id)).findFirst();
    }

    /** O(n) scan; callers should use the HashMap cache in the service layer. */
    public Optional<Vehicle> findByPlateNumber(String plate) {
        return findAll().stream()
                .filter(v -> v.getPlateNumber().equalsIgnoreCase(plate))
                .findFirst();
    }

    public List<Vehicle> findByDriverId(String driverId) {
        List<Vehicle> result = new ArrayList<>();
        for (Vehicle v : findAll()) {
            if (v.getOwnerId().equals(driverId)) result.add(v);
        }
        return result;
    }

    /** Count vehicles per owner (driver) ID. */
    public Map<String, Integer> countByOwnerId() {
        Map<String, Integer> counts = new HashMap<>();
        for (Vehicle v : findAll()) {
            String ownerId = v.getOwnerId();
            if (ownerId == null || ownerId.isBlank()) continue;
            counts.merge(ownerId, 1, Integer::sum);
        }
        return counts;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    public void save(Vehicle vehicle) {
        ensureFileExists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(vehicle.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to vehicles.csv: " + e.getMessage(), e);
        }
    }

    public boolean deleteById(String id) {
        List<Vehicle> all = findAll();
        boolean removed = all.removeIf(v -> v.getId().equals(id));
        if (removed) rewriteAll(all);
        return removed;
    }

    public boolean update(Vehicle updated) {
        List<Vehicle> all = findAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(updated.getId())) {
                all.set(i, updated);
                found = true;
                break;
            }
        }
        if (found) rewriteAll(all);
        return found;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void rewriteAll(List<Vehicle> vehicles) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (Vehicle v : vehicles) pw.println(v.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite vehicles.csv: " + e.getMessage(), e);
        }
    }

    private void ensureFileExists() {
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try { f.createNewFile(); } catch (IOException e) {
                throw new RuntimeException("Cannot create vehicles.csv", e);
            }
        }
    }

    /**
     * Parse a CSV line into the correct Vehicle subclass.
     * type field (index 2) determines Car vs Bike.
     */
    private Vehicle parseLine(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 5) return null;

        String id       = p[0].trim();
        String plate    = p[1].trim();
        String type     = p[2].trim();
        String color    = p[3].trim();
        String driverId = p[4].trim();

        
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setPlateNumber(plate);
        vehicle.setType(type);
        vehicle.setColor(color);
        vehicle.setOwnerId(driverId);
            
        return vehicle;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
    
}
