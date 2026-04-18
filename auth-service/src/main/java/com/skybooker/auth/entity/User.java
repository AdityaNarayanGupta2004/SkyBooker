package com.skybooker.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
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
    @Column(nullable = false)
    private String fullName;

    @Email
    @Column(unique = true, nullable = false)
    private String email;

    private String password;
    @Column(nullable = false)
    private String phone;

    // Personal
    @Column(nullable = false)
    private String gender;
    @Column(nullable = false)
    private LocalDate dateOfBirth;
    @Column(nullable = false)
    private String nationality;

    // Travel
    @Column(nullable = false)
    private String passportNumber;
    @Column(nullable = false)
    private LocalDate passportExpiry;

    // Address
    private String addressLine1;
    private String addressLine2;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String state;
    @Column(nullable = false)
    private String country;
    @Column(nullable = false)
    private String pincode;

    // Role (String based)
    @Column(nullable = false)
    private String role;

    // Status
    @Column(nullable = false)
    private boolean isActive;
    @Column(nullable = false)
    private boolean isVerified;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}