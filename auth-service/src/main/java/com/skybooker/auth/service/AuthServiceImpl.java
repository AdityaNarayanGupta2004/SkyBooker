//package com.skybooker.auth.service;
//
//import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
//import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
//import com.google.api.client.http.javanet.NetHttpTransport;
//import com.google.api.client.json.gson.GsonFactory;
//import com.skybooker.auth.dto.*;
//import com.skybooker.auth.entity.User;
//import com.skybooker.auth.repository.UserRepository;
//import com.skybooker.auth.security.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.Collections;
//import java.util.Optional;
//
//@Service
//@RequiredArgsConstructor
//public class AuthServiceImpl implements AuthService {
//
//    private final UserRepository userRepository;
//    private final JwtUtil jwtUtil;
//    private final PasswordEncoder passwordEncoder;
//
//    @Value("${app.admin.secret-key}")
//    private String adminSecretKey;
//
//    @Value("${app.staff.secret-key}")
//    private String staffSecretKey;
//
//    @Value("${google.client-id}")
//    private String googleClientId;
//
//    // ================================================================
//    //  REGISTER  (Email + Password — original logic kept exactly)
//    // ================================================================
//    @Override
//    public AuthResponse register(RegisterRequest request) {
//
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new RuntimeException("Email already registered: " + request.getEmail());
//        }
//
//        String requestedRole = request.getRole() != null
//                ? request.getRole().toUpperCase().trim()
//                : "PASSENGER";
//
//        String role;
//
//        switch (requestedRole) {
//
//            case "ADMIN" -> {
//                if (request.getAdminSecretKey() == null || request.getAdminSecretKey().isBlank()) {
//                    throw new RuntimeException(
//                            "Admin registration requires the admin secret key. Contact the system owner.");
//                }
//                if (!request.getAdminSecretKey().equals(adminSecretKey)) {
//                    throw new RuntimeException("Invalid admin secret key. Access denied.");
//                }
//                if (userRepository.countByRole("ADMIN") >= 4) {
//                    throw new RuntimeException(
//                            "Maximum 4 admin accounts already exist. No more admins can be registered.");
//                }
//                role = "ADMIN";
//            }
//
//            case "AIRLINE_STAFF" -> {
//                if (request.getStaffSecretKey() == null || request.getStaffSecretKey().isBlank()) {
//                    throw new RuntimeException(
//                            "Staff registration requires the staff secret key. Contact your airline administrator.");
//                }
//                if (!request.getStaffSecretKey().equals(staffSecretKey)) {
//                    throw new RuntimeException(
//                            "Invalid staff secret key. Please contact your airline administrator for the correct key.");
//                }
//                role = "AIRLINE_STAFF";
//            }
//
//            case "PASSENGER" -> role = "PASSENGER";
//
//            default -> throw new RuntimeException(
//                    "Invalid role: " + requestedRole
//                            + ". Allowed: PASSENGER, AIRLINE_STAFF (requires staff key), ADMIN (requires admin key).");
//        }
//
//        User user = new User();
//        user.setFullName(request.getFullName());
//        user.setEmail(request.getEmail());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setPhone(request.getPhone() != null ? request.getPhone() : "NOT_PROVIDED");
//        user.setRole(role);
//        user.setActive(true);
//        user.setVerified(false);
//        user.setProvider("LOCAL");
//        user.setCreatedAt(LocalDateTime.now());
//        user.setUpdatedAt(LocalDateTime.now());
//
//        user.setGender(request.getGender() != null ? request.getGender() : "NOT_SPECIFIED");
//        user.setNationality(request.getNationality() != null ? request.getNationality() : "NOT_SPECIFIED");
//        user.setPassportNumber(request.getPassportNumber() != null ? request.getPassportNumber() : "NOT_PROVIDED");
//
//        userRepository.save(user);
//
//        return AuthResponse.successMessage("Registration successful! Role assigned: " + role);
//    }
//
//    // ================================================================
//    //  LOGIN  (Email + Password — original logic kept exactly)
//    // ================================================================
//    @Override
//    public AuthResponse login(LoginRequest request) {
//
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new RuntimeException(
//                        "No account found with email: " + request.getEmail()));
//
//        if (!user.isActive()) {
//            throw new RuntimeException("Account deactivated. Contact support.");
//        }
//
//        // Google users must use the Google button
//        if ("GOOGLE".equals(user.getProvider())) {
//            throw new RuntimeException(
//                    "This account uses Google Sign-In. Please use the 'Sign in with Google' button.");
//        }
//
//        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
//            throw new RuntimeException("Incorrect password. Please try again.");
//        }
//
//        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
//        return AuthResponse.token(token);
//    }
//
//    // ================================================================
//    //  GOOGLE OAUTH LOGIN / AUTO-REGISTER
//    // ================================================================
//    @Override
//    public AuthResponse googleLogin(GoogleAuthRequest request) {
//
//        // Step 1: Verify token with Google
//        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());
//
//        String email    = payload.getEmail();
//        String fullName = (String) payload.get("name");
//
//        // Step 2: Find existing user or create new one
//        Optional<User> existing = userRepository.findByEmail(email);
//        boolean isNewUser = existing.isEmpty();
//        User user;
//
//        if (isNewUser) {
//            // First Google login → auto-register as PASSENGER
//            user = new User();
//            user.setFullName(fullName != null ? fullName : email.split("@")[0]);
//            user.setEmail(email);
//            user.setPassword(null);                 // No password for Google users
//            user.setPhone("NOT_PROVIDED");
//            user.setRole("PASSENGER");
//            user.setProvider("GOOGLE");
//            user.setActive(true);
//            user.setVerified(true);                 // Google already verified the email
//
//            // FIX: Set all @Column(nullable=false) fields with safe defaults
//            // so the DB insert does not throw a constraint violation
//            user.setGender("NOT_SPECIFIED");
//            user.setNationality("NOT_SPECIFIED");
//            user.setPassportNumber("NOT_PROVIDED");
//            // dateOfBirth, passportExpiry, city, state, country, pincode
//            // are now nullable in User.java — no need to set them
//
//            user.setCreatedAt(LocalDateTime.now());
//            user.setUpdatedAt(LocalDateTime.now());
//            userRepository.save(user);
//
//        } else {
//            user = existing.get();
//
//            if (!user.isActive()) {
//                throw new RuntimeException("Account deactivated. Contact support.");
//            }
//
//            // If user previously registered with email/password,
//            // link their account to Google going forward
//            if (!"GOOGLE".equals(user.getProvider())) {
//                user.setProvider("GOOGLE");
//                user.setVerified(true);
//                user.setUpdatedAt(LocalDateTime.now());
//                userRepository.save(user);
//            }
//        }
//
//        // Step 3: Issue our own JWT
//        String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
//
//        return AuthResponse.googleLogin(
//                jwtToken, user.getEmail(), user.getFullName(), user.getRole(), isNewUser);
//    }
//
//    // ================================================================
//    //  PRIVATE: Verify Google ID Token
//    // ================================================================
//    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
//        try {
//            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
//                    new NetHttpTransport(), new GsonFactory())
//                    .setAudience(Collections.singletonList(googleClientId))
//                    .build();
//
//            GoogleIdToken idToken = verifier.verify(credential);
//
//            if (idToken == null) {
//                throw new RuntimeException(
//                        "Google token verification failed. Please try signing in again.");
//            }
//
//            return idToken.getPayload();
//
//        } catch (RuntimeException e) {
//            throw e;
//        } catch (Exception e) {
//            throw new RuntimeException(
//                    "Google token verification error: " + e.getMessage());
//        }
//    }
//}


package com.skybooker.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.secret-key}")
    private String adminSecretKey;

    @Value("${app.staff.secret-key}")
    private String staffSecretKey;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public AuthResponse register(RegisterRequest request) {
        log.info("Register attempt — email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed — email already exists: {}", request.getEmail());
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        String requestedRole = request.getRole() != null
                ? request.getRole().toUpperCase().trim()
                : "PASSENGER";

        String role;

        switch (requestedRole) {

            case "ADMIN" -> {
                if (request.getAdminSecretKey() == null || request.getAdminSecretKey().isBlank()) {
                    log.warn("Admin registration rejected — secret key missing. email: {}", request.getEmail());
                    throw new RuntimeException(
                            "Admin registration requires the admin secret key. Contact the system owner.");
                }
                if (!request.getAdminSecretKey().equals(adminSecretKey)) {
                    log.warn("Admin registration rejected — invalid secret key. email: {}", request.getEmail());
                    throw new RuntimeException("Invalid admin secret key. Access denied.");
                }
                if (userRepository.countByRole("ADMIN") >= 4) {
                    log.warn("Admin registration rejected — max 4 admins reached.");
                    throw new RuntimeException(
                            "Maximum 4 admin accounts already exist. No more admins can be registered.");
                }
                role = "ADMIN";
            }

            case "AIRLINE_STAFF" -> {
                if (request.getStaffSecretKey() == null || request.getStaffSecretKey().isBlank()) {
                    log.warn("Staff registration rejected — secret key missing. email: {}", request.getEmail());
                    throw new RuntimeException(
                            "Staff registration requires the staff secret key. Contact your airline administrator.");
                }
                if (!request.getStaffSecretKey().equals(staffSecretKey)) {
                    log.warn("Staff registration rejected — invalid secret key. email: {}", request.getEmail());
                    throw new RuntimeException(
                            "Invalid staff secret key. Please contact your airline administrator for the correct key.");
                }
                role = "AIRLINE_STAFF";
            }

            case "PASSENGER" -> role = "PASSENGER";

            default -> throw new RuntimeException(
                    "Invalid role: " + requestedRole
                            + ". Allowed: PASSENGER, AIRLINE_STAFF (requires staff key), ADMIN (requires admin key).");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone() : "NOT_PROVIDED");
        user.setRole(role);
        user.setActive(true);
        user.setVerified(false);
        user.setProvider("LOCAL");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setGender(request.getGender() != null ? request.getGender() : "NOT_SPECIFIED");
        user.setNationality(request.getNationality() != null ? request.getNationality() : "NOT_SPECIFIED");
        user.setPassportNumber(request.getPassportNumber() != null ? request.getPassportNumber() : "NOT_PROVIDED");

        userRepository.save(user);
        log.info("User registered successfully — email: {}, role: {}", request.getEmail(), role);
        return AuthResponse.successMessage("Registration successful! Role assigned: " + role);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — no account found: {}", request.getEmail());
                    return new RuntimeException("No account found with email: " + request.getEmail());
                });

        if (!user.isActive()) {
            log.warn("Login failed — account deactivated: {}", request.getEmail());
            throw new RuntimeException("Account deactivated. Contact support.");
        }

        if ("GOOGLE".equals(user.getProvider())) {
            log.warn("Login failed — Google account tried password login: {}", request.getEmail());
            throw new RuntimeException(
                    "This account uses Google Sign-In. Please use the 'Sign in with Google' button.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — incorrect password: {}", request.getEmail());
            throw new RuntimeException("Incorrect password. Please try again.");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("Login successful — email: {}, role: {}", user.getEmail(), user.getRole());
        return AuthResponse.token(token);
    }

    @Override
    public AuthResponse googleLogin(GoogleAuthRequest request) {
        log.info("Google login attempt");

        GoogleIdToken.Payload payload = verifyGoogleToken(request.getCredential());
        String email    = payload.getEmail();
        String fullName = (String) payload.get("name");

        Optional<User> existing = userRepository.findByEmail(email);
        boolean isNewUser = existing.isEmpty();
        User user;

        if (isNewUser) {
            log.info("New user via Google — auto-registering as PASSENGER: {}", email);
            user = new User();
            user.setFullName(fullName != null ? fullName : email.split("@")[0]);
            user.setEmail(email);
            user.setPassword(null);
            user.setPhone("NOT_PROVIDED");
            user.setRole("PASSENGER");
            user.setProvider("GOOGLE");
            user.setActive(true);
            user.setVerified(true);
            user.setGender("NOT_SPECIFIED");
            user.setNationality("NOT_SPECIFIED");
            user.setPassportNumber("NOT_PROVIDED");
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Google user registered — email: {}", email);
        } else {
            user = existing.get();
            if (!user.isActive()) {
                log.warn("Google login failed — account deactivated: {}", email);
                throw new RuntimeException("Account deactivated. Contact support.");
            }
            if (!"GOOGLE".equals(user.getProvider())) {
                log.info("Linking existing account to Google — email: {}", email);
                user.setProvider("GOOGLE");
                user.setVerified(true);
                user.setUpdatedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }

        String jwtToken = jwtUtil.generateToken(user.getEmail(), user.getRole());
        log.info("Google login successful — email: {}, role: {}, newUser: {}", email, user.getRole(), isNewUser);
        return AuthResponse.googleLogin(jwtToken, user.getEmail(), user.getFullName(), user.getRole(), isNewUser);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String credential) {
        log.debug("Verifying Google ID token");
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(credential);
            if (idToken == null) {
                log.error("Google token verification failed — token returned null");
                throw new RuntimeException("Google token verification failed. Please try signing in again.");
            }
            return idToken.getPayload();

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification error: {}", e.getMessage());
            throw new RuntimeException("Google token verification error: " + e.getMessage());
        }
    }
}
