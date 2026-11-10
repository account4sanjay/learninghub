package com.learninghub.assessment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.learninghub.assessment.common.Courses;
import com.learninghub.assessment.common.Courses.Course;
import com.learninghub.assessment.common.CurrentUser;
import com.learninghub.assessment.common.NotFoundException;
import com.learninghub.assessment.domain.Assessment;
import com.learninghub.assessment.domain.Attempt;
import com.learninghub.assessment.domain.Question;
import com.learninghub.assessment.repo.AssessmentRepository;
import com.learninghub.assessment.repo.AttemptRepository;
import com.learninghub.assessment.repo.QuestionRepository;
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
 * Teacher authoring: create / read / update / delete an assessment together with its
 * multiple-choice questions. Teachers may only touch the assessments they authored (created_by);
 * admins may read any. Ask 46: an assessment belongs to a course (courseId, one of the teacher's
 * classes from enrollment-service); subject, grade level, teacher name and colour come from the course.
 */
@RestController
@Tag(name = "Teacher assessments", description = "Create, edit and delete assessments with their questions for one of the teacher's courses (teacher: own; admin: read any)")
@RequiredArgsConstructor
@RequestMapping("/api/assessment/teacher/assessments")
@PreAuthorize("hasAnyRole('teacher','admin')")
public class TeacherAssessmentController {

    private static final Set<String> TYPES = Set.of("Quiz", "Test", "Assignment", "Practice");

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final Courses courses;

    @Schema(description = "One multiple-choice option")
    public record OptionIn(@Schema(example = "A") String key, @Schema(example = "y = 2x + 1") String text) {}

    @Schema(description = "One question of the assessment")
    public record QuestionIn(@Schema(example = "Which equation has slope 2 and intercept 1?") String text,
                             List<OptionIn> options,
                             @Schema(example = "A") String correct,
                             @Schema(example = "1") Integer points,
                             @Schema(example = "Linear equations") String topic,
                             @Schema(example = "Multiple Choice") String section,
                             @Schema(example = "Slope-intercept form is y = mx + b.") String explanation) {}

    @Schema(description = "Assessment to create or replace (questions replace the existing ones; attempts are kept)")
    public record AssessmentIn(@Schema(example = "Unit 3 Quiz") String title,
                               @Schema(example = "Linear Functions") String subtitle,
                               @Schema(example = "ap-precalc", description = "course id — one of the caller's classes (GET /api/enrollment/my-classes); subject, grade level and colour follow the course") String courseId,
                               @Schema(example = "Quiz", description = "Quiz | Test | Assignment | Practice") String type,
                               @Schema(example = "2026-10-05T23:59:00") LocalDateTime dueAt,
                               @Schema(example = "100") Integer points,
                               @Schema(example = "1") Integer attemptsAllowed,
                               @Schema(example = "30") Integer timeLimitMinutes,
                               @Schema(example = "Answer every question; one attempt.") String instructionsIntro,
                               List<String> instructionsSteps,
                               List<QuestionIn> questions) {}

    // ------------------------------------------------------------------ helpers

