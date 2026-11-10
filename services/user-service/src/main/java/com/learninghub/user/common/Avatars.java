package com.learninghub.user.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Set;

import com.learninghub.user.domain.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Default avatars (by gender) and upload validation. Pictures live in profile.avatar (bytea). */
public final class Avatars {
    private Avatars() {}

    public static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> TYPES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");

    public static String urlFor(String username) {
        return "/api/users/avatar/" + username;
    }

    public static byte[] defaultFor(String gender) {
        String file = "female".equalsIgnoreCase(gender) ? "static/avatar-female.svg" : "static/avatar-male.svg";
        try (InputStream in = new ClassPathResource(file).getInputStream()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Validates and stores an uploaded picture on the profile. */
    public static void store(Profile p, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file is required");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "picture must be 2 MB or smaller");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!TYPES.contains(type)) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "picture must be an image (png, jpeg, gif, webp, svg)");
        try {
            p.setAvatar(file.getBytes());
            p.setAvatarContentType(type);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
