package com.skybooker.auth;

import com.skybooker.auth.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("test-secret-key-must-be-long-enough-1234567890");
    }

    // Test 1: Token generate hona chahiye, null nahi aana chahiye
    @Test
    void generateToken_ShouldNotReturnNull() {
        String token = jwtUtil.generateToken("test@gmail.com", "PASSENGER");
        assertNotNull(token);
    }

    // Test 2: Jo token generate hua wo valid hona chahiye
    @Test
    void validateToken_WithValidToken_ShouldReturnTrue() {
        String token = jwtUtil.generateToken("test@gmail.com", "PASSENGER");
        assertTrue(jwtUtil.validateToken(token));
    }

    // Test 3: Koi bhi random string valid token nahi hoga
    @Test
    void validateToken_WithGarbageString_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("ye.bilkul.galat.token.hai"));
    }

    // Test 4: Token se sahi email nikhle
    @Test
    void extractEmail_ShouldMatchOriginalEmail() {
        String email = "rahul@skybooker.com";
        String token = jwtUtil.generateToken(email, "PASSENGER");
        assertEquals(email, jwtUtil.extractEmail(token));
    }

    // Test 5: Token se sahi role nikhle
    @Test
    void extractRole_ShouldMatchOriginalRole() {
        String token = jwtUtil.generateToken("admin@skybooker.com", "ADMIN");
        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    // Test 6: AIRLINE_STAFF role bhi sahi kaam kare
    @Test
    void extractRole_ForAirlineStaff_ShouldReturnCorrectRole() {
        String token = jwtUtil.generateToken("staff@indigo.com", "AIRLINE_STAFF");
        assertEquals("AIRLINE_STAFF", jwtUtil.extractRole(token));
    }

    // Test 7: Null token pass karo toh crash nahi hona chahiye, false aana chahiye
    @Test
    void validateToken_WithNull_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken(null));
    }

    // Test 8: Empty string bhi valid token nahi hai
    @Test
    void validateToken_WithEmptyString_ShouldReturnFalse() {
        assertFalse(jwtUtil.validateToken(""));
    }

    // Test 9: Alag alag users ke tokens alag hone chahiye
    @Test
    void generateToken_ForDifferentUsers_ShouldReturnDifferentTokens() {
        String token1 = jwtUtil.generateToken("user1@gmail.com", "PASSENGER");
        String token2 = jwtUtil.generateToken("user2@gmail.com", "PASSENGER");
        assertNotEquals(token1, token2);
    }
}
