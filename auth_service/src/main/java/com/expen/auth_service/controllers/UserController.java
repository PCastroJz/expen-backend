package com.expen.auth_service.controllers;

import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import com.expen.auth_service.models.User;
import com.expen.auth_service.services.UserService;
import com.expen.auth_service.dtos.UserUpdateRequest;
import com.expen.auth_service.dtos.ApiResponse;
import com.expen.auth_service.dtos.ChangePasswordRequest;
import com.expen.auth_service.dtos.UserBasicInfo;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<User>> getProfile(@PathVariable String slug) {
        ApiResponse<User> response = userService.getUserBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        ApiResponse<User> response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/password-change")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @RequestBody ChangePasswordRequest request) {
        ApiResponse<Void> response = userService.changePassword(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/profile-picture")
    public ResponseEntity<ApiResponse<User>> updateProfilePicture(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        ApiResponse<User> response = userService.updateProfilePicture(id, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/find-by-email")
    public Long getUserIdByEmail(@RequestParam String email) {
        return userService.getUserIdByEmail(email);
    }

    @GetMapping("/public-profiles")
    public ResponseEntity<ApiResponse<List<UserBasicInfo>>> getPublicProfiles() {
        return ResponseEntity.ok(userService.getAllPublicProfiles());
    }
}