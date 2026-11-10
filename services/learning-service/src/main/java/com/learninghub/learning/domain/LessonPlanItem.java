package com.learninghub.learning.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** One scheduled class of a course (Ask 25): number, date, unit + topic, completed by the teacher. course_id is a logical reference to enrollment-service (Ask 46). */
@Entity
@Table(name = "lesson_plan_item")
@Getter
@Setter
@NoArgsConstructor
public class LessonPlanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "course_id", nullable = false)
    private String courseId;
    @Column(name = "class_number", nullable = false)
    private int classNumber;
    @Column(name = "class_date", nullable = false)
    private LocalDate classDate;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private boolean completed;
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    @Column(name = "completed_by")
    private String completedBy;
}
