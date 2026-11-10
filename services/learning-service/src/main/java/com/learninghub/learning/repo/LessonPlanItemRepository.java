package com.learninghub.learning.repo;

import java.util.Collection;
import java.util.List;

import com.learninghub.learning.domain.LessonPlanItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonPlanItemRepository extends JpaRepository<LessonPlanItem, Long> {
    List<LessonPlanItem> findByCourseIdOrderByClassNumberAsc(String courseId);
    List<LessonPlanItem> findByCourseIdInOrderByClassDateAscClassNumberAsc(Collection<String> courseIds);
}
