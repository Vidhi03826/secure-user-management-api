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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    private User user;

    private Role userRole;

    private Role adminRole;

    @BeforeEach
    void setUp() {

        userService = new UserService(
                userRepository,
                roleRepository,
                passwordEncoder
        );

        userRole = new Role("USER");
        adminRole = new Role("ADMIN");

        user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );

        user.getRoles().add(userRole);
    }

    // ==========================================================
    // GET CURRENT USER
    // ==========================================================

    @Test
    void shouldGetCurrentUser() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getCurrentUser(
                        "test@example.com"
                );

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

        verify(userRepository)
                .findByEmail("test@example.com");
    }

    @Test
    void shouldThrowExceptionWhenCurrentUserNotFound() {

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getCurrentUser(
                        "test@example.com"
                )
        );
    }

    // ==========================================================
    // GET ALL USERS
    // ==========================================================

    @Test
    void shouldGetAllUsers() {

        User secondUser = new User(
                "Second User",
                "second@example.com",
                "encoded-password"
        );

        secondUser.getRoles().add(userRole);

        when(userRepository.findAll())
                .thenReturn(List.of(user, secondUser));

        List<UserResponse> response =
                userService.getAllUsers();

        assertNotNull(response);

        assertEquals(
                2,
                response.size()
        );

        assertEquals(
                "test@example.com",
                response.get(0).getEmail()
        );

        assertEquals(
                "second@example.com",
                response.get(1).getEmail()
        );

        verify(userRepository)
                .findAll();
    }

    // ==========================================================
    // GET USER BY ID
    // ==========================================================

    @Test
    void shouldGetUserById() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserById(1L);

        assertNotNull(response);

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@example.com",
                response.getEmail()
        );

        verify(userRepository)
                .findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserIdNotFound() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.getUserById(99L)
        );
    }

    // ==========================================================
    // UPDATE PROFILE
    // ==========================================================

    @Test
    void shouldUpdateCurrentUser() {

        UpdateProfileRequest request =
                new UpdateProfileRequest(
                        "Updated Name",
                        "updated@example.com"
                );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.findByEmail("updated@example.com"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        UserResponse response =
                userService.updateCurrentUser(
                        "test@example.com",
                        request
                );

        assertEquals(
                "Updated Name",
                response.getName()
        );

        assertEquals(
                "updated@example.com",
                response.getEmail()
        );

        assertEquals(
                "Updated Name",
                user.getName()
        );

        assertEquals(
                "updated@example.com",
                user.getEmail()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void shouldRejectDuplicateEmailDuringProfileUpdate() {

        UpdateProfileRequest request =
                new UpdateProfileRequest(
                        "Updated Name",
                        "existing@example.com"
                );

        User existingUser = new User(
                "Existing User",
                "existing@example.com",
                "encoded-password"
        );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(userRepository.findByEmail("existing@example.com"))
                .thenReturn(Optional.of(existingUser));

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> userService.updateCurrentUser(
                        "test@example.com",
                        request
                )
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ==========================================================
    // CHANGE PASSWORD
    // ==========================================================

    @Test
    void shouldChangePassword() {

        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        "oldPassword",
                        "newPassword123"
                );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "oldPassword",
                "encoded-password"
        )).thenReturn(true);

        when(passwordEncoder.encode("newPassword123"))
                .thenReturn("new-encoded-password");

        userService.changePassword(
                "test@example.com",
                request
        );

        assertEquals(
                "new-encoded-password",
                user.getPassword()
        );

        verify(passwordEncoder)
                .matches(
                        "oldPassword",
                        "encoded-password"
                );

        verify(passwordEncoder)
                .encode("newPassword123");

        verify(userRepository)
                .save(user);
    }

    @Test
    void shouldRejectIncorrectCurrentPassword() {

        ChangePasswordRequest request =
                new ChangePasswordRequest(
                        "wrongPassword",
                        "newPassword123"
                );

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches(
                "wrongPassword",
                "encoded-password"
        )).thenReturn(false);

        assertThrows(
                InvalidCredentialsException.class,
                () -> userService.changePassword(
                        "test@example.com",
                        request
                )
        );

        verify(passwordEncoder, never())
                .encode(anyString());

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ==========================================================
    // UPDATE ROLE
    // ==========================================================

    @Test
    void shouldUpdateUserRole() {

        Role adminRole = new Role("ADMIN");

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("ADMIN"))
                .thenReturn(Optional.of(adminRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        UserResponse response =
                userService.updateRole(
                        1L,
                        "admin"
                );

        assertNotNull(response);

        assertTrue(
                response.getRoles().contains("ADMIN")
        );

        assertFalse(
                response.getRoles().contains("USER")
        );

        verify(roleRepository)
                .findByName("ADMIN");

        verify(userRepository)
                .save(user);
    }

    @Test
    void shouldThrowExceptionWhenRoleDoesNotExist() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MANAGER"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.updateRole(
                        1L,
                        "manager"
                )
        );

        verify(userRepository, never())
                .save(any(User.class));
    }

    // ==========================================================
    // DELETE USER
    // ==========================================================

    @Test
    void shouldDeleteUser() {

        when(userRepository.existsById(1L))
                .thenReturn(true);

        userService.deleteUser(1L);

        verify(userRepository)
                .existsById(1L);

        verify(userRepository)
                .deleteById(1L);
    }

    @Test
    void shouldThrowExceptionWhenDeletingNonExistingUser() {

        when(userRepository.existsById(99L))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.deleteUser(99L)
        );

        verify(userRepository, never())
                .deleteById(anyLong());
    }

    // ==========================================================
    // UNLOCK USER
    // ==========================================================

    @Test
    void shouldUnlockUser() {

        user.setFailedLoginAttempts(5);
        user.setAccountLocked(true);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        userService.unlockUser(1L);

        assertEquals(
                0,
                user.getFailedLoginAttempts()
        );

        assertFalse(
                user.isAccountLocked()
        );

        verify(userRepository)
                .save(user);
    }

    @Test
    void shouldThrowExceptionWhenUnlockingNonExistingUser() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> userService.unlockUser(99L)
        );

        verify(userRepository, never())
                .save(any(User.class));
    }
}