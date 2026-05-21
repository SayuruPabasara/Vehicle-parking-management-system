package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.Optional;


@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestParam String fullName,      //saves request parameters (from the form submission) into local variables
                                      @RequestParam String userName,
                                      @RequestParam String email,
                                      @RequestParam String phone,
                                      @RequestParam String password) {
        
        try {
                //this calls the service layer to perform registration logic, which includes validation and saving the user to the database.
                // If any validation fails, it will throw an IllegalArgumentException with a message that we can return to the client.
                userService.register(fullName, userName, email, phone, password);  
            
            return ResponseEntity.ok(Map.of(
                    "success", true
                ));

        } catch (IllegalArgumentException e) {
            
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email,
                                   @RequestParam String password,
                                   HttpSession session) {

        Optional<User> userOpt = userService.login(email,password);
        
        if (userOpt.isEmpty()) {
        return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "message", "Invalid email or password."
        ));
        }

        User user = userOpt.get();
        session.setAttribute("userId",   user.getId());
        session.setAttribute("userRole", user.getRole());
        session.setAttribute("userName", user.getUserName());

        String redirect = "ADMIN".equals(user.getRole())
                ? "pg-admin-dash"
                : "pg-driver-dash";

        return ResponseEntity.ok(Map.of(
                "success",  true,
                "role",     user.getRole(),
                "name",     user.getUserName(),
                "redirect", redirect
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "redirect", "/login"
        ));
    }

    @GetMapping("/api/driver/profile")
    public ResponseEntity<?> getDriverProfile(HttpSession session) {
        String driverId = (String) session.getAttribute("userId");
        if (driverId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required."));
        }
        if (!"DRIVER".equals(session.getAttribute("userRole"))) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Driver access required."));
        }

        return userService.getDriverProfile(driverId)
                .map(profile -> ResponseEntity.ok(Map.of("success", true, "profile", profile)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of(
                        "success", false, "message", "Driver profile not found."
                )));
    }

    @PostMapping("/api/driver/profile")
    public ResponseEntity<?> updateDriverProfile(@RequestParam String fullName,
                                                 @RequestParam String userName,
                                                 @RequestParam String email,
                                                 @RequestParam(required = false) String phone,
                                                 HttpSession session) {
        String driverId = (String) session.getAttribute("userId");
        if (driverId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Authentication required."));
        }
        if (!"DRIVER".equals(session.getAttribute("userRole"))) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Driver access required."));
        }

        try {
            var driver = userService.updateDriverProfile(driverId, fullName, userName, email, phone);
            session.setAttribute("userName", driver.getUserName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Profile updated successfully."
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
