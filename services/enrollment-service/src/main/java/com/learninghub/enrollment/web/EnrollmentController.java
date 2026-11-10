package com.learninghub.enrollment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.learninghub.enrollment.common.CurrentUser;
import com.learninghub.enrollment.common.Fmt;
import com.learninghub.enrollment.common.NotFoundException;
import com.learninghub.enrollment.common.Remote;
import com.learninghub.enrollment.common.Remote.Slot;
import com.learninghub.enrollment.common.Remote.Term;
import com.learninghub.enrollment.domain.Course;
import com.learninghub.enrollment.domain.Enrollment;
import com.learninghub.enrollment.domain.GradeLevel;
import com.learninghub.enrollment.repo.CourseRepository;
import com.learninghub.enrollment.repo.EnrollmentRepository;
import com.learninghub.enrollment.repo.GradeLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * View + action endpoints for the Schedule Classes / My Classes pages, computed from course and
 * enrollment rows for the calling user. Ask 46: the term and the meeting slot of each course come from
 * schedule-service and the teacher's display name from user-service (read per request, token forwarded).
 */
@RestController
@Tag(name = "Enrollment views", description = "Course catalogue, the caller's classes (role-aware) and enrol/drop actions")
@RequiredArgsConstructor
@RequestMapping("/api/enrollment")
public class EnrollmentController {
    private static final List<String> ALL_DAYS = List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final GradeLevelRepository gradeLevels;
    private final Remote remote;

    /** grade_level.id -> display name ('junior' -> 'Junior') */
    Map<String, String> gradeNames() {
        Map<String, String> m = new LinkedHashMap<>();
        for (GradeLevel g : gradeLevels.findAllByOrderBySortOrderAsc()) m.put(g.getId(), g.getName());
        return m;
    }

    static Map<String, Object> termJson(Term t) {
        return Map.of("name", t.name(), "start", Fmt.date(t.startDate()), "end", Fmt.date(t.endDate()));
    }

    private Set<String> enrolledIds(String user) {
        Set<String> ids = new LinkedHashSet<>();
        for (Enrollment e : enrollments.findByUsernameOrderByEnrolledAtAsc(user)) ids.add(e.getCourseId());
        return ids;
    }

