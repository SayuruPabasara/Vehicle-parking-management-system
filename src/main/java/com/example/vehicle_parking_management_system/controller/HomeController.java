//tells the compiler and JVM that this class lives under com/example/vehicle_parking_management_system/controller
package com.example.vehicle_parking_management_system.controller;

//tell the compiler which external classes HomeController.java intends to use
import com.example.vehicle_parking_management_system.service.AdminService;
import com.example.vehicle_parking_management_system.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

//a handler for web requests. marks the class as a Spring MVC controller component
@Controller
public class HomeController {

    private final AdminService adminService;
    private final ReservationService reservationService;

    public HomeController(AdminService adminService, ReservationService reservationService) {
        this.adminService = adminService;
        this.reservationService = reservationService;
    }

    //handle HTTP GET requests for specific URL paths ("/" and "/home")
    @GetMapping({"/", "/home"})
    public String home() {
        return "index.html";
    }

    @GetMapping("/register")
    public String register() {
        return "register.html";
    }

    @GetMapping("/login")
    public String login() {
        return "login.html";
    }
    @GetMapping("/driver/dashboard")
    public String driverDashboard(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        Object uid = session.getAttribute("userId");
        double amountDue = 0.0;
        if (uid != null) {
            Map<String, Object> bill = reservationService.getDriverBillingSummary(uid.toString());
            amountDue = ((Number) bill.get("amountDue")).doubleValue();
        }
        model.addAttribute("amountDueDisplay", String.format("LKR %,.0f", amountDue));
        return "driver-dashboard.html";
    }
    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        model.addAllAttributes(adminService.getSystemSummary());
        model.addAttribute("todayFormatted",
                DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy").format(LocalDate.now()));
        return "admin-dashboard.html";
    }
    @GetMapping("/driver/vehicles")
    public String driverVehicles() {
        return "vehicle-register.html";
    }
    @GetMapping("/reservation")
    public String reservation() {
        return "slot-booking.html";
    }
    @GetMapping("/slot-map")
    public String slotMap(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "slot-map.html";
    }
    @GetMapping("/driver/profile")
    public String profile() {
        return "driver-profile.html";
    }
    @GetMapping("/feedback")
    public String feedback() {
        return "feedback-submission.html";
    }
    @GetMapping("/driver/billing")
    public String billing() {
        return "billing.html";
    }
    @GetMapping("/admin/slots")
    public String adminSlotMap() {
        return "slot-management.html";
    }
    @GetMapping("/admin/drivers")
    public String adminDrivers() {
        return "driver-management.html";
    }
    @GetMapping("/admin/admins")
    public String adminAdmins() {
        return "admin-management.html";
    }
    @GetMapping("/admin/reservations")
    public String adminReservations() {
        return "reservation-management.html";
    }
    @GetMapping("/admin/vehicles")
    public String adminVehicles() {
        return "vehicle-management.html";
    }
    @GetMapping("/admin/feedback")
    public String adminFeedbacks() {
        return "feedback-management.html";
    }
    

}
