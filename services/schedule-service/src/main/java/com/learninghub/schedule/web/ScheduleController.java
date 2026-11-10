package com.learninghub.schedule.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.learninghub.schedule.common.CourseCatalog;
import com.learninghub.schedule.common.CourseCatalog.Course;
import com.learninghub.schedule.common.CurrentUser;
import com.learninghub.schedule.common.Fmt;
import com.learninghub.schedule.common.NotFoundException;
import com.learninghub.schedule.domain.ClassSession;
import com.learninghub.schedule.domain.Term;
import com.learninghub.schedule.repo.ClassSessionRepository;
import com.learninghub.schedule.repo.TermRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * View endpoints for the Schedule page, computed from term + class_session slots relative to the
 * current date. Ask 46: a slot carries only course_id and its times; the course facts shown on every
 * block (subject, grade level, teacher, colour) are read from enrollment-service per request.
 */
@Tag(name = "Schedule", description = "Terms and timetables computed from class-session slots enriched with course facts from enrollment-service. Role-filtered: students see only enrolled classes, teachers only the classes they teach, admins everything.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedule")
public class ScheduleController {
    private final TermRepository terms;
    private final ClassSessionRepository sessions;
    private final CourseCatalog catalog;

    /** A slot joined (in memory) with its course from enrollment-service. */
    record Row(ClassSession s, Course c) {
        String subject() { return c.name(); }
        String gradeLevel() { return c.gradeLevelName() == null ? "" : c.gradeLevelName(); }
        String color() { return c.color(); }
        String teacher() { return c.teacher(); }
        String teacherUsername() { return c.teacherUsername(); }
    }

    /**
     * Slots the caller may see, each with its course: admin → all; teacher → the courses they teach;
     * student → the courses they are enrolled in (enrollment-service /state). Fails closed: when
     * enrollment-service cannot be reached a student or teacher gets an empty timetable.
     */
    private List<Row> visible(Term t) {
        List<ClassSession> all = sessions.findByTermIdOrderByStartTimeAscCourseIdAsc(t.getId());
        Map<String, Course> courses = catalog.courses();
        if (courses == null) {
            if (!CurrentUser.isAdmin()) return List.of();
            return all.stream().map(s -> new Row(s, Course.unknown(s.getCourseId()))).toList();
        }
        List<Row> rows = new ArrayList<>();
        for (ClassSession s : all) rows.add(new Row(s, courses.getOrDefault(s.getCourseId(), Course.unknown(s.getCourseId()))));
        rows.sort(Comparator.comparing((Row r) -> r.s().getStartTime()).thenComparing(Row::subject, String.CASE_INSENSITIVE_ORDER));
        if (CurrentUser.isAdmin()) return rows;
        if (CurrentUser.isTeacher()) {
            String me = CurrentUser.username();
            return rows.stream().filter(r -> me.equals(r.teacherUsername())).toList();
        }
        Set<String> enrolled = catalog.enrolledCourseIds();
        if (enrolled == null) return List.of();
        return rows.stream().filter(r -> enrolled.contains(r.s().getCourseId())).toList();
    }

    private Term currentTerm() {
        return terms.findFirstByCurrentTrue().orElseThrow(() -> new NotFoundException("current term", "-"));
    }

    private static Map<String, Object> termJson(Term t) {
        return Map.of("name", t.getName(), "start", Fmt.date(t.getStartDate()), "end", Fmt.date(t.getEndDate()));
    }

