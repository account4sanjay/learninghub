package com.learninghub.user.web;

import java.util.List;
import java.util.Map;

import com.learninghub.user.common.KeycloakAdminClient;
import com.learninghub.user.common.KeycloakAdminClient.KcUser;
import com.learninghub.user.domain.Profile;
import com.learninghub.user.repo.ProfileRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Admin-only user management. Identities, passwords, roles and the enabled
 * flag live in Keycloak (so a suspended user cannot log in and a role change
 * shows up in the next JWT); the profile table is kept in sync as a mirror.
 */
@Tag(name = "Admin · users", description = "Create, update, suspend/activate and delete accounts through the Keycloak Admin API (admin role only). The app profile mirrors title, names, gender and grade level.")
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/users/admin")
@PreAuthorize("hasRole('admin')")
public class AdminUserController {

    private final KeycloakAdminClient keycloak;
    private final ProfileRepository profiles;

    @Schema(description = "New account; identity goes to Keycloak, title/gender/gradeLevel to the app profile")
    public record NewUser(@Schema(example = "tnguyen") @NotBlank @Pattern(regexp = "[a-zA-Z0-9._-]{3,64}") String username, @Schema(example = "Taylor") @NotBlank String firstName,
                          @Schema(example = "Nguyen") @NotBlank String lastName, @Schema(example = "tnguyen@learninghub.local") String email, @Schema(example = "student123") @NotBlank String password,
                          @Schema(example = "student", allowableValues = {"admin", "teacher", "student"}) @NotBlank @Pattern(regexp = "admin|teacher|student") String role, @Schema(example = "true") Boolean enabled,
                          @Schema(example = "Ms", allowableValues = {"Mr", "Ms", "Dr", ""}) @Pattern(regexp = "Mr|Ms|Dr|") String title, @Schema(example = "female", allowableValues = {"male", "female", ""}) @Pattern(regexp = "male|female|") String gender,
                          @Schema(example = "junior", allowableValues = {"freshman", "sophomore", "junior", "senior", ""}) @Pattern(regexp = "freshman|sophomore|junior|senior|") String gradeLevel) {}

    @Schema(description = "Partial update; absent keys are left unchanged")
    public record UserUpdate(@Schema(example = "John") String firstName, @Schema(example = "Anderson") String lastName, @Schema(example = "janderson@learninghub.local") String email, @Schema(example = "teacher123") String password,
                             @Schema(example = "teacher", allowableValues = {"admin", "teacher", "student"}) @Pattern(regexp = "admin|teacher|student") String role, @Schema(example = "true") Boolean enabled,
                             @Schema(example = "Dr", allowableValues = {"Mr", "Ms", "Dr", ""}) @Pattern(regexp = "Mr|Ms|Dr|") String title, @Schema(example = "female", allowableValues = {"male", "female", ""}) @Pattern(regexp = "male|female|") String gender,
                             @Pattern(regexp = "freshman|sophomore|junior|senior|") String gradeLevel) {}

    private final com.learninghub.user.common.Academic academic;

