package com.expen.auth_service.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class StorageServiceImpl implements StorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public String uploadFile(Long userId, MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String fileName = userId + "." + getFileExtension(file.getOriginalFilename());

            Path filePath = uploadPath.resolve(fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error al guardar el archivo", e);
        }
    }

    private String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new RuntimeException("El archivo no tiene extensión");
        }
        return fileName.substring(lastDotIndex + 1);
    }
}