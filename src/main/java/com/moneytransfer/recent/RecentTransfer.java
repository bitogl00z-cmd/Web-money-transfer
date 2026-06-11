package com.moneytransfer.recent;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recent_transfers")
public class RecentTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(name = "to_account_number", length = 20, nullable = false)
    private String toAccountNumber;

    @Column(name = "to_account_name", length = 255)
    private String toAccountName;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;

    public RecentTransfer() {}

    public RecentTransfer(Long userId, Long toAccountId, String toAccountNumber, String toAccountName) {
        this.userId = userId;
        this.toAccountId = toAccountId;
        this.toAccountNumber = toAccountNumber;
        this.toAccountName = toAccountName;
        this.transferredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
    public String getToAccountName() { return toAccountName; }
    public void setToAccountName(String toAccountName) { this.toAccountName = toAccountName; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public void setTransferredAt(LocalDateTime transferredAt) { this.transferredAt = transferredAt; }
}
