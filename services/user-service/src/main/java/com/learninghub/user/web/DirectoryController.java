package com.learninghub.user.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.learninghub.user.common.Avatars;
import com.learninghub.user.domain.Profile;
import com.learninghub.user.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
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

/** Every profile, for admins and teachers (name resolution on school-wide pages). */
@Tag(name = "Directory", description = "Every app profile (admin and teacher roles only).")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/directory")
@PreAuthorize("hasAnyRole('admin', 'teacher')")
public class DirectoryController {

    private final ProfileRepository profiles;

    @Operation(summary = "User directory", description = "All profiles sorted by role, last and first name; students are included.")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "[{\"username\":\"admin1\",\"displayName\":\"Alice Admin\",\"role\":\"admin\",\"email\":\"admin1@learninghub.local\",\"avatarUrl\":\"/api/users/avatar/admin1\",\"title\":null,\"firstName\":\"Alice\",\"lastName\":\"Admin\"},{\"username\":\"gadams\",\"displayName\":\"Grace Adams\",\"role\":\"student\",\"email\":\"gadams@learninghub.local\",\"avatarUrl\":\"/api/users/avatar/gadams\",\"title\":null,\"firstName\":\"Grace\",\"lastName\":\"Adams\"}]")}))
    @GetMapping
    public List<Map<String, Object>> list() {
        return profiles.findAll(Sort.by("role", "lastName", "firstName")).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", p.getUsername()); m.put("displayName", p.displayName());
            m.put("role", p.getRole() == null ? null : p.getRole().toLowerCase());
            m.put("email", p.getEmail()); m.put("avatarUrl", Avatars.urlFor(p.getUsername()));
            m.put("title", p.getTitle()); m.put("firstName", p.getFirstName()); m.put("lastName", p.getLastName());
            return m;
        }).toList();
    }
}
