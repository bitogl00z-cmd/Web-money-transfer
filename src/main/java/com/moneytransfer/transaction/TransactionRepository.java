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
    Page<Transaction> findByFromAccountIdInOrToAccountIdInAndCreatedAtAfterOrderByCreatedAtDesc(
            List<Long> fromAccountIds, List<Long> toAccountIds, LocalDateTime cutoff, Pageable pageable);
    @org.springframework.data.jpa.repository.Query("SELECT t FROM Transaction t WHERE (t.fromAccountId = :accId OR t.toAccountId = :accId) AND t.createdAt BETWEEN :from AND :to")
    List<Transaction> findByAccountIdAndCreatedAtBetween(@org.springframework.data.repository.query.Param("accId") Long accId, @org.springframework.data.repository.query.Param("from") LocalDateTime from, @org.springframework.data.repository.query.Param("to") LocalDateTime to);

    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
