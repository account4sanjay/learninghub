package com.learninghub.learning.repo;

import java.util.List;

import com.learninghub.learning.domain.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    List<StudySession> findByUsernameOrderByDayAsc(String username);
    List<StudySession> findByDayGreaterThanEqual(java.time.LocalDate day);
}
