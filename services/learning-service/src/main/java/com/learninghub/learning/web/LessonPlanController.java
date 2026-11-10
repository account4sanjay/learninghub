package com.learninghub.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.learninghub.learning.common.CurrentUser;
import com.learninghub.learning.common.Catalog;
import com.learninghub.learning.common.Catalog.Course;
import com.learninghub.learning.common.NotFoundException;
import com.learninghub.learning.domain.LessonPlanItem;
import com.learninghub.learning.repo.LessonPlanItemRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Term lesson plan of a course (Ask 25): the same list for the enrolled students and the teacher; the teacher
 * (own courses) marks a class complete and every student sees it. Course percent/status are computed from the plan
 * (Ask 46: the course itself is owned by enrollment-service and resolved over REST).
 */
@RestController
@Tag(name = "Lesson plan", description = "Per-course term lesson plan (one item per scheduled class); teacher/admin mark classes complete")
@RequestMapping("/api/learning/plan")
public class LessonPlanController {

    private final LessonPlanItemRepository items;
    private final Catalog catalog;
    private final LearningController learning;

    public LessonPlanController(LessonPlanItemRepository items, Catalog catalog, LearningController learning) {
        this.items = items; this.catalog = catalog; this.learning = learning;
    }

    @Operation(summary = "Lesson plan of a course", description = "Every scheduled class of the course in the current term with number, date, unit, topic and completion; counts, percent and the next class. Student: enrolled courses only; teacher: own courses; admin: all (403 otherwise). Example id: ap-precalc.")
    @ApiResponse(responseCode = "200", description = "Plan", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.PLAN)))
    @ApiResponse(responseCode = "403", description = "Not enrolled / not your course", content = @Content)
    @GetMapping("/{courseId}")
    public Map<String, Object> plan(@PathVariable String courseId) {
        Course c = catalog.visibleById().get(courseId);   // Ask 46: enrollment-service decides what the caller may see
        if (c == null) {
            if (CurrentUser.isAdmin()) throw new NotFoundException("course", courseId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CurrentUser.isTeacher() ? "not your course" : "not enrolled in this course");
        }
        return learning.planJson(c, true);
    }

    @Operation(summary = "Mark a class complete / not complete", description = "Teacher of the course only. Body {\"completed\": true|false}. Records who/when, recomputes the course percent and status; every enrolled student sees the change.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"completed\": true}")))
    @ApiResponse(responseCode = "200", description = "Updated item + course progress", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.PLAN_ITEM_SET)))
    @ApiResponse(responseCode = "403", description = "Not your course", content = @Content)
    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('teacher')")
    public Map<String, Object> setCompleted(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LessonPlanItem it = items.findById(id).orElseThrow(() -> new NotFoundException("lesson plan item", id));
        Course c = catalog.visibleById().get(it.getCourseId());   // teacher → own courses only (from enrollment-service)
        if (c == null || !CurrentUser.username().equals(c.teacherUsername()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your course");
        boolean completed = Boolean.TRUE.equals(body.get("completed")) || "true".equals(String.valueOf(body.get("completed")));
        it.setCompleted(completed);
        it.setCompletedAt(completed ? LocalDateTime.now() : null);
        it.setCompletedBy(completed ? CurrentUser.username() : null);
        items.save(it);
        List<LessonPlanItem> all = learning.planOf(c.id());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("item", learning.itemJson(it, LocalDate.now()));
        out.put("courseId", c.id()); out.put("coursePercent", LearningController.percentOf(all)); out.put("courseStatus", LearningController.statusOf(all));
        out.put("completed", all.stream().filter(LessonPlanItem::isCompleted).count()); out.put("total", all.size());
        return out;
    }
}
