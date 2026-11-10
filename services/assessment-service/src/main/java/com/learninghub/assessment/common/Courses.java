package com.learninghub.assessment.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * Ask 46: courses are owned by enrollment-service. The caller's role-aware class list
 * (GET /api/enrollment/my-classes: student → enrolled, teacher → taught, admin → all) is read once per
 * request and used to enrich assessment rows with subject name, grade level, teacher and colour.
 * Fails closed: when the lookup fails the map is empty, so a student sees nothing rather than everything.
 */
@Component
@RequiredArgsConstructor
public class Courses {
    private final ServiceClient client;

    /** One course as enrollment-service describes it. */
    public record Course(String id, String name, String gradeLevel, String teacher, String teacherUsername, String color) {}

    /** The caller's classes keyed by course id (empty on failure). */
    public Map<String, Course> mine() {
        Map<String, Course> out = new LinkedHashMap<>();
        Map<String, Object> res = client.get("enrollment-service", "/api/enrollment/my-classes",
                new ParameterizedTypeReference<Map<String, Object>>() {}, null);
        if (res == null || !(res.get("classes") instanceof List<?> classes)) return out;
        for (Object o : classes) {
            if (!(o instanceof Map<?, ?> m) || m.get("id") == null) continue;
            String id = String.valueOf(m.get("id"));
            out.put(id, new Course(id, str(m.get("name"), id), str(m.get("gradeLevel"), ""), str(m.get("teacher"), ""),
                    str(m.get("teacherUsername"), ""), str(m.get("color"), "blue")));
        }
        return out;
    }

    /** The course of an assessment, or a placeholder when it is not among the caller's classes. */
    public static Course of(Map<String, Course> courses, String courseId) {
        Course c = courses.get(courseId);
        return c != null ? c : new Course(courseId, courseId, "", "", "", "blue");
    }

    /** Puts the subject / gradeLevel / teacher / teacherUsername / color keys the SPA expects. */
    public static void put(Map<String, Object> m, Course c) {
        m.put("subject", c.name()); m.put("gradeLevel", c.gradeLevel()); m.put("teacher", c.teacher());
        m.put("teacherUsername", c.teacherUsername()); m.put("color", c.color());
    }

    private static String str(Object v, String dflt) { return v == null ? dflt : String.valueOf(v); }
}
