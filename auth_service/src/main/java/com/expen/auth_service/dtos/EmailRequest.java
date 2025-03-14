package com.expen.auth_service.dtos;

import java.util.List;

import lombok.Data;

@Data
public class EmailRequest {
    private List<String> to;
    private String subject;
    private String text;
    private boolean html;
}