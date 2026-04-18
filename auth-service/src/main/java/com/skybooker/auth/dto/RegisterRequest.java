package com.skybooker.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {

    private String fullName;
    private String email;
    private String password;
    private String phone;

    private String gender;
    private String nationality;
    private String passportNumber;

    private String role;

    // AIRLINE_STAFF registration key zaroori hai
    private String staffSecretKey;

    // ADMIN registration ke liye zaroori
    private String adminSecretKey;
}