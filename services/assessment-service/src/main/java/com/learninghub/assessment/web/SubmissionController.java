package com.learninghub.assessment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.learninghub.assessment.common.Courses;
import com.learninghub.assessment.common.Courses.Course;
import com.learninghub.assessment.common.CurrentUser;
import com.learninghub.assessment.common.Fmt;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Finished attempts (Completed · Submitted · Graded) on a teacher's assessments (admin: all), per-attempt review and assignment grading. */
@RestController
@Tag(name = "Submissions", description = "Students' finished attempts on the teacher's assessments (admin: all), the per-attempt review and assignment grading")
@RequiredArgsConstructor
@RequestMapping("/api/assessment/submissions")
@PreAuthorize("hasAnyRole('teacher','admin')")
public class SubmissionController {

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final Courses courses;

    /** Assessments the caller may see submissions for, keyed by id (admin: all; teacher: authored by them). */
    private Map<String, Assessment> visible() {
        List<Assessment> all = CurrentUser.isAdmin() ? assessments.findAll()
                : assessments.findByCreatedByOrderByDueAtAsc(CurrentUser.username());
        return all.stream().collect(Collectors.toMap(Assessment::getId, a -> a, (x, y) -> x, LinkedHashMap::new));
    }

    private Attempt owned(Long attemptId, Map<String, Assessment> vis) {
        Attempt at = attempts.findById(attemptId).orElseThrow(() -> new NotFoundException("attempt", attemptId));
        if (!vis.containsKey(at.getAssessmentId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your assessment");
        return at;
    }

    @Operation(summary = "Submissions", description = "Finished attempts (Completed quiz/test, Submitted and Graded assignments) on the caller's assessments (admin: every assessment), newest first; optionally filtered to one assessment. Submitted rows have no score yet.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBMISSIONS)))
    @GetMapping
    public List<Map<String, Object>> list(@Parameter(description = "assessment id", example = "unit2-quiz") @RequestParam(required = false) String assessment) {
        Map<String, Assessment> vis = visible();
        if (assessment != null && !vis.containsKey(assessment)) {
            if (assessments.existsById(assessment)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not your assessment");
            throw new NotFoundException("assessment", assessment);
        }
        List<String> ids = assessment == null ? new ArrayList<>(vis.keySet()) : List.of(assessment);
        Map<String, Long> totals = new LinkedHashMap<>();
        List<Map<String, Object>> out = new ArrayList<>();
        if (ids.isEmpty()) return out;
        Map<String, Course> mine = courses.mine();
        for (Attempt at : attempts.findByAssessmentIdInAndStatusInOrderByCompletedAtDesc(ids, Attempt.FINISHED)) {
            Assessment a = vis.get(at.getAssessmentId());
            long total = totals.computeIfAbsent(a.getId(), questions::countByAssessmentId);
            boolean pending = Attempt.SUBMITTED.equals(at.getStatus());
            Map<String, Object> m = new LinkedHashMap<>();
            Course c = Courses.of(mine, a.getCourseId());
            m.put("attemptId", at.getId()); m.put("assessmentId", a.getId()); m.put("title", a.getTitle()); m.put("courseId", a.getCourseId()); m.put("subject", c.name());
            m.put("type", a.getType()); m.put("gradeLevel", c.gradeLevel()); m.put("student", at.getUsername());
            m.put("status", at.getStatus()); m.put("completedOn", Fmt.dateTimeAt(at.getCompletedAt()));
            m.put("score", pending ? "Pending" : at.getPercent() + "%"); m.put("percent", pending ? null : at.getPercent());
            m.put("correct", at.getCorrect()); m.put("incorrect", at.getIncorrect()); m.put("skipped", at.getSkipped()); m.put("total", total);
            m.put("secondsTaken", at.getSecondsTaken()); m.put("autoSubmitted", at.isAutoSubmitted());
            m.put("points", a.getPoints()); m.put("pointsAwarded", at.getPointsAwarded()); m.put("files", at.getSubmissionFiles().size());
            out.add(m);
        }
        return out;
    }

    @Operation(summary = "Review one submission", description = "The review shape of that attempt (results, every question with chosen vs correct; for assignments the submission text, files, points and feedback) plus the student's username.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBMISSION_REVIEW)))
    @ApiResponse(responseCode = "403", description = "Not your assessment", content = @Content)
    @GetMapping("/{attemptId}")
    public Map<String, Object> review(@PathVariable Long attemptId) {
        Map<String, Assessment> vis = visible();
        Attempt at = owned(attemptId, vis);
        Assessment a = vis.get(at.getAssessmentId());
        List<Question> qs = questions.findByAssessmentIdOrderByNumberAsc(a.getId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", at.getId()); out.put("student", at.getUsername());
        out.putAll(ReviewSupport.review(a, Courses.of(courses.mine(), a.getCourseId()), at, qs));
        return out;
    }

    @Operation(summary = "Grade an assignment submission", description = "Teacher (own assessments). Body: {\"points\": 88, \"feedback\": \"…\"}; points 0..assessment.points. The attempt becomes Graded with percent = points / assessment.points; the student sees the points and feedback on the details page. Re-grading is allowed.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.GRADE_BODY)))
    @ApiResponse(responseCode = "200", description = "Graded", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.GRADE)))
    @ApiResponse(responseCode = "403", description = "Not your assessment", content = @Content)
    @ApiResponse(responseCode = "409", description = "Not an assignment submission", content = @Content)
    @PostMapping("/{attemptId}/grade")
    @PreAuthorize("hasRole('teacher')")   // Ask 45: grading is a teacher write; admin views
    public Map<String, Object> grade(@PathVariable Long attemptId, @RequestBody Map<String, Object> body) {
        Map<String, Assessment> vis = visible();
        Attempt at = owned(attemptId, vis);
        Assessment a = vis.get(at.getAssessmentId());
        if (!Attempt.SUBMITTED.equals(at.getStatus()) && !Attempt.GRADED.equals(at.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only assignment submissions are graded by hand");
        if (!(body.get("points") instanceof Number n)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "points is required");
        int points = n.intValue();
        if (points < 0 || points > a.getPoints()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "points must be between 0 and " + a.getPoints());
        at.setPointsAwarded(points);
        at.setPercent(a.getPoints() == 0 ? 0 : (int) Math.round(100.0 * points / a.getPoints()));
        at.setFeedback(body.get("feedback") == null ? null : String.valueOf(body.get("feedback")).trim());
        at.setGradedBy(CurrentUser.username()); at.setGradedAt(LocalDateTime.now()); at.setStatus(Attempt.GRADED);
        at = attempts.save(at);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", at.getId()); out.put("student", at.getUsername()); out.put("status", at.getStatus());
        out.put("pointsAwarded", points); out.put("points", a.getPoints()); out.put("percent", at.getPercent());
        out.put("feedback", at.getFeedback()); out.put("gradedBy", at.getGradedBy()); out.put("gradedAt", Fmt.dateTimeAt(at.getGradedAt()));
        return out;
    }
}
