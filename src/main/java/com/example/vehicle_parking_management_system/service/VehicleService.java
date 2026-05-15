package com.example.vehicle_parking_management_system.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import com.example.vehicle_parking_management_system.model.Vehicle;
import com.example.vehicle_parking_management_system.repository.VehicleRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;

import java.util.*;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserService userService;
    private final ActivityLogger activityLogger;
    
    public VehicleService(VehicleRepository vehicleRepository, UserService userService, ActivityLogger activityLogger) {
        this.vehicleRepository = vehicleRepository;
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
}
