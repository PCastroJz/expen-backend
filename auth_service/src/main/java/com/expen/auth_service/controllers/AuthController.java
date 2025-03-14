package com.expen.auth_service.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.expen.auth_service.dtos.ApiResponse;
import com.expen.auth_service.models.LoginRequest;
import com.expen.auth_service.models.RegisterRequest;
import com.expen.auth_service.services.AuthService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<String>> verifyCode(@RequestParam String email, @RequestParam String code) {
        return authService.verifyCode(email, code);
    }

    @PostMapping("/resend-verification-code")
    public ResponseEntity<ApiResponse<String>> resendVerificationCode(@RequestParam String email) {
        return authService.resendVerificationCode(email);
    }

}