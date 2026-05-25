
package com.example.vehicle_parking_management_system.controller;


import com.example.vehicle_parking_management_system.service.AdminService;
import com.example.vehicle_parking_management_system.service.ReservationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;


@Controller
public class HomeController {

    private final AdminService adminService;
    private final ReservationService reservationService;

    public HomeController(AdminService adminService, ReservationService reservationService) {
        this.adminService = adminService;
        this.reservationService = reservationService;
    }


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
        if (uid != null) {
            model.addAllAttributes(reservationService.getDriverDashboardStats(uid.toString()));
        } else {
            model.addAttribute("totalBookings", 0);
            model.addAttribute("activeSessions", 0);
            model.addAttribute("vehicleCount", 0);
            model.addAttribute("amountDueDisplay", "LKR 0");
        }
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
    public String driverVehicles(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "vehicle-register.html";
    }
    @GetMapping("/reservation")
    public String reservation(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "slot-booking.html";
    }
    @GetMapping("/slot-map")
    public String slotMap(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "slot-map.html";
    }
    @GetMapping("/driver/profile")
    public String profile(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "driver-profile.html";
    }
    @GetMapping("/feedback")
    public String feedback(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "feedback-submission.html";
    }
    @GetMapping("/driver/billing")
    public String billing(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "billing.html";
    }
    @GetMapping("/admin/slots")
    public String adminSlotMap(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "slot-management.html";
    }
    @GetMapping("/admin/drivers")
    public String adminDrivers(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "driver-management.html";
    }
    @GetMapping("/admin/admins")
    public String adminAdmins(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "admin-management.html";
    }
    @GetMapping("/admin/reservations")
    public String adminReservations(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "reservation-management.html";
    }
    @GetMapping("/admin/vehicles")
    public String adminVehicles(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "vehicle-management.html";
    }
    @GetMapping("/admin/feedback")
    public String adminFeedbacks(HttpSession session, Model model) {
        Object name = session.getAttribute("userName");
        model.addAttribute("username", name != null ? name.toString() : "Guest");
        return "feedback-management.html";
    }
    

}
