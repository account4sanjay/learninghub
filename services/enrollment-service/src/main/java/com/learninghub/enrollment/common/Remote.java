package com.learninghub.enrollment.common;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class Remote {
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};
    private static final long TTL_MS = 60_000;

    /** A term as published by schedule-service GET /api/schedule/terms. */
    public record Term(String id, String name, LocalDate startDate, LocalDate endDate, boolean current, int maxCredits, Integer quarter, String academicYear) {}

    /** The slot of a course as published by schedule-service GET /api/schedule/sessions. */
    public record Slot(String courseId, String termId, List<String> days, LocalTime startTime, LocalTime endTime, LocalDate startDate, LocalDate endDate) {}

    private final ServiceClient client;
    private volatile List<Term> termsCache; private volatile long termsAt;
    private volatile Map<String, String> teachersCache; private volatile long teachersAt;

    public Remote(ServiceClient client) {
        this.client = client;
    }

    /** All terms in quarter order (cached 60 s); empty when schedule-service is unreachable. */
    public List<Term> terms() {
        List<Term> c = termsCache;
        if (c != null && System.currentTimeMillis() - termsAt < TTL_MS) return c;
        List<Map<String, Object>> rows = client.get("schedule-service", "/api/schedule/terms", LIST, null);
        if (rows == null) return c != null ? c : List.of();
        List<Term> out = new ArrayList<>();
        for (Map<String, Object> m : rows)
            out.add(new Term(str(m.get("id")), str(m.get("name")), date(m.get("start")), date(m.get("end")),
                    Boolean.TRUE.equals(m.get("current")), m.get("maxCredits") instanceof Number n ? n.intValue() : 30,
                    m.get("quarter") instanceof Number q ? q.intValue() : null, str(m.get("academicYear"))));
        termsCache = out; termsAt = System.currentTimeMillis();
        return out;
    }

    /** term id → term */
    public Map<String, Term> termsById() {
        Map<String, Term> m = new LinkedHashMap<>();
        for (Term t : terms()) m.put(t.id(), t);
        return m;
    }

    /** The current term; 503 when schedule-service has not answered. */
    public Term currentTerm() {
        return terms().stream().filter(Term::current).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "current term unavailable (schedule-service)"));
    }

    /** course id → slot (every term); empty when schedule-service is unreachable. */
    public Map<String, Slot> slots() {
        List<Map<String, Object>> rows = client.get("schedule-service", "/api/schedule/sessions", LIST, List.of());
        Map<String, Slot> out = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            List<String> days = new ArrayList<>();
            if (m.get("days") instanceof List<?> ds) ds.forEach(d -> days.add(String.valueOf(d)));
            out.put(str(m.get("courseId")), new Slot(str(m.get("courseId")), str(m.get("termId")), days,
                    time(m.get("startTime")), time(m.get("endTime")), date(m.get("startDate")), date(m.get("endDate"))));
        }
        return out;
    }

    /** teacher username → display name (cached 60 s); empty when user-service is unreachable. */
    public Map<String, String> teacherNames() {
        Map<String, String> c = teachersCache;
        if (c != null && System.currentTimeMillis() - teachersAt < TTL_MS) return c;
        List<Map<String, Object>> rows = client.get("user-service", "/api/users/teachers", LIST, null);
        if (rows == null) return c != null ? c : Map.of();
        Map<String, String> out = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) if (m.get("username") != null) out.put(str(m.get("username")), str(m.get("displayName")));
        teachersCache = out; teachersAt = System.currentTimeMillis();
        return out;
    }

    /** "Dr John Anderson" for a username, falling back to the username itself. */
    public String teacherName(Map<String, String> names, String username) {
        String n = names.get(username);
        return n == null || n.isBlank() ? username : n;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
    private static LocalDate date(Object o) { return o == null ? null : LocalDate.parse(String.valueOf(o)); }
    private static LocalTime time(Object o) { return o == null ? null : LocalTime.parse(String.valueOf(o)); }
}
