package com.learninghub.dashboard.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.Scopes;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata: Keycloak password-flow security — "Authorize" in Swagger UI logs in by username/password (Ask 32: single scheme, no bearer box that could override the token). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI learninghubOpenApi() {
        return new OpenAPI()
                .info(new Info().title("LearningHub · Dashboard Service").version("v1")
                        .description("Role-specific dashboard aggregation (analytics, today) computed live from the other services on every call; the service stores nothing.\n\n"
                                + "Click **Authorize**, enter a username and password and Swagger fetches the Keycloak token itself (password flow, client learninghub-frontend); "
                                + "Tokens last 30 minutes: when calls start returning 401, click Authorize → Logout → Authorize again. Demo users: admin1/admin123, janderson/teacher123, student1/student123."))
                .servers(List.of(new Server().url("/").description("through the gateway")))
                .components(new Components()
                        // Ask 31: Authorize logs in through Keycloak's password flow — no external curl needed
                        .addSecuritySchemes("keycloak", new SecurityScheme().type(SecurityScheme.Type.OAUTH2)
                                .description("Log in with a LearningHub username and password (client learninghub-frontend, no secret); the token is fetched from Keycloak through the gateway and lasts 30 minutes.")
                                .flows(new OAuthFlows().password(new OAuthFlow()
                                        .tokenUrl("/auth/realms/learninghub/protocol/openid-connect/token")
                                        .scopes(new Scopes().addString("openid", "identity")))))
                        )
                .addSecurityItem(new SecurityRequirement().addList("keycloak"));
    }
}
