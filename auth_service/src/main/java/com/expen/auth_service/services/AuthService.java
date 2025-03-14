package com.expen.auth_service.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.expen.auth_service.dtos.ApiResponse;
import com.expen.auth_service.dtos.EmailRequest;
import com.expen.auth_service.jwt.JwtService;
import com.expen.auth_service.models.LoginRequest;
import com.expen.auth_service.models.RegisterRequest;
import com.expen.auth_service.models.User;
import com.expen.auth_service.repositories.UserRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.ClassPathResource;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CacheManager cacheManager;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${auth.service.api-key}")
    private String apiKey;

    private final SecureRandom random = new SecureRandom();

    public ResponseEntity<ApiResponse<String>> login(LoginRequest request) {
        try {
            logger.info("Iniciando autenticación para el usuario: {}", request.getEmail());

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()));

            logger.info("Autenticación exitosa para el usuario: {}", request.getEmail());

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        logger.error("Usuario no encontrado: {}", request.getEmail());
                        return new UsernameNotFoundException("Usuario no encontrado");
                    });

            if (!user.isValidate()) {
                logger.warn("El usuario no ha verificado su correo electrónico: {}", request.getEmail());
                ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                        .code(403)
                        .message("Por favor, verifica tu correo electrónico para acceder al sistema")
                        .data(null)
                        .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            String token = jwtService.getToken(user, user.getId());
            logger.info("Token generado para el usuario: {}", request.getEmail());

            ApiResponse<String> response = ApiResponse.<String>builder()
                    .code(200)
                    .message("Autenticación exitosa")
                    .data(token)
                    .build();

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            ApiResponse<String> errorResponse;
            if (e instanceof UsernameNotFoundException) {
                logger.error("Usuario no encontrado: {}", request.getEmail(), e);
                errorResponse = ApiResponse.<String>builder()
                        .code(404)
                        .message("Usuario no encontrado")
                        .data(null)
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                logger.error("Error de autenticación para el usuario: {}", request.getEmail(), e);
                errorResponse = ApiResponse.<String>builder()
                        .code(401)
                        .message("Credenciales inválidas")
                        .data(null)
                        .build();
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }
        } catch (Exception e) {
            logger.error("Error inesperado durante el login para el usuario: {}", request.getEmail(), e);
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .code(500)
                    .message("Error durante el login")
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    public ResponseEntity<ApiResponse<String>> register(RegisterRequest request) {
        try {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponse<>(409, "El usuario ya existe", null));
            }

            User user = User.builder()
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .name(request.getName())
                    .lastName(request.getLastName())
                    .slug(makeSlugUnique(generateSlug(request.getName(), request.getLastName())))
                    .build();

            userRepository.save(user);

            String verificationCode = generateVerificationCode();
            saveVerificationCode(request.getEmail(), verificationCode);

            sendVerificationEmail(request.getEmail(), verificationCode);

            return ResponseEntity.ok(new ApiResponse<>(200,
                    "Usuario registrado, revisa tu correo para el código de verificación", null));

        } catch (Exception e) {
            logger.error("Error en el registro", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "Error en el registro", null));
        }
    }

    private String generateVerificationCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

    private void saveVerificationCode(String email, String code) {
        Cache cache = cacheManager.getCache("verificationCodes");
        if (cache != null) {
            cache.put(email, code);
        }
    }

    private Optional<String> getVerificationCode(String email) {
        Cache cache = cacheManager.getCache("verificationCodes");
        return cache != null ? Optional.ofNullable(cache.get(email, String.class)) : Optional.empty();
    }

    public String generateSlug(String name, String lastName) {
        String fullName = name + " " + lastName;
        String slug = fullName.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
        return slug;
    }

    public String makeSlugUnique(String slug) {
        String uniqueSlug = slug;
        int counter = 1;

        while (userRepository.findBySlug(uniqueSlug).isPresent()) {
            uniqueSlug = slug + "-" + counter;
            counter++;
        }

        return uniqueSlug;
    }

    private void sendVerificationEmail(String email, String code) {
        try {
            String emailServiceUrl = "http://localhost:8080/email/send";
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", apiKey);
            headers.set("Content-Type", "application/json");

            String htmlTemplate = readFileFromClasspath("templates/verification_email.html");

            String htmlContent = htmlTemplate
                    .replace("{{digits}}", "<div class=\"code\">" + code + "</div>");

            EmailRequest emailRequest = new EmailRequest();
            emailRequest.setTo(List.of(email));
            emailRequest.setSubject("Código de Verificación");
            emailRequest.setText(htmlContent);
            emailRequest.setHtml(true);

            restTemplate.exchange(emailServiceUrl, HttpMethod.POST, new HttpEntity<>(emailRequest, headers),
                    String.class);
            logger.info("Código enviado a {}", email);

        } catch (IOException e) {
            logger.error("Error leyendo el archivo de imagen o plantilla", e);
        } catch (RestClientException e) {
            logger.error("Error enviando el correo electrónico", e);
        } catch (Exception e) {
            logger.error("Error inesperado", e);
        }
    }

    public ResponseEntity<ApiResponse<String>> verifyCode(String email, String code) {
        Optional<String> storedCode = getVerificationCode(email);
        if (storedCode.isPresent() && storedCode.get().equals(code)) {

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            user.setStatus(true);
            user.setValidate(true);

            userRepository.save(user);

            return ResponseEntity.ok(new ApiResponse<>(200, "Código verificado correctamente", null));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Código inválido o expirado", null));
        }
    }

    public ResponseEntity<ApiResponse<String>> resendVerificationCode(String email) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        logger.error("Usuario no encontrado: {}", email);
                        return new UsernameNotFoundException("Usuario no encontrado");
                    });

            if (user.isValidate()) {
                logger.warn("El usuario ya está verificado: {}", email);
                ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                        .code(400)
                        .message("El usuario ya está verificado")
                        .data(null)
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            String newVerificationCode = generateVerificationCode();

            saveVerificationCode(email, newVerificationCode);

            sendVerificationEmail(email, newVerificationCode);

            ApiResponse<String> response = ApiResponse.<String>builder()
                    .code(200)
                    .message("Nuevo código de verificación enviado a " + email)
                    .data(null)
                    .build();

            return ResponseEntity.ok(response);

        } catch (UsernameNotFoundException e) {
            logger.error("Usuario no encontrado: {}", email, e);
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .code(404)
                    .message("Usuario no encontrado")
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            logger.error("Error al reenviar el código de verificación a: {}", email, e);
            ApiResponse<String> errorResponse = ApiResponse.<String>builder()
                    .code(500)
                    .message("Error al reenviar el código de verificación")
                    .data(null)
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private String readFileFromClasspath(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(bytes, StandardCharsets.UTF_8);
    }

}