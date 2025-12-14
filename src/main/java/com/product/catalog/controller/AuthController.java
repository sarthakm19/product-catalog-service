package com.product.catalog.controller;

import com.product.catalog.dto.ErrorResponse;
import com.product.catalog.dto.LoginRequest;
import com.product.catalog.dto.LoginResponse;
import com.product.catalog.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse response = authService.authenticate(loginRequest);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            ErrorResponse error = new ErrorResponse();
            error.setTimestamp(LocalDateTime.now());
            error.setStatus(HttpStatus.UNAUTHORIZED.value());
            error.setError("Unauthorized");
            error.setMessage("Invalid username or password");
            error.setPath("/api/v1/auth/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }
}
