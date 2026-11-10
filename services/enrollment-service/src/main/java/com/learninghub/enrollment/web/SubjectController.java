package com.learninghub.enrollment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.learninghub.enrollment.common.Fmt;
import com.learninghub.enrollment.common.NotFoundException;
import com.learninghub.enrollment.common.Remote;
import com.learninghub.enrollment.common.Remote.Slot;
import com.learninghub.enrollment.common.Remote.Term;
import com.learninghub.enrollment.domain.Course;
import com.learninghub.enrollment.domain.GradeLevel;
import com.learninghub.enrollment.repo.CourseRepository;
import com.learninghub.enrollment.repo.EnrollmentRepository;
import com.learninghub.enrollment.repo.GradeLevelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Admin management of subjects (the course catalogue): a grade level, a teacher, a quarter term, credits
 * and a colour. Ask 46: the meeting slot (days, hour) is written by the admin Subjects page straight into
 * schedule-service (PUT /api/schedule/sessions/by-course/{id}); this service only shows it.
 */
@RestController
@Tag(name = "Subjects (admin)", description = "Create, update and delete subjects: grade level, teacher, quarter term, credits, colour. The slot (days, hour) is kept in schedule-service and written by the Subjects page.")
@RequiredArgsConstructor
@RequestMapping("/api/enrollment/subjects")
@PreAuthorize("hasRole('admin')")
public class SubjectController {
    private final CourseRepository courses;
    private final EnrollmentRepository enrollments;
    private final GradeLevelRepository gradeLevels;
    private final Remote remote;

    /** gradeLevel is a grade_level.id (freshman | sophomore | junior | senior). */
    @Schema(description = "Subject definition (admin). Days and hour are not here: the page sends them to schedule-service PUT /api/schedule/sessions/by-course/{id}.")
    public record SubjectBody(
            @Schema(description = "Optional id; slugged from name + gradeLevel when omitted", example = "ap-precalc") String id,
            @Schema(example = "AP Pre Calculus") String name,
            @Schema(description = "grade_level.id", example = "junior", allowableValues = {"freshman", "sophomore", "junior", "senior"}) String gradeLevel,
            @Schema(description = "Teacher username from /api/users/teachers", example = "janderson") String teacherUsername,
            @Schema(description = "Term id from /api/schedule/terms", example = "q1-2026") String termId,
            @Schema(example = "4", minimum = "1", maximum = "6") Integer credits,
            @Schema(description = "SPA color token", example = "blue", allowableValues = {"blue", "purple", "green", "orange", "red", "teal", "amber"}) String color) {}

