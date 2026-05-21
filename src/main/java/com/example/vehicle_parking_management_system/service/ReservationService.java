package com.example.vehicle_parking_management_system.service;

import com.example.vehicle_parking_management_system.model.ParkingSlot;
import com.example.vehicle_parking_management_system.model.Reservation;
import com.example.vehicle_parking_management_system.model.User;
import com.example.vehicle_parking_management_system.model.Vehicle;
import com.example.vehicle_parking_management_system.repository.ReservationRepository;
import com.example.vehicle_parking_management_system.repository.UserRepository;
import com.example.vehicle_parking_management_system.repository.VehicleRepository;
import com.example.vehicle_parking_management_system.util.ActivityLogger;
import com.example.vehicle_parking_management_system.util.FeeCalculator;
import com.example.vehicle_parking_management_system.util.IdGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SlotService           slotService;
    private final UserRepository        userRepository;
    private final VehicleRepository     vehicleRepository;
    private final ActivityLogger        activityLogger;

    public ReservationService(ReservationRepository reservationRepository,
                              SlotService slotService,
                              UserRepository userRepository,
                              VehicleRepository vehicleRepository,
                              ActivityLogger activityLogger) {
        this.reservationRepository = reservationRepository;
        this.slotService           = slotService;
        this.userRepository        = userRepository;
        this.vehicleRepository     = vehicleRepository;
        this.activityLogger        = activityLogger;
    }

    /**
     * Completes ACTIVE reservations whose scheduled end time has passed.
     * Releases slots and persists final fees to the database.
     */
    public int expireOverdueActiveSessions() {
        LocalDateTime now = LocalDateTime.now();
        int expired = 0;
        for (Reservation r : new ArrayList<>(reservationRepository.findAllActive())) {
            if (r.getEndTime() != null && !r.getEndTime().isAfter(now)) {
                if (completeSessionAtScheduledEnd(r)) {
                    expired++;
                }
            }
        }
        return expired;
    }

    private boolean completeSessionAtScheduledEnd(Reservation reservation) {
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            return false;
        }
        LocalDateTime endTime = reservation.getEndTime();
        if (endTime == null) {
            return false;
        }

        reservation.setStatus(Reservation.ReservationStatus.COMPLETED);

        double rate = slotService.findById(reservation.getSlotId())
                .map(ParkingSlot::getHourlyRate)
                .orElse(150.0);
        double fee = FeeCalculator.calculate(reservation.getStartTime(), endTime, rate);
        reservation.setFee(fee);
        reservation.setPaymentStatus(Reservation.PaymentStatus.UNPAID);

        if (!reservationRepository.update(reservation)) {
            return false;
        }

        try {
            slotService.releaseSlot(reservation.getSlotId(), reservation.getDriverId());
        } catch (RuntimeException e) {
            System.err.println("[ReservationService] Slot release on auto-complete: " + e.getMessage());
        }

        activityLogger.log(reservation.getDriverId(), "DRIVER", "BOOKING_AUTO_COMPLETED",
                "Reservation: " + reservation.getId() + " | Fee: LKR " + fee);
        return true;
    }

    public Reservation createBooking(String driverId, String slotId, String vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        expireOverdueActiveSessions();

        boolean slotAlreadyBooked = reservationRepository.findAllActive().stream()
                .anyMatch(r -> r.getSlotId().equals(slotId));
        if (slotAlreadyBooked) {
            throw new IllegalStateException("Slot is already occupied by another reservation.");
        }

        slotService.allocateSlot(slotId, vehicleId, driverId);

        double rate = slotService.findById(slotId)
                .map(ParkingSlot::getHourlyRate)
                .orElse(150.0);
        double estimatedFee = FeeCalculator.calculate(startTime, endTime, rate);

        Reservation reservation = new Reservation(
                IdGenerator.next("RES"),
                driverId,
                slotId,
                vehicleId,
                Reservation.ReservationStatus.ACTIVE,
                startTime
        );
        reservation.setEndTime(endTime);
        reservation.setFee(estimatedFee);
        reservation.setPaymentStatus(Reservation.PaymentStatus.UNPAID);

        reservationRepository.save(reservation);

        activityLogger.log(driverId, "DRIVER", "BOOKING_CREATED",
                "Reservation: " + reservation.getId() + " | Slot: " + slotId + " | Fee (est.): " + estimatedFee);
        return reservation;
    }

    public Reservation checkOut(String reservationId, String driverId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Reservation not found: " + reservationId));

        if (driverId != null && !reservation.getDriverId().equals(driverId)) {
            throw new SecurityException("You do not own this reservation.");
        }
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Reservation is not ACTIVE.");
        }

        LocalDateTime endTime = LocalDateTime.now();
        reservation.setEndTime(endTime);
        reservation.setStatus(Reservation.ReservationStatus.COMPLETED);

        double rate = slotService.findById(reservation.getSlotId())
                .map(ParkingSlot::getHourlyRate)
                .orElse(150.0);

        double fee = FeeCalculator.calculate(reservation.getStartTime(), endTime, rate);
        reservation.setFee(fee);
        reservation.setPaymentStatus(Reservation.PaymentStatus.UNPAID);

        reservationRepository.update(reservation);

        slotService.releaseSlot(reservation.getSlotId(), driverId);

        activityLogger.log(driverId, "DRIVER", "BOOKING_CHECKED_OUT",
                "Reservation: " + reservationId + " | Fee: LKR " + fee);
        return reservation;
    }


    public int confirmPaymentsPaid(List<String> reservationIds, String adminId) {
        if (reservationIds == null || reservationIds.isEmpty()) return 0;
        int updated = 0;
        for (String id : reservationIds) {
            if (id == null || id.isBlank()) continue;
            Optional<Reservation> opt = reservationRepository.findById(id.trim());
            if (opt.isEmpty()) continue;
            Reservation r = opt.get();
            if (r.getPaymentStatus() != Reservation.PaymentStatus.UNPAID) continue;
            r.setPaymentStatus(Reservation.PaymentStatus.PAID);
            if (reservationRepository.update(r)) {
                updated++;
                activityLogger.log(adminId != null ? adminId : "ADMIN", "ADMIN", "PAYMENT_CONFIRMED",
                        "Reservation: " + id + " | LKR " + r.getFee());
            }
        }
        return updated;
    }
    

    public Map<String, Object> confirmDriverCashPayments(String driverId) {
        List<Reservation> list = reservationRepository.findByDriverId(driverId);
        int updated = 0;
        for (Reservation r : list) {
            if (r.getStatus() != Reservation.ReservationStatus.COMPLETED) continue;
            if (r.getPaymentStatus() != Reservation.PaymentStatus.UNPAID) continue;
            if (r.getFee() <= 0) continue;
            r.setPaymentStatus(Reservation.PaymentStatus.PAID);
            if (reservationRepository.update(r)) {
                updated++;
                activityLogger.log(driverId, "DRIVER", "PAYMENT_CONFIRMED_CASH",
                        "Reservation: " + r.getId() + " | LKR " + r.getFee());
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("updated", updated);
        out.put("message", updated == 0
                ? "No unpaid completed reservations to confirm."
                : updated + " reservation(s) marked as paid.");
        return out;
    }


    public int markPaymentsUnpaid(List<String> reservationIds, String adminId) {
        if (reservationIds == null || reservationIds.isEmpty()) return 0;
        int updated = 0;
        for (String id : reservationIds) {
            if (id == null || id.isBlank()) continue;
            Optional<Reservation> opt = reservationRepository.findById(id.trim());
            if (opt.isEmpty()) continue;
            Reservation r = opt.get();
            if (r.getStatus() != Reservation.ReservationStatus.COMPLETED) continue;
            if (r.getPaymentStatus() != Reservation.PaymentStatus.PAID) continue;
            r.setPaymentStatus(Reservation.PaymentStatus.UNPAID);
            if (reservationRepository.update(r)) {
                updated++;
                activityLogger.log(adminId != null ? adminId : "ADMIN", "ADMIN", "PAYMENT_REVERTED",
                        "Reservation: " + id + " marked unpaid.");
            }
        }
        return updated;
    }

    public Map<String, Object> getDriverBillingSummary(String driverId) {
        expireOverdueActiveSessions();
        List<Reservation> list = new ArrayList<>(reservationRepository.findByDriverId(driverId));
        list.sort(Comparator.comparing(Reservation::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        double totalPaid = 0.0;
        double amountDue = 0.0;
        int paidSessions = 0;
        int unpaidSessions = 0;

        DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

        List<Map<String, Object>> history = new ArrayList<>();
        for (Reservation r : list) {
            if (r.getPaymentStatus() == Reservation.PaymentStatus.PAID) {
                totalPaid += r.getFee();
                if (r.getFee() > 0) paidSessions++;
            } else {
                amountDue += r.getFee();
                if (r.getFee() > 0) unpaidSessions++;
            }

            String slotLabel = slotService.findById(r.getSlotId())
                    .map(ParkingSlot::getSlotNumber)
                    .orElse(r.getSlotId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", r.getStartTime() != null ? r.getStartTime().format(dateFmt) : "—");
            row.put("reference", r.getId());
            row.put("slot", slotLabel);
            row.put("amount", r.getFee());
            row.put("status", r.getPaymentStatus() == Reservation.PaymentStatus.PAID ? "Paid" : "Unpaid");
            row.put("reservationStatus", r.getStatus().name());
            history.add(row);
        }

        double defaultRate = slotService.getAllSlots().stream()
                .findFirst()
                .map(ParkingSlot::getHourlyRate)
                .orElse(150.0);

        YearMonth ym = YearMonth.now();
        double monthFees = 0.0;
        for (Reservation r : list) {
            if (r.getStartTime() != null && YearMonth.from(r.getStartTime()).equals(ym)) {
                monthFees += r.getFee();
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalPaidAllTime", totalPaid);
        out.put("amountDue", amountDue);
        out.put("paidSessionCount", paidSessions);
        out.put("unpaidSessionCount", unpaidSessions);
        out.put("defaultHourlyRate", defaultRate);
        out.put("history", history);
        out.put("monthlyParkingFees", monthFees);
        out.put("monthLabel", ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)));
        return out;
    }


    public List<Map<String, Object>> getAdminReservationRows() {
        expireOverdueActiveSessions();
        List<Reservation> all = new ArrayList<>(reservationRepository.findAll());
        all.sort(Comparator.comparing(Reservation::getStartTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Reservation r : all) {
            String driverName = userRepository.findById(r.getDriverId())
                    .map(User::getFullName)
                    .orElse(r.getDriverId());
            String plate = vehicleRepository.findById(r.getVehicleId())
                    .map(Vehicle::getPlateNumber)
                    .orElse(r.getVehicleId());
            String slotLabel = slotService.findById(r.getSlotId())
                    .map(ParkingSlot::getSlotNumber)
                    .orElse(r.getSlotId());

            String uiStatus;
            if (r.getStatus() == Reservation.ReservationStatus.ACTIVE) {
                uiStatus = "active";
            } else if (r.getStatus() == Reservation.ReservationStatus.COMPLETED
                    && r.getPaymentStatus() == Reservation.PaymentStatus.UNPAID
                    && r.getFee() > 0) {
                uiStatus = "payment_pending";
            } else {
                uiStatus = "completed";
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("driverName", driverName);
            m.put("plate", plate);
            m.put("slot", slotLabel);
            m.put("checkIn", r.getStartTime() != null ? r.getStartTime().toString() : "");
            m.put("checkOut", r.getStatus() == Reservation.ReservationStatus.COMPLETED && r.getEndTime() != null
                    ? r.getEndTime().toString() : "");
            m.put("scheduledEnd", r.getEndTime() != null ? r.getEndTime().toString() : "");
            m.put("status", r.getStatus().name());
            m.put("fee", r.getFee());
            m.put("paymentStatus", r.getPaymentStatus().name());
            m.put("uiStatus", uiStatus);
            rows.add(m);
        }
        return rows;
    }

    public int countPendingPaymentReservations() {
        return (int) reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == Reservation.ReservationStatus.COMPLETED
                        && r.getPaymentStatus() == Reservation.PaymentStatus.UNPAID
                        && r.getFee() > 0)
                .count();
    }

    public List<Reservation> getActiveSessions(String driverId) {
        expireOverdueActiveSessions();
        return reservationRepository.findActiveByDriver(driverId);
    }

    public List<Reservation> getAllActiveSessions() {
        expireOverdueActiveSessions();
        return reservationRepository.findAllActive();
    }

    public List<Reservation> getDriverHistory(String driverId) {
        return reservationRepository.findByDriverId(driverId);
    }

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Optional<Reservation> findById(String reservationId) {
        return reservationRepository.findById(reservationId);
    }

    public int getActiveSessionCount() {
        return reservationRepository.findAllActive().size();
    }

    public double getRunningFee(String reservationId) {
        return reservationRepository.findById(reservationId).map(r -> {
            double rate = slotService.findById(r.getSlotId())
                    .map(ParkingSlot::getHourlyRate).orElse(150.0);
            return FeeCalculator.calculateRunning(r.getStartTime(), rate);
        }).orElse(0.0);
    }
}
