package com.learninghub.enrollment.repo;

import java.util.List;

import com.learninghub.enrollment.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, String> {
    List<Course> findByTermIdOrderByNameAscGradeLevelAsc(String termId);
    List<Course> findByTermIdAndTeacherUsernameOrderByNameAscGradeLevelAsc(String termId, String teacherUsername);
    List<Course> findAllByOrderByTermIdAscNameAscGradeLevelAsc();
}
