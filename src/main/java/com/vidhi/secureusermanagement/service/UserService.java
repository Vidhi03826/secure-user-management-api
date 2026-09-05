package com.vidhi.secureusermanagement.service;

import com.vidhi.secureusermanagement.dto.ChangePasswordRequest;
import com.vidhi.secureusermanagement.dto.UpdateProfileRequest;
import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.entity.Role;
import com.vidhi.secureusermanagement.entity.User;
import com.vidhi.secureusermanagement.exception.InvalidCredentialsException;
import com.vidhi.secureusermanagement.exception.ResourceAlreadyExistsException;
import com.vidhi.secureusermanagement.exception.ResourceNotFoundException;
import com.vidhi.secureusermanagement.repository.RoleRepository;
import com.vidhi.secureusermanagement.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // =========================
    // GET CURRENT USER
    // =========================

    public UserResponse getCurrentUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        return mapToUserResponse(user);
    }

    // =========================
    // GET ALL USERS
    // =========================

    public List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    // =========================
    // GET USER BY ID
    // =========================

    public UserResponse getUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        return mapToUserResponse(user);
    }

    // =========================
    // DELETE USER
    // =========================

    public void deleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "User not found"
            );
        }

        userRepository.deleteById(id);
    }

    // =========================
    // UPDATE ROLE
    // =========================

    public UserResponse updateRole(
            Long id,
            String roleName
    ) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        Role role = roleRepository.findByName(
                        roleName.toUpperCase()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role not found"
                        ));

        user.getRoles().clear();
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    // =========================
    // UPDATE CURRENT USER
    // =========================

    public UserResponse updateCurrentUser(
            String currentEmail,
            UpdateProfileRequest request
    ) {

        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        if (!currentEmail.equalsIgnoreCase(request.getEmail())
                && userRepository.findByEmail(request.getEmail()).isPresent()) {

            throw new ResourceAlreadyExistsException(
                    "Email already registered"
            );
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    // =========================
    // CHANGE PASSWORD
    // =========================

    public void changePassword(
            String email,
            ChangePasswordRequest request
    ) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPassword()
        )) {

            throw new InvalidCredentialsException(
                    "Current password is incorrect"
            );
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);
    }

    // =========================
    // ENTITY → RESPONSE DTO
    // =========================

    private UserResponse mapToUserResponse(User user) {

        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                roles
        );
    }

    public void unlockUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found"
                        ));

        user.setFailedLoginAttempts(0);
        user.setAccountLocked(false);

        userRepository.save(user);
    }
}
