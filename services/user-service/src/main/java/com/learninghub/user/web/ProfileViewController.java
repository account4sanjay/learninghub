package com.learninghub.user.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learninghub.user.common.Academic;
import com.learninghub.user.common.CurrentUser;
import com.learninghub.user.common.Fmt;
import com.learninghub.user.common.KeycloakAdminClient;
import com.learninghub.user.common.NotFoundException;
import com.learninghub.user.domain.Profile;
import com.learninghub.user.repo.ProfileRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * The caller's own profile, in the shape Profile.svelte renders, assembled
 * from the profile row; classes, grade names and the academic year come from enrollment-service /
 * schedule-service (Ask 46: one owner per entity).
 */
@Tag(name = "Profile", description = "The caller's own profile (role-shaped) and self-service edits.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ProfileViewController {


    private final ProfileRepository profiles;
    private final Academic academic;
    private final KeycloakAdminClient keycloak;

    private Profile mine() {
        String u = CurrentUser.username();
        return profiles.findByUsername(u).orElseThrow(() -> new NotFoundException("profile for", u));
    }

    private static Map<String, Object> stat(String label, String value, String sub, String color, String icon) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label); m.put("value", value); m.put("sub", sub); m.put("color", color); m.put("icon", icon);
        return m;
    }

    private static Map<String, Object> named(String name, String color) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name); m.put("color", color);
        return m;
    }

    @Operation(summary = "My profile", description = "Students get classes, guardian, academic info and stat tiles; teachers/admins get only the personal block. Name is title + first + last.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"id\":1,\"username\":\"student1\",\"name\":\"Sam Student\",\"title\":null,\"firstName\":\"Sam\",\"lastName\":\"Student\",\"gender\":\"male\",\"avatarUrl\":\"/api/users/avatar/student1\",\"grade\":\"Junior\",\"role\":\"Student\",\"email\":\"student1@learninghub.local\",\"phone\":\"(408) 555-0123\",\"location\":\"Sunnyvale, CA\",\"bio\":\"I enjoy math and computer science.\",\"stats\":[{\"label\":\"Enrolled Classes\",\"value\":\"5\",\"sub\":\"2026 - 2027 year\",\"color\":\"blue\",\"icon\":\"classes\"}],\"personal\":{\"fullName\":\"Sam Student\",\"dateOfBirth\":\"Mar 15, 2010\",\"grade\":\"11\",\"school\":\"Fremont High School\",\"email\":\"student1@learninghub.local\",\"phone\":\"(408) 555-0123\",\"address\":\"Sunnyvale, CA\"},\"academic\":{\"currentGrade\":\"Junior\",\"academicYear\":\"2026 - 2027\",\"school\":\"Fremont High School\",\"classes\":[{\"name\":\"AP Pre Calculus\",\"color\":\"blue\"}]},\"guardian\":{\"name\":\"Sarah Smith\",\"relation\":\"Parent/Guardian\",\"email\":\"sarah.smith@gmail.com\",\"phone\":\"(408) 555-0124\"},\"preferences\":{\"timezone\":\"Pacific Time (PT)\",\"notifications\":\"Email & Push\"}}")}))
    @GetMapping("/profile")
    public Map<String, Object> profile() {
        Profile p = mine();
        boolean student = "student".equalsIgnoreCase(p.getRole());
        List<Map<String, Object>> cls = student ? academic.myClasses() : List.of();
        String academicYear = student ? academic.academicYear() : null;
        String gradeName = academic.gradeName(p.getGradeLevel());
        String termLabel = academicYear != null ? academicYear + " year" : "This term";
        List<Map<String, Object>> stats = List.of(
                stat("Enrolled Classes", String.valueOf(cls.size()), termLabel, "blue", "classes"));

        Map<String, Object> personal = new LinkedHashMap<>();
        personal.put("fullName", p.displayName()); personal.put("dateOfBirth", Fmt.date(p.getDateOfBirth()));
        personal.put("grade", academic.gradeNumber(p.getGradeLevel())); personal.put("school", p.getSchool()); personal.put("email", p.getEmail());
        personal.put("phone", p.getPhone()); personal.put("address", p.getAddress());

        Map<String, Object> academicInfo = new LinkedHashMap<>();
        academicInfo.put("currentGrade", gradeName); academicInfo.put("academicYear", academicYear); academicInfo.put("school", p.getSchool());
        academicInfo.put("classes", cls.stream().map(c -> named((String) c.get("name"), (String) c.get("color"))).toList());


        Map<String, Object> guardian = new LinkedHashMap<>();
        guardian.put("name", p.getGuardianName()); guardian.put("relation", p.getGuardianRelation());
        guardian.put("email", p.getGuardianEmail()); guardian.put("phone", p.getGuardianPhone());

        Map<String, Object> prefs = new LinkedHashMap<>();
        prefs.put("timezone", p.getPrefTimezone()); prefs.put("notifications", p.getPrefNotifications());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", p.getId()); out.put("username", p.getUsername());
        out.put("name", p.displayName()); out.put("title", p.getTitle()); out.put("firstName", p.getFirstName());
        out.put("lastName", p.getLastName()); out.put("gender", p.getGender()); out.put("avatarUrl", com.learninghub.user.common.Avatars.urlFor(p.getUsername()));
        out.put("grade", gradeName); out.put("role", p.getRole());
        out.put("email", p.getEmail()); out.put("phone", p.getPhone()); out.put("location", p.getLocation()); out.put("bio", p.getBio());
        out.put("stats", student ? stats : List.of());
        out.put("personal", personal);
        if (student) {
            // guardian and academic information are student-only (Ask 2)
            out.put("academic", academicInfo); out.put("guardian", guardian);
        }
        out.put("preferences", prefs);
        return out;
    }

    /** Editable fields of the caller's own profile; any key absent from the body is left unchanged. */
    @Schema(description = "Partial update of the caller's profile; absent keys are unchanged")
    public record ProfileUpdate(@Schema(example = "Sam Student", description = "legacy: split into first/last when title/firstName/lastName are absent") String fullName, @Schema(example = "Mr", allowableValues = {"Mr", "Ms", "Dr", ""}) String title, @Schema(example = "Sam") String firstName, @Schema(example = "Student") String lastName, @Schema(example = "male", allowableValues = {"male", "female"}) String gender,
                                @Schema(example = "(408) 555-0123") String phone, @Schema(example = "Sunnyvale, CA") String location, @Schema(example = "I enjoy math and computer science.") String bio, @Schema(example = "1234 Oak Ave, Sunnyvale, CA 94086") String address, @Schema(example = "student1@learninghub.local") String email,
                                @Schema(example = "{\"timezone\":\"Pacific Time (PT)\",\"notifications\":\"Email only\"}") Map<String, String> preferences, @Schema(example = "{\"name\":\"Sarah Smith\",\"relation\":\"Parent/Guardian\",\"email\":\"sarah.smith@gmail.com\",\"phone\":\"(408) 555-0124\"}") Map<String, String> guardian) {}

    @Operation(summary = "Update my profile", description = "Any subset of title, firstName, lastName, gender, phone, location, bio, address, email, preferences{timezone,notifications}, guardian{name,relation,email,phone}. Name and email are mirrored to Keycloak.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"id\":1,\"username\":\"student1\",\"name\":\"Sam Student\",\"title\":null,\"firstName\":\"Sam\",\"lastName\":\"Student\",\"gender\":\"male\",\"avatarUrl\":\"/api/users/avatar/student1\",\"grade\":\"Junior\",\"role\":\"Student\",\"email\":\"student1@learninghub.local\",\"phone\":\"(408) 555-0123\",\"location\":\"Sunnyvale, CA\",\"bio\":\"I enjoy math and computer science.\",\"stats\":[{\"label\":\"Enrolled Classes\",\"value\":\"5\",\"sub\":\"2026 - 2027 year\",\"color\":\"blue\",\"icon\":\"classes\"}],\"personal\":{\"fullName\":\"Sam Student\",\"dateOfBirth\":\"Mar 15, 2010\",\"grade\":\"11\",\"school\":\"Fremont High School\",\"email\":\"student1@learninghub.local\",\"phone\":\"(408) 555-0123\",\"address\":\"Sunnyvale, CA\"},\"academic\":{\"currentGrade\":\"Junior\",\"academicYear\":\"2026 - 2027\",\"school\":\"Fremont High School\",\"classes\":[{\"name\":\"AP Pre Calculus\",\"color\":\"blue\"}]},\"guardian\":{\"name\":\"Sarah Smith\",\"relation\":\"Parent/Guardian\",\"email\":\"sarah.smith@gmail.com\",\"phone\":\"(408) 555-0124\"},\"preferences\":{\"timezone\":\"Pacific Time (PT)\",\"notifications\":\"Email & Push\"}}")}))
    @PutMapping("/profile")
    @Transactional
    public Map<String, Object> update(@RequestBody ProfileUpdate body) {
        Profile p = mine();
        if (body.title() != null) p.setTitle(body.title().isBlank() ? null : body.title().trim());
        if (body.firstName() != null && !body.firstName().isBlank()) p.setFirstName(body.firstName().trim());
        if (body.lastName() != null) p.setLastName(body.lastName().trim());
        if (body.gender() != null) p.setGender(body.gender().isBlank() ? null : body.gender().trim().toLowerCase());
        if (body.fullName() != null && !body.fullName().isBlank() && body.firstName() == null && body.lastName() == null) {
            String[] parts = body.fullName().trim().split("\\s+", 2);
            p.setFirstName(parts[0]); p.setLastName(parts.length > 1 ? parts[1] : "");
        }
        p.recomputeFullName();
        if (body.phone() != null) p.setPhone(body.phone());
        if (body.location() != null) p.setLocation(body.location());
        if (body.bio() != null) p.setBio(body.bio());
        if (body.address() != null) p.setAddress(body.address());
        if (body.email() != null) p.setEmail(body.email());
        if (body.preferences() != null) {
            Map<String, String> pr = body.preferences();
            if (pr.get("timezone") != null) p.setPrefTimezone(pr.get("timezone"));
            if (pr.get("notifications") != null) p.setPrefNotifications(pr.get("notifications"));
        }
        if (body.guardian() != null) {
            Map<String, String> g = body.guardian();
            if (g.get("name") != null) p.setGuardianName(g.get("name"));
            if (g.get("relation") != null) p.setGuardianRelation(g.get("relation"));
            if (g.get("email") != null) p.setGuardianEmail(g.get("email"));
            if (g.get("phone") != null) p.setGuardianPhone(g.get("phone"));
        }
        profiles.save(p);
        // keep Keycloak's name/email in step so the header greeting and login email match (best effort)
        if (body.fullName() != null || body.firstName() != null || body.lastName() != null || body.email() != null) {
            try {
                KeycloakAdminClient.KcUser kc = keycloak.findByUsername(p.getUsername());
                if (kc != null) {
                    keycloak.updateUser(kc.id(), p.getFirstName(), p.getLastName() == null ? "" : p.getLastName(), p.getEmail(), null, null, null);
                }
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(ProfileViewController.class).warn("Keycloak sync skipped: {}", e.getMessage());
            }
        }
        return profile();
    }
}
