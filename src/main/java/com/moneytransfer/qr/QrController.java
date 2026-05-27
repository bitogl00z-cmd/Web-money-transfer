package com.moneytransfer.qr;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
public class QrController {
    private final QrService qrService;
    private final AccountRepository accountRepository;

    public QrController(QrService qrService, AccountRepository accountRepository) {
        this.qrService = qrService;
        this.accountRepository = accountRepository;
    }

    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQr(@RequestParam Long accountId) throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        String qrData = "MT://pay?account=" + account.getAccountNumber();
        byte[] qrImage = qrService.generateQr(qrData, 300, 300);
        return ResponseEntity.ok(qrImage);
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanQr(@RequestBody Map<String, String> body) {
        String qrData = body.get("data");
        String accountNumber = qrData.replace("MT://pay?account=", "");
        return ResponseEntity.ok(Map.of("accountNumber", accountNumber));
    }
}
