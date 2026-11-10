package com.learninghub.dashboard.web;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.learninghub.dashboard.common.CurrentUser;
import com.learninghub.dashboard.common.Fmt;
import com.learninghub.dashboard.common.ServiceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * View endpoints for the Dashboard page. Every number is aggregated live, per request, from
 * user-, enrollment-, learning-, schedule- and assessment-service (the caller's token is forwarded);
 * this service keeps no data of its own. The response carries "sources" = live | unavailable per
 * upstream service so a fallback (zeros / empty lists) is never mistaken for real data (Ask 33).
 */
@Tag(name = "Dashboard", description = "Role-specific dashboard data aggregated live, per request, from user-, enrollment-, learning-, assessment- and schedule-service (token forwarded). No data is stored here; `sources` in the response says which upstream services answered (live) and which fell back (unavailable).")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final ParameterizedTypeReference<List<Map<String, Object>>> LIST = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> OBJ = new ParameterizedTypeReference<>() {};

    private final ServiceClient client;

    /** Today's classes, owned by schedule-service. */
    @Operation(summary = "Today's classes", description = "Proxied from schedule-service /api/schedule/today (role-filtered there). Empty on days without classes, e.g. weekends; the example shows a weekday.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"start\":\"8:00 AM\",\"end\":\"9:00 AM\",\"subject\":\"AP Chemistry\",\"detail\":\"Junior\",\"gradeLevel\":\"Junior\",\"color\":\"red\",\"teacher\":\"Dr Priya Patel\",\"teacherUsername\":\"ppatel\",\"courseId\":\"ap-chemistry\"}]")}))
    @GetMapping("/today")
    public List<Map<String, Object>> today() {
        return client.get("schedule-service", "/api/schedule/today", LIST, List.of());
    }

    @Operation(summary = "Dashboard analytics by role", description = "The response shape depends on the caller's role (the Examples dropdown has one entry per role — Swagger shows the first one, student, by default). Shape depends on the caller's role: student (progress, pending, classes completed), teacher (classes taught, students, assessments, submissions, myClasses), admin (users by role, subjects, enrollment, performance, subjects).")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "student", summary = "role = student (progress, pending, classes completed)", value = "{\"greetingName\":\"Sam\",\"date\":\"Sun, Sep 27, 2026\",\"role\":\"student\",\"stats\":[{\"label\":\"Enrolled Classes\",\"value\":\"5\",\"icon\":\"classes\",\"color\":\"blue\"},{\"label\":\"Overall Progress\",\"value\":\"12%\",\"icon\":\"progress\",\"color\":\"green\"},{\"label\":\"Pending Assignments\",\"value\":\"6\",\"icon\":\"assignments\",\"color\":\"purple\"},{\"label\":\"Classes Completed\",\"value\":\"71 / 130\",\"icon\":\"completed\",\"color\":\"amber\"}],\"learningProgress\":{\"overall\":12,\"subjects\":[{\"name\":\"AP Chemistry\",\"percent\":0,\"color\":\"red\"}]},\"performance\":[{\"subject\":\"AP Pre Calculus\",\"percent\":84,\"color\":\"blue\"}],\"sources\":{\"enrollment-service\":\"live\",\"learning-service\":\"live\",\"assessment-service\":\"live\"}}"), @ExampleObject(name = "teacher", summary = "role = teacher (classes taught, students, submissions)", value = "{\"greetingName\":\"John\",\"date\":\"Sun, Sep 27, 2026\",\"role\":\"teacher\",\"stats\":[{\"label\":\"Classes I Teach\",\"value\":\"6\",\"icon\":\"classes\",\"color\":\"blue\"},{\"label\":\"Students\",\"value\":\"28\",\"icon\":\"students\",\"color\":\"green\"},{\"label\":\"Assessments\",\"value\":\"3\",\"icon\":\"assessments\",\"color\":\"purple\"},{\"label\":\"Submissions\",\"value\":\"2\",\"icon\":\"submissions\",\"color\":\"amber\"}],\"myClasses\":[{\"id\":\"ap-calc-ab\",\"name\":\"AP Calc AB\",\"gradeLevel\":\"Senior\",\"days\":[\"Mon\",\"Wed\",\"Fri\"],\"time\":\"1:00 PM - 2:00 PM\",\"studentCount\":5,\"color\":\"blue\"}],\"performance\":[],\"sources\":{\"enrollment-service\":\"live\",\"assessment-service\":\"live\"}}"), @ExampleObject(name = "admin", summary = "role = admin (users by role, subjects, enrollment, performance)", value = "{\"greetingName\":\"Alice\",\"date\":\"Sun, Sep 27, 2026\",\"role\":\"admin\",\"stats\":[{\"label\":\"Students\",\"value\":\"21\",\"icon\":\"students\",\"color\":\"blue\"},{\"label\":\"Teachers\",\"value\":\"6\",\"icon\":\"teachers\",\"color\":\"green\"},{\"label\":\"Subjects\",\"value\":\"18\",\"icon\":\"classes\",\"color\":\"purple\"},{\"label\":\"Enrollment\",\"value\":\"80\",\"icon\":\"enrollments\",\"color\":\"amber\"}],\"usersByRole\":{\"admin\":1,\"teacher\":6,\"student\":21},\"performance\":[{\"subject\":\"AP Pre Calculus\",\"percent\":84,\"color\":\"blue\"}],\"subjects\":[{\"id\":\"ap-biology\",\"name\":\"AP Biology\",\"gradeLevel\":\"Sophomore\",\"teacher\":\"Mr Tom Teacher\",\"teacherUsername\":\"teacher1\",\"days\":[\"Mon\",\"Wed\",\"Fri\"],\"time\":\"1:00 PM - 2:00 PM\",\"color\":\"teal\",\"studentCount\":4}],\"sources\":{\"user-service\":\"live\",\"enrollment-service\":\"live\",\"assessment-service\":\"live\"}}")}))
    @GetMapping("/analytics")
    public Map<String, Object> analytics() {
        ServiceClient.resetSources();
        Map<String, Object> out = CurrentUser.isAdmin() ? adminAnalytics() : CurrentUser.isTeacher() ? teacherAnalytics() : studentAnalytics();
        out.put("sources", ServiceClient.sources());
        return out;
    }

    private Map<String, Object> header(String role) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("greetingName", CurrentUser.firstName());
        out.put("date", Fmt.dateWithDow(LocalDate.now()));
        out.put("role", role);
        return out;
    }

    /** Student: progress (term lesson plans), pending work, classes completed, subject progress, performance. */
    private Map<String, Object> studentAnalytics() {
        Map<String, Object> myClasses = client.get("enrollment-service", "/api/enrollment/my-classes", OBJ, Map.of());
        List<Map<String, Object>> learning = client.get("learning-service", "/api/learning/classes", LIST, List.of());
        List<Map<String, Object>> assessments = client.get("assessment-service", "/api/assessment/list", LIST, List.of());
        List<Map<String, Object>> performance = client.get("assessment-service", "/api/assessment/performance", LIST, List.of());

        int enrolled = classesOf(myClasses).size();
        // pending = still to be done by the student: Upcoming · Due Today · In Progress · Overdue (Submitted awaits the teacher, Completed is done)
        long pending = assessments.stream().filter(a -> List.of("Upcoming", "Due Today", "In Progress", "Overdue").contains(String.valueOf(a.get("status")))).count();
        int overall = (int) Math.round(learning.stream().mapToDouble(c -> num(c.get("percent"))).average().orElse(0));
        long classesDone = learning.stream().mapToLong(c -> (long) num(c.get("classesCompleted"))).sum();   // Ask 25: term lesson plan
        long classesTotal = learning.stream().mapToLong(c -> (long) num(c.get("classes"))).sum();

        List<Map<String, Object>> stats = List.of(
                stat("Enrolled Classes", String.valueOf(enrolled), "classes", "blue"),
                stat("Overall Progress", overall + "%", "progress", "green"),
                stat("Pending Assignments", String.valueOf(pending), "assignments", "purple"),
                stat("Classes Completed", classesDone + " / " + classesTotal, "completed", "amber"));

        List<Map<String, Object>> subjects = new ArrayList<>();
        for (Map<String, Object> c : learning) {
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("name", c.get("name")); s.put("percent", (int) num(c.get("percent"))); s.put("color", c.get("color"));
            subjects.add(s);
        }
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("overall", overall); progress.put("subjects", subjects);

        Map<String, Object> out = header("student");
        out.put("stats", stats);
        out.put("learningProgress", progress);
        out.put("performance", performance);
        return out;
    }

    /** Teacher: classes taught (with student counts), assessments and submissions. */
    private Map<String, Object> teacherAnalytics() {
        Map<String, Object> myClasses = client.get("enrollment-service", "/api/enrollment/my-classes", OBJ, Map.of());
        List<Map<String, Object>> assessments = client.get("assessment-service", "/api/assessment/list", LIST, List.of());

        List<Map<String, Object>> classes = new ArrayList<>();
        int students = 0;
        for (Map<String, Object> c : classesOf(myClasses)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.get("id")); m.put("name", c.get("name")); m.put("gradeLevel", c.get("gradeLevel"));
            m.put("days", c.get("days")); m.put("time", c.get("time"));
            int count = (int) num(c.get("studentCount"));
            m.put("studentCount", count);
            m.put("color", c.get("color"));
            students += count;
            classes.add(m);
        }
        long submissions = assessments.stream().mapToLong(a -> (long) num(a.get("submissions"))).sum();

        List<Map<String, Object>> stats = List.of(
                stat("Classes I Teach", String.valueOf(classes.size()), "classes", "blue"),
                stat("Students", String.valueOf(students), "students", "green"),
                stat("Assessments", String.valueOf(assessments.size()), "assessments", "purple"),
                stat("Submissions", String.valueOf(submissions), "submissions", "amber"));

        Map<String, Object> out = header("teacher");
        out.put("stats", stats);
        out.put("myClasses", classes);
        out.put("performance", List.of());
        return out;
    }

    /** Admin: school-wide counts, users by role, subjects with enrolment, performance across all students. */
    private Map<String, Object> adminAnalytics() {
        List<Map<String, Object>> directory = client.get("user-service", "/api/users/directory", LIST, List.of());
        Map<String, Object> allClasses = client.get("enrollment-service", "/api/enrollment/my-classes", OBJ, Map.of());
        List<Map<String, Object>> performance = client.get("assessment-service", "/api/assessment/performance", LIST, List.of());

        Map<String, Integer> byRole = new LinkedHashMap<>();
        byRole.put("admin", 0); byRole.put("teacher", 0); byRole.put("student", 0);
        for (Map<String, Object> u : directory) {
            String role = String.valueOf(u.get("role")).toLowerCase();
            if (byRole.containsKey(role)) byRole.merge(role, 1, Integer::sum);
        }

        List<Map<String, Object>> subjects = new ArrayList<>();
        int enrollments = 0;
        for (Map<String, Object> c : classesOf(allClasses)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.get("id")); m.put("name", c.get("name")); m.put("gradeLevel", c.get("gradeLevel"));
            m.put("teacher", c.get("teacher")); m.put("teacherUsername", c.get("teacherUsername"));
            m.put("days", c.get("days")); m.put("time", c.get("time")); m.put("color", c.get("color"));
            int count = (int) num(c.get("studentCount"));
            m.put("studentCount", count);
            enrollments += count;
            subjects.add(m);
        }

        List<Map<String, Object>> stats = List.of(
                stat("Students", String.valueOf(byRole.get("student")), "students", "blue"),
                stat("Teachers", String.valueOf(byRole.get("teacher")), "teachers", "green"),
                stat("Subjects", String.valueOf(subjects.size()), "classes", "purple"),
                stat("Enrollment", String.valueOf(enrollments), "enrollments", "amber"));

        Map<String, Object> out = header("admin");
        out.put("stats", stats);
        out.put("usersByRole", byRole);
        out.put("performance", performance);
        out.put("subjects", subjects);
        return out;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> classesOf(Map<String, Object> myClasses) {
        return myClasses.get("classes") instanceof List<?> l ? (List<Map<String, Object>>) l : List.of();
    }

    private static Map<String, Object> stat(String label, String value, String icon, String color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label); m.put("value", value); m.put("icon", icon); m.put("color", color);
        return m;
    }

    private static double num(Object o) {
        return o instanceof Number n ? n.doubleValue() : 0;
    }
}
