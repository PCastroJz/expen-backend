package com.expen.auth_service.dtos;

import lombok.Data;

@Data
public class UserBasicInfo {
    private String urlImage;
    private String name;
    private String lastName;
    private String email;
    private String slug;

    public UserBasicInfo(String urlImage, String name, String lastName, String email, String slug) {
        this.urlImage = urlImage;
        this.name = name;
        this.lastName = lastName;
        this.email = email;
        this.slug = slug;
    }
}