package com.example.vehicle_parking_management_system.repository;

import com.example.vehicle_parking_management_system.model.ParkingSlot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;

/**
 * SlotRepository — reads and writes slots.csv.
 *
 * CSV format: id,slotNumber,status,occupantVehicleId,hourlyRate
 *
 * On first run (empty CSV), seeds the configured number of parking slots.
 */
@Repository
public class SlotRepository {

    @Value("${parknow.data.slots}")
    private String filePath;

    @Value("${parknow.slots.total-count:30}")
    private int totalCount;

    @Value("${parknow.rate.default:150.0}")
    private double defaultRate;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Seed slots on startup if the CSV is empty or missing.
     * Generates totalCount generic parking slots.
     */
    @PostConstruct
    public void seedIfEmpty() {
        ensureFileExists();
        if (findAll().isEmpty()) {
            List<ParkingSlot> seeds = new ArrayList<>();
            for (int i = 1; i <= totalCount; i++) {
                // Create 3 sections (A, B, C) with 10 slots each for a total of 30
                char section = (char) ('A' + (i - 1) / 10);
                int numInSection = (i - 1) % 10 + 1;
                String slotNumber = String.format("%c-%02d", section, numInSection);
                String id = String.format("SLT-%02d", i);
                
                seeds.add(new ParkingSlot(id, slotNumber,
                        ParkingSlot.SlotStatus.AVAILABLE, null, defaultRate));
            }
            rewriteAll(seeds);
            System.out.println("[SlotRepository] Seeded " + seeds.size() + " parking slots.");
        }
    }

    // ── Read operations ───────────────────────────────────────────────────────

    public List<ParkingSlot> findAll() {
        List<ParkingSlot> slots = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return slots;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                ParkingSlot slot = parseLine(line);
                if (slot != null) slots.add(slot);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read slots.csv: " + e.getMessage(), e);
        }
        return slots;
    }

    public Optional<ParkingSlot> findById(String id) {
        return findAll().stream().filter(s -> s.getId().equals(id)).findFirst();
    }

    public List<ParkingSlot> findByStatus(ParkingSlot.SlotStatus status) {
        List<ParkingSlot> result = new ArrayList<>();
        for (ParkingSlot s : findAll()) {
            if (s.getStatus() == status) result.add(s);
        }
        return result;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    public boolean update(ParkingSlot updated) {
        List<ParkingSlot> all = findAll();
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

    public void rewriteAll(List<ParkingSlot> slots) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (ParkingSlot s : slots) pw.println(s.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite slots.csv: " + e.getMessage(), e);
        }
    }

    private void ensureFileExists() {
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try { f.createNewFile(); } catch (IOException e) {
                throw new RuntimeException("Cannot create slots.csv", e);
            }
        }
    }

    /**
     * Parse: id,slotNumber,status,occupantVehicleId,hourlyRate
     */
    private ParkingSlot parseLine(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 5) return null;

        String id          = p[0].trim();
        String slotNumber  = p[1].trim();
        String statusStr   = p[2].trim();
        String occupant    = p[3].trim().isEmpty() ? null : p[3].trim();
        double rate        = parseDoubleSafe(p[4].trim());

        ParkingSlot.SlotStatus status;
        try { status = ParkingSlot.SlotStatus.valueOf(statusStr); }
        catch (IllegalArgumentException e) { status = ParkingSlot.SlotStatus.AVAILABLE; }

        return new ParkingSlot(id, slotNumber, status, occupant, rate);
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }
}
