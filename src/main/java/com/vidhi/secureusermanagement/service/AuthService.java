package com.vidhi.secureusermanagement.service;

import com.vidhi.secureusermanagement.dto.LoginRequest;
import com.vidhi.secureusermanagement.dto.RegisterRequest;
import com.vidhi.secureusermanagement.dto.TokenResponse;
import com.vidhi.secureusermanagement.dto.UserResponse;
import com.vidhi.secureusermanagement.entity.RefreshToken;
import com.vidhi.secureusermanagement.entity.Role;
import com.vidhi.secureusermanagement.entity.User;
import com.vidhi.secureusermanagement.exception.InvalidCredentialsException;
import com.vidhi.secureusermanagement.exception.ResourceAlreadyExistsException;
import com.vidhi.secureusermanagement.exception.ResourceNotFoundException;
import com.vidhi.secureusermanagement.jwt.JwtService;
import com.vidhi.secureusermanagement.repository.RoleRepository;
import com.vidhi.secureusermanagement.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    // =========================
    // REGISTER
    // =========================

    public UserResponse register(RegisterRequest request) {

        // Check whether email is already registered
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Email already registered"
            );
        }

        // Find default USER role
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "USER role not found"
                        )
                );

        // Encrypt password before storing
        String encodedPassword =
                passwordEncoder.encode(request.getPassword());

        // Create user
        User user = new User(
                request.getName(),
                request.getEmail(),
                encodedPassword
        );

        // Assign USER role
        user.getRoles().add(userRole);

        // Save user
        User savedUser = userRepository.save(user);

        return mapToUserResponse(savedUser);
    }

    // =========================
    // LOGIN
    // =========================

    public TokenResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid email or password"
                        )
                );

        // Stop login if account is already locked
        if (user.isAccountLocked()) {
            throw new InvalidCredentialsException(
                    "Account is locked"
            );
        }

        try {

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    );

            // Spring Security authenticates the user
            Authentication authentication =
                    authenticationManager.authenticate(
                            authenticationToken
                    );

            // Successful login → reset failed attempts
            user.setFailedLoginAttempts(0);
            userRepository.save(user);

            // Get authenticated username
            String username = authentication.getName();

            // Get roles/authorities
            Set<String> roles = authentication
                    .getAuthorities()
                    .stream()
                    .map(authority -> authority.getAuthority())
                    .collect(Collectors.toSet());

            // Generate access token
            String accessToken =
                    jwtService.generateToken(
                            username,
                            roles
                    );

            // Generate refresh token
            RefreshToken refreshToken =
                    refreshTokenService.createRefreshToken(
                            username
                    );

            return new TokenResponse(
                    accessToken,
                    refreshToken.getToken()
            );

        } catch (AuthenticationException exception) {

            // Increase failed login count
            int attempts =
                    user.getFailedLoginAttempts() + 1;

            user.setFailedLoginAttempts(attempts);

            // Lock account after 5 failed attempts
            if (attempts >= 5) {
                user.setAccountLocked(true);
            }

            userRepository.save(user);

            if (attempts >= 5) {
                throw new InvalidCredentialsException(
                        "Account locked due to too many failed attempts"
                );
            }

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }
    }

    // =========================
    // REFRESH ACCESS TOKEN
    // =========================

    @Transactional
    public TokenResponse refreshAccessToken(
            String refreshTokenValue
    ) {

        // 1. Check whether refresh token is valid
        RefreshToken oldRefreshToken =
                refreshTokenService.verifyRefreshToken(
                        refreshTokenValue
                );

        // 2. Get the user associated with refresh token
        User user = oldRefreshToken.getUser();

        // 3. Get current user roles
        Set<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());

        // 4. Generate a new access token
        String accessToken =
                jwtService.generateToken(
                        user.getEmail(),
                        roles
                );

        // 5. Rotate refresh token
        //    Old token becomes revoked
        //    New refresh token is generated
        RefreshToken newRefreshToken =
                refreshTokenService.rotateRefreshToken(
                        oldRefreshToken
                );

        // 6. Return both tokens
        return new TokenResponse(
                accessToken,
                newRefreshToken.getToken()
        );
    }

    // =========================
    // LOGOUT
    // =========================

    public void logout(String refreshToken) {

        refreshTokenService.revoke(refreshToken);
    }

    // =========================
    // USER RESPONSE MAPPING
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
}