package com.moneytransfer.dashboard;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import com.moneytransfer.transaction.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(accountRepository, transactionRepository);
    }

    @Test
    void getStats_buildsDailyBalanceChartFromCurrentBalance() {
        Account account = new Account();
        account.setId(1L);
        account.setUserId(10L);
        account.setAccountNumber("123456789");
        account.setBalance(new BigDecimal("900.00"));
        when(accountRepository.findByUserId(10L)).thenReturn(List.of(account));

        Transaction todayWithdrawal = new Transaction();
        todayWithdrawal.setFromAccountId(1L);
        todayWithdrawal.setToAccountId(2L);
        todayWithdrawal.setAmount(new BigDecimal("100.00"));
        todayWithdrawal.setType(TransactionType.WITHDRAW);
        todayWithdrawal.setCreatedAt(LocalDate.now().atTime(12, 0));

        when(transactionRepository.findByFromAccountIdOrToAccountIdAndCreatedAtBetween(eq(1L), eq(1L), any(), any()))
                .thenAnswer(invocation -> {
                    LocalDateTime start = invocation.getArgument(2);
                    LocalDateTime end = invocation.getArgument(3);
                    LocalDateTime txTime = todayWithdrawal.getCreatedAt();
                    if (!txTime.isBefore(start) && !txTime.isAfter(end)) {
                        return List.of(todayWithdrawal);
                    }
                    return Collections.emptyList();
                });

        Map<String, Object> stats = dashboardService.getStats(10L);
        List<Map<String, Object>> chartData = (List<Map<String, Object>>) stats.get("chartData");

        assertEquals(7, chartData.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(new BigDecimal("1000.00"), new BigDecimal(chartData.get(i).get("balance").toString()));
        }
        assertEquals(new BigDecimal("900.00"), new BigDecimal(chartData.get(6).get("balance").toString()));
    }
}
