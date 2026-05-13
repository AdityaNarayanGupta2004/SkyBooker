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

    // nullable = true — Google users have no password
    private String password;

    @Column(nullable = false)
    private String phone;

    // Personal
    @Column(nullable = false)
    private String gender;

    // FIX: nullable = true — Google users don't provide this at signup
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String nationality;

    // Travel
    @Column(nullable = false)
    private String passportNumber;

    // FIX: nullable = true — Google users don't provide this at signup
    private LocalDate passportExpiry;

    // Address — FIX: all nullable = true (optional for Google users)
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String country;
    private String pincode;

    // Role
    @Column(nullable = false)
    private String role;

    // OAuth Provider — "LOCAL" or "GOOGLE"
    // FIX: added this new column; @PrePersist sets default "LOCAL"
    @Column(nullable = false)
    private String provider;

    // Status
    // KEEPING original field names isActive / isVerified (matching your existing DB + code)
    @Column(nullable = false)
    private boolean isActive;

    @Column(nullable = false)
    private boolean isVerified;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Sets provider = "LOCAL" before first DB insert if not already set
    @PrePersist
    public void prePersist() {
        if (this.provider == null) {
            this.provider = "LOCAL";
        }
    }
}