    private static void bad(String msg) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }

    private Assessment owned(String id) {
        Assessment a = assessments.findById(id).orElseThrow(() -> new NotFoundException("assessment", id));
        if (!CurrentUser.isAdmin() && !CurrentUser.username().equals(a.getCreatedBy()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your assessment");
        return a;
    }

    private static String slug(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
        return n.toLowerCase(Locale.US).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    /** The course the body names; it must be one of the caller's own classes (enrollment-service /my-classes). */
    private Course courseOf(AssessmentIn in, Map<String, Course> mine) {
        if (in.courseId() == null || in.courseId().isBlank()) bad("courseId is required");
        Course c = mine.get(in.courseId().trim());
        if (c == null) bad("courseId must be one of your classes: " + mine.keySet());
        return c;
    }

    private void validate(AssessmentIn in) {
        if (in.title() == null || in.title().isBlank()) bad("title is required");
        if (in.type() == null || !TYPES.contains(in.type())) bad("type must be one of " + TYPES);
        if (in.dueAt() == null) bad("dueAt is required (ISO date-time)");
        int n = 0;
        for (QuestionIn q : in.questions() == null ? List.<QuestionIn>of() : in.questions()) {
            n++;
            if (q.text() == null || q.text().isBlank()) bad("question " + n + ": text is required");
            if (q.options() == null || q.options().size() < 2) bad("question " + n + ": at least 2 options");
            if (q.correct() == null || q.options().stream().noneMatch(o -> q.correct().equalsIgnoreCase(o.key())))
                bad("question " + n + ": correct must be one of the option keys");
        }
    }

    private void apply(Assessment a, AssessmentIn in, Course c) {
        a.setTitle(in.title().trim()); a.setSubtitle(in.subtitle() == null ? "" : in.subtitle().trim());
        a.setCourseId(c.id()); a.setType(in.type());
        a.setDueAt(in.dueAt()); a.setPoints(in.points() == null ? 100 : in.points());
        a.setAttemptsAllowed(in.attemptsAllowed() == null ? 1 : in.attemptsAllowed());
        a.setTimeLimitMinutes(in.timeLimitMinutes());
        a.setInstructionsIntro(in.instructionsIntro());
        a.setInstructionsSteps(in.instructionsSteps() == null ? new ArrayList<>() : new ArrayList<>(in.instructionsSteps()));
    }

    private void replaceQuestions(Assessment a, List<QuestionIn> qs) {
        questions.deleteAll(questions.findByAssessmentIdOrderByNumberAsc(a.getId()));
        questions.flush(); // the (assessment_id, number) unique key must be free before re-inserting
        int n = 0;
        for (QuestionIn in : qs == null ? List.<QuestionIn>of() : qs) {
            Question q = new Question();
            q.setAssessmentId(a.getId()); q.setNumber(++n);
            q.setSection(in.section() == null || in.section().isBlank() ? "Multiple Choice" : in.section());
            q.setPoints(in.points() == null ? 1 : in.points()); q.setKind("choice"); q.setText(in.text().trim());
            List<Map<String, Object>> opts = new ArrayList<>();
            for (OptionIn o : in.options()) { Map<String, Object> m = new LinkedHashMap<>(); m.put("key", o.key()); m.put("text", o.text()); opts.add(m); }
            q.setOptions(opts); q.setCorrect(in.correct().toUpperCase(Locale.US)); q.setExplanation(in.explanation()); q.setTopic(in.topic());
            questions.save(q);
        }
    }

    private Map<String, Object> form(Assessment a, Course c) {
        List<Question> qs = questions.findByAssessmentIdOrderByNumberAsc(a.getId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId()); m.put("title", a.getTitle()); m.put("subtitle", a.getSubtitle()); m.put("courseId", a.getCourseId()); m.put("subject", c.name());
        m.put("gradeLevel", c.gradeLevel()); m.put("type", a.getType()); m.put("teacher", c.teacher()); m.put("teacherUsername", a.getCreatedBy());
        m.put("color", c.color()); m.put("dueAt", a.getDueAt()); m.put("points", a.getPoints()); m.put("attemptsAllowed", a.getAttemptsAllowed());
        m.put("timeLimitMinutes", a.getTimeLimitMinutes()); m.put("instructionsIntro", a.getInstructionsIntro()); m.put("instructionsSteps", a.getInstructionsSteps());
        List<Map<String, Object>> ql = new ArrayList<>();
        for (Question q : qs) ql.add(ReviewSupport.questionJson(q, qs.size(), true));
        m.put("questions", ql);
        m.put("submissions", attempts.findByAssessmentIdInAndStatusOrderByCompletedAtDesc(List.of(a.getId()), "Completed").size());
        return m;
    }

    // ------------------------------------------------------------------ endpoints

    @Operation(summary = "Create an assessment with its questions", description = "Teacher only. Body names the courseId (one of the caller's classes); id = slug of the title + the course's grade level (numeric suffix if taken); createdBy = the caller. Questions are numbered in order. 400 on missing title/subject/gradeLevel/type/dueAt, fewer than 2 options, or a correct key not among the options.")
    @ApiResponse(responseCode = "201", description = "Created", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.TEACHER_ASSESSMENT)))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    @PostMapping
    @PreAuthorize("hasRole('teacher')")   // Ask 45: admins view, they do not author
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> create(@RequestBody AssessmentIn in) {
        validate(in);
        Map<String, Course> mine = courses.mine();
        Course c = courseOf(in, mine);
        String base = slug(in.title() + "-" + (c.gradeLevel().isEmpty() ? c.id() : c.gradeLevel()));
        String id = base; int i = 2;
        while (assessments.existsById(id)) id = base + "-" + i++;
        Assessment a = new Assessment();
        a.setId(id); a.setCreatedBy(CurrentUser.username());
        apply(a, in, c);
        a = assessments.save(a);
        replaceQuestions(a, in.questions());
        return form(a, c);
    }

    @Operation(summary = "Editable form of an assessment", description = "Fields (courseId plus the course's subject, grade level, teacher and colour) and every question including correct answers and explanations, and the submission count. Teacher: own only; admin: any.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.TEACHER_ASSESSMENT)))
    @ApiResponse(responseCode = "403", description = "Not the owner", content = @Content)
    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable String id) {
        Assessment a = owned(id);
        return form(a, Courses.of(courses.mine(), a.getCourseId()));
    }

    @Operation(summary = "Replace an assessment and its questions", description = "Same body as create. Attempts already made are kept. Teacher: own only; admin: any.")
    @ApiResponse(responseCode = "200", description = "Updated", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.TEACHER_ASSESSMENT)))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('teacher')")
    @Transactional
    public Map<String, Object> update(@PathVariable String id, @RequestBody AssessmentIn in) {
        Assessment a = owned(id);
        validate(in);
        Map<String, Course> mine = courses.mine();
        Course c = courseOf(in, mine);
        apply(a, in, c);
        a = assessments.save(a);
        replaceQuestions(a, in.questions());
        return form(a, c);
    }

    @Operation(summary = "Delete an assessment", description = "Removes the assessment, its questions and all attempts. Teacher: own only; admin: any.")
    @ApiResponse(responseCode = "204", description = "Deleted", content = @Content)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('teacher')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable String id) {
        Assessment a = owned(id);
        List<Attempt> ats = attempts.findByAssessmentIdInAndStatusOrderByCompletedAtDesc(List.of(a.getId()), "Completed");
        attempts.deleteAll(ats);
        attempts.deleteAll(attempts.findAll().stream().filter(x -> a.getId().equals(x.getAssessmentId())).toList());
        questions.deleteAll(questions.findByAssessmentIdOrderByNumberAsc(a.getId()));
        assessments.delete(a);
    }
}
