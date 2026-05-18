package com.example.vehicle_parking_management_system.model;

public class Admin extends User {
    
    public enum AdminLevel { SUPER,FINANCE,PARKING,READONLY }

    private AdminLevel adminLevel;
    private String createdBy;
    
    public Admin() {
        super();
        setRole("ADMIN");
    }
    public Admin(String id, String fullName, String userName, String email, String phone, String password, String role, AdminLevel adminLevel, String createdBy) {
        super(id, fullName, userName, email, phone, password, "ADMIN");
        this.adminLevel = adminLevel;
        this.createdBy = createdBy;
    }
    public AdminLevel getAdminLevel() { return adminLevel; }
    public void setAdminLevel(AdminLevel adminLevel) { this.adminLevel = adminLevel; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }  

    @Override
    public String toCsvRow() {
        return String.join(",", getId(), getFullName(), getUserName(), getEmail(), getPhone(), getPassword(), getRole(), adminLevel.name(), createdBy);
    }

}
