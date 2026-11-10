package com.learninghub.schedule.web;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.learninghub.schedule.common.Fmt;
import com.learninghub.schedule.common.NotFoundException;
import com.learninghub.schedule.domain.ClassSession;
import com.learninghub.schedule.domain.Term;
import com.learninghub.schedule.repo.ClassSessionRepository;
import com.learninghub.schedule.repo.TermRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Class-session slots keyed by course id (Ask 46). A slot is only the term, days and times of a course;
 * the admin Subjects page writes it here after saving the course in enrollment-service, so no service
 * writes another service's data. GET /sessions is read by enrollment-service to show times on its pages.
 */
@Tag(name = "Class sessions", description = "The time slot of every course (term, days, hour). Read by enrollment-service; written by the admin Subjects page.")
@RestController
@RequestMapping("/api/schedule/sessions")
public class ClassSessionController {
    private final ClassSessionRepository sessions;
    private final TermRepository terms;

    public ClassSessionController(ClassSessionRepository sessions, TermRepository terms) {
        this.sessions = sessions; this.terms = terms;
    }

    /** Body of the by-course upsert: slot only. */
    @Schema(description = "Time slot of a course (Ask 46: no course facts here — name, grade level, teacher and colour belong to enrollment-service)")
    public record SlotBody(@Schema(example = "q1-2026") String termId,
                           @Schema(example = "[\"Mon\",\"Wed\",\"Fri\"]") List<String> days,
                           @Schema(example = "08:00", type = "string") LocalTime startTime,
                           @Schema(example = "09:00", type = "string", description = "defaults to startTime + 1 h") LocalTime endTime,
                           @Schema(example = "2026-08-24", description = "defaults to the term start") LocalDate startDate,
                           @Schema(example = "2026-10-23", description = "defaults to the term end") LocalDate endDate) {}

    static Map<String, Object> json(ClassSession s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseId", s.getCourseId()); m.put("termId", s.getTermId()); m.put("days", s.getDays());
        m.put("startTime", s.getStartTime().toString()); m.put("endTime", s.getEndTime().toString());
        m.put("start", Fmt.time(s.getStartTime())); m.put("end", Fmt.time(s.getEndTime()));
        m.put("startDate", s.getStartDate() == null ? null : s.getStartDate().toString());
        m.put("endDate", s.getEndDate() == null ? null : s.getEndDate().toString());
        return m;
    }

    @Operation(summary = "Class-session slots", description = "Every course's slot across all terms: courseId, termId, days, startTime/endTime (HH:mm), start/end (display), startDate/endDate (ISO). Read by enrollment-service to show days and times on the catalog, My classes, roster and Subjects pages.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"courseId\":\"ap-chemistry\",\"termId\":\"q1-2026\",\"days\":[\"Mon\",\"Wed\",\"Fri\"],\"startTime\":\"08:00\",\"endTime\":\"09:00\",\"start\":\"8:00 AM\",\"end\":\"9:00 AM\",\"startDate\":\"2026-08-24\",\"endDate\":\"2026-10-23\"}]")}))
    @GetMapping
    public List<Map<String, Object>> list() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (ClassSession s : sessions.findAllByOrderByTermIdAscStartTimeAscCourseIdAsc()) out.add(json(s));
        return out;
    }

    @Operation(summary = "Upsert the slot of a course (admin)", description = "Creates or updates the single slot for a course id: termId, 1–7 days of Mon–Sun, startTime on the hour; endTime defaults to startTime + 1 h and anything else is rejected; startDate/endDate default to the term's dates. Called by the admin Subjects page after the course is saved in enrollment-service.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"courseId\":\"ap-chemistry\",\"termId\":\"q1-2026\",\"days\":[\"Mon\",\"Wed\",\"Fri\"],\"startTime\":\"08:00\",\"endTime\":\"09:00\",\"start\":\"8:00 AM\",\"end\":\"9:00 AM\",\"startDate\":\"2026-08-24\",\"endDate\":\"2026-10-23\"}")}))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content)
    @PutMapping("/by-course/{courseId}")
    @PreAuthorize("hasRole('admin')")
    @Transactional
    public Map<String, Object> upsertByCourse(@PathVariable String courseId, @RequestBody SlotBody b) {
        if (b.termId() == null || b.startTime() == null || b.days() == null || b.days().isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "termId, days and startTime are required");
        Term t = terms.findById(b.termId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown termId " + b.termId()));
        LocalTime end = b.endTime() != null ? b.endTime() : b.startTime().plusHours(1);
        if (!end.equals(b.startTime().plusHours(1)))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "classes are exactly one hour");
        ClassSession s = sessions.findFirstByCourseId(courseId).orElseGet(ClassSession::new);
        s.setCourseId(courseId); s.setTermId(t.getId());
        s.setDays(b.days()); s.setStartTime(b.startTime()); s.setEndTime(end);
        s.setStartDate(b.startDate() != null ? b.startDate() : t.getStartDate());
        s.setEndDate(b.endDate() != null ? b.endDate() : t.getEndDate());
        return json(sessions.save(s));
    }

    @Operation(summary = "Delete the slot of a course (admin)", description = "204 when removed, 404 when no slot exists for the course. Called by the admin Subjects page after the course is deleted in enrollment-service.")
    @ApiResponse(responseCode = "204", description = "Deleted", content = @Content)
    @DeleteMapping("/by-course/{courseId}")
    @PreAuthorize("hasRole('admin')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteByCourse(@PathVariable String courseId) {
        ClassSession s = sessions.findFirstByCourseId(courseId).orElseThrow(() -> new NotFoundException("session for course", courseId));
        sessions.delete(s);
    }
}
