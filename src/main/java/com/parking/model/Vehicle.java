package com.parking.model;

public class Vehicle {
    
    private String id;
    private String plateNumber;
    private String type;
    private String color;
    private String ownerId;

    public Vehicle() {}

    public Vehicle(String id, String plateNumber, String type, String color, String ownerId) {
        this.id = id;
        this.plateNumber = plateNumber;
        this.type = type;
        this.color = color;
        this.ownerId = ownerId;
    }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPlateNumber() { return plateNumber; }
    public void setPlateNumber(String plateNumber) { this.plateNumber = plateNumber; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String toCsvRow() {
        return String.join(",", id, plateNumber, type, color, ownerId);
    }


}
