package com.learninghub.assessment.repo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.learninghub.assessment.domain.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    List<Attempt> findByUsernameAndStatusOrderByCompletedAtDesc(String username, String status);
    List<Attempt> findByUsernameAndStatusInOrderByCompletedAtDesc(String username, Collection<String> statuses);
    List<Attempt> findByUsernameOrderByIdDesc(String username);
    List<Attempt> findByStatusOrderByCompletedAtDesc(String status);
    List<Attempt> findByStatusInOrderByCompletedAtDesc(Collection<String> statuses);
    List<Attempt> findByAssessmentIdInAndStatusOrderByCompletedAtDesc(Collection<String> assessmentIds, String status);
    List<Attempt> findByAssessmentIdInAndStatusInOrderByCompletedAtDesc(Collection<String> assessmentIds, Collection<String> statuses);
    Optional<Attempt> findFirstByAssessmentIdAndUsernameAndStatusOrderByCompletedAtDesc(String assessmentId, String username, String status);
    Optional<Attempt> findFirstByAssessmentIdAndUsernameAndStatusInOrderByCompletedAtDesc(String assessmentId, String username, Collection<String> statuses);
    Optional<Attempt> findFirstByAssessmentIdAndUsernameAndStatus(String assessmentId, String username, String status);
    List<Attempt> findByAssessmentIdAndUsernameOrderByIdDesc(String assessmentId, String username);
    long countByAssessmentIdAndUsernameAndStatusIn(String assessmentId, String username, Collection<String> statuses);
}
