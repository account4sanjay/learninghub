package com.learninghub.payment.repo;

import java.util.Optional;

import com.learninghub.payment.domain.BillingProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingProfileRepository extends JpaRepository<BillingProfile, Long> {
    Optional<BillingProfile> findByUsername(String username);
}
