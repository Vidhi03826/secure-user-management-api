package com.vidhi.secureusermanagement.controller;

import com.vidhi.secureusermanagement.dto.ChangePasswordRequest;
import com.vidhi.secureusermanagement.dto.RoleUpdateRequest;
import com.vidhi.secureusermanagement.dto.UpdateProfileRequest;
import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/api/users/me")
    public UserResponse getCurrentUser(
            Authentication authentication
    ) {
        return userService.getCurrentUser(
                authentication.getName()
        );
    }

    @GetMapping("/api/users")
    public List<UserResponse> getAllUsers() {

        return userService.getAllUsers();
    }

    @GetMapping("/api/users/{id}")
    public UserResponse getUserById(@PathVariable Long id) {

        return userService.getUserById(id);
    }

    @DeleteMapping("/api/users/{id}")
    public String deleteUser(@PathVariable Long id) {

        userService.deleteUser(id);

        return "User deleted successfully";
    }
    @PutMapping("/api/users/{id}/role")
    public UserResponse updateRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request
    ) {

        return userService.updateRole(
                id,
                request.getRole()
        );
    }

    @PutMapping("/api/users/me")
    public UserResponse updateCurrentUser(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {

        return userService.updateCurrentUser(
                authentication.getName(),
                request
        );
    }

    @PutMapping("/api/users/me/password")
    public String changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {

        userService.changePassword(
                authentication.getName(),
                request
        );

        return "Password changed successfully";
    }

    @PutMapping("/api/users/{id}/unlock")
    public String unlockUser(
            @PathVariable Long id
    ) {

        userService.unlockUser(id);

        return "User unlocked successfully";
    }
}