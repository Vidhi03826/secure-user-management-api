package com.vidhi.secureusermanagement.security;

import com.vidhi.secureusermanagement.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "VGhpc0lzQURldmVsb3BtZW50U2VjcmV0S2V5VGhhdElzTG9uZ0Vub3VnaA",
            3600000
    );

    @Test
    void shouldGenerateAndValidateToken() {

        UserDetails userDetails = User
                .withUsername("test@example.com")
                .password("password")
                .roles("USER")
                .build();

        String token = jwtService.generateToken(
                userDetails.getUsername(),
                Set.of("ROLE_USER")
        );

        assertNotNull(token);

        String username = jwtService.extractUsername(token);

        assertEquals(
                "test@example.com",
                username
        );

        assertTrue(
                jwtService.isTokenValid(
                        token,
                        userDetails
                )
        );
    }
}