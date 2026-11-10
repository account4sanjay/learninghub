package com.learninghub.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A high-school class: Freshman (9), Sophomore (10), Junior (11), Senior (12). */
@Entity
@Table(name = "grade_level")
@Getter
@Setter
@NoArgsConstructor
public class GradeLevel {
    @Id
    private String id;
    @Column(nullable = false)
    private String name;
    @Column(name = "grade_number", nullable = false)
    private int gradeNumber;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
