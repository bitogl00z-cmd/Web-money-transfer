package com.moneytransfer.dashboard;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class DashboardService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardService(AccountRepository accountRepository,
                            TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public Map<String, Object> getStats(Long userId) {
        List<Account> accounts = accountRepository.findByUserId(userId);
        BigDecimal totalBalance = accounts.stream()
                .map(Account::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Long> accountIds = accounts.stream().map(Account::getId).toList();
        List<Transaction> recentTransactions = new ArrayList<>();
        for (Long accId : accountIds) {
            recentTransactions.addAll(
                    transactionRepository.findByFromAccountIdOrToAccountIdAndCreatedAtBetween(
                            accId, accId, LocalDateTime.now().minusDays(7), LocalDateTime.now()));
        }
        recentTransactions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        if (recentTransactions.size() > 10) {
            recentTransactions = recentTransactions.subList(0, 10);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            BigDecimal dayTotal = BigDecimal.ZERO;
            for (Long accId : accountIds) {
                List<Transaction> dayTxs = transactionRepository
                        .findByFromAccountIdOrToAccountIdAndCreatedAtBetween(accId, accId, start, end);
                for (Transaction tx : dayTxs) {
                    if (tx.getType() == com.moneytransfer.transaction.TransactionType.WITHDRAW ||
                        tx.getType() == com.moneytransfer.transaction.TransactionType.TRANSFER) {
                        dayTotal = dayTotal.subtract(tx.getAmount());
                    } else {
                        dayTotal = dayTotal.add(tx.getAmount());
                    }
                }
            }
            Map<String, Object> point = new HashMap<>();
            point.put("date", date.toString());
            point.put("net", dayTotal);
            chartData.add(point);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBalance", totalBalance);
        stats.put("accountCount", accounts.size());
        stats.put("recentTransactions", recentTransactions);
        stats.put("chartData", chartData);
        return stats;
    }
}
