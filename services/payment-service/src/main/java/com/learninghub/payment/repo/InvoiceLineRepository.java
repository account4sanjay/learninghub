package com.learninghub.payment.repo;

import java.util.List;

import com.learninghub.payment.domain.InvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceLineRepository extends JpaRepository<InvoiceLine, Long> {
    List<InvoiceLine> findByInvoiceIdOrderByIdAsc(Long invoiceId);
    void deleteByInvoiceId(Long invoiceId);
}
