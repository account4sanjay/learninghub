package com.learninghub.payment.repo;

import java.util.List;
import java.util.Optional;

import com.learninghub.payment.domain.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUsernameAndStatusOrderByPaidOnDescIdDesc(String username, String status);
    List<Invoice> findByUsernameAndStatusNotOrderByDueDateAscIdAsc(String username, String status);
    Optional<Invoice> findByIdAndUsername(Long id, String username);
    List<Invoice> findByStatusOrderByPaidOnDescIdDesc(String status);
    List<Invoice> findByStatusNotOrderByDueDateAscIdAsc(String status);
    Optional<Invoice> findFirstByUsernameAndDescriptionAndStatusNot(String username, String description, String status);
    Optional<Invoice> findFirstByUsernameAndTermId(String username, String termId);
}
