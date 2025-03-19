package com.expen.auth_service.controllers;

import java.util.Map;

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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
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

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestParam String email) {
        return authService.forgotPassword(email);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody Map<String, Object> json) {
        String token = (String) json.get("token");
        String newPassword = (String) json.get("newPassword");
        return authService.resetPassword(token, newPassword);
    }

    @PostMapping("/validate-reset-token")
    public ResponseEntity<ApiResponse<String>> validResetPassword(@RequestParam String token) {
        return authService.valdiateResetToken(token);
    }
    

}