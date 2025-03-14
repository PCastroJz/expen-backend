package com.expen.auth_service.services;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String uploadFile(Long id, MultipartFile file);
}