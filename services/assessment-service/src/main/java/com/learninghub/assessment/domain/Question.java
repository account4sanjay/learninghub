package com.learninghub.assessment.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One question of an assessment. */
@Entity
@Table(name = "question")
@Getter
@Setter
@NoArgsConstructor
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assessment_id", nullable = false)
    private String assessmentId;
    @Column(nullable = false)
    private int number;
    @Column(nullable = false)
    private String section = "Multiple Choice";
    @Column(nullable = false)
    private int points = 1;
    @Column(nullable = false)
    private String kind = "choice";
    @Column(nullable = false)
    private String text;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> options = new ArrayList<>();
    @Column(nullable = false)
    private String correct;
    private String explanation;
    private String topic;
}
