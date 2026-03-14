//tells the compiler and JVM that this class lives under com/example/vehicle_parking_management_system/controller
package com.example.vehicle_parking_management_system.controller;

//tell the compiler which external classes HomeController.java intends to use
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

//a handler for web requests. marks the class as a Spring MVC controller component
@Controller
public class HomeController {

    //handle HTTP GET requests for specific URL paths ("/" and "/home")
    @GetMapping({"/", "/home"})
    public String home() {
        return "parknow-core-ui";
    }
}
