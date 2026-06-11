package com.moneytransfer.pin;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PinService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void setPin(User user, String pin, String oldPin) {
        if (pin == null || pin.length() < 4 || pin.length() > 6 || !pin.matches("\\d+")) {
            throw new IllegalArgumentException("PIN must be 4-6 digits");
        }
        if (user.getPinSet()) {
            if (oldPin == null || !passwordEncoder.matches(oldPin, user.getPinHash())) {
                throw new IllegalArgumentException("Old PIN is incorrect");
            }
        }
        user.setPinHash(passwordEncoder.encode(pin));
        user.setPinSet(true);
        userRepository.save(user);
    }

    public boolean verifyPin(User user, String pin) {
        if (!user.getPinSet() || user.getPinHash() == null) return false;
        return passwordEncoder.matches(pin, user.getPinHash());
    }

    public void removePin(User user) {
        user.setPinHash(null);
        user.setPinSet(false);
        userRepository.save(user);
    }
}
