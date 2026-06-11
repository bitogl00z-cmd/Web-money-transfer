package com.moneytransfer.recent;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RecentTransferService {
    private final RecentTransferRepository repository;

    public RecentTransferService(RecentTransferRepository repository) {
        this.repository = repository;
    }

    public void recordTransfer(Long userId, Long toAccountId, String toAccountNumber, String toAccountName) {
        List<RecentTransfer> existing = repository.findTop5ByUserIdOrderByTransferredAtDesc(userId);
        for (RecentTransfer rt : existing) {
            if (rt.getToAccountId().equals(toAccountId)) {
                repository.delete(rt);
            }
        }
        repository.save(new RecentTransfer(userId, toAccountId, toAccountNumber, toAccountName));
    }

    public List<RecentTransfer> getRecentTransfers(Long userId) {
        return repository.findTop5ByUserIdOrderByTransferredAtDesc(userId);
    }
}
