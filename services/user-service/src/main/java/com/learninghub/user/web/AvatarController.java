package com.learninghub.user.web;

import java.util.Map;

import com.learninghub.user.common.Avatars;
import com.learninghub.user.common.CurrentUser;
import com.learninghub.user.common.NotFoundException;
import com.learninghub.user.domain.Profile;
import com.learninghub.user.repo.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Profile pictures. GET is public (loaded by <img> tags); it serves the stored
 * picture or the gender default. Upload replaces the caller's own picture.
 */
@Tag(name = "Avatars", description = "Profile pictures stored in the database (bytea). GET is public so <img> tags can load it; a default male/female SVG is served when nothing was uploaded.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class AvatarController {

    private final ProfileRepository profiles;

    @Operation(summary = "Get a user's picture (public)", description = "Returns the stored picture (image/png, image/jpeg, image/svg+xml, …) or the default avatar for the profile's gender. No token required.")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "image bytes", content = @Content(mediaType = "image/*"))
    @GetMapping("/avatar/{username}")
    public ResponseEntity<byte[]> avatar(@PathVariable String username) {
        Profile p = profiles.findByUsername(username).orElse(null);
        byte[] body; String type;
        if (p != null && p.hasAvatar()) {
            body = p.getAvatar(); type = p.getAvatarContentType();
        } else {
            body = Avatars.defaultFor(p == null ? null : p.getGender()); type = "image/svg+xml";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .cacheControl(CacheControl.noCache())
                .body(body);
    }

    /** Multipart field "file": the caller's own picture (≤ 2 MB, image/*). */
    @Operation(summary = "Upload my picture", description = "Replaces the caller's own picture (multipart part 'file', image/*, ≤ 2 MB).")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"avatarUrl\":\"/api/users/avatar/student1\",\"contentType\":\"image/png\",\"size\":1032}")}))
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "multipart form with one part named file (image/*, up to 2 MB)", content = @Content(mediaType = "multipart/form-data"))
    @PutMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> upload(@RequestPart("file") MultipartFile file) {
        String u = CurrentUser.username();
        Profile p = profiles.findByUsername(u).orElseThrow(() -> new NotFoundException("profile for", u));
        Avatars.store(p, file);
        profiles.save(p);
        return Map.of("username", u, "avatarUrl", Avatars.urlFor(u), "contentType", p.getAvatarContentType(), "bytes", p.getAvatar().length);
    }
}
