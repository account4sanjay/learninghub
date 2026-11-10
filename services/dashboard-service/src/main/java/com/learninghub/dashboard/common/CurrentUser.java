package com.learninghub.dashboard.common;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the caller's identity from the validated Keycloak JWT. */
public final class CurrentUser {
    private CurrentUser() {}

    public static Jwt jwt() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a != null && a.getPrincipal() instanceof Jwt j ? j : null;
    }

    public static String username() {
        Jwt j = jwt();
        return j == null ? "anonymous" : j.getClaimAsString("preferred_username");
    }

    public static String firstName() {
        Jwt j = jwt();
        if (j == null) return "there";
        String g = j.getClaimAsString("given_name");
        return g != null && !g.isBlank() ? g : username();
    }

    private static final Set<String> APP_ROLES = Set.of("admin", "teacher", "student");

    /** The caller's application roles (realm_access.roles ∩ {admin, teacher, student}). */
    public static List<String> roles() {
        Jwt j = jwt();
        if (j == null) return List.of();
        Map<String, Object> realm = j.getClaimAsMap("realm_access");
        if (realm == null || !(realm.get("roles") instanceof List<?> rs)) return List.of();
        return rs.stream().map(Object::toString).filter(APP_ROLES::contains).toList();
    }

    public static boolean hasRole(String role) { return roles().contains(role); }
    public static boolean isAdmin() { return hasRole("admin"); }
    public static boolean isTeacher() { return hasRole("teacher"); }
    public static boolean isStudent() { return hasRole("student"); }

    public static String bearerToken() {
        Jwt j = jwt();
        return j == null ? null : j.getTokenValue();
    }
}
