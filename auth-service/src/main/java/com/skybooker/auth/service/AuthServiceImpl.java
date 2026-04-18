package com.skybooker.auth.service;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // application.properties se inject
    @Value("${app.admin.secret-key}")
    private String adminSecretKey;

    @Value("${app.staff.secret-key}")
    private String staffSecretKey;

    @Override
    public AuthResponse register(RegisterRequest request) {

        // ── 1. Email already registered check ─────────────────────
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // ── 2. Role determine karo — SECURE LOGIC ─────────────────
        String requestedRole = request.getRole() != null
                ? request.getRole().toUpperCase().trim()
                : "PASSENGER";

        String role;

        switch (requestedRole) {

//            case "ADMIN" -> {
//                // ADMIN: secret key validate karo
//                if (request.getAdminSecretKey() == null || request.getAdminSecretKey().isBlank()) {
//                    throw new RuntimeException(
//                            "Admin registration requires the admin secret key. " +
//                                    "Contact the system owner."
//                    );
//                }
//                if (!request.getAdminSecretKey().equals(adminSecretKey)) {
//                    throw new RuntimeException(
//                            "Invalid admin secret key. Access denied."
//                    );
//                }
//                // Sirf ek ADMIN allowed hai
//                if (userRepository.existsByRole("ADMIN")) {
//                    throw new RuntimeException(
//                            "An admin account already exists. Only one admin is allowed in the system."
//                    );
//                }
//                role = "ADMIN";
//            }
            case "ADMIN" -> {
                // ADMIN: secret key validate karo
                if (request.getAdminSecretKey() == null || request.getAdminSecretKey().isBlank()) {
                    throw new RuntimeException(
                            "Admin registration requires the admin secret key. " +
                                    "Contact the system owner."
                    );
                }
                if (!request.getAdminSecretKey().equals(adminSecretKey)) {
                    throw new RuntimeException(
                            "Invalid admin secret key. Access denied."
                    );
                }
                // Maximum 5 ADMIN allowed hain
                if (userRepository.countByRole("ADMIN") >= 4) {
                    throw new RuntimeException(
                            "Maximum 4 admin accounts already exist. No more admins can be registered."
                    );
                }
                role = "ADMIN";
            }

            case "AIRLINE_STAFF" -> {
                // STAFF: secret key validate karo
                if (request.getStaffSecretKey() == null || request.getStaffSecretKey().isBlank()) {
                    throw new RuntimeException(
                            "Staff registration requires the staff secret key. " +
                                    "Contact your airline administrator."
                    );
                }
                if (!request.getStaffSecretKey().equals(staffSecretKey)) {
                    throw new RuntimeException(
                            "Invalid staff secret key. " +
                                    "Please contact your airline administrator for the correct key."
                    );
                }
                role = "AIRLINE_STAFF";
            }

            case "PASSENGER" -> {
                // PASSENGER: koi restriction nahi — public
                role = "PASSENGER";
            }

            default -> throw new RuntimeException(
                    "Invalid role: " + requestedRole +
                            ". Allowed: PASSENGER, AIRLINE_STAFF (requires staff key), ADMIN (requires admin key)."
            );
        }

        // ── 3. User banao aur save karo ───────────────────────────
        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone() : "NOT_PROVIDED");
        user.setRole(role);
        user.setActive(true);
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // Optional fields — null safe defaults
        user.setGender(request.getGender() != null ? request.getGender() : "NOT_SPECIFIED");
        user.setNationality(request.getNationality() != null ? request.getNationality() : "NOT_SPECIFIED");
        user.setPassportNumber(request.getPassportNumber() != null ? request.getPassportNumber() : "NOT_PROVIDED");

        userRepository.save(user);

        return AuthResponse.successMessage("Registration successful! Role assigned: " + role);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "No account found with email: " + request.getEmail()
                ));

        if (!user.isActive()) {
            throw new RuntimeException("Account deactivated. Contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Incorrect password. Please try again.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        return AuthResponse.token(token);
    }
}
