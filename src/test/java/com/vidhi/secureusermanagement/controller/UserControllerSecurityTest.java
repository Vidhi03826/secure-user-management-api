package com.vidhi.secureusermanagement.controller;

import com.vidhi.secureusermanagement.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldReturn401WhenUserIsNotAuthenticated() throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldAllowUserToAccessOwnProfile() throws Exception {

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenUserAccessesAdminEndpoint() throws Exception {

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldAllowAdminToAccessAllUsers() throws Exception {

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }
}