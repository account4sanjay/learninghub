package com.learninghub.user.web;

import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Profile of the currently logged-in user, taken from the Keycloak JWT.
 * User management itself (create/update/roles) is delegated to Keycloak.
 */
@Tag(name = "Me", description = "Identity claims from the caller's JWT.")
@RestController
@RequestMapping("/api/users")
public class MeController {

    @Operation(summary = "Who am I", description = "Username, name, email and app roles straight from the Keycloak token (also used by the SPA as a cold-start probe).")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = "application/json", examples = {@ExampleObject(name = "example", value = "{\"firstName\":\"Sam\",\"lastName\":\"Student\",\"email\":\"student1@learninghub.local\",\"username\":\"student1\",\"roles\":[\"student\"]}")}))
    @GetMapping("/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .toList();
        return Map.of(
                "username", jwt.getClaimAsString("preferred_username"),
                "firstName", String.valueOf(jwt.getClaimAsString("given_name")),
                "lastName", String.valueOf(jwt.getClaimAsString("family_name")),
                "email", String.valueOf(jwt.getClaimAsString("email")),
                "roles", roles);
    }
}
