package com.moneytransfer.admin;

import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import com.moneytransfer.transaction.TransactionRepository;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserDto;
import com.moneytransfer.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public AdminService(UserRepository userRepository, AuditLogRepository auditLogRepository,
                        TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    public Page<UserDto> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::from);
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

    public Page<AdminTransactionDto> getTransactions(Pageable pageable) {
        Map<Long, String> accountNumbers = accountRepository.findAll().stream()
                .collect(Collectors.toMap(a -> a.getId(), a -> a.getAccountNumber()));
        return transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(tx -> AdminTransactionDto.from(tx, accountNumbers));
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
