package com.learninghub.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A subject offered to one grade level in one term — the catalogue entry students enrol in (Ask 46).
 * The meeting slot (days, hour) is owned by schedule-service; the teacher's display name by user-service.
 */
@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
public class Course {
    @Id
    private String id;

    /** logical reference → schedule-service term.id */
    @Column(name = "term_id", nullable = false)
    private String termId;

    @Column(nullable = false)
    private String name;

    /** grade_level.id, e.g. 'junior' */
    @Column(name = "grade_level", nullable = false)
    private String gradeLevel;

    /** logical reference → user-service profile.username */
    @Column(name = "teacher_username", nullable = false)
    private String teacherUsername;

    @Column(nullable = false)
    private int credits = 4;

    @Column(nullable = false)
    private String color = "blue";
}
