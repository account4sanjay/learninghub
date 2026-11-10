package com.learninghub.schedule.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "term")
@Getter
@Setter
@NoArgsConstructor
public class Term {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "is_current", nullable = false)
    private boolean current;
    @Column(name = "max_credits", nullable = false)
    private int maxCredits = 30;
    private Integer quarter;
    @Column(name = "academic_year")
    private String academicYear;
}
