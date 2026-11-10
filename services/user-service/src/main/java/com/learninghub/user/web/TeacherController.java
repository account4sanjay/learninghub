package com.learninghub.user.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learninghub.user.common.Avatars;
import com.learninghub.user.domain.Profile;
import com.learninghub.user.repo.ProfileRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** Teacher directory (profiles with role Teacher) for pickers and name resolution in other services. */
@Tag(name = "Teachers", description = "Directory of profiles with role Teacher (used by the admin Subjects form).")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/teachers")
public class TeacherController {

    private final ProfileRepository profiles;

    public static Map<String, Object> summary(Profile p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", p.getUsername()); m.put("title", p.getTitle());
        m.put("firstName", p.getFirstName()); m.put("lastName", p.getLastName());
        m.put("displayName", p.displayName()); m.put("email", p.getEmail()); m.put("gender", p.getGender());
        m.put("avatarUrl", Avatars.urlFor(p.getUsername()));
        return m;
    }

    @Operation(summary = "List teachers", description = "All teacher profiles with display name (title + first + last) and avatar URL.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"username\":\"janderson\",\"title\":\"Dr\",\"firstName\":\"John\",\"lastName\":\"Anderson\",\"displayName\":\"Dr John Anderson\",\"email\":\"janderson@learninghub.local\",\"gender\":\"male\",\"avatarUrl\":\"/api/users/avatar/janderson\"}]")}))
    @GetMapping
    public List<Map<String, Object>> list() {
        return profiles.findByRoleIgnoreCaseOrderByLastNameAscFirstNameAsc("Teacher").stream().map(TeacherController::summary).toList();
    }

}
