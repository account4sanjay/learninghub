package com.learninghub.user.common;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.learninghub.user.config.KeycloakProps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thin wrapper over the Keycloak Admin REST API, authenticated with the
 * learninghub-user-admin service account (client_credentials). Keycloak stays
 * the single source of truth for identities, credentials, roles and the
 * enabled flag; this service mirrors name/role/email into its profile table.
 */
@Slf4j
@Component
public class KeycloakAdminClient {

    public static final Set<String> APP_ROLES = Set.of("admin", "teacher", "student");

    private final KeycloakProps props;
    private final RestClient http = RestClient.create();
    private String token;
    private Instant tokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(KeycloakProps props) {
        this.props = props;
    }

    /** A Keycloak user as the admin UI sees it. */
    public record KcUser(String id, String username, String firstName, String lastName, String email,
                         boolean enabled, String role, Long createdTimestamp) {}

    // ---------------------------------------------------------------- auth

    private synchronized String token() {
        if (token != null && Instant.now().isBefore(tokenExpiry.minusSeconds(15))) return token;
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        Map<String, Object> res = http.post().uri(props.realmUrl() + "/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(new ParameterizedTypeReference<>() {});
        token = (String) res.get("access_token");
        tokenExpiry = Instant.now().plusSeconds(((Number) res.getOrDefault("expires_in", 60)).longValue());
        return token;
    }

    private RestClient.RequestHeadersSpec<?> get(String path) {
        return http.get().uri(props.adminUrl() + path).header("Authorization", "Bearer " + token());
    }

    private <T> T call(java.util.function.Supplier<T> op, String what) {
        try {
            return op.get();
        } catch (HttpClientErrorException e) {
            String msg = e.getResponseBodyAsString();
            log.warn("Keycloak {} failed: {} {}", what, e.getStatusCode(), msg);
            HttpStatus status = e.getStatusCode() == HttpStatus.CONFLICT ? HttpStatus.CONFLICT
                    : e.getStatusCode() == HttpStatus.NOT_FOUND ? HttpStatus.NOT_FOUND : HttpStatus.BAD_GATEWAY;
            String detail = msg == null || msg.isBlank() ? e.getStatusText() : msg.replaceAll("[{}\"]", "").replace("errorMessage:", "").trim();
            throw new ResponseStatusException(status, "Keycloak: " + detail);
        }
    }

    // --------------------------------------------------------------- users

    public List<KcUser> listUsers() {
        return call(() -> {
            List<Map<String, Object>> raw = get("/users?max=500&briefRepresentation=true").retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            Map<String, String> roleOf = new HashMap<>();
            for (String role : APP_ROLES) {
                List<Map<String, Object>> members = get("/roles/" + role + "/users?max=500").retrieve()
                        .body(new ParameterizedTypeReference<>() {});
                if (members != null) members.forEach(m -> roleOf.put((String) m.get("id"), role));
            }
            List<KcUser> out = new ArrayList<>();
            for (Map<String, Object> u : raw) {
                String username = (String) u.get("username");
                if (username.startsWith("service-account-")) continue;
                out.add(toUser(u, roleOf.get((String) u.get("id"))));
            }
            out.sort((a, b) -> a.username().compareToIgnoreCase(b.username()));
            return out;
        }, "list users");
    }

    public KcUser getUser(String id) {
        return call(() -> {
            Map<String, Object> u = get("/users/" + id).retrieve().body(new ParameterizedTypeReference<>() {});
            return toUser(u, roleFor(id));
        }, "get user");
    }

    public KcUser findByUsername(String username) {
        return call(() -> {
            List<Map<String, Object>> raw = get("/users?exact=true&username=" + username).retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (raw == null || raw.isEmpty()) return null;
            Map<String, Object> u = raw.get(0);
            return toUser(u, roleFor((String) u.get("id")));
        }, "find user");
    }

    public KcUser createUser(String username, String firstName, String lastName, String email, String password,
                             String role, boolean enabled) {
        return call(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("username", username); body.put("firstName", firstName); body.put("lastName", lastName);
            body.put("email", email); body.put("enabled", enabled); body.put("emailVerified", true);
            if (password != null && !password.isBlank())
                body.put("credentials", List.of(Map.of("type", "password", "value", password, "temporary", false)));
            URI location = http.post().uri(props.adminUrl() + "/users").header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity().getHeaders().getLocation();
            String id = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);
            setRole(id, role);
            return getUser(id);
        }, "create user");
    }

    public KcUser updateUser(String id, String firstName, String lastName, String email, Boolean enabled, String role, String password) {
        return call(() -> {
            Map<String, Object> body = new LinkedHashMap<>();
            if (firstName != null) body.put("firstName", firstName);
            if (lastName != null) body.put("lastName", lastName);
            if (email != null) body.put("email", email);
            if (enabled != null) body.put("enabled", enabled);
            if (!body.isEmpty())
                http.put().uri(props.adminUrl() + "/users/" + id).header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
            if (password != null && !password.isBlank())
                http.put().uri(props.adminUrl() + "/users/" + id + "/reset-password").header("Authorization", "Bearer " + token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("type", "password", "value", password, "temporary", false)).retrieve().toBodilessEntity();
            if (role != null) setRole(id, role);
            return getUser(id);
        }, "update user");
    }

    public void setEnabled(String id, boolean enabled) {
        updateUser(id, null, null, null, enabled, null, null);
    }

    public void deleteUser(String id) {
        call(() -> http.delete().uri(props.adminUrl() + "/users/" + id).header("Authorization", "Bearer " + token())
                .retrieve().toBodilessEntity(), "delete user");
    }

    // --------------------------------------------------------------- roles


    private String roleFor(String userId) {
        List<Map<String, Object>> mapped = get("/users/" + userId + "/role-mappings/realm").retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (mapped == null) return null;
        return mapped.stream().map(m -> (String) m.get("name")).filter(APP_ROLES::contains).findFirst().orElse(null);
    }

    /** Gives the user exactly one application role (admin | teacher | student). */
    private void setRole(String userId, String role) {
        if (role == null) return;
        if (!APP_ROLES.contains(role)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unknown role " + role);
        List<Map<String, Object>> mapped = get("/users/" + userId + "/role-mappings/realm").retrieve()
                .body(new ParameterizedTypeReference<>() {});
        List<Map<String, Object>> remove = mapped == null ? List.of() : mapped.stream()
                .filter(m -> APP_ROLES.contains((String) m.get("name")) && !role.equals(m.get("name"))).toList();
        if (!remove.isEmpty())
            http.method(org.springframework.http.HttpMethod.DELETE).uri(props.adminUrl() + "/users/" + userId + "/role-mappings/realm")
                    .header("Authorization", "Bearer " + token()).contentType(MediaType.APPLICATION_JSON).body(remove)
                    .retrieve().toBodilessEntity();
        boolean has = mapped != null && mapped.stream().anyMatch(m -> role.equals(m.get("name")));
        if (!has) {
            Map<String, Object> rep = get("/roles/" + role).retrieve().body(new ParameterizedTypeReference<>() {});
            http.post().uri(props.adminUrl() + "/users/" + userId + "/role-mappings/realm").header("Authorization", "Bearer " + token())
                    .contentType(MediaType.APPLICATION_JSON).body(List.of(Map.of("id", rep.get("id"), "name", role)))
                    .retrieve().toBodilessEntity();
        }
    }

    private static KcUser toUser(Map<String, Object> u, String role) {
        return new KcUser((String) u.get("id"), (String) u.get("username"), (String) u.get("firstName"), (String) u.get("lastName"),
                (String) u.get("email"), Boolean.TRUE.equals(u.get("enabled")), role,
                u.get("createdTimestamp") == null ? null : ((Number) u.get("createdTimestamp")).longValue());
    }
}
