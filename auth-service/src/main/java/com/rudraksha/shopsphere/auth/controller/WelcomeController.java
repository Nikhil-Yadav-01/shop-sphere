package com.rudraksha.shopsphere.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {
    
    @GetMapping("/")
    public ResponseEntity<String> welcome(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean throwErrorForChaosMonkeyTest) {
        if (Boolean.TRUE.equals(throwErrorForChaosMonkeyTest)) {
            throw new RuntimeException("Simulated Chaos Monkey Exception");
        }
        return ResponseEntity.ok("Welcome to Auth Service");
    }
}
