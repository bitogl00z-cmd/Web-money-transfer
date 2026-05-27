package com.moneytransfer.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByUserIdAndCodeAndTypeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, String code, OtpType type);
}
