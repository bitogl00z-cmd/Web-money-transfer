package com.moneytransfer.admin;

import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void lockUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setLocked(true);
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setLocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
