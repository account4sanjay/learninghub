package com.learninghub.schedule.common;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

@Component
public class CourseCatalog {
    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> OBJ = new ParameterizedTypeReference<>() {};

    private final ServiceClient client;

    public CourseCatalog(ServiceClient client) {
        this.client = client;
    }

    /** One course row of GET /api/enrollment/courses. */
    public record Course(String id, String termId, String name, String gradeLevel, String gradeLevelName,
                         String teacherUsername, String teacher, Integer credits, String color) {
        /** A slot whose course is unknown to enrollment-service: shown by id so nothing is silently dropped for admins. */
        public static Course unknown(String courseId) {
            return new Course(courseId, null, courseId, "", "", null, null, null, "blue");
        }
    }

    /** course id → course, or {@code null} when enrollment-service could not be reached. */
    public Map<String, Course> courses() {
        List<Map<String, Object>> rows = client.get("enrollment-service", "/api/enrollment/courses", LIST, null);
        if (rows == null) return null;
        Map<String, Course> out = new LinkedHashMap<>();
        for (Map<String, Object> m : rows) {
            Object credits = m.get("credits");
            out.put(str(m.get("id")), new Course(str(m.get("id")), str(m.get("termId")), str(m.get("name")),
                    str(m.get("gradeLevel")), str(m.get("gradeLevelName")), str(m.get("teacherUsername")), str(m.get("teacher")),
                    credits instanceof Number n ? n.intValue() : null, m.get("color") == null ? "blue" : str(m.get("color"))));
        }
        return out;
    }

    /** Course ids the calling student is enrolled in, or {@code null} when the lookup failed. */
    public Set<String> enrolledCourseIds() {
        Map<String, Object> state = client.get("enrollment-service", "/api/enrollment/state", OBJ, null);
        if (state == null || !(state.get("enrolled") instanceof List<?> ids)) return null;
        Set<String> out = new HashSet<>();
        ids.forEach(i -> out.add(String.valueOf(i)));
        return out;
    }

    private static String str(Object o) { return o == null ? null : String.valueOf(o); }
}
