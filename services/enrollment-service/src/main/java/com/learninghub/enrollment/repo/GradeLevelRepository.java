package com.learninghub.enrollment.repo;

import java.util.List;

import com.learninghub.enrollment.domain.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeLevelRepository extends JpaRepository<GradeLevel, String> {
    List<GradeLevel> findAllByOrderBySortOrderAsc();
}
