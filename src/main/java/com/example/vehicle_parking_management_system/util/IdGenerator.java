package com.example.vehicle_parking_management_system.util;

import java.util.UUID;

/**
 * IdGenerator — produces short unique IDs for entities.
 * Uses the first 8 characters of a UUID for readability.
 */
public class IdGenerator {

    private IdGenerator() {}

    public static String next() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    /** Generate a prefixed ID, e.g. "USR-A1B2C3D4" */
    public static String next(String prefix) {
        return prefix + "-" + next();
    }
}
