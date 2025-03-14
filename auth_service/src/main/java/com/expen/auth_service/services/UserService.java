package com.expen.auth_service.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.expen.auth_service.models.User;
import com.expen.auth_service.repositories.UserRepository;
import com.expen.auth_service.dtos.UserUpdateRequest;
import com.expen.auth_service.dtos.ApiResponse;
import com.expen.auth_service.dtos.ChangePasswordRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StorageService storageService;
    private final AuthService authService;

    public ApiResponse<User> getUserBySlug(String slug) {
        Long authenticatedUserId = getAuthenticatedUserId();

        User user = userRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!authenticatedUserId.equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para ver este usuario");
        }

        return new ApiResponse<>(200, "Usuario encontrado", user);
    }

    public ApiResponse<User> updateUser(Long id, UserUpdateRequest request) {
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!authenticatedUserId.equals(id)) {
            throw new RuntimeException("No tienes permiso para modificar este usuario");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String slug = authService.generateSlug(request.getName(), request.getLastName());
        slug = authService.makeSlugUnique(slug);

        user.setName(request.getName());
        user.setLastName(request.getLastName());
        user.setSlug(slug);

        User updatedUser = userRepository.save(user);
        return new ApiResponse<>(200, "Usuario actualizado correctamente", updatedUser);
    }

    public ApiResponse<Void> changePassword(Long id, ChangePasswordRequest request) {
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!authenticatedUserId.equals(id)) {
            throw new RuntimeException("No tienes permiso para modificar este usuario");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña actual incorrecta");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return new ApiResponse<>(200, "Contraseña actualizada correctamente", null);
    }

    public ApiResponse<String> updateProfilePicture(Long id, MultipartFile file) {
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!authenticatedUserId.equals(id)) {
            throw new RuntimeException("No tienes permiso para modificar este usuario");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String imageUrl = storageService.uploadFile(id, file);
        user.setUrlImage(imageUrl);
        userRepository.save(user);

        return new ApiResponse<>(200, "Foto de perfil actualizada correctamente", imageUrl);
    }

    public Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();

            User user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            return user.getId();
        } else {
            throw new RuntimeException("Usuario no autenticado");
        }
    }

    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"))
                .getId();
    }
}