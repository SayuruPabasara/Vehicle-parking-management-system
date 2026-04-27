package com.parking.model;

public class ParkingSlot {
    
    public enum SlotStatus { AVAILABLE, OCCUPIED, MAINTENANCE }

    private String id;
    private String slotNumber;
    private SlotStatus status;
    private String currentVehicleId;
    private double hourlyRate;

    public ParkingSlot() {}

    public ParkingSlot(String id, String slotNumber, SlotStatus status, String currentVehicleId, double hourlyRate) {
        this.id = id;
        this.slotNumber = slotNumber;
        this.status = status;
        this.currentVehicleId = currentVehicleId;
        this.hourlyRate = hourlyRate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSlotNumber() { return slotNumber; }
    public void setSlotNumber(String slotNumber) { this.slotNumber = slotNumber; }

    public SlotStatus getStatus() { return status; }
    public void setStatus(SlotStatus status) { this.status = status; }

    public String getCurrentVehicleId() { return currentVehicleId; }
    public void setCurrentVehicleId(String currentVehicleId) { this.currentVehicleId = currentVehicleId; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public String toCsvRow() {
        return String.join(",", id, slotNumber, status.name(), currentVehicleId, String.valueOf(hourlyRate));
    }

}
