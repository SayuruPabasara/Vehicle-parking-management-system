package com.example.vehicle_parking_management_system.util;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * FeeCalculator — encapsulates the parking fee formula.
 *
 * Abstraction: ReservationService calls calculate() without knowing
 * the billing logic internals.
 *
 * Formula: fee = durationInHours × ratePerHour
 * Minimum billable duration: 1 hour (partial hours rounded up).
 */
public class FeeCalculator {

    private FeeCalculator() {} // Utility class

    /**
     * Calculate the parking fee.
     *
     * @param startTime   when the vehicle entered
     * @param endTime     when the vehicle exited
     * @param ratePerHour hourly rate of the slot (LKR)
     * @return total fee in LKR, rounded to 2 decimal places
     */
    public static double calculate(LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   double ratePerHour) {
        if (startTime == null || endTime == null) return 0.0;
        if (!endTime.isAfter(startTime)) return 0.0;

        long minutes = Duration.between(startTime, endTime).toMinutes();

        // Round up to the nearest hour; minimum 1 hour
        long billableHours = Math.max(1, (long) Math.ceil(minutes / 60.0));

        double fee = billableHours * ratePerHour;
        return Math.round(fee * 100.0) / 100.0; // 2 decimal places
    }

    /**
     * Convenience overload — calculates from now to startTime
     * for displaying a live "running cost" on the active session page.
     */
    public static double calculateRunning(LocalDateTime startTime, double ratePerHour) {
        return calculate(startTime, LocalDateTime.now(), ratePerHour);
    }
}
