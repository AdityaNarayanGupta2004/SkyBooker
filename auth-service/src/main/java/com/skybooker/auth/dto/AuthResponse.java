package com.skybooker.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL) // null fields hide karega
public class AuthResponse {

    private String message;
    private String token;

    // register ke liye
    public static AuthResponse successMessage(String msg){
        AuthResponse res = new AuthResponse();
        res.setMessage(msg);
        return res;
    }

    // login ke liye
    public static AuthResponse token(String token){
        AuthResponse res = new AuthResponse();
        res.setToken(token);
        return res;
    }

    
}