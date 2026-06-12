package com.moneytransfer.transaction;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.account.AccountStatus;
import com.moneytransfer.audit.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountService accountService;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountRepository, transactionRepository,
                accountService, auditLogRepository, messagingTemplate);
    }

    @Test
    void testTransfer_Success() {
        Account from = new Account(1L, "MT00000000000001");
        from.setId(1L);
        from.setBalance(new BigDecimal("1000.00"));
        from.setStatus(AccountStatus.ACTIVE);

        Account to = new Account(2L, "MT00000000000002");
        to.setId(2L);
        to.setBalance(new BigDecimal("500.00"));
        to.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(to));
        when(accountService.computeChecksum(any())).thenReturn("checksum");
        Transaction savedTx = new Transaction();
        savedTx.setTransactionCode("TX20260524000001");
        when(transactionRepository.save(any())).thenReturn(savedTx);

        Transaction result = transactionService.transfer(1L, 2L, new BigDecimal("200.00"), "test", 1L, "127.0.0.1");

        assertNotNull(result);
        assertEquals(new BigDecimal("800.00"), from.getBalance());
        assertEquals(new BigDecimal("700.00"), to.getBalance());
        verify(accountRepository, times(2)).save(any());
    }

    @Test
    void testTransfer_InsufficientBalance() {
        Account from = new Account(1L, "MT00000000000001");
        from.setId(1L);
        from.setBalance(new BigDecimal("100.00"));
        from.setStatus(AccountStatus.ACTIVE);

        Account to = new Account(2L, "MT00000000000002");
        to.setId(2L);
        to.setBalance(new BigDecimal("500.00"));
        to.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(to));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(1L, 2L, new BigDecimal("200.00"), "test", 1L, "127.0.0.1"));
    }

    @Test
    void testDeposit_Success() {
        Account account = new Account(1L, "MT00000000000001");
        account.setId(1L);
        account.setBalance(new BigDecimal("500.00"));
        account.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));
        when(accountService.computeChecksum(any())).thenReturn("checksum");
        Transaction savedTx = new Transaction();
        savedTx.setTransactionCode("TX20260524000002");
        when(transactionRepository.save(any())).thenReturn(savedTx);

        Transaction result = transactionService.deposit(1L, new BigDecimal("300.00"), "Deposit", 1L, "127.0.0.1");

        assertNotNull(result);
        assertEquals(new BigDecimal("800.00"), account.getBalance());
    }

    @Test
    void testWithdraw_InsufficientBalance() {
        Account account = new Account(1L, "MT00000000000001");
        account.setId(1L);
        account.setBalance(new BigDecimal("50.00"));
        account.setStatus(AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(account));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.withdraw(1L, new BigDecimal("100.00"), "Withdrawal", 1L, "127.0.0.1"));
    }
}
