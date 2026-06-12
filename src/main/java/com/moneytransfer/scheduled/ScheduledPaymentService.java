package com.moneytransfer.scheduled;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import com.moneytransfer.transaction.TransactionType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class ScheduledPaymentService {
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ScheduledPaymentService(ScheduledPaymentRepository scheduledPaymentRepository,
                                   AccountRepository accountRepository, AccountService accountService,
                                   TransactionRepository transactionRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public Optional<ScheduledPayment> findById(Long id) {
        return scheduledPaymentRepository.findById(id);
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

    public synchronized void processDuePayments() {
        List<ScheduledPayment> due = scheduledPaymentRepository
                .findByStatusAndNextRunLessThanEqual(PaymentStatus.ACTIVE, LocalDateTime.now());
        for (ScheduledPayment payment : due) {
            processSinglePayment(payment.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSinglePayment(Long paymentId) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getStatus() != PaymentStatus.ACTIVE) return;
        try {
            processPayment(payment);
            if (payment.getFrequency() == PaymentFrequency.ONE_TIME) {
                payment.setStatus(PaymentStatus.COMPLETED);
            } else {
                LocalDateTime next = computeNextRun(payment);
                if (payment.getEndDate() != null && next.isAfter(payment.getEndDate())) {
                    payment.setStatus(PaymentStatus.COMPLETED);
                } else {
                    payment.setNextRun(next);
                }
            }
        } catch (InsufficientBalanceException e) {
            payment.setStatus(PaymentStatus.CANCELLED);
            notifyFailure(payment, "Số dư không đủ để thực hiện thanh toán định kỳ");
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.PAUSED);
            notifyFailure(payment, "Lỗi không xác định khi xử lý thanh toán định kỳ");
        }
        scheduledPaymentRepository.save(payment);
    }

    private void processPayment(ScheduledPayment payment) {
        Account recipient = accountRepository.findByAccountNumber(
                payment.getToAccountNumber()).orElse(null);
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient account not found: " + payment.getToAccountNumber());
        }
        Account senderAccount = accountRepository.findByIdWithLock(payment.getFromAccountId())
                .orElseThrow();
        Account recipientLocked = accountRepository.findByIdWithLock(recipient.getId())
                .orElseThrow();
        accountService.verifyChecksum(senderAccount);
        accountService.verifyChecksum(recipientLocked);
        if (senderAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance");
        }
        senderAccount.setBalance(senderAccount.getBalance().subtract(payment.getAmount()));
        senderAccount.setBalanceChecksum(accountService.computeChecksum(senderAccount));
        accountRepository.save(senderAccount);

        recipientLocked.setBalance(recipientLocked.getBalance().add(payment.getAmount()));
        recipientLocked.setBalanceChecksum(accountService.computeChecksum(recipientLocked));
        accountRepository.save(recipientLocked);

        String txCode = "TX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", new Random().nextInt(999999));
        transactionRepository.save(new Transaction(txCode, payment.getFromAccountId(), recipientLocked.getId(),
                payment.getAmount(), senderAccount.getBalance(), recipientLocked.getBalance(), TransactionType.RECURRING,
                payment.getDescription()));

        String formattedAmount = NumberFormat.getNumberInstance(Locale.of("vi", "VN")).format(payment.getAmount()) + "đ";
        String message = "Đã chuyển " + formattedAmount + " đến " + payment.getToAccountNumber();
        Long senderUserId = senderAccount.getUserId();
        messagingTemplate.convertAndSend("/topic/notifications/" + senderUserId,
                Map.of("type", "SCHEDULED_PAYMENT", "title", "Thanh toán định kỳ",
                       "message", message, "balance", senderAccount.getBalance(),
                       "transactionCode", txCode));
    }

    private void notifyFailure(ScheduledPayment payment, String reason) {
        Long senderUserId = accountRepository.findById(payment.getFromAccountId())
                .map(Account::getUserId).orElse(null);
        if (senderUserId == null) return;
        messagingTemplate.convertAndSend("/topic/notifications/" + senderUserId,
                Map.of("type", "SCHEDULED_PAYMENT_FAILED", "title", "Thanh toán định kỳ thất bại",
                       "message", reason + " - " + formatVND(payment.getAmount()) + " đến " + payment.getToAccountNumber() + " đã bị hủy",
                       "balance", null, "transactionCode", null));
    }

    private String formatVND(java.math.BigDecimal amount) {
        return NumberFormat.getNumberInstance(Locale.of("vi", "VN")).format(amount) + "đ";
    }

    private LocalDateTime computeNextRun(ScheduledPayment payment) {
        return switch (payment.getFrequency()) {
            case WEEKLY -> payment.getNextRun().plusWeeks(1);
            case MONTHLY -> payment.getNextRun().plusMonths(1);
            default -> payment.getNextRun();
        };
    }
}
