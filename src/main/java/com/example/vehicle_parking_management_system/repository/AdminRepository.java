package com.example.vehicle_parking_management_system.repository;

import com.example.vehicle_parking_management_system.model.Admin;
import com.example.vehicle_parking_management_system.util.IdGenerator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.*;


@Repository
public class AdminRepository {

    @Value("${parknow.data.admins}")
    private String filePath;





    public List<Admin> findAll() {
        List<Admin> admins = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) return admins;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Admin a = parseLine(line);
                if (a != null) admins.add(a);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read admins.csv: " + e.getMessage(), e);
        }
        return admins;
    }

    public Optional<Admin> findById(String id) {
        return findAll().stream().filter(a -> a.getId().equals(id)).findFirst();
    }

    public Optional<Admin> findByEmail(String email) {
        return findAll().stream()
                .filter(a -> a.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }



    public void save(Admin admin) {
        ensureFileExists();
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, true))) {
            pw.println(admin.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to admins.csv: " + e.getMessage(), e);
        }
    }

    public boolean deleteById(String id) {
        List<Admin> all = findAll();
        boolean removed = all.removeIf(a -> a.getId().equals(id));
        if (removed) rewriteAll(all);
        return removed;
    }

    public boolean update(Admin updated) {
        List<Admin> all = findAll();
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



    private void rewriteAll(List<Admin> admins) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath, false))) {
            for (Admin a : admins) pw.println(a.toCsvRow());
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite admins.csv: " + e.getMessage(), e);
        }
    }

    private void ensureFileExists() {
        File f = new File(filePath);
        if (!f.exists()) {
            f.getParentFile().mkdirs();
            try { f.createNewFile(); } catch (IOException e) {
                throw new RuntimeException("Cannot create admins.csv", e);
            }
        }
    }

    private Admin parseLine(String line) {
        String[] p = line.split(",", -1);
        if (p.length < 9) return null;

        Admin a = new Admin();
        a.setId(p[0].trim());
        a.setFullName(p[1].trim());
        a.setUserName(p[2].trim());
        a.setEmail(p[3].trim());
        a.setPhone(p[4].trim());
        a.setPassword(p[5].trim());
        a.setRole(p[6].trim().isEmpty() ? "ADMIN" : p[6].trim());
        try {
            a.setAdminLevel(Admin.AdminLevel.valueOf(p[7].trim()));
        } catch (IllegalArgumentException e) {
            a.setAdminLevel(Admin.AdminLevel.READONLY);
        }
        a.setCreatedBy(p[8].trim());
        return a;
    }
}
