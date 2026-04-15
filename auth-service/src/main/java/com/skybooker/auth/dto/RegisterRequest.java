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

    // PASSENGER, AIRLINE_STAFF, ADMIN
    // agar nahi diya toh default PASSENGER rahega
    private String role;
}