    private static String dow(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.US); // Mon
    }

    private static boolean activeOn(ClassSession s, LocalDate d) {
        return s.getDays().contains(dow(d))
                && (s.getStartDate() == null || !d.isBefore(s.getStartDate()))
                && (s.getEndDate() == null || !d.isAfter(s.getEndDate()));
    }

    /** Ask 46: terms are owned here; every other service reads them from this endpoint. */
    @Operation(summary = "Terms", description = "The quarter terms of the academic year in quarter order with ISO dates; the current one is flagged. Read by the term picker, the admin Subjects page and every other service (enrollment, learning, assessment, payment, user).")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"id\":\"q1-2026\",\"name\":\"Q1 · Fall 2026\",\"start\":\"2026-08-24\",\"end\":\"2026-10-23\",\"current\":true,\"maxCredits\":30,\"quarter\":1,\"academicYear\":\"2026 - 2027\"},{\"id\":\"q2-2026\",\"name\":\"Q2 · Winter 2026\",\"start\":\"2026-10-26\",\"end\":\"2027-01-15\",\"current\":false,\"maxCredits\":30,\"quarter\":2,\"academicYear\":\"2026 - 2027\"}]")}))
    @GetMapping("/terms")
    public List<Map<String, Object>> termsList() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Term t : terms.findAllByOrderByQuarterAscStartDateAsc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId()); m.put("name", t.getName());
            m.put("start", t.getStartDate().toString()); m.put("end", t.getEndDate().toString());
            m.put("current", t.isCurrent()); m.put("maxCredits", t.getMaxCredits());
            m.put("quarter", t.getQuarter()); m.put("academicYear", t.getAcademicYear());
            out.add(m);
        }
        return out;
    }

    /** Weekly timetable for the week containing today. */
    @Operation(summary = "Weekly timetable", description = "Mon–Sun of the current week: hours span, blocks, mini-calendar dots and the next 4 meetings. Role-filtered: students see only enrolled classes, teachers only the classes they teach, admins everything. Subject, grade level, teacher and colour come from enrollment-service.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"term\":{\"start\":\"Aug 24, 2026\",\"end\":\"Oct 23, 2026\",\"name\":\"Q1 · Fall 2026\"},\"weekLabel\":\"Sep 21 - 27, 2026\",\"days\":[{\"name\":\"Mon\",\"date\":\"Sep 21\"},{\"name\":\"Tue\",\"date\":\"Sep 22\"},{\"name\":\"Sun\",\"date\":\"Sep 27\"}],\"hours\":[\"7:00 AM\",\"8:00 AM\",\"9:00 AM\"],\"blocks\":[{\"subject\":\"AP Chemistry\",\"gradeLevel\":\"Junior\",\"color\":\"red\",\"teacher\":\"Dr Priya Patel\",\"teacherUsername\":\"ppatel\",\"days\":[\"Mon\",\"Wed\",\"Fri\"],\"start\":\"8:00 AM\",\"end\":\"9:00 AM\"}],\"miniCalendar\":{\"month\":\"September 2026\",\"selected\":27,\"highlight\":27,\"dotDays\":[1,2,3]},\"upcoming\":[{\"subject\":\"AP Chemistry\",\"when\":\"Mon, Sep 28\",\"time\":\"8:00 - 9:00 AM\",\"gradeLevel\":\"Junior\",\"teacher\":\"Dr Priya Patel\",\"teacherUsername\":\"ppatel\",\"courseId\":\"ap-chemistry\",\"color\":\"red\"}]}")}))
    @GetMapping("/week")
    public Map<String, Object> week() {
        Term t = currentTerm();
        List<Row> list = visible(t);
        LocalDate today = LocalDate.now();
        LocalDate mon = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sun = mon.plusDays(6);

        List<Map<String, Object>> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = mon.plusDays(i);
            days.add(Map.of("name", dow(d), "date", Fmt.dateShort(d)));
        }
        String weekLabel = mon.getMonth() == sun.getMonth()
                ? Fmt.dateShort(mon) + " - " + sun.getDayOfMonth() + ", " + sun.getYear()
                : Fmt.dateShort(mon) + " - " + Fmt.dateShort(sun) + ", " + sun.getYear();

        LocalTime first = list.stream().map(r -> r.s().getStartTime()).min(Comparator.naturalOrder()).orElse(LocalTime.of(9, 0));
        LocalTime last = list.stream().map(r -> r.s().getEndTime()).max(Comparator.naturalOrder()).orElse(LocalTime.of(16, 0));
        List<String> hours = new ArrayList<>();
        for (LocalTime h = first.minusHours(1).withMinute(0); !h.isAfter(last); h = h.plusHours(1)) hours.add(Fmt.time(h));

        List<Map<String, Object>> blocks = new ArrayList<>();
        for (Row r : list) {
            ClassSession s = r.s();
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("subject", r.subject()); b.put("gradeLevel", r.gradeLevel()); b.put("color", r.color());
            b.put("teacher", r.teacher()); b.put("teacherUsername", r.teacherUsername()); b.put("days", s.getDays());
            b.put("start", Fmt.time(s.getStartTime())); b.put("end", Fmt.time(s.getEndTime()));
            blocks.add(b);
        }

        YearMonth ym = YearMonth.from(today);
        List<Integer> dotDays = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            if (list.stream().anyMatch(r -> activeOn(r.s(), date))) dotDays.add(d);
        }
        Map<String, Object> mini = new LinkedHashMap<>();
        mini.put("month", Fmt.monthYear(today)); mini.put("selected", today.getDayOfMonth());
        mini.put("highlight", today.getDayOfMonth()); mini.put("dotDays", dotDays);

        // next 4 class meetings from now
        List<Map<String, Object>> upcoming = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<Row> byStart = list.stream().sorted(Comparator.comparing(r -> r.s().getStartTime())).toList();
        outer:
        for (int i = 0; i < 14; i++) {
            LocalDate d = today.plusDays(i);
            for (Row r : byStart) {
                ClassSession s = r.s();
                if (!activeOn(s, d) || !d.atTime(s.getStartTime()).isAfter(now)) continue;
                Map<String, Object> u = new LinkedHashMap<>();
                u.put("subject", r.subject()); u.put("when", Fmt.dowShort(d));
                u.put("time", Fmt.timeRangeCompact(s.getStartTime(), s.getEndTime()));
                u.put("gradeLevel", r.gradeLevel()); u.put("teacher", r.teacher()); u.put("teacherUsername", r.teacherUsername()); u.put("courseId", s.getCourseId()); u.put("color", r.color());
                upcoming.add(u);
                if (upcoming.size() == 4) break outer;
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("term", termJson(t)); out.put("weekLabel", weekLabel); out.put("days", days); out.put("hours", hours);
        out.put("blocks", blocks); out.put("miniCalendar", mini); out.put("upcoming", upcoming);
        return out;
    }

    /** Term overview: one bar per class across the term's months. */
    @Operation(summary = "Term overview", description = "One bar per class with % positions across the quarter term.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"term\":{\"start\":\"Aug 24, 2026\",\"end\":\"Oct 23, 2026\",\"name\":\"Q1 · Fall 2026\"},\"months\":[\"August\",\"September\",\"October\"],\"bars\":[{\"subject\":\"AP Chemistry\",\"color\":\"red\",\"label\":\"Aug 24 - Oct 23, 2026\",\"startPct\":0,\"endPct\":100}]}")}))
    @GetMapping("/term")
    public Map<String, Object> term() {
        Term t = currentTerm();
        List<String> months = new ArrayList<>();
        for (YearMonth m = YearMonth.from(t.getStartDate()); !m.isAfter(YearMonth.from(t.getEndDate())); m = m.plusMonths(1))
            months.add(m.getMonth().getDisplayName(TextStyle.FULL, Locale.US));
        long total = ChronoUnit.DAYS.between(t.getStartDate(), t.getEndDate());
        List<Map<String, Object>> bars = new ArrayList<>();
        for (Row r : visible(t)) {
            ClassSession s = r.s();
            LocalDate a = s.getStartDate() != null ? s.getStartDate() : t.getStartDate();
            LocalDate b = s.getEndDate() != null ? s.getEndDate() : t.getEndDate();
            Map<String, Object> bar = new LinkedHashMap<>();
            bar.put("subject", r.subject()); bar.put("color", r.color());
            bar.put("label", Fmt.dateShort(a) + " - " + Fmt.date(b));
            bar.put("startPct", Math.round(100.0 * ChronoUnit.DAYS.between(t.getStartDate(), a) / total));
            bar.put("endPct", Math.round(100.0 * ChronoUnit.DAYS.between(t.getStartDate(), b) / total));
            bars.add(bar);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("term", termJson(t)); out.put("months", months); out.put("bars", bars);
        return out;
    }

    /** Today's class meetings (also consumed by dashboard-service). */
    @Operation(summary = "Today's classes", description = "Class meetings on the current date (also consumed by dashboard-service). detail = grade level. Empty on days without classes, e.g. weekends; the example shows a weekday.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"start\":\"8:00 AM\",\"end\":\"9:00 AM\",\"subject\":\"AP Chemistry\",\"detail\":\"Junior\",\"gradeLevel\":\"Junior\",\"color\":\"red\",\"teacher\":\"Dr Priya Patel\",\"teacherUsername\":\"ppatel\",\"courseId\":\"ap-chemistry\"}]")}))
    @GetMapping("/today")
    public List<Map<String, Object>> today() {
        return onDate(LocalDate.now());
    }

    private List<Map<String, Object>> onDate(LocalDate d) {
        Term t = currentTerm();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Row r : visible(t)) {
            ClassSession s = r.s();
            if (!activeOn(s, d)) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("start", Fmt.time(s.getStartTime())); m.put("end", Fmt.time(s.getEndTime()));
            m.put("subject", r.subject()); m.put("detail", r.gradeLevel()); m.put("gradeLevel", r.gradeLevel()); m.put("color", r.color());
            m.put("teacher", r.teacher()); m.put("teacherUsername", r.teacherUsername()); m.put("courseId", s.getCourseId());
            out.add(m);
        }
        return out;
    }

    /** Month grid for the calendar view: /api/schedule/month?year=2026&month=9 (defaults to the current month). */
    @Operation(summary = "Month grid", description = "Calendar data for a month (defaults to the current month): first weekday (0 = Sunday), day count, today, per-day class entries.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"month\":\"September 2026\",\"year\":2026,\"monthNumber\":9,\"firstDow\":2,\"days\":30,\"today\":27,\"classes\":[{\"day\":1,\"subject\":\"AP Physics 2\",\"color\":\"purple\",\"start\":\"9:00 AM\",\"end\":\"10:00 AM\",\"gradeLevel\":\"Junior\"}]}")}))
    @Parameter(name = "year", example = "2026") @Parameter(name = "month", example = "9")
    @GetMapping("/month")
    public Map<String, Object> month(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
        LocalDate today = LocalDate.now();
        YearMonth ym = year == null || month == null ? YearMonth.from(today) : YearMonth.of(year, month);
        Term t = currentTerm();
        List<Row> list = visible(t);
        List<Map<String, Object>> entries = new ArrayList<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            LocalDate date = ym.atDay(d);
            for (Row r : list) {
                ClassSession s = r.s();
                if (!activeOn(s, date)) continue;
                Map<String, Object> e = new LinkedHashMap<>();
                e.put("day", d); e.put("subject", r.subject()); e.put("color", r.color());
                e.put("start", Fmt.time(s.getStartTime())); e.put("end", Fmt.time(s.getEndTime())); e.put("gradeLevel", r.gradeLevel());
                entries.add(e);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("month", ym.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + ym.getYear());
        out.put("year", ym.getYear()); out.put("monthNumber", ym.getMonthValue());
        out.put("firstDow", ym.atDay(1).getDayOfWeek().getValue() % 7); // Sun=0 … Sat=6
        out.put("days", ym.lengthOfMonth());
        out.put("today", YearMonth.from(today).equals(ym) ? today.getDayOfMonth() : null);
        out.put("classes", entries);
        return out;
    }
}
