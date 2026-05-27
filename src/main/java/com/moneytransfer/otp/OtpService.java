package com.moneytransfer.otp;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    public OtpService(OtpRepository otpRepository, JavaMailSender mailSender) {
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public String generateAndSendOtp(Long userId, String email, OtpType type) {
        String code = String.format("%06d", new Random().nextInt(999999));
        OtpCode otp = new OtpCode(userId, code, type, LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Money Transfer - OTP Code");
        message.setText("Your OTP code is: " + code + "\nExpires in 5 minutes.");
        mailSender.send(message);

        return code;
    }

    public boolean verifyOtp(Long userId, String code, OtpType type) {
        return otpRepository.findByUserIdAndCodeAndTypeAndUsedFalseOrderByCreatedAtDesc(userId, code, type)
                .map(otp -> {
                    if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    otp.setUsed(true);
                    otpRepository.save(otp);
                    return true;
                })
                .orElse(false);
    }
}
