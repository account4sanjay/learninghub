package com.learninghub.user.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Academic {
    private static final ParameterizedTypeReference<Map<String, Object>> OBJ = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};
    private static final long TTL_MS = 60_000;

    private final ServiceClient client;
    private volatile Map<String, GradeLevel> cachedLevels;
    private volatile long levelsAt;

    public record GradeLevel(String id, String name, String gradeNumber) {}

    /** grade_level.id → level (cached 60 s; empty map when enrollment-service cannot answer). */
    public Map<String, GradeLevel> gradeLevels() {
        long now = System.currentTimeMillis();
        Map<String, GradeLevel> cached = cachedLevels;
        if (cached != null && now - levelsAt < TTL_MS) return cached;
        List<Map<String, Object>> rows = client.get("enrollment-service", "/api/enrollment/grade-levels", LIST, null);
        if (rows == null) return cached != null ? cached : Map.of();
        Map<String, GradeLevel> out = new LinkedHashMap<>();
        for (Map<String, Object> r : rows)
            if (r.get("id") != null) out.put(str(r.get("id")), new GradeLevel(str(r.get("id")), str(r.get("name")), str(r.get("gradeNumber"))));
        cachedLevels = out; levelsAt = now;
        return out;
    }

    /** "Junior" for "junior"; null when unknown. */
    public String gradeName(String gradeLevelId) {
        if (gradeLevelId == null) return null;
        GradeLevel g = gradeLevels().get(gradeLevelId);
        return g == null ? null : g.name();
    }

    /** "11" for "junior"; null when unknown. */
    public String gradeNumber(String gradeLevelId) {
        if (gradeLevelId == null) return null;
        GradeLevel g = gradeLevels().get(gradeLevelId);
        return g == null ? null : g.gradeNumber();
    }

    /** The caller's classes [{name, color}] from enrollment-service; empty when unavailable. */
    public List<Map<String, Object>> myClasses() {
        Map<String, Object> mine = client.get("enrollment-service", "/api/enrollment/my-classes", OBJ, null);
        List<Map<String, Object>> out = new ArrayList<>();
        if (mine == null || !(mine.get("classes") instanceof List<?> classes)) return out;
        for (Object o : classes) {
            if (!(o instanceof Map<?, ?> m) || m.get("name") == null) continue;
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("name", str(m.get("name"))); c.put("color", m.get("color") == null ? "blue" : str(m.get("color")));
            out.add(c);
        }
        return out;
    }

    /** Academic year of the current term ("2026 - 2027") from schedule-service; null when unavailable. */
    public String academicYear() {
        List<Map<String, Object>> terms = client.get("schedule-service", "/api/schedule/terms", LIST, null);
        if (terms == null) return null;
        for (Map<String, Object> t : terms)
            if (Boolean.TRUE.equals(t.get("current")) && t.get("academicYear") != null) return str(t.get("academicYear"));
        return null;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
