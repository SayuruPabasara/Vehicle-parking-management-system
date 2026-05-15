package com.example.vehicle_parking_management_system.controller;

import com.example.vehicle_parking_management_system.model.Vehicle;
import com.example.vehicle_parking_management_system.service.VehicleService;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping("/api/my-vehicles")
    public ResponseEntity<?> getMyVehicles(HttpSession session) {
        String ownerId = (String) session.getAttribute("userId");

        if (ownerId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        return ResponseEntity.ok(vehicleService.getVehiclesByOwner(ownerId));
    }

    @DeleteMapping("/api/my-vehicles/{id}")
    public ResponseEntity<?> removeVehicle(@PathVariable String id, HttpSession session) {
        String ownerId = (String) session.getAttribute("userId");
        if (ownerId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        }

        boolean deleted = vehicleService.deleteVehicle(id, ownerId);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Vehicle removed successfully"));
        }
        return ResponseEntity.status(404).body(Map.of("success", false, "message", "Vehicle not found"));
    }

    //register vehicle
    @PostMapping("/register-vehicle")
    public ResponseEntity<?> registerVehicle(@RequestParam String plate,
                                             @RequestParam String color,
                                             @RequestParam String vtype,
                                             HttpSession session) {

        String ownerId = (String) session.getAttribute("userId");

        if (ownerId == null) 
            return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));

        try{

            vehicleService.registerVehicle(plate, color, vtype, session, ownerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Vehicle registered successfully"
            ));

        }catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }
}
