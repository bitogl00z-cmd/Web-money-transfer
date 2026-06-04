package com.moneytransfer.admin;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import com.moneytransfer.transaction.TransactionService;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserDto;
import com.moneytransfer.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moneytransfer.account.AccountStatus;
import com.moneytransfer.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionService transactionService;

    public AdminService(UserRepository userRepository, AuditLogRepository auditLogRepository,
                        TransactionRepository transactionRepository, AccountRepository accountRepository,
                        TransactionService transactionService) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionService = transactionService;
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

    public Map<String, Object> getStats() {
        List<User> allUsers = userRepository.findAll();
        List<Account> allAccounts = accountRepository.findAll();
        long totalUsers = allUsers.size();
        long lockedUsers = allUsers.stream().filter(User::isLocked).count();
        long activeUsers = totalUsers - lockedUsers;
        long totalAccounts = allAccounts.size();
        long activeAccounts = allAccounts.stream().filter(a -> a.getStatus() == AccountStatus.ACTIVE).count();
        long totalTransactions = transactionRepository.count();
        BigDecimal totalBalance = allAccounts.stream().map(Account::getBalance).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("lockedUsers", lockedUsers);
        stats.put("totalAccounts", totalAccounts);
        stats.put("activeAccounts", activeAccounts);
        stats.put("totalTransactions", totalTransactions);
        stats.put("totalBalance", totalBalance);
        return stats;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount, String description, Long adminUserId, String ip) {
        return transactionService.deposit(accountId, amount, description, adminUserId, ip);
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, String description, Long adminUserId, String ip) {
        return transactionService.withdraw(accountId, amount, description, adminUserId, ip);
    }
}
