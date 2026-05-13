package com.skybooker.auth.service;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.GoogleAuthRequest;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    // NEW: Google OAuth — verify token, find/create user, return JWT
    AuthResponse googleLogin(GoogleAuthRequest request);
}
