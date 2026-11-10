package com.learninghub.enrollment.web;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.learninghub.enrollment.common.CurrentUser;
import com.learninghub.enrollment.common.Fmt;
import com.learninghub.enrollment.common.Remote;
import com.learninghub.enrollment.common.Remote.Slot;
import com.learninghub.enrollment.common.Remote.Term;
import com.learninghub.enrollment.domain.Course;
import com.learninghub.enrollment.domain.Enrollment;
import com.learninghub.enrollment.domain.GradeLevel;
import com.learninghub.enrollment.repo.CourseRepository;
import com.learninghub.enrollment.repo.EnrollmentRepository;
import com.learninghub.enrollment.repo.GradeLevelRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enrolment roster: the current term's subjects with their enrolled students.
 * Admins see every subject, teachers only the ones they teach; students get 403.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/enrollment/roster")
@PreAuthorize("hasAnyRole('admin','teacher')")
@Tag(name = "Roster", description = "Current enrolment by subject (admin: all subjects, teacher: own subjects)")
public class RosterController {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final GradeLevelRepository gradeLevels;
    private final Remote remote;

    @Operation(summary = "Enrolment roster by subject",
            description = "Every subject of the current term (admin) or the caller's subjects (teacher), ordered by grade level then name, "
                    + "each with the enrolled students. Days and time come from schedule-service. Students receive 403.")
    @ApiResponse(responseCode = "200", description = "Subjects with their enrolled students",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "roster", value = Examples.ROSTER)))
    @ApiResponse(responseCode = "403", description = "Caller is a student", content = @Content)
    @GetMapping
    public List<Map<String, Object>> roster() {
        Term t = remote.currentTerm();
        Map<String, GradeLevel> grades = new LinkedHashMap<>();
        for (GradeLevel g : gradeLevels.findAllByOrderBySortOrderAsc()) grades.put(g.getId(), g);
        List<Course> list = CurrentUser.isAdmin()
                ? courses.findByTermIdOrderByNameAscGradeLevelAsc(t.id())
                : courses.findByTermIdAndTeacherUsernameOrderByNameAscGradeLevelAsc(t.id(), CurrentUser.username());
        List<Course> sorted = new ArrayList<>(list);
        sorted.sort(Comparator
                .comparingInt((Course c) -> grades.containsKey(c.getGradeLevel()) ? grades.get(c.getGradeLevel()).getSortOrder() : Integer.MAX_VALUE)
                .thenComparing(Course::getName, String.CASE_INSENSITIVE_ORDER));
        Map<String, Slot> slots = remote.slots();
        Map<String, String> names = remote.teacherNames();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Course c : sorted) {
            List<Map<String, Object>> students = new ArrayList<>();
            List<Enrollment> es = enrollments.findByCourseId(c.getId());
            es.sort(Comparator.comparing(Enrollment::getEnrolledAt).thenComparing(Enrollment::getUsername));
            for (Enrollment e : es) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("username", e.getUsername());
                s.put("enrolledAt", e.getEnrolledAt() == null ? null : e.getEnrolledAt().toString());
                students.add(s);
            }
            GradeLevel g = grades.get(c.getGradeLevel());
            Slot s = slots.get(c.getId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("name", c.getName());
            m.put("gradeLevel", c.getGradeLevel()); m.put("gradeLevelName", g == null ? c.getGradeLevel() : g.getName());
            m.put("teacher", remote.teacherName(names, c.getTeacherUsername())); m.put("teacherUsername", c.getTeacherUsername());
            m.put("days", s == null ? List.of() : s.days()); m.put("time", s == null ? "" : Fmt.timeRange(s.startTime(), s.endTime()));
            m.put("studentCount", students.size()); m.put("students", students);
            out.add(m);
        }
        return out;
    }
}
