package com.example.vehicle_parking_management_system.util;

import java.time.Duration;
import java.time.LocalDateTime;


public class FeeCalculator {

    private FeeCalculator() {}


    public static double calculate(LocalDateTime startTime,
                                   LocalDateTime endTime,
                                   double ratePerHour) {
        if (startTime == null || endTime == null) return 0.0;
        if (!endTime.isAfter(startTime)) return 0.0;

        long minutes = Duration.between(startTime, endTime).toMinutes();


        long billableHours = Math.max(1, (long) Math.ceil(minutes / 60.0));

        double fee = billableHours * ratePerHour;
        return Math.round(fee * 100.0) / 100.0;
    }


    public static double calculateRunning(LocalDateTime startTime, double ratePerHour) {
        return calculate(startTime, LocalDateTime.now(), ratePerHour);
    }
}
