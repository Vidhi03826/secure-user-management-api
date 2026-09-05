package com.vidhi.secureusermanagement.service;

import com.vidhi.secureusermanagement.dto.LoginRequest;
import com.vidhi.secureusermanagement.dto.RegisterRequest;
import com.vidhi.secureusermanagement.dto.TokenResponse;
import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.entity.RefreshToken;
import com.vidhi.secureusermanagement.entity.Role;
import com.vidhi.secureusermanagement.entity.User;
import com.vidhi.secureusermanagement.exception.InvalidCredentialsException;
import com.vidhi.secureusermanagement.exception.ResourceAlreadyExistsException;
import com.vidhi.secureusermanagement.jwt.JwtService;
import com.vidhi.secureusermanagement.repository.RoleRepository;
import com.vidhi.secureusermanagement.repository.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private Authentication authentication;

    private AuthService authService;

    private Role userRole;

    private User user;

    @BeforeEach
    void setUp() {

        authService = new AuthService(
                userRepository,
                roleRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                refreshTokenService
        );

        userRole = new Role("USER");

        user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );

        user.getRoles().add(userRole);
    }

    // ==========================================================
    // REGISTER
    // ==========================================================

    @Test
    void shouldRegisterNewUser() {

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "password123"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(roleRepository.findByName("USER"))
                .thenReturn(Optional.of(userRole));

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response =
                authService.register(request);

        assertNotNull(response);

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@example.com",
                response.getEmail()
        );

        assertTrue(
                response.getRoles().contains("USER")
        );

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));
    }

    @Test
    void shouldRejectDuplicateEmail() {

        RegisterRequest request = new RegisterRequest(
                "Test User",
                "test@example.com",
                "password123"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(userRepository, never())
                .save(any(User.class));

        verify(passwordEncoder, never())
                .encode(anyString());
    }

    // ==========================================================
    // LOGIN
    // ==========================================================

    @Test
    void shouldLoginSuccessfully() {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken("refresh-token-123");
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(
                Instant.now().plusSeconds(3600)
        );
        refreshToken.setRevoked(false);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any()))
                .thenReturn(authentication);

        when(authentication.getName())
                .thenReturn("test@example.com");

        doReturn(
                Set.of(
                        new SimpleGrantedAuthority("ROLE_USER")
                )
        ).when(authentication).getAuthorities();

        when(jwtService.generateToken(
                eq("test@example.com"),
                any()
        )).thenReturn("access-token-123");

        when(refreshTokenService.createRefreshToken(
                "test@example.com"
        )).thenReturn(refreshToken);

        TokenResponse response =
                authService.login(request);

        assertNotNull(response);

        assertEquals(
                "access-token-123",
                response.getAccessToken()
        );

        assertEquals(
                "refresh-token-123",
                response.getRefreshToken()
        );

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        verify(authenticationManager)
                .authenticate(any());

        verify(jwtService)
                .generateToken(
                        eq("test@example.com"),
                        any()
                );

        verify(refreshTokenService)
                .createRefreshToken("test@example.com");
    }

    @Test
    void shouldRejectInvalidCredentials() {

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any()))
                .thenThrow(
                        new BadCredentialsException(
                                "Bad credentials"
                        )
                );

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        assertEquals(
                1,
                user.getFailedLoginAttempts()
        );

        assertFalse(
                user.isAccountLocked()
        );

        verify(userRepository)
                .save(user);
    }

    // ==========================================================
    // ACCOUNT LOCKOUT
    // ==========================================================

    @Test
    void shouldLockAccountAfterFiveFailedAttempts() {

        user.setFailedLoginAttempts(4);

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "wrong-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(authenticationManager.authenticate(any()))
                .thenThrow(
                        new BadCredentialsException(
                                "Bad credentials"
                        )
                );

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                5,
                user.getFailedLoginAttempts()
        );

        assertTrue(
                user.isAccountLocked()
        );

        assertEquals(
                "Account locked due to too many failed attempts",
                exception.getMessage()
        );

        verify(userRepository)
                .save(user);
    }

    // ==========================================================
    // ALREADY LOCKED ACCOUNT
    // ==========================================================

    @Test
    void shouldRejectAlreadyLockedAccount() {

        user.setAccountLocked(true);

        LoginRequest request = new LoginRequest(
                "test@example.com",
                "password123"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        InvalidCredentialsException exception =
                assertThrows(
                        InvalidCredentialsException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Account is locked",
                exception.getMessage()
        );

        verify(authenticationManager, never())
                .authenticate(any());
    }

    // ==========================================================
    // REFRESH TOKEN
    // ==========================================================

    @Test
    void shouldRefreshAccessToken() {

        RefreshToken oldRefreshToken =
                new RefreshToken();

        oldRefreshToken.setToken(
                "old-refresh-token"
        );

        oldRefreshToken.setUser(user);

        oldRefreshToken.setExpiryDate(
                Instant.now().plusSeconds(3600)
        );

        oldRefreshToken.setRevoked(false);

        RefreshToken newRefreshToken =
                new RefreshToken();

        newRefreshToken.setToken(
                "new-refresh-token"
        );

        newRefreshToken.setUser(user);

        newRefreshToken.setExpiryDate(
                Instant.now().plusSeconds(7200)
        );

        newRefreshToken.setRevoked(false);

        when(refreshTokenService.verifyRefreshToken(
                "old-refresh-token"
        )).thenReturn(oldRefreshToken);

        when(jwtService.generateToken(
                eq("test@example.com"),
                any()
        )).thenReturn("new-access-token");

        when(refreshTokenService.rotateRefreshToken(
                oldRefreshToken
        )).thenReturn(newRefreshToken);

        TokenResponse response =
                authService.refreshAccessToken(
                        "old-refresh-token"
                );

        assertNotNull(response);

        assertEquals(
                "new-access-token",
                response.getAccessToken()
        );

        assertEquals(
                "new-refresh-token",
                response.getRefreshToken()
        );

        verify(refreshTokenService)
                .verifyRefreshToken("old-refresh-token");

        verify(jwtService)
                .generateToken(
                        eq("test@example.com"),
                        any()
                );

        verify(refreshTokenService)
                .rotateRefreshToken(oldRefreshToken);
    }

    // ==========================================================
    // LOGOUT
    // ==========================================================

    @Test
    void shouldLogoutUser() {

        authService.logout("refresh-token-123");

        verify(refreshTokenService)
                .revoke("refresh-token-123");
    }
}