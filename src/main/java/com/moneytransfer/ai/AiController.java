package com.moneytransfer.ai;

import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.transaction.TransactionService;
import com.moneytransfer.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AiController(AiService aiService, TransactionService transactionService,
                        AccountRepository accountRepository, UserRepository userRepository) {
        this.aiService = aiService;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(Authentication auth, @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập câu hỏi"));
        }

        Map<String, Object> parsed = aiService.parse(text);
        String intent = (String) parsed.get("intent");
        Map<String, Object> entities = (Map<String, Object>) parsed.get("entities");

        return switch (intent) {
            case "transfer" -> handleTransfer(auth, entities);
            case "balance" -> handleBalance(auth);
            case "history" -> handleHistory(auth);
            case "create_user" -> ResponseEntity.ok(Map.of("message", "Chức năng tạo user yêu cầu admin. Liên hệ quản trị viên."));
            case "help" -> ResponseEntity.ok(Map.of("message", helpText()));
            case "greeting" -> ResponseEntity.ok(Map.of("message", "Xin chào! Tôi có thể giúp gì cho bạn?"));
            default -> ResponseEntity.ok(Map.of("message", "Xin lỗi, tôi chưa hiểu ý bạn. Hãy thử: chuyển tiền, xem số dư, xem lịch sử."));
        };
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Claims claims)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return ((Integer) claims.get("userId")).longValue();
    }

    private ResponseEntity<?> handleTransfer(Authentication auth, Map<String, Object> entities) {
        try {
            Long userId = extractUserId(auth);
            Object targetObj = entities.get("target");
            Object amountObj = entities.get("amount");
            if (targetObj == null) return ResponseEntity.ok(Map.of("message", "Vui lòng cho tôi biết chuyển cho ai?"));
            if (amountObj == null) return ResponseEntity.ok(Map.of("message", "Vui lòng cho tôi biết số tiền?"));

            String target = targetObj.toString();
            int amount = Integer.parseInt(amountObj.toString());

            var fromAccounts = accountRepository.findByUserId(userId);
            if (fromAccounts.isEmpty()) return ResponseEntity.ok(Map.of("message", "Bạn chưa có tài khoản"));
            Long fromAccountId = fromAccounts.get(0).getId();

            var toUser = userRepository.findByUsername(target);
            if (toUser.isEmpty()) return ResponseEntity.ok(Map.of("message", "Không tìm thấy người dùng " + target));
            var toAccounts = accountRepository.findByUserId(toUser.get().getId());
            if (toAccounts.isEmpty()) return ResponseEntity.ok(Map.of("message", "Người dùng " + target + " chưa có tài khoản"));

            transactionService.transfer(fromAccountId, toAccounts.get(0).getId(),
                    BigDecimal.valueOf(amount), "Chuyển qua AI", userId, "AI");

            return ResponseEntity.ok(Map.of("message", "Đã chuyển " + formatVND(amount) + " cho " + target + " thành công"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> handleBalance(Authentication auth) {
        try {
            Long userId = extractUserId(auth);
            var accounts = accountRepository.findByUserId(userId);
            long total = accounts.stream().mapToLong(a -> a.getBalance().longValue()).sum();
            return ResponseEntity.ok(Map.of("message", "Số dư tài khoản của bạn là " + formatVND((int) total)));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Lỗi khi xem số dư"));
        }
    }

    private ResponseEntity<?> handleHistory(Authentication auth) {
        try {
            Long userId = extractUserId(auth);
            var accounts = accountRepository.findByUserId(userId);
            var accountIds = accounts.stream().map(a -> a.getId()).collect(Collectors.toList());
            if (accountIds.isEmpty()) return ResponseEntity.ok(Map.of("message", "Bạn chưa có tài khoản"));

            var txPage = transactionService.getRecentTransactions(accountIds, 5);
            var txs = txPage.getContent();
            if (txs.isEmpty()) return ResponseEntity.ok(Map.of("message", "Chưa có giao dịch nào."));

            StringBuilder sb = new StringBuilder("5 giao dịch gần đây:\n");
            for (var tx : txs) {
                sb.append("• ").append(tx.getTransactionCode()).append(": ").append(formatVND(tx.getAmount().intValue())).append("\n");
            }
            return ResponseEntity.ok(Map.of("message", sb.toString()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Lỗi khi xem lịch sử"));
        }
    }

    private String helpText() {
        return "Tôi có thể giúp bạn:\n" +
               "• Chuyển tiền: \"chuyển 500k cho user2\"\n" +
               "• Xem số dư: \"xem số dư\"\n" +
               "• Lịch sử: \"xem giao dịch gần đây\"\n" +
               "• Hỏi lại câu khác nếu tôi chưa hiểu!";
    }

    private String formatVND(int amount) {
        return String.format("%,d₫", amount);
    }
}
