package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.Driver;
import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.repository.UserRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UserService — business logic for User Management
 *
 * Abstraction: exposes high-level operations; callers don't need to know
 * how passwords are hashed or how the CSV is structured.
 */
@Service
public class UserService {

    private final UserRepository    userRepository;
    private final ActivityLogger    activityLogger;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserService(UserRepository userRepository, ActivityLogger activityLogger) {
        this.userRepository  = userRepository;
        this.activityLogger  = activityLogger;
    }

    // register a driver
    public Driver register(String fullName, String userName, String email, String phone, String password) {
        
        //check if already registered
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + email);
        }

        Driver driver = new Driver(
                IdGenerator.next("USR"),
                fullName,
                userName,
                email,
                phone,
                passwordEncoder.encode(password),
                "LICENSE-" + IdGenerator.next("LIC"),
                0
        );

        userRepository.save(driver);

        activityLogger.log(driver.getId(), "DRIVER", "USER_REGISTERED",
                "New driver: " + email);

        return driver;
    }

}
