package com.parking.model;

import java.time.LocalDateTime;

public class Reservation {

    public enum ReservationStatus { ACTIVE, COMPLETED, CANCELLED }

    private String id;
    private String driverId;
    private String slotId;
    private String vehicleId;
    private ReservationStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double fee;

    public Reservation() {}

    public Reservation(String id, String driverId, String slotId, String vehicleId, ReservationStatus status, LocalDateTime startTime) {
        this.id = id;
        this.driverId = driverId;
        this.slotId = slotId;
        this.vehicleId = vehicleId;
        this.status = status;
        this.startTime = startTime;
        this.fee = 0.0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getVehicleId() { return vehicleId; }
    public void setVehicleId(String vehicleId) { this.vehicleId = vehicleId; }

    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public String toCsvRow(){
        return String.join(id,driverId,slotId,vehicleId,status.name(),startTime.toString(),endTime.toString(),String.valueOf(fee));
    }

}