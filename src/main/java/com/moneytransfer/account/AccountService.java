package com.moneytransfer.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final String checksumSecretKey;

    public AccountService(AccountRepository accountRepository,
                          @Value("${app.checksum.secret-key}") String checksumSecretKey) {
        this.accountRepository = accountRepository;
        this.checksumSecretKey = checksumSecretKey;
    }

    public List<Account> getUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId);
    }

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    public String computeChecksum(Account account) {
        try {
            String input = account.getAccountNumber() + account.getBalance().toPlainString() + checksumSecretKey;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    public void verifyChecksum(Account account) {
        if (account.getBalanceChecksum() != null) {
            String expected = computeChecksum(account);
            if (!expected.equals(account.getBalanceChecksum())) {
                account.setStatus(AccountStatus.FROZEN);
                accountRepository.save(account);
                throw new SecurityException("Balance integrity check failed for account " + account.getAccountNumber());
            }
        }
    }

    @Transactional
    public Account createAccount(Long userId) {
        Account account = new Account(userId, generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setBalanceChecksum(computeChecksum(account));
        return accountRepository.save(account);
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("MT");
        for (int i = 0; i < 14; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
