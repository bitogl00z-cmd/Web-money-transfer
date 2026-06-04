package com.moneytransfer;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
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
    @GetMapping("/profile") public String profile() { return "redirect:/settings"; }
    @GetMapping("/settings") public String settings() { return "settings"; }
    @GetMapping("/") public String root() { return "redirect:/login"; }
    @GetMapping("/logout") public String logout(HttpServletResponse response) {
        clearAuthCookies(response);
        return "redirect:/login";
    }
    @GetMapping("/favicon.ico") public void favicon(HttpServletResponse response) throws Exception {
        response.setContentType("image/svg+xml");
        response.getWriter().write("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 32 32'><rect width='32' height='32' rx='6' fill='#2563eb'/><text x='16' y='23' font-size='20' fill='white' text-anchor='middle'>🏦</text></svg>");
    }
    @GetMapping("/admin/users") public String adminUsers() { return "admin/users"; }
    @GetMapping("/admin/transactions") public String adminTransactions() { return "admin/transactions"; }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("access_token", null);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0);
        accessCookie.setHttpOnly(true);
        response.addCookie(accessCookie);
        Cookie refreshCookie = new Cookie("refresh_token", null);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0);
        refreshCookie.setHttpOnly(true);
        response.addCookie(refreshCookie);
    }
}
