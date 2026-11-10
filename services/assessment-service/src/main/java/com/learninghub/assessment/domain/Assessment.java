package com.learninghub.assessment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A quiz, test, assignment or practice item of one course (Ask 46: course_id is a logical reference to
 * enrollment-service course.id; subject name, grade level, teacher and colour are read from the course,
 * never stored here). created_by is the authoring teacher's username.
 */
@Entity
@Table(name = "assessment")
@Getter
@Setter
@NoArgsConstructor
public class Assessment {
    @Id
    private String id;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(nullable = false)
    private String title;
    private String subtitle;
    @Column(nullable = false)
    private String type;
    @Column(name = "due_at", nullable = false)
    private LocalDateTime dueAt;
    @Column(nullable = false)
    private int points = 100;
    @Column(name = "attempts_allowed", nullable = false)
    private int attemptsAllowed = 1;
    @Column(name = "time_limit_minutes")
    private Integer timeLimitMinutes;
    @Column(name = "instructions_intro")
    private String instructionsIntro;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instructions_steps", columnDefinition = "jsonb", nullable = false)
    private List<String> instructionsSteps = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> files = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> rubric = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_learning", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> relatedLearning = new ArrayList<>();
    @Column(name = "created_by", nullable = false)
    private String createdBy;
}
