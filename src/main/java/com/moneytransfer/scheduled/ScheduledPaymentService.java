package com.moneytransfer.scheduled;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import com.moneytransfer.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class ScheduledPaymentService {
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public ScheduledPaymentService(ScheduledPaymentRepository scheduledPaymentRepository,
                                   AccountRepository accountRepository, AccountService accountService,
                                   TransactionRepository transactionRepository) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ScheduledPayment create(ScheduledPayment payment) {
        return scheduledPaymentRepository.save(payment);
    }

    public List<ScheduledPayment> getUserPayments(Long fromAccountId) {
        return scheduledPaymentRepository.findByFromAccountId(fromAccountId);
    }

    @Transactional
    public void cancel(Long id) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        payment.setStatus(PaymentStatus.CANCELLED);
        scheduledPaymentRepository.save(payment);
    }

    @Transactional
    public void processDuePayments() {
        List<ScheduledPayment> due = scheduledPaymentRepository
                .findByStatusAndNextRunLessThanEqual(PaymentStatus.ACTIVE, LocalDate.now());
        for (ScheduledPayment payment : due) {
            try {
                processPayment(payment);
                payment.setNextRun(computeNextRun(payment));
            } catch (Exception e) {
                payment.setStatus(PaymentStatus.PAUSED);
            }
            scheduledPaymentRepository.save(payment);
        }
    }

    private void processPayment(ScheduledPayment payment) {
        Account from = accountRepository.findByAccountNumber(
                payment.getToAccountNumber()).orElse(null);
        if (from == null) return;
        Account senderAccount = accountRepository.findByIdWithLock(payment.getFromAccountId())
                .orElseThrow();
        accountService.verifyChecksum(senderAccount);
        if (senderAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        senderAccount.setBalance(senderAccount.getBalance().subtract(payment.getAmount()));
        senderAccount.setBalanceChecksum(accountService.computeChecksum(senderAccount));
        accountRepository.save(senderAccount);
        String txCode = "TX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", new Random().nextInt(999999));
        transactionRepository.save(new Transaction(txCode, payment.getFromAccountId(), from.getId(),
                payment.getAmount(), senderAccount.getBalance(), null, TransactionType.RECURRING,
                payment.getDescription()));
    }

    private LocalDate computeNextRun(ScheduledPayment payment) {
        return switch (payment.getFrequency()) {
            case WEEKLY -> payment.getNextRun().plusWeeks(1);
            case MONTHLY -> payment.getNextRun().plusMonths(1);
        };
    }
}