    /** Keycloak identity + the profile-table extras the admin UI shows. */
    private Map<String, Object> view(KcUser u) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", u.id()); m.put("username", u.username()); m.put("firstName", u.firstName()); m.put("lastName", u.lastName());
        m.put("email", u.email()); m.put("enabled", u.enabled()); m.put("role", u.role()); m.put("createdTimestamp", u.createdTimestamp());
        Profile p = profiles.findByUsername(u.username()).orElse(null);
        m.put("title", p == null ? null : p.getTitle()); m.put("gender", p == null ? null : p.getGender());
        m.put("gradeLevel", p == null ? null : p.getGradeLevel()); m.put("grade", p == null ? null : academic.gradeName(p.getGradeLevel()));
        m.put("displayName", p == null ? ((u.firstName() == null ? "" : u.firstName() + " ") + (u.lastName() == null ? "" : u.lastName())).trim() : p.displayName());
        m.put("avatarUrl", com.learninghub.user.common.Avatars.urlFor(u.username()));
        m.put("hasAvatar", p != null && p.hasAvatar());
        return m;
    }

    /** one user row (as returned by list / create / update / suspend / activate / avatar) */
    private static final String USER_JSON = "{\"id\":\"b20b44e9-f986-4e1d-83e4-de90a0e21c8f\",\"username\":\"aclark\",\"firstName\":\"Aiden\",\"lastName\":\"Clark\",\"email\":\"aclark@learninghub.local\",\"enabled\":true,\"role\":\"student\",\"createdTimestamp\":1790446226406,\"title\":null,\"gender\":\"male\",\"gradeLevel\":\"sophomore\",\"grade\":\"Sophomore\",\"displayName\":\"Aiden Clark\",\"avatarUrl\":\"/api/users/avatar/aclark\",\"hasAvatar\":false}";


    @Operation(summary = "List users", description = "Every Keycloak user (service accounts excluded) with app role, enabled flag and profile extras.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"id\":\"b20b44e9-f986-4e1d-83e4-de90a0e21c8f\",\"username\":\"aclark\",\"firstName\":\"Aiden\",\"lastName\":\"Clark\",\"email\":\"aclark@learninghub.local\",\"enabled\":true,\"role\":\"student\",\"createdTimestamp\":1790446226406,\"title\":null,\"gender\":\"male\",\"gradeLevel\":\"sophomore\",\"grade\":\"Sophomore\",\"displayName\":\"Aiden Clark\",\"avatarUrl\":\"/api/users/avatar/aclark\",\"hasAvatar\":false}]")}))
    @GetMapping("/users")
    public List<Map<String, Object>> list() {
        return keycloak.listUsers().stream().map(this::view).toList();
    }

    @Operation(summary = "Create a user", description = "Creates the Keycloak account (password, role, enabled) and the app profile (title, gender, grade level).")
    @ApiResponse(responseCode = "201", description = "Created user", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "user", value = USER_JSON)))
    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody NewUser body) {
        KcUser u = keycloak.createUser(body.username(), body.firstName(), body.lastName(), body.email(), body.password(),
                body.role(), body.enabled() == null || body.enabled());
        syncProfile(u, body.title(), body.gender(), body.gradeLevel());
        return view(u);
    }

    @Operation(summary = "Update a user", description = "Any subset of name, email, password, role, enabled, title, gender, gradeLevel; absent keys are unchanged.")
    @ApiResponse(responseCode = "200", description = "Updated user", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "user", value = USER_JSON)))
    @PutMapping("/users/{id}")
    @Transactional
    public Map<String, Object> update(@PathVariable String id, @Valid @RequestBody UserUpdate body) {
        KcUser u = keycloak.updateUser(id, body.firstName(), body.lastName(), body.email(), body.enabled(), body.role(), body.password());
        syncProfile(u, body.title(), body.gender(), body.gradeLevel());
        return view(u);
    }

    /** Multipart field "file": store a picture for any user (≤ 2 MB, image/*). */
    @Operation(summary = "Upload a user's picture", description = "Stores the picture in the profile table; it is then served at /api/users/avatar/{username}.")
    @ApiResponse(responseCode = "200", description = "User with the new picture", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "user", value = USER_JSON)))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "multipart form with one part named file (image/*, up to 2 MB)", content = @Content(mediaType = "multipart/form-data"))
    @PostMapping(value = "/users/{id}/avatar", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> avatar(@PathVariable String id, @org.springframework.web.bind.annotation.RequestPart("file") org.springframework.web.multipart.MultipartFile file) {
        KcUser u = keycloak.getUser(id);
        Profile p = profiles.findByUsername(u.username()).orElseGet(() -> { syncProfile(u, null, null, null); return profiles.findByUsername(u.username()).orElseThrow(); });
        com.learninghub.user.common.Avatars.store(p, file);
        profiles.save(p);
        return view(u);
    }

    @Operation(summary = "Suspend a user", description = "Sets enabled=false in Keycloak; the user gets 'Account disabled' at login. You cannot suspend yourself.")
    @ApiResponse(responseCode = "200", description = "Suspended user (enabled = false)", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "user", value = USER_JSON)))
    @PostMapping("/users/{id}/suspend")
    public Map<String, Object> suspend(@PathVariable String id) {
        guardSelf(id, "suspend");
        keycloak.setEnabled(id, false);
        return view(keycloak.getUser(id));
    }

    @Operation(summary = "Activate a user", description = "Sets enabled=true in Keycloak.")
    @ApiResponse(responseCode = "200", description = "Activated user (enabled = true)", content = @Content(mediaType = "application/json", examples = @ExampleObject(name = "user", value = USER_JSON)))
    @PostMapping("/users/{id}/activate")
    public Map<String, Object> activate(@PathVariable String id) {
        keycloak.setEnabled(id, true);
        return view(keycloak.getUser(id));
    }

    @Operation(summary = "Delete a user", description = "Removes the Keycloak account and the app profile. You cannot delete yourself.")
    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable String id) {
        guardSelf(id, "delete");
        KcUser u = keycloak.getUser(id);
        keycloak.deleteUser(id);
        profiles.findByUsername(u.username()).ifPresent(profiles::delete);
    }

    private void guardSelf(String id, String action) {
        String me = com.learninghub.user.common.CurrentUser.username();
        KcUser u = keycloak.getUser(id);
        if (u.username().equals(me))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot " + action + " your own account");
    }

    /** Mirror name / email / role (+ title, gender, grade level when given) into the profile table (created on first sight). */
    private void syncProfile(KcUser u, String title, String gender, String gradeLevel) {
        Profile p = profiles.findByUsername(u.username()).orElseGet(() -> {
            Profile n = new Profile();
            n.setUsername(u.username());
            return n;
        });
        if (u.firstName() != null) p.setFirstName(u.firstName());
        if (u.lastName() != null) p.setLastName(u.lastName());
        if (title != null) p.setTitle(title.isBlank() ? null : title);
        if (gender != null) p.setGender(gender.isBlank() ? null : gender.toLowerCase());
        p.recomputeFullName();
        if (u.email() != null) p.setEmail(u.email());
        if (u.role() != null) p.setRole(Character.toUpperCase(u.role().charAt(0)) + u.role().substring(1));
        if (gradeLevel != null && !gradeLevel.isBlank()) p.setGradeLevel(gradeLevel.toLowerCase());
        profiles.save(p);
    }
}
