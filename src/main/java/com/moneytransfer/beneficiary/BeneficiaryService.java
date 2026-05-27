package com.moneytransfer.beneficiary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public List<Beneficiary> getUserBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserId(userId);
    }

    @Transactional
    public Beneficiary addBeneficiary(Long userId, String accountNumber, String nickname) {
        Beneficiary beneficiary = new Beneficiary(userId, accountNumber, nickname);
        return beneficiaryRepository.save(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(Long id, Long userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        if (!beneficiary.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized");
        }
        beneficiaryRepository.delete(beneficiary);
    }
}
