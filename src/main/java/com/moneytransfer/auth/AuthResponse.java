package com.moneytransfer.auth;

public record AuthResponse(String accessToken, String refreshToken, Long userId, String username) {}
