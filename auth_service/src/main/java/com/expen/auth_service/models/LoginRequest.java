package com.expen.auth_service.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "El correo electrónico no puede estar vacío")
    @Email(message = "Por favor ingrese un correo electrónico válido")
    String email;
    
    @NotBlank(message = "La contraseña no puede estar vacía")
    String password;
}
