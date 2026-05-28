package com.moneytransfer.user;

public record UserDto(Long id, String username, String fullName, String email,
                       String role, String status) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name(),
            user.isLocked() ? "LOCKED" : "ACTIVE"
        );
    }
}
