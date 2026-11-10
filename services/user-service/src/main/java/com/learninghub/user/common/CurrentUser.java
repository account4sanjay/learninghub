package com.learninghub.user.common;

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

    public static String bearerToken() {
        Jwt j = jwt();
        return j == null ? null : j.getTokenValue();
    }

    /** App roles carried by the JWT (realm_access.roles ∩ {admin, teacher, student}). */
    public static java.util.Set<String> roles() {
        Jwt j = jwt();
        if (j == null) return java.util.Set.of();
        java.util.Map<String, Object> ra = j.getClaimAsMap("realm_access");
        if (ra == null || !(ra.get("roles") instanceof java.util.List<?> list)) return java.util.Set.of();
        java.util.Set<String> out = new java.util.HashSet<>();
        for (Object r : list) if (APP_ROLES.contains(String.valueOf(r))) out.add(String.valueOf(r));
        return out;
    }

    private static final java.util.Set<String> APP_ROLES = java.util.Set.of("admin", "teacher", "student");

    public static boolean hasRole(String role) { return roles().contains(role); }
    public static boolean isAdmin() { return hasRole("admin"); }
    public static boolean isTeacher() { return hasRole("teacher"); }
    public static boolean isStudent() { return hasRole("student"); }
}
