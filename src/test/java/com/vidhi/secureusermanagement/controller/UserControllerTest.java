package com.vidhi.secureusermanagement.controller;

import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.test.context.support.WithMockUser;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    private UserResponse userResponse;

    @BeforeEach
    void setUp() {

        userResponse = new UserResponse(
                1L,
                "Test User",
                "test@example.com",
                Set.of("USER")
        );
    }

    @Test
    @WithMockUser(
            username = "test@example.com",
            roles = "USER"
    )
    void shouldGetCurrentUser() throws Exception {

        when(userService.getCurrentUser(
                "test@example.com"
        )).thenReturn(userResponse);

        mockMvc.perform(
                        get("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("USER"));
    }

    @Test
    @WithMockUser(
            username = "test@example.com",
            roles = "USER"
    )
    void shouldUpdateCurrentUser() throws Exception {

        when(userService.updateCurrentUser(
                any(String.class),
                any()
        )).thenReturn(userResponse);

        String requestBody = """
                {
                    "name": "Test User",
                    "email": "test@example.com"
                }
                """;

        mockMvc.perform(
                        put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }
}