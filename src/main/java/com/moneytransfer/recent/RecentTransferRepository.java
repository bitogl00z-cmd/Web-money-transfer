package com.moneytransfer.recent;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecentTransferRepository extends JpaRepository<RecentTransfer, Long> {
    List<RecentTransfer> findTop5ByUserIdOrderByTransferredAtDesc(Long userId);
}