    /** Ask 46: the plain catalogue other services enrich their rows from. Never calls schedule-service (schedule reads this). */
    @Operation(summary = "Courses (catalogue)", description = "Every course of every term with its grade level, teacher (username + display name from user-service), credits and colour — no times (those are schedule-service slots). Read by schedule-, learning-, assessment- and payment-service to resolve a course id.")
    @ApiResponse(responseCode = "200", description = "Courses", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.COURSES)))
    @GetMapping("/courses")
    public List<Map<String, Object>> coursesList() {
        Map<String, String> grades = gradeNames();
        Map<String, String> names = remote.teacherNames();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Course c : courses.findAllByOrderByTermIdAscNameAscGradeLevelAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("termId", c.getTermId()); m.put("name", c.getName());
            m.put("gradeLevel", c.getGradeLevel()); m.put("gradeLevelName", grades.getOrDefault(c.getGradeLevel(), c.getGradeLevel()));
            m.put("teacherUsername", c.getTeacherUsername()); m.put("teacher", remote.teacherName(names, c.getTeacherUsername()));
            m.put("credits", c.getCredits()); m.put("color", c.getColor());
            out.add(m);
        }
        return out;
    }

    /** Browsable catalog with the caller's enrollment status and filter options. */
    @Operation(summary = "Class catalog", description = "Every subject of the current term with the caller's enrolment status (added/available) and the filter options (subjects, days Mon–Sun, teachers, gradeLevels). Days and times come from schedule-service slots.")
    @ApiResponse(responseCode = "200", description = "Catalog", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.CLASSES)))
    @GetMapping("/classes")
    public Map<String, Object> classes() {
        Term t = remote.currentTerm();
        Set<String> mine = enrolledIds(CurrentUser.username());
        List<Course> list = courses.findByTermIdOrderByNameAscGradeLevelAsc(t.id());
        Map<String, String> grades = gradeNames();
        Map<String, String> names = remote.teacherNames();
        Map<String, Slot> slots = remote.slots();
        List<Map<String, Object>> available = new ArrayList<>();
        Map<String, String> subjects = new LinkedHashMap<>();
        Set<String> teachers = new LinkedHashSet<>();
        for (Course c : list) {
            Slot s = slots.get(c.getId());
            String teacher = remote.teacherName(names, c.getTeacherUsername());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId()); m.put("name", c.getName()); m.put("gradeLevel", grades.getOrDefault(c.getGradeLevel(), c.getGradeLevel()));
            m.put("teacher", teacher); m.put("teacherUsername", c.getTeacherUsername());
            m.put("color", c.getColor()); m.put("days", s == null ? List.of() : s.days());
            m.put("start", s == null ? "" : Fmt.time(s.startTime())); m.put("end", s == null ? "" : Fmt.time(s.endTime()));
            m.put("credits", c.getCredits());
            m.put("dates", Fmt.dateShort(s != null && s.startDate() != null ? s.startDate() : t.startDate())
                    + " - " + Fmt.dateShort(s != null && s.endDate() != null ? s.endDate() : t.endDate()));
            m.put("status", mine.contains(c.getId()) ? "added" : "available");
            available.add(m);
            subjects.putIfAbsent(c.getName(), c.getColor());
            teachers.add(teacher);
        }
        List<Map<String, Object>> subjectList = new ArrayList<>();
        subjects.forEach((n, col) -> subjectList.add(Map.of("name", n, "color", col)));
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("subjects", subjectList); filters.put("days", ALL_DAYS);
        filters.put("teachers", new ArrayList<>(teachers));
        filters.put("gradeLevels", new ArrayList<>(grades.values()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("term", termJson(t)); out.put("available", available); out.put("filters", filters);
        return out;
    }

    /**
     * Role-aware class list for the current term:
     * student → enrolled classes; teacher → classes they teach (status "Teaching", studentCount);
     * admin → every class (status "Active", studentCount).
     */
    @Operation(summary = "My classes (role-aware)", description = "student → enrolled classes with credits; teacher → classes taught with studentCount (status Teaching); admin → every class (status Active). Also the role-aware course list every other service starts from.")
    @ApiResponse(responseCode = "200", description = "Shape depends on the caller's role — pick your role in the Examples dropdown", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "student", summary = "role = student (enrolled classes, credits)", value = Examples.MY_CLASSES_STUDENT),
            @ExampleObject(name = "teacher", summary = "role = teacher (classes taught, studentCount)", value = Examples.MY_CLASSES_TEACHER),
            @ExampleObject(name = "admin", summary = "role = admin (every class, studentCount)", value = Examples.MY_CLASSES_ADMIN)}))
    @GetMapping("/my-classes")
    public Map<String, Object> myClasses() {
        Term t = remote.currentTerm();
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("term", termJson(t));
        Map<String, String> grades = gradeNames();
        Map<String, String> names = remote.teacherNames();
        Map<String, Slot> slots = remote.slots();
        if (CurrentUser.isAdmin() || CurrentUser.isTeacher()) {
            List<Course> list = CurrentUser.isAdmin()
                    ? courses.findByTermIdOrderByNameAscGradeLevelAsc(t.id())
                    : courses.findByTermIdAndTeacherUsernameOrderByNameAscGradeLevelAsc(t.id(), CurrentUser.username());
            for (Course c : list) {
                Map<String, Object> m = classJson(c, t, grades, names, slots.get(c.getId()));
                m.put("studentCount", enrollments.countByCourseId(c.getId()));
                m.put("status", CurrentUser.isAdmin() ? "Active" : "Teaching");
                out.add(m);
            }
            res.put("role", CurrentUser.isAdmin() ? "admin" : "teacher");
            res.put("totalCredits", out.size() + (out.size() == 1 ? " class" : " classes"));
            res.put("classes", out);
            return res;
        }
        int credits = 0;
        for (Enrollment e : enrollments.findByUsernameOrderByEnrolledAtAsc(CurrentUser.username())) {
            Course c = courses.findById(e.getCourseId()).orElse(null);
            if (c == null || !c.getTermId().equals(t.id())) continue;
            credits += c.getCredits();
            Map<String, Object> m = classJson(c, t, grades, names, slots.get(c.getId()));
            m.put("status", e.getStatus());
            out.add(m);
        }
        res.put("role", "student");
        res.put("totalCredits", credits + " / " + t.maxCredits()); res.put("classes", out);
        return res;
    }

    private Map<String, Object> classJson(Course c, Term t, Map<String, String> grades, Map<String, String> names, Slot s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName()); m.put("gradeLevel", grades.getOrDefault(c.getGradeLevel(), c.getGradeLevel()));
        m.put("teacher", remote.teacherName(names, c.getTeacherUsername())); m.put("teacherUsername", c.getTeacherUsername());
        m.put("color", c.getColor()); m.put("days", s == null ? List.of() : s.days()); m.put("credits", c.getCredits());
        m.put("time", s == null ? "" : Fmt.timeRange(s.startTime(), s.endTime()));
        m.put("duration", s == null ? "" : Fmt.duration(s.startTime(), s.endTime()));
        m.put("startDate", Fmt.date(s != null && s.startDate() != null ? s.startDate() : t.startDate()));
        m.put("endDate", Fmt.date(s != null && s.endDate() != null ? s.endDate() : t.endDate()));
        return m;
    }

    /** Course ids the caller is enrolled in (students only; empty for other roles). */
    @Operation(summary = "Enrolled course ids", description = "Course ids the calling student is enrolled in; empty for teachers and admins.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.STATE)))
    @GetMapping("/state")
    public Map<String, Object> state() {
        if (!CurrentUser.isStudent()) return Map.of("enrolled", List.of());
        return Map.of("enrolled", new ArrayList<>(enrolledIds(CurrentUser.username())));
    }

    @Operation(summary = "Enrol in a subject (student)", description = "Creates the caller's enrollment for the course id (idempotent). Example courseId: ap-statistics.")
    @ApiResponse(responseCode = "200", description = "Enrolled", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.ENROLLED)))
    @PostMapping("/enroll/{courseId}")
    @PreAuthorize("hasRole('student')")   // Ask 45: enrolling is a student activity
    @Transactional
    public Map<String, Object> enroll(@PathVariable String courseId) {
        courses.findById(courseId).orElseThrow(() -> new NotFoundException("course", courseId));
        String user = CurrentUser.username();
        if (enrollments.findByCourseIdAndUsername(courseId, user).isEmpty()) {
            Enrollment e = new Enrollment();
            e.setCourseId(courseId); e.setUsername(user);
            enrollments.save(e);
        }
        return Map.of("status", "enrolled", "classId", courseId);
    }

    @Operation(summary = "Drop a subject (student)", description = "Removes the caller's enrollment for the course id.")
    @ApiResponse(responseCode = "200", description = "Dropped", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.DROPPED)))
    @DeleteMapping("/drop/{courseId}")
    @PreAuthorize("hasRole('student')")
    @Transactional
    public Map<String, Object> drop(@PathVariable String courseId) {
        courses.findById(courseId).orElseThrow(() -> new NotFoundException("course", courseId));
        enrollments.findByCourseIdAndUsername(courseId, CurrentUser.username()).ifPresent(enrollments::delete);
        return Map.of("status", "dropped", "classId", courseId);
    }
}
