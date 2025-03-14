package com.expen.email_service.controllers;

import com.expen.email_service.models.EmailRequest;
import com.expen.email_service.services.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @Value("${email.service.api-key}")
    private String apiKey;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(
            @RequestHeader(value = "x-api-key", required = false) String clientApiKey,
            @RequestBody EmailRequest emailRequest) {
        if (clientApiKey == null || !clientApiKey.equals(apiKey)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Acceso denegado");
        }

        if (emailRequest.getTo() == null || emailRequest.getTo().isEmpty()) {
            return ResponseEntity.badRequest().body("El campo 'to' es obligatorio");
        }
        if (emailRequest.getSubject() == null || emailRequest.getSubject().isEmpty()) {
            return ResponseEntity.badRequest().body("El campo 'subject' es obligatorio");
        }
        if (emailRequest.getText() == null || emailRequest.getText().isEmpty()) {
            return ResponseEntity.badRequest().body("El campo 'text' es obligatorio");
        }

        try {
            List<File> attachments = null;
            if (emailRequest.getAttachments() != null && !emailRequest.getAttachments().isEmpty()) {
                attachments = emailRequest.getAttachments().stream()
                        .map(multipartFile -> {
                            try {
                                return convertMultipartFileToFile(multipartFile);
                            } catch (IOException e) {
                                throw new RuntimeException(
                                        "Error al convertir el archivo: " + multipartFile.getOriginalFilename(), e);
                            }
                        })
                        .collect(Collectors.toList());
            }

            emailService.sendEmail(
                    emailRequest.getTo().toArray(new String[0]),
                    emailRequest.getSubject(),
                    emailRequest.getText(),
                    emailRequest.isHtml(),
                    attachments != null ? attachments.toArray(new File[0]) : null);

            return ResponseEntity.ok("Correo enviado con éxito");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al procesar los archivos adjuntos: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al enviar el correo: " + e.getMessage());
        }
    }

    private File convertMultipartFileToFile(MultipartFile multipartFile) throws IOException {
        Path tempFile = Files.createTempFile("email-attachment-", multipartFile.getOriginalFilename());
        Files.copy(multipartFile.getInputStream(), tempFile, StandardCopyOption.REPLACE_EXISTING);
        return tempFile.toFile();
    }
}