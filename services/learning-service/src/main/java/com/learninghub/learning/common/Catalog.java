package com.learninghub.learning.common;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class Catalog {
    private static final ParameterizedTypeReference<Map<String, Object>> OBJ = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};
    private static final long TERM_TTL_MS = 60_000;

    private final ServiceClient client;
    private volatile Term cachedTerm;
    private volatile long cachedAt;

    /** A course as enrollment-service describes it. */
    public record Course(String id, String name, String gradeLevel, String teacher, String teacherUsername, String color) {}

    /** The current term as schedule-service describes it. */
    public record Term(String id, String name, LocalDate start, LocalDate end) {}

    /** Courses the caller may see, in enrollment-service order; empty when the lookup fails. */
    public List<Course> visibleCourses() {
        Map<String, Object> mine = client.get("enrollment-service", "/api/enrollment/my-classes", OBJ, null);
        List<Course> out = new ArrayList<>();
        if (mine == null || !(mine.get("classes") instanceof List<?> classes)) return out;
        for (Object o : classes) {
            if (!(o instanceof Map<?, ?> m) || m.get("id") == null) continue;
            out.add(new Course(str(m.get("id")), str(m.get("name")), str(m.get("gradeLevel")), str(m.get("teacher")),
                    str(m.get("teacherUsername")), str(m.get("color"))));
        }
        return out;
    }

    /** id → course for the caller. */
    public Map<String, Course> visibleById() {
        Map<String, Course> byId = new LinkedHashMap<>();
        for (Course c : visibleCourses()) byId.put(c.id(), c);
        return byId;
    }

    /** The current term from schedule-service (cached 60 s); 503 when schedule-service cannot answer. */
    public Term currentTerm() {
        long now = System.currentTimeMillis();
        Term t = cachedTerm;
        if (t != null && now - cachedAt < TERM_TTL_MS) return t;
        List<Map<String, Object>> terms = client.get("schedule-service", "/api/schedule/terms", LIST, null);
        if (terms != null) {
            for (Map<String, Object> m : terms) {
                if (Boolean.TRUE.equals(m.get("current"))) {
                    t = new Term(str(m.get("id")), str(m.get("name")), LocalDate.parse(str(m.get("start"))), LocalDate.parse(str(m.get("end"))));
                    cachedTerm = t; cachedAt = now;
                    return t;
                }
            }
        }
        if (t != null) return t; // stale but better than nothing
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "current term unavailable (schedule-service)");
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
