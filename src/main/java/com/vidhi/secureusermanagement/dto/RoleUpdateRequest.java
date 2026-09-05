package com.vidhi.secureusermanagement.dto;

import jakarta.validation.constraints.NotBlank;

public class RoleUpdateRequest {

    @NotBlank
    private String role;

    public RoleUpdateRequest() {
    }

    public RoleUpdateRequest(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    
}