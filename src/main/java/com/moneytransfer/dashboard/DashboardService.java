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
                    transactionRepository.findByAccountIdAndCreatedAtBetween(
                            accId, LocalDateTime.now().minusDays(7), LocalDateTime.now()));
        }
        recentTransactions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        if (recentTransactions.size() > 10) {
            recentTransactions = recentTransactions.subList(0, 10);
        }

        List<Map<String, Object>> chartData = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        List<BigDecimal> dailyNets = new ArrayList<>();
        List<BigDecimal> dailyIncomes = new ArrayList<>();
        List<BigDecimal> dailyExpenses = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            BigDecimal dayIncome = BigDecimal.ZERO;
            BigDecimal dayExpense = BigDecimal.ZERO;
            for (Long accId : accountIds) {
                List<Transaction> dayTxs = transactionRepository
                        .findByAccountIdAndCreatedAtBetween(accId, start, end);
                for (Transaction tx : dayTxs) {
                    boolean isOutgoing = accId.equals(tx.getFromAccountId());
                    if (isOutgoing && (tx.getType() == com.moneytransfer.transaction.TransactionType.WITHDRAW ||
                                       tx.getType() == com.moneytransfer.transaction.TransactionType.TRANSFER)) {
                        dayExpense = dayExpense.add(tx.getAmount());
                    } else if (!isOutgoing) {
                        dayIncome = dayIncome.add(tx.getAmount());
                    }
                }
            }
            dates.add(date);
            dailyIncomes.add(dayIncome);
            dailyExpenses.add(dayExpense);
            dailyNets.add(dayIncome.subtract(dayExpense));
        }

        BigDecimal runningBalance = totalBalance;
        BigDecimal[] balances = new BigDecimal[dailyNets.size()];
        for (int i = dailyNets.size() - 1; i >= 0; i--) {
            balances[i] = runningBalance;
            runningBalance = runningBalance.subtract(dailyNets.get(i));
        }

        for (int i = 0; i < dates.size(); i++) {
            Map<String, Object> point = new HashMap<>();
            point.put("date", dates.get(i).toString());
            point.put("balance", balances[i]);
            point.put("net", dailyNets.get(i));
            point.put("income", dailyIncomes.get(i));
            point.put("expense", dailyExpenses.get(i));
            chartData.add(point);
        }

        List<Map<String, Object>> accountInfo = new ArrayList<>();
        for (Account a : accounts) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", a.getId());
            info.put("accountNumber", a.getAccountNumber());
            info.put("balance", a.getBalance());
            info.put("status", a.getStatus());
            accountInfo.add(info);
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalBalance", totalBalance);
        stats.put("accountCount", accounts.size());
        stats.put("accounts", accountInfo);
        stats.put("recentTransactions", recentTransactions);
        stats.put("chartData", chartData);
        return stats;
    }
}
