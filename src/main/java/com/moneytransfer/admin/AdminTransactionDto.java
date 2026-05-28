package com.moneytransfer.admin;

import com.moneytransfer.transaction.Transaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTransactionDto(
    Long id, String transactionCode, String fromAccountNumber, String toAccountNumber,
    BigDecimal amount, String type, String status, String description, LocalDateTime createdAt) {

    public static AdminTransactionDto from(Transaction tx, java.util.Map<Long, String> accountNumbers) {
        return new AdminTransactionDto(
            tx.getId(), tx.getTransactionCode(),
            accountNumbers.getOrDefault(tx.getFromAccountId(), ""),
            accountNumbers.getOrDefault(tx.getToAccountId(), ""),
            tx.getAmount(), tx.getType().name(), tx.getStatus().name(),
            tx.getDescription(), tx.getCreatedAt()
        );
    }
}
