package com.moneytransfer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/register") public String register() { return "register"; }
    @GetMapping("/dashboard") public String dashboard() { return "dashboard"; }
    @GetMapping("/transfer") public String transfer() { return "transfer"; }
    @GetMapping("/history") public String history() { return "history"; }
    @GetMapping("/beneficiaries") public String beneficiaries() { return "beneficiaries"; }
    @GetMapping("/scheduled") public String scheduled() { return "scheduled"; }
    @GetMapping("/profile") public String profile() { return "profile"; }
    @GetMapping("/admin/users") public String adminUsers() { return "admin/users"; }
    @GetMapping("/admin/transactions") public String adminTransactions() { return "admin/transactions"; }
}
