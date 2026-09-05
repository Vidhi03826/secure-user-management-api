package com.vidhi.secureusermanagement.controller;

import com.vidhi.secureusermanagement.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    // ==========================================================
    // NO AUTHENTICATION
    // ==========================================================

    @Test
    void shouldReturn401WhenUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/me")
                )
                .andExpect(status().isUnauthorized());
    }

    // ==========================================================
    // USER → OWN PROFILE
    // ==========================================================

    @Test
    @WithMockUser(
            username = "user@example.com",
            roles = "USER"
    )
    void shouldAllowUserToAccessOwnProfile()
            throws Exception {

        mockMvc.perform(
                        get("/api/users/me")
                )
                .andExpect(status().isOk());
    }

    // ==========================================================
    // USER → ADMIN ENDPOINT
    // ==========================================================

    @Test
    @WithMockUser(
            username = "user@example.com",
            roles = "USER"
    )
    void shouldReturn403WhenUserAccessesAdminEndpoint()
            throws Exception {

        mockMvc.perform(
                        get("/api/users")
                )
                .andExpect(status().isForbidden());
    }

    // ==========================================================
    // ADMIN → ALL USERS
    // ==========================================================

    @Test
    @WithMockUser(
            username = "admin@example.com",
            roles = "ADMIN"
    )
    void shouldAllowAdminToAccessAllUsers()
            throws Exception {

        mockMvc.perform(
                        get("/api/users")
                )
                .andExpect(status().isOk());
    }
}