    @Operation(summary = "List subjects", description = "All subjects across terms with grade level, teacher, timetable (from schedule-service) and studentCount.")
    @ApiResponse(responseCode = "200", description = "Subjects", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBJECTS)))
    @GetMapping
    public List<Map<String, Object>> list() {
        Map<String, Term> termById = remote.termsById();
        Map<String, Slot> slots = remote.slots();
        Map<String, String> names = remote.teacherNames();
        Map<String, String> grades = gradeNames();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Course c : courses.findAllByOrderByTermIdAscNameAscGradeLevelAsc()) out.add(json(c, termById.get(c.getTermId()), slots.get(c.getId()), names, grades));
        return out;
    }

    @Operation(summary = "Create a subject", description = "Validates: gradeLevel from /grade-levels, termId from schedule-service /terms, teacherUsername present, credits 1–6. The id is slugged from name + gradeLevel when omitted. Then PUT the slot to /api/schedule/sessions/by-course/{id}.")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBJECT)))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    @ApiResponse(responseCode = "409", description = "Subject id already exists", content = @Content)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> create(@RequestBody SubjectBody b) {
        validate(b);
        String id = b.id() != null && !b.id().isBlank() ? slug(b.id()) : slug(b.name() + "-" + b.gradeLevel());
        if (courses.existsById(id)) throw new ResponseStatusException(HttpStatus.CONFLICT, "subject " + id + " already exists");
        Course c = new Course();
        c.setId(id);
        apply(c, b);
        courses.save(c);
        return json(c, remote.termsById().get(c.getTermId()), remote.slots().get(c.getId()), remote.teacherNames(), gradeNames());
    }

    @Operation(summary = "Update a subject", description = "Same body and validation as create. The slot is updated separately in schedule-service.")
    @ApiResponse(responseCode = "200", description = "Updated", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBJECT)))
    @PutMapping("/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable String id, @RequestBody SubjectBody b) {
        Course c = courses.findById(id).orElseThrow(() -> new NotFoundException("subject", id));
        validate(b);
        apply(c, b);
        courses.save(c);
        return json(c, remote.termsById().get(c.getTermId()), remote.slots().get(c.getId()), remote.teacherNames(), gradeNames());
    }

    @Operation(summary = "Delete a subject", description = "Removes the subject and its enrollments. The page then deletes the slot in schedule-service.")
    @ApiResponse(responseCode = "204", description = "Deleted", content = @Content)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable String id) {
        Course c = courses.findById(id).orElseThrow(() -> new NotFoundException("subject", id));
        enrollments.deleteAll(enrollments.findByCourseId(id));
        courses.delete(c);
    }

    // ---------------------------------------------------------------- helpers

    private void validate(SubjectBody b) {
        if (b.name() == null || b.name().isBlank()) bad("name is required");
        if (b.teacherUsername() == null || b.teacherUsername().isBlank()) bad("teacherUsername is required");
        if (b.termId() == null || !remote.termsById().containsKey(b.termId())) bad("termId must reference an existing term (schedule-service /api/schedule/terms)");
        if (b.gradeLevel() == null || !gradeLevels.existsById(b.gradeLevel()))
            bad("gradeLevel must be one of " + gradeLevels.findAllByOrderBySortOrderAsc().stream().map(GradeLevel::getId).toList());
        if (b.credits() != null && (b.credits() < 1 || b.credits() > 6)) bad("credits must be 1-6");
    }

    private static void bad(String msg) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg);
    }

    private static void apply(Course c, SubjectBody b) {
        c.setName(b.name().trim()); c.setGradeLevel(b.gradeLevel()); c.setTeacherUsername(b.teacherUsername());
        c.setTermId(b.termId());
        if (b.credits() != null) c.setCredits(b.credits());
        if (b.color() != null && !b.color().isBlank()) c.setColor(b.color());
    }

    /** grade_level.id -> display name */
    private Map<String, String> gradeNames() {
        Map<String, String> m = new LinkedHashMap<>();
        for (GradeLevel g : gradeLevels.findAllByOrderBySortOrderAsc()) m.put(g.getId(), g.getName());
        return m;
    }

    private Map<String, Object> json(Course c, Term t, Slot s, Map<String, String> names, Map<String, String> grades) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("name", c.getName()); m.put("gradeLevel", c.getGradeLevel());
        m.put("gradeLevelName", grades.getOrDefault(c.getGradeLevel(), c.getGradeLevel()));
        m.put("teacher", remote.teacherName(names, c.getTeacherUsername())); m.put("teacherUsername", c.getTeacherUsername());
        m.put("termId", c.getTermId()); m.put("termName", t == null ? null : t.name());
        m.put("days", s == null ? List.of() : s.days());
        m.put("startTime", s == null ? null : s.startTime().toString()); m.put("endTime", s == null ? null : s.endTime().toString());
        m.put("time", s == null ? "" : Fmt.timeRange(s.startTime(), s.endTime()));
        m.put("credits", c.getCredits()); m.put("studentCount", enrollments.countByCourseId(c.getId()));
        m.put("color", c.getColor());
        m.put("startDate", Fmt.date(s != null && s.startDate() != null ? s.startDate() : t == null ? null : t.startDate()));
        m.put("endDate", Fmt.date(s != null && s.endDate() != null ? s.endDate() : t == null ? null : t.endDate()));
        return m;
    }

    private static String slug(String s) {
        String out = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return out.isBlank() ? "subject" : out;
    }
}
