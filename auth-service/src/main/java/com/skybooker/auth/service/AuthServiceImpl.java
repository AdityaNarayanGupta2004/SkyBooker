package com.skybooker.auth.service;

import com.skybooker.auth.dto.*;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        // role validate karo - sirf valid roles allow karo
        String role = request.getRole() != null ? request.getRole().toUpperCase() : "PASSENGER";
        if (!role.equals("PASSENGER") && !role.equals("AIRLINE_STAFF") && !role.equals("ADMIN")) {
            throw new RuntimeException("Invalid role. Allowed: PASSENGER, AIRLINE_STAFF, ADMIN");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setActive(true);
        user.setVerified(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        return AuthResponse.successMessage("User registered successfully with role: " + role);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.getEmail()));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // token mein role bhi daalo
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.token(token);
    }
}
