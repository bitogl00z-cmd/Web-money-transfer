package com.moneytransfer.config;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import com.moneytransfer.user.UserRole;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Random;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initSuperAdmin(UserRepository userRepository, AccountRepository accountRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User("admin", passwordEncoder.encode("admin123"),
                        "admin@moneytransfer.com", "Super Admin");
                admin.setRole(UserRole.ADMIN);
                admin = userRepository.save(admin);

                Account account = new Account(admin.getId(), generateAccountNumber());
                account.setBalance(BigDecimal.valueOf(100000));
                accountRepository.save(account);

                System.out.println("Super admin created: admin / admin123");
            }
        };
    }

    private String generateAccountNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("MT");
        for (int i = 0; i < 14; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
