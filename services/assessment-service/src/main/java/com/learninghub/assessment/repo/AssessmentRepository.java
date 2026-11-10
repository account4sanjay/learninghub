package com.learninghub.assessment.repo;

import java.util.Collection;
import java.util.List;

import com.learninghub.assessment.domain.Assessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentRepository extends JpaRepository<Assessment, String> {
    List<Assessment> findAllByOrderByDueAtAsc();
    List<Assessment> findByCreatedByOrderByDueAtAsc(String createdBy);
    List<Assessment> findByCourseIdInOrderByDueAtAsc(Collection<String> courseIds);
}
