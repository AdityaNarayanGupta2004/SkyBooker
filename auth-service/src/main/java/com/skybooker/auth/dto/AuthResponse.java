package com.skybooker.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String message;
    private String token;
    private String role;
    private String email;
    private String fullName;
    private Boolean isNewUser;

    public static AuthResponse successMessage(String msg) {
        AuthResponse res = new AuthResponse();
        res.setMessage(msg);
        return res;
    }

    public static AuthResponse token(String token) {
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        return res;
    }

    public static AuthResponse googleLogin(String token, String email,
                                           String fullName, String role, boolean isNewUser) {
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        res.setEmail(email);
        res.setFullName(fullName);
        res.setRole(role);
        res.setIsNewUser(isNewUser);
        res.setMessage(isNewUser
                ? "Welcome to SkyBooker! Account created via Google."
                : "Welcome back! Logged in via Google.");
        return res;
    }
}
