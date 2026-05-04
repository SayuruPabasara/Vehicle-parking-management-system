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

    /** Load all users from CSV into a list. */
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return users;

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
        return users;
    }

    /** Find a user by email (used for login). */
    public Optional<User> findByEmail(String email) {
        return findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
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
     * Driver: 7 fields — id,name,email,passwordHash,role,licenseNumber,vehicleCount
     * Admin:  7 fields — id,name,email,passwordHash,role,adminLevel,createdBy
     */
    private User parseLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) return null;

        String id           = parts[0].trim();
        String name         = parts[1].trim();
        String email        = parts[2].trim();
        String passwordHash = parts[3].trim();
        String role         = parts[4].trim();

        if ("DRIVER".equalsIgnoreCase(role)) {
            Driver d = new Driver();
            d.setId(id); d.setUserName(name); d.setEmail(email);
            d.setPassword(passwordHash); d.setRole("DRIVER");
            if (parts.length >= 7) {
                d.setLicenseNumber(parts[5].trim());
                d.setVehicleCount(parseIntSafe(parts[6].trim()));
            }
            return d;
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            Admin a = new Admin();
            a.setId(id); a.setUserName(name); a.setEmail(email);
            a.setPassword(passwordHash); a.setRole("ADMIN");
            if (parts.length >= 7) {
                try { a.setAdminLevel(Admin.AdminLevel.valueOf(parts[5].trim())); }
                catch (IllegalArgumentException ex) { a.setAdminLevel(Admin.AdminLevel.READONLY); }
                a.setCreatedBy(parts[6].trim());
            }
            return a;
        }
        return null;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }
    
}
