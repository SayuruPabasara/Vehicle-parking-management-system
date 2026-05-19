package com.example.vehicle_parking_management_system.repository;

import com.example.vehicle_parking_management_system.model.Reservation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ReservationRepository — reads and writes reservations.csv.
 *
 * CSV format: id,driverId,slotId,vehicleId,status,startTime,endTime,fee,paymentStatus
 */
@Repository
public class ReservationRepository {

    @Value("${parknow.data.reservations}")
    private String filePath;

    // ── Read operations ───────────────────────────────────────────────────────

    public List<Reservation> findAll() {
        List<Reservation> reservations = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return reservations;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Reservation r = parseLine(line);
                if (r != null) reservations.add(r);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read reservations.csv: " + e.getMessage(), e);
        }
        return reservations;
    }

    public Optional<Reservation> findById(String id) {
        return findAll().stream().filter(r -> r.getId().equals(id)).findFirst();
    }

    public List<Reservation> findByDriverId(String driverId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : findAll()) {
            if (r.getDriverId().equals(driverId)) result.add(r);
        }
        return result;
    }

    public List<Reservation> findActiveByDriver(String driverId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : findAll()) {
            if (r.getDriverId().equals(driverId)
                    && r.getStatus() == Reservation.ReservationStatus.ACTIVE) {
                result.add(r);
            }
        }
        return result;
    }

    public List<Reservation> findAllActive() {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : findAll()) {
            if (r.getStatus() == Reservation.ReservationStatus.ACTIVE) result.add(r);
        }
        return result;
    }

    public List<Reservation> findBySlotId(String slotId) {
        List<Reservation> result = new ArrayList<>();
        for (Reservation r : findAll()) {
            if (r.getSlotId().equals(slotId)) result.add(r);
        }
        return result;
    }

    // ── Write operations ──────────────────────────────────────────────────────

    public void save(Reservation reservation) {
        ensureFileExists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(reservation.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to reservations.csv: " + e.getMessage(), e);
        }
    }

    public boolean update(Reservation updated) {
        List<Reservation> all = findAll();
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

    /** Remove all reservations for a driver. Returns reservations removed (for slot cleanup). */
    public List<Reservation> deleteByDriverId(String driverId) {
        List<Reservation> all = findAll();
        List<Reservation> removed = new ArrayList<>();
        boolean changed = all.removeIf(r -> {
            if (driverId != null && driverId.equals(r.getDriverId())) {
                removed.add(r);
                return true;
            }
            return false;
        });
        if (changed) rewriteAll(all);
        return removed;
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void rewriteAll(List<Reservation> reservations) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (Reservation r : reservations) pw.println(r.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite reservations.csv: " + e.getMessage(), e);
        }
    }

    private void ensureFileExists() {
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try { f.createNewFile(); } catch (IOException e) {
                throw new RuntimeException("Cannot create reservations.csv", e);
            }
        }
    }

    /**
     * Parse: id,driverId,slotId,vehicleId,status,startTime,endTime,fee[,paymentStatus]
     */
    private Reservation parseLine(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 8) return null;

        Reservation r = new Reservation();
        r.setId(p[0].trim());
        r.setDriverId(p[1].trim());
        r.setSlotId(p[2].trim());
        r.setVehicleId(p[3].trim());

        try {
            r.setStatus(Reservation.ReservationStatus.valueOf(p[4].trim()));
        } catch (IllegalArgumentException e) {
            r.setStatus(Reservation.ReservationStatus.ACTIVE);
        }

        r.setStartTime(parseDateTime(p[5].trim()));
        r.setEndTime(p[6].trim().isEmpty() ? null : parseDateTime(p[6].trim()));
        r.setFee(parseDoubleSafe(p[7].trim()));

        if (p.length >= 9 && !p[8].trim().isEmpty()) {
            try {
                r.setPaymentStatus(Reservation.PaymentStatus.valueOf(p[8].trim()));
            } catch (IllegalArgumentException e) {
                r.setPaymentStatus(Reservation.PaymentStatus.UNPAID);
            }
        } else {
            r.setPaymentStatus(Reservation.PaymentStatus.UNPAID);
        }
        return r;
    }

    private LocalDateTime parseDateTime(String s) {
        try { return LocalDateTime.parse(s); }
        catch (Exception e) { return LocalDateTime.now(); }
    }

    private double parseDoubleSafe(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0.0; }
    }
}
