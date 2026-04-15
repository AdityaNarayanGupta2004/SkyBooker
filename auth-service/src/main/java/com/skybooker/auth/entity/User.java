package com.skybooker.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Basic Info
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    private String phone;

    // Personal
    private String gender;
    private LocalDate dateOfBirth;
    private String nationality;

    // Travel
    private String passportNumber;
    private LocalDate passportExpiry;

    // Address
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;

    // Role (String based)
    private String role;

    // Status
    private boolean isActive;
    private boolean isVerified;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}