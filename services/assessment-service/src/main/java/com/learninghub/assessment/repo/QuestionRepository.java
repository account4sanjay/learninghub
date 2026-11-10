package com.learninghub.assessment.repo;

import java.util.List;

import com.learninghub.assessment.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByAssessmentIdOrderByNumberAsc(String assessmentId);
    long countByAssessmentId(String assessmentId);
}
