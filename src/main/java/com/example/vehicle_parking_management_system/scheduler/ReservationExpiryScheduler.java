package com.example.vehicle_parking_management_system.scheduler;

import com.example.vehicle_parking_management_system.service.ReservationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpiryScheduler {

    private final ReservationService reservationService;

    public ReservationExpiryScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /** Auto-complete active sessions once their scheduled end time is reached. */
    @Scheduled(fixedRate = 60_000)
    public void expireOverdueSessions() {
        reservationService.expireOverdueActiveSessions();
    }
}
