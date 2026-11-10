package com.learninghub.assessment.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

/**
 * A user's attempt at an assessment.
 * status: InProgress (timer running) · Completed (auto-scored quiz/test/practice)
 *         · Submitted (assignment awaiting grade) · Graded (assignment graded by the teacher).
 */
@Entity
@Table(name = "attempt")
@Getter
@Setter
@NoArgsConstructor
public class Attempt {
    public static final String IN_PROGRESS = "InProgress", COMPLETED = "Completed", SUBMITTED = "Submitted", GRADED = "Graded";
    /** statuses that count as a finished attempt */
    public static final List<String> FINISHED = List.of(COMPLETED, SUBMITTED, GRADED);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "assessment_id", nullable = false)
    private String assessmentId;
    @Column(nullable = false)
    private String username;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "seconds_taken")
    private Integer secondsTaken;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private Map<String, String> answers = new LinkedHashMap<>();
    @Column(nullable = false)
    private int correct;
    @Column(nullable = false)
    private int incorrect;
    @Column(nullable = false)
    private int skipped;
    @Column(nullable = false)
    private int percent;
    @Column(nullable = false)
    private String status = COMPLETED;
    // Ask 20: timer + assignment submission + grading
    @Column(name = "deadline_at")
    private LocalDateTime deadlineAt;
    @Column(name = "auto_submitted", nullable = false)
    private boolean autoSubmitted;
    @Column(name = "submission_text")
    private String submissionText;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "submission_files", columnDefinition = "jsonb", nullable = false)
    private List<Map<String, Object>> submissionFiles = new ArrayList<>();
    @Column(name = "points_awarded")
    private Integer pointsAwarded;
    private String feedback;
    @Column(name = "graded_by")
    private String gradedBy;
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    public boolean isFinished() { return FINISHED.contains(status); }
}
