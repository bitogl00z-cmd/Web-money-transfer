package com.moneytransfer.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            Long fromAccountId, Long toAccountId, Pageable pageable);
    Page<Transaction> findByFromAccountIdInOrToAccountIdInOrderByCreatedAtDesc(
            List<Long> fromAccountIds, List<Long> toAccountIds, Pageable pageable);
    List<Transaction> findByFromAccountIdOrToAccountIdAndCreatedAtBetween(
            Long fromAccountId, Long toAccountId, LocalDateTime from, LocalDateTime to);
}
