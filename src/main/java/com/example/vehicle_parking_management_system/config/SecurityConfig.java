package com.example.vehicle_parking_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

//marks class as a configuration class that can define beans and configuration settings for the Spring application context
@Configuration
public class SecurityConfig {

    //method-level annotation used to define and register a bean
    @Bean

    //defines a SecurityFilterChain bean that configures the security settings for the application
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Allow the home page and static resources without authentication
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/home", "/h2-console/**", "/css/**", "/js/**").permitAll()
                .anyRequest().authenticated()
            )
            // Use form login for everything else (default login page)
            .formLogin(Customizer.withDefaults())

            // Disable CSRF for H2 console (not for production)
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            
            // Allow frames for H2 console
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        //return value as a bean instance of SecurityFilterChain that Spring Security will use to apply the defined security configuration
        return http.build();
    }
}
