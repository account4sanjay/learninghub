package com.learninghub.payment.repo;

import java.util.List;
import java.util.Optional;

import com.learninghub.payment.domain.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    List<PaymentMethod> findByUsernameOrderByIsDefaultDescIdAsc(String username);
    Optional<PaymentMethod> findFirstByUsernameAndIsDefaultTrue(String username);
}
