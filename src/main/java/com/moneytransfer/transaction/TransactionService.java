package com.moneytransfer.transaction;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.account.AccountStatus;
import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AuditLogRepository auditLogRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              AccountService accountService,
                              AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Transaction transfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                 String description, Long userId, String ip) {
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        Account first = accountRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstId));
        Account second = accountRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondId));

        accountService.verifyChecksum(first);
        accountService.verifyChecksum(second);

        Account from = first.getId().equals(fromAccountId) ? first : second;
        Account to = first.getId().equals(fromAccountId) ? second : first;

        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Sender account is not active");
        }
        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Receiver account is not active");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        from.setBalanceChecksum(accountService.computeChecksum(from));
        to.setBalanceChecksum(accountService.computeChecksum(to));

        accountRepository.save(from);
        accountRepository.save(to);

        String txCode = generateTransactionCode();
        Transaction tx = new Transaction(txCode, fromAccountId, toAccountId, amount,
                from.getBalance(), to.getBalance(), TransactionType.TRANSFER, description);
        tx = transactionRepository.save(tx);

        auditLogRepository.save(new AuditLog(userId, "TRANSFER",
                "Transferred " + amount + " from " + fromAccountId + " to " + toAccountId, ip));

        return tx;
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount, String description, Long userId, String ip) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        accountService.verifyChecksum(account);

        account.setBalance(account.getBalance().add(amount));
        account.setBalanceChecksum(accountService.computeChecksum(account));
        accountRepository.save(account);

        String txCode = generateTransactionCode();
        Transaction tx = new Transaction(txCode, null, accountId, amount,
                null, account.getBalance(), TransactionType.DEPOSIT, description);
        tx = transactionRepository.save(tx);

        auditLogRepository.save(new AuditLog(userId, "DEPOSIT",
                "Deposited " + amount + " to account " + accountId, ip));
        return tx;
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, String description, Long userId, String ip) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        accountService.verifyChecksum(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setBalanceChecksum(accountService.computeChecksum(account));
        accountRepository.save(account);

        String txCode = generateTransactionCode();
        Transaction tx = new Transaction(txCode, accountId, null, amount,
                account.getBalance(), null, TransactionType.WITHDRAW, description);
        tx = transactionRepository.save(tx);

        auditLogRepository.save(new AuditLog(userId, "WITHDRAW",
                "Withdrew " + amount + " from account " + accountId, ip));
        return tx;
    }

    public Page<Transaction> getHistory(List<Long> accountIds, Pageable pageable) {
        if (accountIds.isEmpty()) {
            return Page.empty();
        }
        return transactionRepository.findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDesc(accountIds, accountIds, pageable);
    }

    public List<Transaction> getHistoryBetween(Long accountId, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByFromAccountIdOrToAccountIdAndCreatedAtBetween(accountId, accountId, from, to);
    }

    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }

    private String generateTransactionCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        StringBuilder sb = new StringBuilder("TX").append(datePart);
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
