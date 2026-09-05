package com.vidhi.secureusermanagement;

import com.vidhi.secureusermanagement.entity.Role;
import com.vidhi.secureusermanagement.entity.User;
import com.vidhi.secureusermanagement.repository.RoleRepository;
import com.vidhi.secureusermanagement.repository.UserRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureTestRestTemplate
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // ==========================================================
    // APPLICATION CONTEXT
    // ==========================================================

    @Test
    void contextLoads() {
        // If this test reaches here, Spring successfully
        // created the complete application context.
    }

    // ==========================================================
    // NO JWT
    // ==========================================================

    @Test
    void shouldReturn401WithoutJwt() {

        ResponseEntity<String> response =
                restTemplate.getForEntity(
                        "/api/users/me",
                        String.class
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                response.getStatusCode()
        );
    }

    // ==========================================================
    // REAL JWT → USER PROFILE
    // ==========================================================

    @Test
    void shouldAccessProfileWithRealJwt() {

        String email =
                "user-" + UUID.randomUUID() + "@example.com";

        String password = "password123";

        registerUser(email, password);

        String accessToken =
                loginAndGetAccessToken(
                        email,
                        password
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/users/me",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        assertTrue(
                response.getBody().contains(email)
        );
    }

    // ==========================================================
    // REAL USER JWT → ADMIN ENDPOINT
    // ==========================================================

    @Test
    void shouldReturn403ForUserAccessingAdminEndpoint() {

        String email =
                "user-" + UUID.randomUUID() + "@example.com";

        String password = "password123";

        registerUser(email, password);

        String accessToken =
                loginAndGetAccessToken(
                        email,
                        password
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/users",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.FORBIDDEN,
                response.getStatusCode()
        );
    }

    // ==========================================================
    // REAL ADMIN JWT → ADMIN ENDPOINT
    // ==========================================================

    @Test
    void shouldAllowAdminWithRealJwt() {

        String email =
                "admin-" + UUID.randomUUID() + "@example.com";

        String password = "password123";

        registerUser(email, password);

        // Find the newly created user
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        // Find ADMIN role
        Role adminRole =
                roleRepository.findByName("ADMIN")
                        .orElseThrow();

        // Give the user ADMIN role
        user.getRoles().add(adminRole);

        userRepository.save(user);

        // Login again so the authentication contains
        // the ADMIN authority
        String accessToken =
                loginAndGetAccessToken(
                        email,
                        password
                );

        HttpHeaders headers = new HttpHeaders();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<String> response =
                restTemplate.exchange(
                        "/api/users",
                        org.springframework.http.HttpMethod.GET,
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );
    }

    // ==========================================================
    // HELPER: REGISTER
    // ==========================================================

    private void registerUser(
            String email,
            String password
    ) {

        Map<String, String> body = Map.of(
                "name", "Integration Test User",
                "email", email,
                "password", password
        );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        "/api/auth/register",
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );
    }

    // ==========================================================
    // HELPER: LOGIN
    // ==========================================================

    private String loginAndGetAccessToken(
            String email,
            String password
    ) {

        Map<String, String> body = Map.of(
                "email", email,
                "password", password
        );

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<Map<String, String>> request =
                new HttpEntity<>(
                        body,
                        headers
                );

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        "/api/auth/login",
                        request,
                        Map.class
                );

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode()
        );

        assertNotNull(response.getBody());

        Object token =
                response.getBody().get("accessToken");

        assertNotNull(token);

        return token.toString();
    }
}