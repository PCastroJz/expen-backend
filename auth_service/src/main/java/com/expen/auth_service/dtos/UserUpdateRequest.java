package com.expen.auth_service.dtos;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String name;
    private String lastName;
    private String email;
}