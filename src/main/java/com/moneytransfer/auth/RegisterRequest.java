package com.moneytransfer.auth;

public record RegisterRequest(String username, String password, String email, String fullName) {}
