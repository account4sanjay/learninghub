package com.learninghub.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Keycloak Admin REST API access (learninghub.keycloak.* in application.yml / env). */
@ConfigurationProperties(prefix = "learninghub.keycloak")
public record KeycloakProps(String baseUrl, String realm, String clientId, String clientSecret) {
    public String realmUrl() { return baseUrl + "/realms/" + realm; }
    public String adminUrl() { return baseUrl + "/admin/realms/" + realm; }
}
