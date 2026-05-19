package com.example.vehicle_parking_management_system.repository;

import com.example.vehicle_parking_management_system.model.Admin;
import com.example.vehicle_parking_management_system.model.Driver;
import com.example.vehicle_parking_management_system.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/*
 * UserRepository — reads and writes users.csv.
 *
 * CSV format (Driver): id,name,email,passwordHash,role,licenseNumber,vehicleCount
 * CSV format (Admin):  id,name,email,passwordHash,role,adminLevel,createdBy
 *
 * All users (both Driver and Admin) are stored in the same users.csv file,
 * differentiated by the 'role' column.
 */
@Repository
public class UserRepository {

    @Value("${parknow.data.users}")
    private String filePath;

    @Value("${parknow.data.admins}")
    private String adminFilePath;

    /** Load all users from CSV into a list. */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        File file = new File(filePath);
        File adminFile = new File(adminFilePath);
        if (!file.exists() && !adminFile.exists()) return users;

        // Read from users.csv
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    User user = parseLine(line);
                    if (user != null) users.add(user);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read users.csv: " + e.getMessage(), e);
            }
        }

        // Read from admins.csv
        if (adminFile.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(adminFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.strip();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    User user = parseLine(line);
                    if (user != null) users.add(user);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read admins.csv: " + e.getMessage(), e);
            }
        }

        return users;
    }

    /** Find a user by email (used for login). */
    public Optional<User> findByEmail(String email) {
        return findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    /** Count registered drivers (users.csv rows with role DRIVER). */
    public int countDrivers() {
        return findAllDrivers().size();
    }

    /** All accounts with role DRIVER (from users.csv and admins.csv). */
    public List<Driver> findAllDrivers() {
        List<Driver> drivers = new ArrayList<>();
        for (User u : findAll()) {
            if (u instanceof Driver d) {
                drivers.add(d);
            }
        }
        return drivers;
    }

    /** Find a user by their unique ID. */
    public Optional<User> findById(String id) {
        return findAll().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }
    
    //Append a new user to the CSV file.
    //Precondition: caller has verified no duplicate email.
    public void save(User user) {
        ensureFileExists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {

            pw.println(user.toCsvRow());
            
        } catch (IOException e) {
            
            throw new RuntimeException("Failed to write to users.csv: " + e.getMessage(), e);
        }
    }


    //check if users.csv exists. if not, create it.
    private void ensureFileExists() {
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try { f.createNewFile(); } catch (IOException e) {
                throw new RuntimeException("Cannot create users.csv", e);
            }
        }
    }

    /**
     * Parse a single CSV line into a User subclass.
     * Driver: 9 fields — id,fullName,userName,email,phone,passwordHash,role,licenseNumber,vehicleCount
     * Admin:  9 fields — id,fullName,userName,email,phone,passwordHash,role,adminLevel,createdBy
     */
    private User parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 7) return null;

        String id           = parts[0].trim();
        String fullName     = parts[1].trim();
        String userName     = parts[2].trim();
        String email        = parts[3].trim();
        String phone        = parts[4].trim();
        String passwordHash = parts[5].trim();
        String role         = parts[6].trim();

        if ("DRIVER".equalsIgnoreCase(role)) {
            Driver d = new Driver();
            d.setId(id);
            d.setFullName(fullName);
            d.setUserName(userName);
            d.setEmail(email);
            d.setPhone(phone);
            d.setPassword(passwordHash);
            d.setRole("DRIVER");
            if (parts.length >= 9) {
                d.setLicenseNumber(parts[7].trim());
                d.setVehicleCount(parseIntSafe(parts[8].trim()));
            }
            return d;
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            Admin a = new Admin();
            a.setId(id);
            a.setFullName(fullName);
            a.setUserName(userName);
            a.setEmail(email);
            a.setPhone(phone);
            a.setPassword(passwordHash);
            a.setRole("ADMIN");
            if (parts.length >= 9) {
                try { a.setAdminLevel(Admin.AdminLevel.valueOf(parts[7].trim())); }
                catch (IllegalArgumentException ex) { a.setAdminLevel(Admin.AdminLevel.READONLY); }
                a.setCreatedBy(parts[8].trim());
            }
            return a;
        }
        return null;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Update an existing user by rewriting the entire file.
     * Finds the row with the matching ID and replaces it.
     */
    public boolean update(User updated) {
        List<User> all = findAll();
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

    private void rewriteAll(List<User> users) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (User u : users) pw.println(u.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite users.csv: " + e.getMessage(), e);
        }
    }

    /**
     * Remove a driver from users.csv only (does not affect admins.csv).
     */
    public boolean deleteDriverById(String id) {
        File file = new File(filePath);
        if (!file.exists()) return false;

        List<String> kept = new ArrayList<>();
        boolean removed = false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.strip();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    kept.add(line);
                    continue;
                }
                User user = parseLine(trimmed);
                if (user instanceof Driver d && d.getId().equals(id)) {
                    removed = true;
                    continue;
                }
                kept.add(line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read users.csv: " + e.getMessage(), e);
        }

        if (!removed) return false;

        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (String row : kept) pw.println(row);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite users.csv: " + e.getMessage(), e);
        }
        return true;
    }

}
