package com.learninghub.schedule.repo;

import java.util.List;
import java.util.Optional;

import com.learninghub.schedule.domain.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    List<ClassSession> findByTermIdOrderByStartTimeAscCourseIdAsc(String termId);
    List<ClassSession> findAllByOrderByTermIdAscStartTimeAscCourseIdAsc();
    Optional<ClassSession> findFirstByCourseId(String courseId);
}
