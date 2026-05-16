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

    private static final String ROLE_PASSENGER = "PASSENGER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_AIRLINE_STAFF = "AIRLINE_STAFF";
    private static final String PROVIDER_GOOGLE = "GOOGLE";
    private static final String PROVIDER_LOCAL = "LOCAL";
    private static final String VAL_NOT_PROVIDED = "NOT_PROVIDED";
    private static final String VAL_NOT_SPECIFIED = "NOT_SPECIFIED";

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
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        String role = validateAndGetRole(request);

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() != null ? request.getPhone() : VAL_NOT_PROVIDED);
        user.setRole(role);
        user.setActive(true);
        user.setVerified(false);
        user.setProvider(PROVIDER_LOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setGender(request.getGender() != null ? request.getGender() : VAL_NOT_SPECIFIED);
        user.setNationality(request.getNationality() != null ? request.getNationality() : VAL_NOT_SPECIFIED);
        user.setPassportNumber(request.getPassportNumber() != null ? request.getPassportNumber() : VAL_NOT_PROVIDED);

        userRepository.save(user);
        log.info("User registered successfully — email: {}, role: {}", request.getEmail(), role);
        return AuthResponse.successMessage("Registration successful! Role assigned: " + role);
    }

    private String validateAndGetRole(RegisterRequest request) {
        String requestedRole = request.getRole() != null ? request.getRole().toUpperCase().trim() : ROLE_PASSENGER;
        switch (requestedRole) {
            case ROLE_ADMIN -> {
                validateAdminRole(request);
                return ROLE_ADMIN;
            }
            case ROLE_AIRLINE_STAFF -> {
                validateStaffRole(request);
                return ROLE_AIRLINE_STAFF;
            }
            case ROLE_PASSENGER -> {
                return ROLE_PASSENGER;
            }
            default -> throw new IllegalArgumentException(
                    "Invalid role: " + requestedRole
                            + ". Allowed: PASSENGER, AIRLINE_STAFF (requires staff key), ADMIN (requires admin key).");
        }
    }

    private void validateAdminRole(RegisterRequest request) {
        if (request.getAdminSecretKey() == null || request.getAdminSecretKey().isBlank()) {
            log.warn("Admin registration rejected — secret key missing. email: {}", request.getEmail());
            throw new IllegalArgumentException("Admin registration requires the admin secret key. Contact the system owner.");
        }
        if (!request.getAdminSecretKey().equals(adminSecretKey)) {
            log.warn("Admin registration rejected — invalid secret key. email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid admin secret key. Access denied.");
        }
        if (userRepository.countByRole(ROLE_ADMIN) >= 4) {
            log.warn("Admin registration rejected — max 4 admins reached.");
            throw new IllegalStateException("Maximum 4 admin accounts already exist. No more admins can be registered.");
        }
    }

    private void validateStaffRole(RegisterRequest request) {
        if (request.getStaffSecretKey() == null || request.getStaffSecretKey().isBlank()) {
            log.warn("Staff registration rejected — secret key missing. email: {}", request.getEmail());
            throw new IllegalArgumentException("Staff registration requires the staff secret key. Contact your airline administrator.");
        }
        if (!request.getStaffSecretKey().equals(staffSecretKey)) {
            log.warn("Staff registration rejected — invalid secret key. email: {}", request.getEmail());
            throw new IllegalArgumentException("Invalid staff secret key. Please contact your airline administrator for the correct key.");
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt — email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed — no account found: {}", request.getEmail());
                    return new IllegalArgumentException("No account found with email: " + request.getEmail());
                });

        if (!user.isActive()) {
            log.warn("Login failed — account deactivated: {}", request.getEmail());
            throw new IllegalStateException("Account deactivated. Contact support.");
        }

        if (PROVIDER_GOOGLE.equals(user.getProvider())) {
            log.warn("Login failed — Google account tried password login: {}", request.getEmail());
            throw new IllegalArgumentException(
                    "This account uses Google Sign-In. Please use the 'Sign in with Google' button.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed — incorrect password: {}", request.getEmail());
            throw new IllegalArgumentException("Incorrect password. Please try again.");
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
            user.setPhone(VAL_NOT_PROVIDED);
            user.setRole(ROLE_PASSENGER);
            user.setProvider(PROVIDER_GOOGLE);
            user.setActive(true);
            user.setVerified(true);
            user.setGender(VAL_NOT_SPECIFIED);
            user.setNationality(VAL_NOT_SPECIFIED);
            user.setPassportNumber(VAL_NOT_PROVIDED);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
            log.info("Google user registered — email: {}", email);
        } else {
            user = existing.get();
            if (!user.isActive()) {
                log.warn("Google login failed — account deactivated: {}", email);
                throw new IllegalStateException("Account deactivated. Contact support.");
            }
            if (!PROVIDER_GOOGLE.equals(user.getProvider())) {
                log.info("Linking existing account to Google — email: {}", email);
                user.setProvider(PROVIDER_GOOGLE);
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
                throw new IllegalArgumentException("Google token verification failed. Please try signing in again.");
            }
            return idToken.getPayload();

        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification error: {}", e.getMessage());
            throw new IllegalArgumentException("Google token verification error: " + e.getMessage());
        }
    }
}
