package com.vidhi.secureusermanagement.dto;

import com.vidhi.secureusermanagement.entity.RefreshToken;

public class RefreshTokenResult {

    private final RefreshToken refreshToken;
    private final String rawToken;

    public RefreshTokenResult(
            RefreshToken refreshToken,
            String rawToken
    ) {
        this.refreshToken = refreshToken;
        this.rawToken = rawToken;
    }

    public RefreshToken getRefreshToken() {
        return refreshToken;
    }

    public String getRawToken() {
        return rawToken;
    }
}