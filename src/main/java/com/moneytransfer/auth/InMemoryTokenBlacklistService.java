package com.moneytransfer.auth;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Primary
public class InMemoryTokenBlacklistService extends TokenBlacklistService {

    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    public InMemoryTokenBlacklistService() {
        super(null);
    }

    @Override
    public void blacklistToken(String token, long ttlMillis) {
        blacklist.put(token, System.currentTimeMillis() + ttlMillis);
    }

    @Override
    public boolean isBlacklisted(String token) {
        Long expiry = blacklist.get(token);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }
}
