package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.Optional;

/**
 * UserController — HTTP endpoints for User Management
 *
 * POST /register        Register a new driver account
 * POST /login           Authenticate; set session; redirect by role
 * GET  /profile         View logged-in user profile
 * POST /profile/update  Update name, email, (optionally) password
 * POST /logout          Invalidate session
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    
    //POST /register  -  Register a new driver account
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

}
