package com.skybooker.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    // ── Full Name ──────────────────────────────────────────────────────────────
    @NotBlank(message = "Full name is required")
    @Pattern(
            regexp = "^[A-Za-z ]{2,60}$",
            message = "Full name must be 2-60 characters and contain only letters and spaces"
    )
    private String fullName;

    // ── Email ──────────────────────────────────────────────────────────────────
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address (e.g. user@example.com)")
    private String email;

    // ── Password ───────────────────────────────────────────────────────────────
    // Not required for Google OAuth registrations
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,20}$",
            message = "Password must be 8-20 chars with at least 1 uppercase, 1 lowercase, 1 digit, 1 special char (@$!%*?&)"
    )
    private String password;

    // ── Phone ──────────────────────────────────────────────────────────────────
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9"
    )
    private String phone;

    // ── Personal ───────────────────────────────────────────────────────────────
    @Pattern(
            regexp = "^(MALE|FEMALE|OTHER|NOT_SPECIFIED)$",
            message = "Gender must be MALE, FEMALE, OTHER, or NOT_SPECIFIED"
    )
    private String gender;

    @Pattern(
            regexp = "^[A-Za-z ]{2,50}$",
            message = "Nationality must be 2-50 letters"
    )
    private String nationality;

    // Passport: optional on register, validated if provided
    @Pattern(
            regexp = "^([A-Z]{1}[0-9]{7}|[A-Z]{2}[0-9]{7}|NOT_PROVIDED)$",
            message = "Passport number must be like A1234567 or AB1234567"
    )
    private String passportNumber;

    // ── Role ───────────────────────────────────────────────────────────────────
    private String role;  // PASSENGER / AIRLINE_STAFF / ADMIN

    // Staff secret key — required when role = AIRLINE_STAFF
    private String staffSecretKey;

    // Admin secret key — required when role = ADMIN
    private String adminSecretKey;
}
