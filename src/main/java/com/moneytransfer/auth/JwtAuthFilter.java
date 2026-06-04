package com.moneytransfer.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/favicon.ico") || uri.startsWith("/css/") || uri.startsWith("/js/") || uri.startsWith("/img/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            if (blacklistService.isBlacklisted(token)) {
                SecurityContextHolder.clearContext();
            } else {
                try {
                    Claims claims = jwtUtil.validateToken(token);
                    String role = claims.get("role", String.class);
                    List<SimpleGrantedAuthority> authorities = role != null
                            ? List.of(new SimpleGrantedAuthority("ROLE_" + role))
                            : Collections.emptyList();
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
                    auth.setDetails(claims);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception e) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
