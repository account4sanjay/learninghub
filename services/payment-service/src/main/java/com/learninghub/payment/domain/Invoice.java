package com.learninghub.payment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "invoice")
@Getter
@Setter
@NoArgsConstructor
public class Invoice {
    public static final String PAID = "Paid";
    public static final String UPCOMING = "Upcoming";
    public static final String SCHEDULED = "Scheduled";
    public static final String PENDING = "Pending";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String username;
    /** Logical reference to schedule-service term.id (the term this invoice was raised for). */
    @Column(name = "term_id", nullable = false)
    private String termId;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private BigDecimal amount;
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    @Column(nullable = false)
    private String status = UPCOMING;
    @Column(name = "paid_on")
    private LocalDate paidOn;
    @Column(name = "method_id")
    private Long methodId;

    public boolean isPaid() { return PAID.equals(status); }
}
