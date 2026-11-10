package com.learninghub.assessment.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.learninghub.assessment.common.Courses;
import com.learninghub.assessment.common.Courses.Course;
import com.learninghub.assessment.common.CurrentUser;
import com.learninghub.assessment.common.Terms;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * View endpoints for the Assessments, Assessment Details, Quiz Take and Quiz Review
 * pages, computed from assessment / question / attempt rows for the calling user.
 *
 * Ask 20 — attempt workflow (students):
 *   Quiz / Test / Practice: POST /{id}/start (server-side deadline = now + time limit) → GET /{id}/questions
 *   → PUT /{id}/answers (autosave) → POST /{id}/submit (or the server finalises the attempt itself once the
 *   deadline has passed — "auto submitted"). Past-due assessments cannot be started; attempts_allowed is enforced.
 *   Assignment: POST /{id}/assignment (text + file names) before the due date → teacher grades it
 *   (SubmissionController) → the details page shows points and feedback.
 */
@RestController
@Tag(name = "Assessment views", description = "Assessments list/summary (role-aware), details, timed attempts (start · answers · submit), assignment submission and review")
@RequiredArgsConstructor
@RequestMapping("/api/assessment")
public class AssessmentViewController {

    private static final Map<String, String> SHORT_SUBJECT = Map.of("Mathematics", "Math", "Computer Science", "CS");
    private static final int DEFAULT_LIMIT_MINUTES = 30;
    private static final List<String> SCORED = List.of(Attempt.COMPLETED, Attempt.GRADED);

    private final AssessmentRepository assessments;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final Courses courses;
    private final Terms terms;

    /**
     * Ask 46: admin → every assessment; teacher → the ones they authored (created_by); student → assessments of the
     * courses they are enrolled in (course ids from enrollment-service /my-classes). Fails closed: when the course
     * lookup fails a student sees nothing.
     */
    private List<Assessment> visible(Map<String, Course> mine) {
        if (CurrentUser.isAdmin()) return assessments.findAllByOrderByDueAtAsc();
        if (CurrentUser.isTeacher()) return assessments.findByCreatedByOrderByDueAtAsc(CurrentUser.username());
        if (mine.isEmpty()) return List.of();
        return assessments.findByCourseIdInOrderByDueAtAsc(mine.keySet());
    }

    private static boolean staff() { return CurrentUser.isAdmin() || CurrentUser.isTeacher(); }

    private Assessment find(String id) {
        return assessments.findById(id).orElseThrow(() -> new NotFoundException("assessment", id));
    }

    private static boolean isAssignment(Assessment a) { return "Assignment".equals(a.getType()); }

    private static int limitMinutes(Assessment a) {
        return a.getTimeLimitMinutes() == null || a.getTimeLimitMinutes() <= 0 ? DEFAULT_LIMIT_MINUTES : a.getTimeLimitMinutes();
    }

    private static ResponseStatusException conflict(String why) { return new ResponseStatusException(HttpStatus.CONFLICT, why); }

    // ------------------------------------------------------------------ attempt state (caller = student)

    /** Latest scored attempt (Completed / Graded) of the caller on one assessment. */
    private Optional<Attempt> latestAttempt(String assessmentId) {
        return attempts.findFirstByAssessmentIdAndUsernameAndStatusInOrderByCompletedAtDesc(assessmentId, CurrentUser.username(), SCORED);
    }

    private Optional<Attempt> inProgress(String assessmentId) {
        return attempts.findFirstByAssessmentIdAndUsernameAndStatus(assessmentId, CurrentUser.username(), Attempt.IN_PROGRESS);
    }

    /** Latest scored attempt per assessment for the caller (drives averages, review, performance). */
    private Map<String, Attempt> latestAttempts() {
        Map<String, Attempt> m = new LinkedHashMap<>();
        for (Attempt a : attempts.findByUsernameAndStatusInOrderByCompletedAtDesc(CurrentUser.username(), SCORED))
            m.putIfAbsent(a.getAssessmentId(), a);
        return m;
    }

    /**
     * The attempt that decides the caller's status per assessment: an in-progress attempt wins, then the
     * newest finished one (Submitted / Completed / Graded).
     */
    private Map<String, Attempt> stateAttempts() {
        Map<String, Attempt> m = new LinkedHashMap<>();
        for (Attempt a : attempts.findByUsernameOrderByIdDesc(CurrentUser.username())) {
            Attempt cur = m.get(a.getAssessmentId());
            if (cur == null || (Attempt.IN_PROGRESS.equals(a.getStatus()) && !Attempt.IN_PROGRESS.equals(cur.getStatus())))
                m.put(a.getAssessmentId(), a);
        }
        return m;
    }

