package com.learninghub.enrollment.repo;

import java.util.List;
import java.util.Optional;

import com.learninghub.enrollment.domain.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByUsernameOrderByEnrolledAtAsc(String username);
    Optional<Enrollment> findByCourseIdAndUsername(String courseId, String username);
    List<Enrollment> findByCourseId(String courseId);
    long countByCourseId(String courseId);
}
