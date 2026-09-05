package com.vidhi.secureusermanagement.controller;

import com.vidhi.secureusermanagement.jwt.JwtService;
import com.vidhi.secureusermanagement.security.CustomAccessDeniedHandler;
import com.vidhi.secureusermanagement.security.CustomAuthenticationEntryPoint;
import com.vidhi.secureusermanagement.security.CustomUserDetailsService;
import com.vidhi.secureusermanagement.security.JwtAuthenticationFilter;
import com.vidhi.secureusermanagement.security.SecurityConfig;
import com.vidhi.secureusermanagement.dto.UpdateProfileRequest;
import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private CustomAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private CustomAccessDeniedHandler accessDeniedHandler;

    @Test
    void shouldGetCurrentUser() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "Vidhi",
                "vidhi@gmail.com",
                Set.of("USER")
        );

        when(userService.getCurrentUser("vidhi@gmail.com"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/users/me")
                                .with(user("vidhi@gmail.com")
                                        .roles("USER"))
                )
                .andExpect(status().isOk());
    }

    @Test
    void shouldUpdateCurrentUser() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "Updated Vidhi",
                "updated@gmail.com",
                Set.of("USER")
        );

        when(userService.updateCurrentUser(
                eq("vidhi@gmail.com"),
                any(UpdateProfileRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/users/me")
                                .with(user("vidhi@gmail.com")
                                        .roles("USER"))
                                .with(csrf())
                                .contentType("application/json")
                                .content("""
                                        {
                                          "name": "Updated Vidhi",
                                          "email": "updated@gmail.com"
                                        }
                                        """)
                )
                .andExpect(status().isOk());
    }
}