    /** Timer enforcement: finalise the caller's in-progress attempts whose deadline has passed. */
    private void finalizeExpired(LocalDateTime now) {
        if (staff()) return;
        for (Attempt at : attempts.findByUsernameAndStatusInOrderByCompletedAtDesc(CurrentUser.username(), List.of(Attempt.IN_PROGRESS)))
            if (at.getDeadlineAt() != null && !at.getDeadlineAt().isAfter(now))
                assessments.findById(at.getAssessmentId()).ifPresent(a -> finish(a, at, at.getAnswers(), at.getDeadlineAt(), true));
    }

    /** Score the answers against question.correct and store the attempt as Completed. */
    private Attempt finish(Assessment a, Attempt at, Map<String, ?> answersIn, LocalDateTime when, boolean auto) {
        List<Question> qs = questions.findByAssessmentIdOrderByNumberAsc(a.getId());
        Map<String, String> answers = new LinkedHashMap<>();
        int correct = 0, incorrect = 0, skipped = 0;
        for (Question q : qs) {
            Object v = answersIn == null ? null : answersIn.get(String.valueOf(q.getNumber()));
            if (v == null || String.valueOf(v).isBlank()) { skipped++; continue; }
            answers.put(String.valueOf(q.getNumber()), String.valueOf(v));
            if (String.valueOf(v).equalsIgnoreCase(q.getCorrect())) correct++; else incorrect++;
        }
        at.setAnswers(answers); at.setCorrect(correct); at.setIncorrect(incorrect); at.setSkipped(skipped);
        at.setPercent(qs.isEmpty() ? 0 : (int) Math.round(100.0 * correct / qs.size()));
        at.setCompletedAt(when);
        if (at.getStartedAt() != null) {
            long secs = Duration.between(at.getStartedAt(), when).getSeconds();
            at.setSecondsTaken((int) Math.max(0, Math.min(secs, limitMinutes(a) * 60L)));
        }
        at.setAutoSubmitted(auto); at.setStatus(Attempt.COMPLETED);
        return attempts.save(at);
    }

    private static long secondsLeft(Attempt at, LocalDateTime now) {
        return at.getDeadlineAt() == null ? 0 : Math.max(0, Duration.between(now, at.getDeadlineAt()).getSeconds());
    }

    private static Map<String, Object> attemptJson(Attempt at, LocalDateTime now) {
        if (at == null) return null;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("attemptId", at.getId()); m.put("status", at.getStatus()); m.put("startedAt", Fmt.dateTimeAt(at.getStartedAt()));
        m.put("deadlineAt", at.getDeadlineAt() == null ? null : at.getDeadlineAt().toString());
        m.put("secondsLeft", secondsLeft(at, now)); m.put("answers", at.getAnswers());
        return m;
    }

    private static String status(Assessment a, Attempt at, LocalDateTime now) {
        if (at != null) {
            if (Attempt.IN_PROGRESS.equals(at.getStatus())) return "In Progress";
            if (Attempt.SUBMITTED.equals(at.getStatus())) return "Submitted";
            if (at.isFinished()) return "Completed";
        }
        if (a.getDueAt().isBefore(now)) return "Overdue";
        return a.getDueAt().toLocalDate().equals(now.toLocalDate()) ? "Due Today" : "Upcoming";
    }

    private static String action(Assessment a, String status) {
        return switch (status) {
            case "In Progress" -> "Resume";
            case "Submitted" -> "View Submission";
            case "Completed" -> isAssignment(a) ? "View Submission" : "Review";
            case "Overdue" -> "View Details";
            default -> isAssignment(a) ? "Submit" : "Start";
        };
    }

