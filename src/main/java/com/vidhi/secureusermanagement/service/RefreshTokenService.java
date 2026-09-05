package com.vidhi.secureusermanagement.service;

import com.vidhi.secureusermanagement.entity.RefreshToken;
import com.vidhi.secureusermanagement.entity.User;
import com.vidhi.secureusermanagement.exception.InvalidRefreshTokenException;
import com.vidhi.secureusermanagement.exception.ResourceNotFoundException;
import com.vidhi.secureusermanagement.repository.RefreshTokenRepository;
import com.vidhi.secureusermanagement.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public RefreshToken createRefreshToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        // Generate the raw token that will be given to the client
        String rawToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = new RefreshToken();

        // Store only the SHA-256 hash in the database
        refreshToken.setToken(hashToken(rawToken));

        refreshToken.setUser(user);

        refreshToken.setExpiryDate(
                Instant.now().plusMillis(refreshExpiration)
        );

        refreshToken.setRevoked(false);

        RefreshToken savedToken =
                refreshTokenRepository.save(refreshToken);

        // Put the raw token back into the entity temporarily
        // so AuthService can return it to the client.
        savedToken.setToken(rawToken);

        return savedToken;
    }

    public RefreshToken verifyRefreshToken(String rawToken) {

        String hashedToken = hashToken(rawToken);

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(hashedToken)
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException(
                                        "Invalid refresh token"
                                )
                        );

        if (refreshToken.isRevoked()) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException(
                    "Refresh token has expired"
            );
        }

        return refreshToken;
    }

    @Transactional
    public void revoke(String rawToken) {

        String hashedToken = hashToken(rawToken);

        RefreshToken refreshToken =
                refreshTokenRepository.findByToken(hashedToken)
                        .orElseThrow(() ->
                                new InvalidRefreshTokenException(
                                        "Invalid refresh token"
                                )
                        );

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(
            RefreshToken oldToken
    ) {

        oldToken.setRevoked(true);

        refreshTokenRepository.save(oldToken);

        return createRefreshToken(
                oldToken.getUser().getEmail()
        );
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(StandardCharsets.UTF_8)
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }
}