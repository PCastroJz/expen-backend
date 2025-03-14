package com.expen.email_service.models;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class EmailRequest {
    private List<String> to;
    private String subject;
    private String text;
    private boolean html;
    private List<MultipartFile> attachments;
}