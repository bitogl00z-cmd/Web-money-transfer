package com.moneytransfer.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public void updateProfile(String username, Map<String, String> updates) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (updates.containsKey("fullName")) user.setFullName(updates.get("fullName"));
        if (updates.containsKey("phone")) user.setPhone(updates.get("phone"));
        if (updates.containsKey("email")) user.setEmail(updates.get("email"));
        if (updates.containsKey("otpEnabled")) user.setOtpEnabled(Boolean.parseBoolean(updates.get("otpEnabled")));
        if (updates.containsKey("faceEnabled")) user.setFaceEnabled(Boolean.parseBoolean(updates.get("faceEnabled")));
        if (updates.containsKey("emailNotifications")) user.setEmailNotifications(Boolean.parseBoolean(updates.get("emailNotifications")));
        if (updates.containsKey("language")) user.setLanguage(updates.get("language"));
        userRepository.save(user);
    }
}