    private static Map<String, Object> stat(String label, String value, String sub, String color, String icon) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label); m.put("value", value); m.put("sub", sub); m.put("color", color); m.put("icon", icon);
        return m;
    }

    // ------------------------------------------------------------------ Assessments page

    @Operation(summary = "Assessments summary (role-aware)", description = "student → total, average score, high scores, upcoming this week (due today · due this week) + recent results; teacher/admin → total, submissions, average, overdue + latest submissions with the student.")
    @ApiResponse(responseCode = "200", description = "Shape depends on the caller's role — pick your role in the Examples dropdown", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "student", summary = "role = student (own results)", value = Examples.SUMMARY),
            @ExampleObject(name = "teacher-admin", summary = "role = teacher or admin (submissions, averages, overdue)", value = Examples.SUMMARY_STAFF)}))
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Terms.Term t = terms.current();
        Map<String, Course> mine = courses.mine();
        List<Assessment> all = visible(mine);
        Map<String, Assessment> byId = all.stream().collect(Collectors.toMap(Assessment::getId, a -> a));
        List<Map<String, Object>> stats = new ArrayList<>();
        List<Map<String, Object>> recent = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (staff()) {
            List<Attempt> subs = attempts.findByAssessmentIdInAndStatusInOrderByCompletedAtDesc(byId.keySet(), Attempt.FINISHED);
            List<Attempt> scored = subs.stream().filter(a -> SCORED.contains(a.getStatus())).toList();
            long pending = subs.size() - scored.size();
            int avg = scored.isEmpty() ? 0 : (int) Math.round(scored.stream().mapToInt(Attempt::getPercent).average().orElse(0));
            long overdue = all.stream().filter(a -> a.getDueAt().isBefore(now)).count();
            long distinctStudents = subs.stream().map(Attempt::getUsername).distinct().count();
            stats.add(stat("Total Assessments", String.valueOf(all.size()), CurrentUser.isAdmin() ? "All teachers, this term" : "My assessments, this term", "blue", "total"));
            stats.add(stat("Submissions", String.valueOf(subs.size()), distinctStudents + " student" + (distinctStudents == 1 ? "" : "s") + (pending > 0 ? " · " + pending + " to grade" : ""), "green", "score"));
            stats.add(stat("Average Score", avg + "%", scored.isEmpty() ? "No submissions yet" : "Across all graded submissions", "purple", "trophy"));
            stats.add(stat("Overdue", String.valueOf(overdue), "Past due date", "red", "target"));
            for (Attempt a : scored) {
                Assessment as = byId.get(a.getAssessmentId());
                if (as == null) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                Course c = Courses.of(mine, as.getCourseId());
                r.put("title", as.getTitle()); r.put("subject", c.name()); r.put("score", a.getPercent() + "%");
                r.put("date", Fmt.date(a.getCompletedAt().toLocalDate())); r.put("color", c.color()); r.put("student", a.getUsername());
                recent.add(r);
                if (recent.size() == 6) break;
            }
        } else {
            finalizeExpired(now);
            Map<String, Attempt> latest = latestAttempts();
            List<Attempt> scored = latest.values().stream().filter(a -> byId.containsKey(a.getAssessmentId())).toList();
            int avg = scored.isEmpty() ? 0 : (int) Math.round(scored.stream().mapToInt(Attempt::getPercent).average().orElse(0));
            long high = scored.stream().filter(a -> a.getPercent() >= 90).count();
            stats.add(stat("Total Assessments", String.valueOf(all.size()), "This term", "blue", "total"));
            stats.add(stat("Average Score", avg + "%", scored.isEmpty() ? "No results yet" : "Across " + scored.size() + " result" + (scored.size() == 1 ? "" : "s"), "green", "score"));
            stats.add(stat("High Score", String.valueOf(high), "Scored 90%+", "purple", "trophy"));
            // Ask 21: open (not finished) assessments due in the current Mon–Sun week
            java.time.LocalDate today = now.toLocalDate(), monday = today.with(java.time.DayOfWeek.MONDAY), sunday = monday.plusDays(6);
            Map<String, Attempt> state = stateAttempts();
            long dueToday = 0, dueWeek = 0;
            for (Assessment a : all) {
                Attempt at = state.get(a.getId());
                if ((at != null && at.isFinished()) || a.getDueAt().isBefore(now)) continue; // finished or already overdue
                java.time.LocalDate d = a.getDueAt().toLocalDate();
                if (d.equals(today)) dueToday++;
                if (!d.isBefore(monday) && !d.isAfter(sunday)) dueWeek++;
            }
            stats.add(stat("Upcoming This Week", String.valueOf(dueWeek), dueToday + " due today · " + dueWeek + " due this week", "red", "target"));
            for (Attempt a : attempts.findByUsernameAndStatusInOrderByCompletedAtDesc(CurrentUser.username(), SCORED)) {
                Assessment as = byId.get(a.getAssessmentId());
                if (as == null) continue;
                Map<String, Object> r = new LinkedHashMap<>();
                Course c = Courses.of(mine, as.getCourseId());
                r.put("title", as.getTitle()); r.put("subject", c.name()); r.put("score", a.getPercent() + "%");
                r.put("date", Fmt.date(a.getCompletedAt().toLocalDate())); r.put("color", c.color());
                recent.add(r);
                if (recent.size() == 4) break;
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("term", Terms.json(t)); out.put("stats", stats); out.put("recentResults", recent);
        out.put("role", CurrentUser.isAdmin() ? "admin" : CurrentUser.isTeacher() ? "teacher" : "student");
        return out;
    }

    @Operation(summary = "Assessments list (role-aware)", description = "student → each assessment with its status (Upcoming / Due Today / In Progress / Submitted / Completed / Overdue), due date (dueAt ISO), score and action (Start · Resume · Submit · View Submission · Review · View Details — a past-due assessment is never startable); teacher → own assessments with submissions and average; admin → all with teacher (both with dueAt for the weekly/monthly view).")
    @ApiResponse(responseCode = "200", description = "Shape depends on the caller's role — pick your role in the Examples dropdown", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "student", summary = "role = student (status, score, action per assessment)", value = Examples.LIST),
            @ExampleObject(name = "teacher-admin", summary = "role = teacher or admin (submissions, average; Open/Closed)", value = Examples.LIST_STAFF)}))
    @GetMapping("/list")
    public List<Map<String, Object>> list() {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> out = new ArrayList<>();
        Map<String, Course> mine = courses.mine();
        List<Assessment> all = visible(mine);
        if (staff()) {
            Map<String, List<Attempt>> subs = new HashMap<>();
            for (Attempt a : attempts.findByAssessmentIdInAndStatusInOrderByCompletedAtDesc(all.stream().map(Assessment::getId).toList(), Attempt.FINISHED))
                subs.computeIfAbsent(a.getAssessmentId(), k -> new ArrayList<>()).add(a);
            for (Assessment a : all) {
                List<Attempt> s = subs.getOrDefault(a.getId(), List.of());
                List<Attempt> scored = s.stream().filter(x -> SCORED.contains(x.getStatus())).toList();
                Course c = Courses.of(mine, a.getCourseId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", a.getId()); m.put("title", a.getTitle()); m.put("subtitle", a.getSubtitle()); m.put("courseId", a.getCourseId()); m.put("subject", c.name());
                m.put("type", a.getType()); m.put("gradeLevel", c.gradeLevel()); m.put("due", Fmt.date(a.getDueAt().toLocalDate())); m.put("dueTime", Fmt.time(a.getDueAt().toLocalTime()));
                m.put("status", a.getDueAt().isBefore(now) ? "Closed" : "Open"); m.put("dueAt", a.getDueAt().toString());
                m.put("teacher", c.teacher()); m.put("teacherUsername", a.getCreatedBy());
                m.put("submissions", s.size()); m.put("toGrade", s.size() - scored.size());
                m.put("average", scored.isEmpty() ? null : Math.round(scored.stream().mapToInt(Attempt::getPercent).average().orElse(0)) + "%");
                m.put("score", null); m.put("action", "View"); m.put("color", c.color());
                out.add(m);
            }
            return out;
        }
        finalizeExpired(now);
        Map<String, Attempt> state = stateAttempts();
        for (Assessment a : all) {
            Attempt at = state.get(a.getId());
            String status = status(a, at, now);
            boolean scored = at != null && SCORED.contains(at.getStatus());
            Course c = Courses.of(mine, a.getCourseId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId()); m.put("title", a.getTitle()); m.put("subtitle", a.getSubtitle()); m.put("courseId", a.getCourseId()); m.put("subject", c.name());
            m.put("type", a.getType()); m.put("gradeLevel", c.gradeLevel()); m.put("due", Fmt.date(a.getDueAt().toLocalDate())); m.put("dueTime", Fmt.time(a.getDueAt().toLocalTime()));
            m.put("status", status); m.put("score", scored ? at.getPercent() + "%" : null); m.put("action", action(a, status)); m.put("color", c.color());
            m.put("dueAt", a.getDueAt().toString());
            m.put("timeLimitMinutes", isAssignment(a) ? null : limitMinutes(a));
            m.put("secondsLeft", at != null && Attempt.IN_PROGRESS.equals(at.getStatus()) ? secondsLeft(at, now) : null);
            out.add(m);
        }
        return out;
    }

    /** Average score per subject: caller's latest attempts (student/teacher) or every scored attempt (admin). */
    @Operation(summary = "Average score by subject", description = "Caller's latest attempts (student/teacher) or every scored attempt (admin).")
    @ApiResponse(responseCode = "200", description = "Performance", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.PERFORMANCE)))
    @GetMapping("/performance")
    public List<Map<String, Object>> performance() {
        List<Attempt> source = CurrentUser.isAdmin()
                ? attempts.findByStatusInOrderByCompletedAtDesc(SCORED)
                : new ArrayList<>(latestAttempts().values());
        Map<String, Course> mine = courses.mine();
        Map<String, List<Integer>> pct = new LinkedHashMap<>();
        Map<String, String> color = new HashMap<>();
        Map<String, Assessment> cache = new HashMap<>();
        for (Attempt a : source) {
            Assessment as = cache.computeIfAbsent(a.getAssessmentId(), id -> assessments.findById(id).orElse(null));
            if (as == null) continue;
            Course c = Courses.of(mine, as.getCourseId());
            pct.computeIfAbsent(c.name(), k -> new ArrayList<>()).add(a.getPercent());
            color.putIfAbsent(c.name(), c.color());
        }
        List<Map<String, Object>> out = new ArrayList<>();
        pct.forEach((subject, list) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("subject", SHORT_SUBJECT.getOrDefault(subject, subject));
            m.put("percent", (int) Math.round(list.stream().mapToInt(i -> i).average().orElse(0)));
            m.put("color", color.get(subject));
            out.add(m);
        });
        return out;
    }

    // ------------------------------------------------------------------ Assessment Details page

    @Operation(summary = "Assessment details", description = "Details page (no tabs): instructions, files, rubric, status block (status, action, attempts used, time limit, seconds to due, closed flag), the caller's in-progress attempt, the assignment submission (text, files, points, feedback) or the quiz result. Example id: lab-report.")
    @ApiResponse(responseCode = "200", description = "Details", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.DETAILS)))
    @GetMapping("/{id}")
    public Map<String, Object> details(@PathVariable String id) {
        Assessment a = find(id);
        List<Question> qs = questions.findByAssessmentIdOrderByNumberAsc(id);
        LocalDateTime now = LocalDateTime.now();
        finalizeExpired(now);
        List<Attempt> mine = staff() ? List.of() : attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, CurrentUser.username());
        Attempt current = mine.stream().filter(x -> Attempt.IN_PROGRESS.equals(x.getStatus())).findFirst()
                .orElse(mine.stream().filter(Attempt::isFinished).findFirst().orElse(null));
        String status = status(a, current, now);
        long used = mine.stream().filter(Attempt::isFinished).count();
        boolean closed = a.getDueAt().isBefore(now);
        boolean canStart = !staff() && !isAssignment(a) && !closed && current == null && used < a.getAttemptsAllowed();
        boolean canRetake = !staff() && !isAssignment(a) && !closed && current != null && current.isFinished() && used < a.getAttemptsAllowed();

        Map<String, Object> st = new LinkedHashMap<>();
        st.put("status", status); st.put("action", action(a, status)); st.put("dueDate", Fmt.dateTime(a.getDueAt()));
        st.put("timeRemaining", "Completed".equals(status) || "Submitted".equals(status) ? "Submitted" : closed ? "Closed" : Fmt.remaining(a.getDueAt(), now));
        st.put("secondsToDue", Math.max(0, Duration.between(now, a.getDueAt()).getSeconds()));
        st.put("closed", closed); st.put("points", String.valueOf(a.getPoints()));
        st.put("attempts", used + " of " + a.getAttemptsAllowed() + " used"); st.put("attemptsUsed", used); st.put("attemptsAllowed", a.getAttemptsAllowed());
        st.put("timeLimitMinutes", isAssignment(a) ? null : limitMinutes(a));
        st.put("timeLimit", isAssignment(a) ? "Until the due date" : limitMinutes(a) + " min");
        st.put("canStart", canStart); st.put("canRetake", canRetake);

        Map<String, Object> sub = new LinkedHashMap<>();
        Attempt fin = current != null && current.isFinished() ? current : null;
        sub.put("open", !closed && (fin == null || Attempt.SUBMITTED.equals(fin.getStatus())));
        sub.put("note", closed ? "The due date " + Fmt.dateTime(a.getDueAt()) + " has passed" : "You can submit until " + Fmt.dateTime(a.getDueAt()));
        sub.put("submitted", fin != null); sub.put("status", fin == null ? null : fin.getStatus());
        sub.put("submittedAt", fin == null ? null : Fmt.dateTimeAt(fin.getCompletedAt()));
        sub.put("text", fin == null ? null : fin.getSubmissionText()); sub.put("files", fin == null ? List.of() : fin.getSubmissionFiles());
        sub.put("points", a.getPoints()); sub.put("pointsAwarded", fin == null ? null : fin.getPointsAwarded());
        sub.put("percent", fin == null || !SCORED.contains(fin.getStatus()) ? null : fin.getPercent());
        sub.put("feedback", fin == null ? null : fin.getFeedback()); sub.put("gradedBy", fin == null ? null : fin.getGradedBy());
        sub.put("gradedAt", fin == null ? null : Fmt.dateTimeAt(fin.getGradedAt()));
        sub.put("autoSubmitted", fin != null && fin.isAutoSubmitted());
        sub.put("correct", fin == null ? null : fin.getCorrect()); sub.put("total", qs.size());

        Course c = Courses.of(courses.mine(), a.getCourseId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", a.getId()); out.put("title", a.getTitle()); out.put("courseId", a.getCourseId()); out.put("subject", c.name()); out.put("type", a.getType());
        out.put("gradeLevel", c.gradeLevel()); out.put("teacher", c.teacher()); out.put("due", Fmt.dateTimeAt(a.getDueAt()));
        out.put("instructions", Map.of("intro", a.getInstructionsIntro() == null ? "" : a.getInstructionsIntro(), "steps", a.getInstructionsSteps()));
        out.put("files", a.getFiles());
        out.put("questionsPreview", qs.stream().limit(3).map(Question::getText).toList());
        out.put("status", st);
        out.put("attempt", current != null && Attempt.IN_PROGRESS.equals(current.getStatus()) ? attemptJson(current, now) : null);
        out.put("classInfo", Map.of("subject", c.name(), "gradeLevel", c.gradeLevel(), "teacher", c.teacher()));
        out.put("rubric", a.getRubric());
        out.put("submission", sub);
        return out;
    }

    // ------------------------------------------------------------------ Quiz Take page (timed attempt)

    @Operation(summary = "Start (or resume) a timed attempt", description = "Student only. Creates an InProgress attempt with started_at = now and deadline_at = now + time_limit_minutes and returns the seconds left; an existing in-progress attempt is returned instead (resumed = true). 409 when the due date has passed, when attempts_allowed is used up, or for an Assignment (use /assignment).")
    @ApiResponse(responseCode = "200", description = "Attempt started or resumed", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.START)))
    @ApiResponse(responseCode = "409", description = "Closed (past due) · no attempts left · not a timed assessment", content = @Content)
    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('student')")
    public Map<String, Object> start(@PathVariable String id) {
        Assessment a = find(id);
        LocalDateTime now = LocalDateTime.now();
        finalizeExpired(now);
        if (isAssignment(a)) throw conflict("This is an assignment: submit your work from the details page");
        Optional<Attempt> open = inProgress(id);
        Attempt at;
        boolean resumed = open.isPresent();
        if (resumed) at = open.get();
        else {
            if (a.getDueAt().isBefore(now)) throw conflict("Closed: the due date " + Fmt.dateTime(a.getDueAt()) + " has passed");
            long used = attempts.countByAssessmentIdAndUsernameAndStatusIn(id, CurrentUser.username(), Attempt.FINISHED);
            if (used >= a.getAttemptsAllowed()) throw conflict("No attempts left (" + used + " of " + a.getAttemptsAllowed() + " used)");
            at = new Attempt();
            at.setAssessmentId(id); at.setUsername(CurrentUser.username()); at.setStatus(Attempt.IN_PROGRESS);
            at.setStartedAt(now); at.setDeadlineAt(now.plusMinutes(limitMinutes(a)));
            at = attempts.save(at);
        }
        Map<String, Object> out = attemptJson(at, now);
        out.put("resumed", resumed); out.put("timeLimitMinutes", limitMinutes(a));
        return out;
    }

    @Operation(summary = "Quiz questions", description = "All questions in order without the correct answers, plus topics and the time limit. When the caller has an in-progress attempt the `attempt` block carries its seconds left and saved answers (timeRemaining follows the server clock). Example id: unit2-quiz.")
    @ApiResponse(responseCode = "200", description = "Questions (trimmed)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.QUESTIONS)))
    @GetMapping("/{id}/questions")
    public Map<String, Object> questions(@PathVariable String id) {
        Assessment a = find(id);
        LocalDateTime now = LocalDateTime.now();
        finalizeExpired(now);
        List<Question> qs = questions.findByAssessmentIdOrderByNumberAsc(id);
        int limit = limitMinutes(a);
        Attempt at = staff() ? null : inProgress(id).orElse(null);
        List<Map<String, Object>> list = qs.stream().map(q -> ReviewSupport.questionJson(q, qs.size(), false)).toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", a.getId()); out.put("title", a.getTitle()); out.put("type", a.getType());
        Course c = Courses.of(courses.mine(), a.getCourseId());
        out.put("subtitle", a.getSubtitle() + " · " + c.name() + (c.gradeLevel().isEmpty() ? "" : " (" + c.gradeLevel() + ")"));
        out.put("timeLimitMinutes", limit);
        out.put("timeRemaining", at == null ? String.format("%d:00", limit) : Fmt.mmss(secondsLeft(at, now)));
        out.put("attempt", attemptJson(at, now));
        out.put("totalQuestions", qs.size()); out.put("questions", list);
        out.put("question", list.isEmpty() ? null : list.get(0));
        out.put("topics", qs.stream().map(q -> q.getTopic() == null ? "Question " + q.getNumber() : q.getTopic()).toList());
        return out;
    }

    @Operation(summary = "Save answers (autosave)", description = "Student only. Stores the answers so far on the in-progress attempt and returns the seconds left. Body: {\"answers\": {\"1\": \"B\", …}}. 409 when there is no in-progress attempt or the time is up (the attempt is then finalised from the saved answers).")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.ANSWERS_BODY)))
    @ApiResponse(responseCode = "200", description = "Saved", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.ANSWERS)))
    @ApiResponse(responseCode = "409", description = "No attempt in progress · time is up", content = @Content)
    @PutMapping("/{id}/answers")
    @PreAuthorize("hasRole('student')")
    @SuppressWarnings("unchecked")
    public Map<String, Object> saveAnswers(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Assessment a = find(id);
        LocalDateTime now = LocalDateTime.now();
        Attempt at = inProgress(id).orElseThrow(() -> conflict("No attempt in progress: start the assessment first"));
        if (!at.getDeadlineAt().isAfter(now)) { finish(a, at, at.getAnswers(), at.getDeadlineAt(), true); throw conflict("Time is up: your attempt has been submitted"); }
        Map<String, Object> raw = body == null ? Map.of() : body;
        Map<String, Object> answersIn = raw.get("answers") instanceof Map<?, ?> m ? (Map<String, Object>) m : raw;
        Map<String, String> answers = new LinkedHashMap<>();
        answersIn.forEach((k, v) -> { if (v != null && !String.valueOf(v).isBlank()) answers.put(k, String.valueOf(v)); });
        at.setAnswers(answers);
        attempts.save(at);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", at.getId()); out.put("saved", answers.size()); out.put("secondsLeft", secondsLeft(at, now));
        return out;
    }

    /** Body: {"answers":{"1":"B",...}, "secondsTaken": 1470} or a bare {"1":"B",...} map. */
    @Operation(summary = "Submit answers", description = "Student only. Finalises the caller's in-progress attempt: scores the answers (body answers win over the saved ones) against question.correct, records seconds taken from started_at and marks it auto-submitted when the deadline had already passed. Without an in-progress attempt a direct attempt is created — only while the assessment is open and attempts remain (409 otherwise). Body: {\"answers\": {\"1\": \"B\", …}, \"secondsTaken\": 1470} or a bare {\"1\": \"B\", …} map.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBMIT_BODY)))
    @ApiResponse(responseCode = "201", description = "Attempt stored", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.SUBMIT)))
    @ApiResponse(responseCode = "409", description = "Closed (past due) · no attempts left · assignment", content = @Content)
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('student')")
    @ResponseStatus(HttpStatus.CREATED)
    @SuppressWarnings("unchecked")
    public Map<String, Object> submit(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Assessment a = find(id);
        if (isAssignment(a)) throw conflict("This is an assignment: use /assignment");
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> raw = body == null ? Map.of() : body;
        Map<String, Object> answersIn = raw.get("answers") instanceof Map<?, ?> m ? (Map<String, Object>) m : raw;
        Integer seconds = raw.get("secondsTaken") instanceof Number n ? n.intValue() : null;

        Attempt at;
        Optional<Attempt> open = inProgress(id);
        if (open.isPresent()) {
            at = open.get();
            boolean late = !at.getDeadlineAt().isAfter(now);
            Map<String, ?> answers = answersIn.isEmpty() ? at.getAnswers() : answersIn;
            at = finish(a, at, answers, late ? at.getDeadlineAt() : now, late);
        } else {
            if (a.getDueAt().isBefore(now)) throw conflict("Closed: the due date " + Fmt.dateTime(a.getDueAt()) + " has passed");
            long used = attempts.countByAssessmentIdAndUsernameAndStatusIn(id, CurrentUser.username(), Attempt.FINISHED);
            if (used >= a.getAttemptsAllowed()) throw conflict("No attempts left (" + used + " of " + a.getAttemptsAllowed() + " used)");
            at = new Attempt();
            at.setAssessmentId(a.getId()); at.setUsername(CurrentUser.username());
            at.setStartedAt(seconds == null ? null : now.minusSeconds(seconds));
            at = finish(a, at, answersIn, now, false);
            if (at.getSecondsTaken() == null) { at.setSecondsTaken(seconds); at = attempts.save(at); }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", at.getId()); out.put("correct", at.getCorrect()); out.put("incorrect", at.getIncorrect()); out.put("skipped", at.getSkipped());
        out.put("total", at.getCorrect() + at.getIncorrect() + at.getSkipped()); out.put("percent", at.getPercent()); out.put("status", at.getStatus());
        out.put("secondsTaken", at.getSecondsTaken()); out.put("autoSubmitted", at.isAutoSubmitted());
        return out;
    }

    // ------------------------------------------------------------------ Assignment submission

    @Operation(summary = "Submit an assignment", description = "Student only. Stores the response text and the attached file names/sizes/types as a Submitted attempt (resubmitting before the due date replaces it). Body: {\"text\": \"…\", \"files\": [{\"name\", \"size\", \"type\"}]}. 409 when the due date has passed, the assessment is not an Assignment, or the submission is already graded.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.ASSIGNMENT_BODY)))
    @ApiResponse(responseCode = "201", description = "Submission stored", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.ASSIGNMENT)))
    @ApiResponse(responseCode = "409", description = "Closed (past due) · not an assignment · already graded", content = @Content)
    @PostMapping("/{id}/assignment")
    @PreAuthorize("hasRole('student')")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> submitAssignment(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        Assessment a = find(id);
        if (!isAssignment(a)) throw conflict("Not an assignment: start it with /start and submit with /submit");
        LocalDateTime now = LocalDateTime.now();
        if (a.getDueAt().isBefore(now)) throw conflict("Closed: the due date " + Fmt.dateTime(a.getDueAt()) + " has passed");
        Map<String, Object> raw = body == null ? Map.of() : body;
        String text = raw.get("text") == null ? "" : String.valueOf(raw.get("text")).trim();
        List<Map<String, Object>> files = new ArrayList<>();
        if (raw.get("files") instanceof List<?> l)
            for (Object o : l) if (o instanceof Map<?, ?> f && f.get("name") != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", String.valueOf(f.get("name")));
                m.put("size", f.get("size") instanceof Number n ? n.longValue() : 0L);
                m.put("type", f.get("type") == null ? "" : String.valueOf(f.get("type")));
                files.add(m);
            }
        if (text.isEmpty() && files.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Write a response or attach at least one file");
        List<Attempt> mine = attempts.findByAssessmentIdAndUsernameOrderByIdDesc(id, CurrentUser.username());
        if (mine.stream().anyMatch(x -> Attempt.GRADED.equals(x.getStatus()))) throw conflict("Already graded: this assignment cannot be resubmitted");
        Attempt at = mine.stream().filter(x -> Attempt.SUBMITTED.equals(x.getStatus())).findFirst().orElseGet(Attempt::new);
        boolean resubmitted = at.getId() != null;
        at.setAssessmentId(id); at.setUsername(CurrentUser.username()); at.setStatus(Attempt.SUBMITTED);
        if (at.getStartedAt() == null) at.setStartedAt(now);
        at.setCompletedAt(now); at.setSubmissionText(text); at.setSubmissionFiles(files);
        at = attempts.save(at);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attemptId", at.getId()); out.put("status", at.getStatus()); out.put("submittedAt", Fmt.dateTimeAt(now));
        out.put("files", files.size()); out.put("resubmitted", resubmitted);
        return out;
    }

    // ------------------------------------------------------------------ Quiz Review page

    @Operation(summary = "Quiz review", description = "The caller's latest finished attempt (Completed · Graded · Submitted-pending assignment): results, per-question topics and outcomes, every question with the chosen and correct answers, the assignment submission block, related learning. 404 if the caller has no finished attempt.")
    @ApiResponse(responseCode = "200", description = "Review (trimmed)", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = Examples.REVIEW)))
    @ApiResponse(responseCode = "404", description = "No completed attempt", content = @Content)
    @GetMapping("/{id}/review")
    @PreAuthorize("hasRole('student')")   // Ask 45: a student's own attempt; staff review via /submissions
    public Map<String, Object> review(@PathVariable String id) {
        Assessment a = find(id);
        finalizeExpired(LocalDateTime.now());
        Attempt at = attempts.findFirstByAssessmentIdAndUsernameAndStatusInOrderByCompletedAtDesc(id, CurrentUser.username(), Attempt.FINISHED)
                .orElseThrow(() -> new NotFoundException("completed attempt for " + id + " by user", CurrentUser.username()));
        return ReviewSupport.review(a, Courses.of(courses.mine(), a.getCourseId()), at, questions.findByAssessmentIdOrderByNumberAsc(id));
    }
}
