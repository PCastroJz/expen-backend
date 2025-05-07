package com.expen.auth_service.services;

import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.expen.auth_service.models.User;
import com.expen.auth_service.repositories.UserRepository;
import com.expen.auth_service.dtos.UserUpdateRequest;
import com.expen.auth_service.controllers.UserUpdateController;
import com.expen.auth_service.dtos.ApiResponse;
import com.expen.auth_service.dtos.ChangePasswordRequest;
import com.expen.auth_service.dtos.UserBasicInfo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final UserUpdateController userUpdateController;

    @Value("${image.service.upload-url}")
    private String imageUploadUrl;

    @Value("${image.service.public-url-base}")
    private String imagePublicUrlBase;

    public ApiResponse<User> getUserBySlug(String email) {
        Long authenticatedUserId = getAuthenticatedUserId();

        User user = userRepository.findBySlug(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!authenticatedUserId.equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para ver este usuario");
        }

        return new ApiResponse<>(200, "Usuario encontrado", null, user);
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
        return new ApiResponse<>(200, "Usuario actualizado correctamente", null, updatedUser);
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

        return new ApiResponse<>(200, "Contraseña actualizada correctamente", null, null);
    }

    public ApiResponse<User> updateProfilePicture(Long id, MultipartFile file) {
        Long authenticatedUserId = getAuthenticatedUserId();

        if (!authenticatedUserId.equals(id)) {
            throw new RuntimeException("No tienes permiso para modificar este usuario");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        RestTemplate restTemplate = new RestTemplate();
        String imageServiceUrl = imageUploadUrl;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file.getResource());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    imageServiceUrl,
                    requestEntity,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                String filename = response.getBody();
                String imageUrl = imagePublicUrlBase + filename;

                user.setUrlImage(imageUrl);
                userRepository.save(user);

                userUpdateController.broadcastUserUpdate(user);

                return new ApiResponse<>(200, "Foto de perfil actualizada correctamente", null, user);
            } else {
                throw new RuntimeException("Error al subir la imagen al servicio de imágenes");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al comunicarse con el servicio de imágenes: " + e.getMessage());
        }
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

    public ApiResponse<List<UserBasicInfo>> getAllPublicProfiles() {
        List<User> users = userRepository.findAll();

        List<UserBasicInfo> basicProfiles = users.stream()
                .map(user -> new UserBasicInfo(
                        user.getUrlImage(),
                        user.getName(),
                        user.getLastName(),
                        user.getEmail(),
                        user.getSlug()))
                .collect(Collectors.toList());

        return new ApiResponse<>(200, "Perfiles públicos obtenidos", null, basicProfiles);
    }

}