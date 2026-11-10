package com.learninghub.learning.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.learninghub.learning.common.CurrentUser;
import com.learninghub.learning.common.Fmt;
import com.learninghub.learning.common.Catalog;
import com.learninghub.learning.common.Catalog.Course;
import com.learninghub.learning.common.Catalog.Term;
import com.learninghub.learning.domain.LessonPlanItem;
import com.learninghub.learning.domain.StudySession;
import com.learninghub.learning.repo.LessonPlanItemRepository;
import com.learninghub.learning.repo.StudySessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * View endpoints for the Learning page (Ask 25): overview tiles, courses with plan progress, study hours.
 * Ask 46: courses and the term are not stored here — they come from enrollment-service / schedule-service
 * (see {@link Catalog}); progress is computed from the lesson plan on every read.
 */
@RestController
@Tag(name = "Learning views", description = "Role-aware overview and courses (term lesson-plan progress) and study hours")
@RequiredArgsConstructor
@RequestMapping("/api/learning")
public class LearningController {

    private static final DateTimeFormatter DOW_DATE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US);

    private final LessonPlanItemRepository items;
    private final StudySessionRepository studySessions;
    private final Catalog catalog;

    Term currentTerm() {
        return catalog.currentTerm();
    }

    private static Map<String, Object> termJson(Term t) {
        return Map.of("name", t.name(), "start", Fmt.date(t.start()), "end", Fmt.date(t.end()));
    }

    /** {thisWeek, lastWeek, best} in hours for the caller. */
    private Map<String, Double> studyHours(String username) {
        LocalDate today = LocalDate.now();
        LocalDate thisMon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastMon = thisMon.minusWeeks(1);
        Map<LocalDate, Integer> perWeek = new TreeMap<>();
        for (StudySession s : studySessions.findByUsernameOrderByDayAsc(username)) {
            LocalDate wk = s.getDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            perWeek.merge(wk, s.getMinutes(), Integer::sum);
        }
        double thisWeek = perWeek.getOrDefault(thisMon, 0) / 60.0;
        double lastWeek = perWeek.getOrDefault(lastMon, 0) / 60.0;
        double best = perWeek.values().stream().mapToInt(Integer::intValue).max().orElse(0) / 60.0;
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("thisWeek", round1(thisWeek)); m.put("lastWeek", round1(lastWeek)); m.put("best", round1(best));
        return m;
    }

    private static double round1(double v) { return Math.round(v * 10) / 10.0; }

    private static String hours(double h) {
        return h == Math.floor(h) ? String.valueOf((long) h) : String.valueOf(h);
    }

    private static Map<String, Object> stat(String label, String value, String sub, String color, String icon, Integer progress) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label); m.put("value", value);
        if (sub != null) m.put("sub", sub);
        m.put("color", color); m.put("icon", icon);
        if (progress != null) m.put("progress", progress);
        return m;
    }

    /** Courses visible to the caller — enrollment-service /my-classes (student → enrolled, teacher → taught, admin → all); empty when it cannot answer. */
    List<Course> visibleCourses() {
        return catalog.visibleCourses();
    }

    // ------------------------------------------------------------------ lesson plan helpers (shared with LessonPlanController)

    List<LessonPlanItem> planOf(String courseId) {
        return items.findByCourseIdOrderByClassNumberAsc(courseId);
    }

    static int percentOf(List<LessonPlanItem> plan) {
        long done = plan.stream().filter(LessonPlanItem::isCompleted).count();
        return plan.isEmpty() ? 0 : (int) Math.round(100.0 * done / plan.size());
    }

    static String statusOf(List<LessonPlanItem> plan) {
        long done = plan.stream().filter(LessonPlanItem::isCompleted).count();
        return !plan.isEmpty() && done == plan.size() ? "completed" : done == 0 ? "not-started" : "in-progress";
    }

    static LessonPlanItem nextClass(List<LessonPlanItem> plan, LocalDate today) {
        return plan.stream().filter(i -> !i.isCompleted() && !i.getClassDate().isBefore(today)).findFirst()
                .orElse(plan.stream().filter(i -> !i.isCompleted()).findFirst().orElse(null));
    }

    Map<String, Object> itemJson(LessonPlanItem i, LocalDate today) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId()); m.put("classNumber", i.getClassNumber()); m.put("date", i.getClassDate().toString());
        m.put("dateLabel", DOW_DATE.format(i.getClassDate())); m.put("title", i.getTitle()); m.put("topic", i.getTopic());
        m.put("completed", i.isCompleted()); m.put("completedAt", Fmt.dateTimeAt(i.getCompletedAt())); m.put("completedBy", i.getCompletedBy());
        m.put("status", i.isCompleted() ? "Completed" : i.getClassDate().isBefore(today) ? "Not completed" : i.getClassDate().equals(today) ? "Today" : "Upcoming");
        return m;
    }

    /** Course + plan summary (and the items when withItems). */
    Map<String, Object> planJson(Course c, boolean withItems) {
        LocalDate today = LocalDate.now();
        List<LessonPlanItem> plan = planOf(c.id());
        long done = plan.stream().filter(LessonPlanItem::isCompleted).count();
        LessonPlanItem next = nextClass(plan, today);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.id()); m.put("name", c.name()); m.put("gradeLevel", c.gradeLevel()); m.put("teacher", c.teacher());
        m.put("teacherUsername", c.teacherUsername()); m.put("color", c.color());
        m.put("classes", plan.size()); m.put("classesCompleted", done); m.put("classesRemaining", plan.size() - done);
        m.put("percent", percentOf(plan)); m.put("status", statusOf(plan));
        m.put("nextClass", next == null ? null : itemJson(next, today));
        m.put("canMark", CurrentUser.isTeacher() && CurrentUser.username().equals(c.teacherUsername()));   // Ask 45: admin views only
        if (withItems) {
            m.put("term", termJson(currentTerm()));
            m.put("items", plan.stream().map(i -> itemJson(i, today)).toList());
        }
        return m;
    }

    // ------------------------------------------------------------------ views

    @Operation(summary = "Learning overview (role-aware)", description = "Stat tiles from the term lesson plans — student: enrolled classes, classes completed (progress), next class, study hours; teacher: courses taught, classes this term, classes completed (progress), next class; admin: courses, classes this term, classes completed, study hours (all students).")
    @ApiResponse(responseCode = "200", description = "Stat tiles depend on the caller's role — pick your role in the Examples dropdown", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "student", summary = "role = student", value = Examples.OVERVIEW_STUDENT),
            @ExampleObject(name = "teacher", summary = "role = teacher", value = Examples.OVERVIEW_TEACHER),
            @ExampleObject(name = "admin", summary = "role = admin", value = Examples.OVERVIEW_ADMIN)}))
    @GetMapping("/overview")
    public Map<String, Object> overview() {
        String user = CurrentUser.username();
        LocalDate today = LocalDate.now();
        Term t = currentTerm();
        List<Course> mine = visibleCourses();
        List<LessonPlanItem> all = mine.isEmpty() ? List.of() : items.findByCourseIdInOrderByClassDateAscClassNumberAsc(mine.stream().map(Course::id).toList());
        long done = all.stream().filter(LessonPlanItem::isCompleted).count();
        int pct = all.isEmpty() ? 0 : (int) Math.round(100.0 * done / all.size());
        LessonPlanItem next = nextClass(all, today);
        Map<String, Course> byId = new LinkedHashMap<>();
        for (Course c : mine) byId.put(c.id(), c);
        String nextLabel = next == null ? "—" : DOW_DATE.format(next.getClassDate());
        String nextSub = next == null ? "No classes left this term" : byId.get(next.getCourseId()).name() + " · class #" + next.getClassNumber();

        List<Map<String, Object>> stats = new ArrayList<>();
        if (CurrentUser.isAdmin()) {
            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            double hrs = studySessions.findByDayGreaterThanEqual(weekStart).stream().mapToInt(StudySession::getMinutes).sum() / 60.0;
            stats.add(stat("Courses", String.valueOf(mine.size()), "All teachers", "blue", "classes", null));
            stats.add(stat("Classes This Term", String.valueOf(all.size()), "Across all courses", "green", "lessons", null));
            stats.add(stat("Classes Completed", done + " / " + all.size(), "Marked by teachers", "purple", "check", pct));
            stats.add(stat("Study Hours This Week", hours(round1(hrs)), "All students", "red", "hours", null));
        } else if (CurrentUser.isTeacher()) {
            stats.add(stat("Courses Taught", String.valueOf(mine.size()), "This term", "blue", "classes", null));
            stats.add(stat("Classes This Term", String.valueOf(all.size()), "Across my courses", "green", "lessons", null));
            stats.add(stat("Classes Completed", done + " / " + all.size(), "Marked complete", "purple", "check", pct));
            stats.add(stat("Next Class", nextLabel, nextSub, "red", "schedule", null));
        } else {
            Map<String, Double> sh = studyHours(user);
            double tw = sh.get("thisWeek"), lw = sh.get("lastWeek");
            String delta = lw == 0 ? "No study time last week" : String.format("%+d%% from last week", Math.round(100.0 * (tw - lw) / lw));
            stats.add(stat("Enrolled Classes", String.valueOf(mine.size()), "This term", "blue", "classes", null));
            stats.add(stat("Classes Completed", done + " / " + all.size(), (all.size() - done) + " remaining", "green", "check", pct));
            stats.add(stat("Next Class", nextLabel, nextSub, "purple", "schedule", null));
            stats.add(stat("Study Hours This Week", hours(tw), delta, "red", "hours", null));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("term", termJson(t)); out.put("stats", stats);
        out.put("role", CurrentUser.isAdmin() ? "admin" : CurrentUser.isTeacher() ? "teacher" : "student");
        return out;
    }

    @Operation(summary = "Courses (role-aware)", description = "student → enrolled courses (ids from enrollment-service); teacher → own courses; admin → all. Each row: classes this term, classesCompleted, classesRemaining, percent, status, nextClass, canMark.")
    @ApiResponse(responseCode = "200", description = "Rows depend on the caller's role — pick your role in the Examples dropdown", content = @Content(mediaType = "application/json", examples = {
            @ExampleObject(name = "student", summary = "role = student (enrolled courses)", value = Examples.CLASSES_STUDENT),
            @ExampleObject(name = "teacher", summary = "role = teacher (own courses)", value = Examples.CLASSES_TEACHER),
            @ExampleObject(name = "admin", summary = "role = admin (all courses)", value = Examples.CLASSES_ADMIN)}))
    @GetMapping("/classes")
    public List<Map<String, Object>> classes() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Course c : visibleCourses()) out.add(planJson(c, false));
        return out;
    }
}
