package com.skybooker.auth;

import com.skybooker.auth.dto.AuthResponse;
import com.skybooker.auth.dto.LoginRequest;
import com.skybooker.auth.dto.RegisterRequest;
import com.skybooker.auth.entity.User;
import com.skybooker.auth.repository.UserRepository;
import com.skybooker.auth.security.JwtUtil;
import com.skybooker.auth.service.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // @Mock matlab yeh fake objects hain — real DB nahi chalega
    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;

    // @InjectMocks matlab upar ke teeno mocks is class ke andar inject ho jayenge
    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    // Ek ready-made user banana ka helper — baar baar likhna na pade
    private User banaoUser(String email, String role, String provider, boolean active) {
        User u = new User();
        u.setId(1L);
        u.setFullName("Test User");
        u.setEmail(email);
        u.setPassword("$2a$encodedpassword");
        u.setPhone("9876543210");
        u.setRole(role);
        u.setProvider(provider);
        u.setActive(active);
        u.setVerified(false);
        u.setGender("MALE");
        u.setNationality("Indian");
        u.setPassportNumber("A1234567");
        u.setCreatedAt(LocalDateTime.now());
        u.setUpdatedAt(LocalDateTime.now());
        return u;
    }

    // Ek ready-made RegisterRequest banana ka helper
    private RegisterRequest banaoRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Rahul Sharma");
        req.setEmail("rahul@gmail.com");
        req.setPassword("Test@1234");
        req.setPhone("9876543210");
        req.setGender("MALE");
        req.setNationality("Indian");
        req.setPassportNumber("A1234567");
        req.setRole("PASSENGER");
        return req;
    }

    @BeforeEach
    void setUp() {
        // @Value wale fields Spring inject nahi kar sakta test mein
        // isliye ReflectionTestUtils use karo — seedha field set karo
        ReflectionTestUtils.setField(authServiceImpl, "adminSecretKey", "SkyAdmin#9999");
        ReflectionTestUtils.setField(authServiceImpl, "staffSecretKey", "SkyStaff#2025");
        ReflectionTestUtils.setField(authServiceImpl, "googleClientId", "dummy-client-id");
    }

    // ---------------------------------------------------------------
    // REGISTER TESTS
    // ---------------------------------------------------------------

    // Test 1: Normal passenger register ho jaye
    @Test
    void register_NormalPassenger_ShouldSucceed() {
        RegisterRequest req = banaoRegisterRequest();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse res = authServiceImpl.register(req);

        assertNotNull(res);
        assertTrue(res.getMessage().contains("Registration successful"));
        // Verify karo ki save ek baar call hua
        verify(userRepository, times(1)).save(any(User.class));
    }

    // Test 2: Same email dobara register na ho sake
    @Test
    void register_DuplicateEmail_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("Email already registered"));
        // Save bilkul call nahi hona chahiye
        verify(userRepository, never()).save(any());
    }

    // Test 3: Admin bina secret key ke register na ho sake
    @Test
    void register_AdminWithoutSecretKey_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("ADMIN");
        req.setAdminSecretKey(null);

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("admin secret key"));
    }

    // Test 4: Galat admin key se registration fail ho
    @Test
    void register_AdminWithWrongKey_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("ADMIN");
        req.setAdminSecretKey("galat-key-123");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("Invalid admin secret key"));
    }

    // Test 5: 4 admin already hain toh 5th na bane
    @Test
    void register_AdminLimitReached_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("ADMIN");
        req.setAdminSecretKey("SkyAdmin#9999");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(userRepository.countByRole("ADMIN")).thenReturn(4L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("Maximum 4 admin"));
    }

    // Test 6: Airline staff sahi key se register ho jaye
    @Test
    void register_AirlineStaffWithCorrectKey_ShouldSucceed() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("AIRLINE_STAFF");
        req.setStaffSecretKey("SkyStaff#2025");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        AuthResponse res = authServiceImpl.register(req);

        assertTrue(res.getMessage().contains("AIRLINE_STAFF"));
    }

    // ---------------------------------------------------------------
    // LOGIN TESTS
    // ---------------------------------------------------------------

    // Test 7: Sahi email aur password se login ho jaye
    @Test
    void login_WithCorrectCredentials_ShouldReturnToken() {
        LoginRequest req = new LoginRequest();
        req.setEmail("rahul@gmail.com");
        req.setPassword("Test@1234");

        User user = banaoUser("rahul@gmail.com", "PASSENGER", "LOCAL", true);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(req.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(user.getEmail(), user.getRole())).thenReturn("fake.jwt.token");

        AuthResponse res = authServiceImpl.login(req);

        assertNotNull(res.getToken());
        assertEquals("fake.jwt.token", res.getToken());
    }

    // Test 8: Galat password se login na ho sake
    @Test
    void login_WithWrongPassword_ShouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("rahul@gmail.com");
        req.setPassword("GalatPassword@99");

        User user = banaoUser("rahul@gmail.com", "PASSENGER", "LOCAL", true);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.login(req));

        assertTrue(ex.getMessage().contains("Incorrect password"));
    }

    // Test 9: Account band ho toh login fail ho
    @Test
    void login_DeactivatedAccount_ShouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("rahul@gmail.com");
        req.setPassword("Test@1234");

        // isActive = false
        User user = banaoUser("rahul@gmail.com", "PASSENGER", "LOCAL", false);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.login(req));

        assertTrue(ex.getMessage().contains("Account deactivated"));
    }

    // Test 10: Google user agar password se login karne ki koshish kare toh error aaye
    @Test
    void login_GoogleUserTriesPasswordLogin_ShouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("googleuser@gmail.com");
        req.setPassword("Test@1234");

        User user = banaoUser("googleuser@gmail.com", "PASSENGER", "GOOGLE", true);

        when(userRepository.findByEmail(req.getEmail())).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authServiceImpl.login(req));

        assertTrue(ex.getMessage().contains("Google Sign-In"));
    }

    // ---------------------------------------------------------------
    // EXTRA EDGE CASE TESTS
    // ---------------------------------------------------------------

    @Test
    void register_WithInvalidRole_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("SUPERMAN");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("Invalid role"));
    }

    @Test
    void register_StaffWithoutSecretKey_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("AIRLINE_STAFF");
        req.setStaffSecretKey("");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("Staff registration requires the staff secret key"));
    }

    @Test
    void register_StaffWithWrongKey_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("AIRLINE_STAFF");
        req.setStaffSecretKey("wrong-staff-key");

        when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("Invalid staff secret key"));
    }

    @Test
    void login_WithNonExistentAccount_ShouldThrowException() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nobody@gmail.com");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authServiceImpl.login(req));

        assertTrue(ex.getMessage().contains("No account found"));
    }

    @Test
    void register_AdminWithEmptyKey_ShouldThrowException() {
        RegisterRequest req = banaoRegisterRequest();
        req.setRole("ADMIN");
        req.setAdminSecretKey("   ");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authServiceImpl.register(req));

        assertTrue(ex.getMessage().contains("admin secret key"));
    }
}
