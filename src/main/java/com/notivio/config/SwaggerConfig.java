package com.notivio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 / Swagger UI configuration.
 * Access at: /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Bean
    public OpenAPI notivioOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Notivio API")
                        .description("AI-Powered Gmail Reminder and Deadline Detection System\n\n" +
                                "## Authentication\n" +
                                "1. Login via `GET /oauth2/authorization/google`\n" +
                                "2. Receive JWT in response\n" +
                                "3. Include `Authorization: Bearer <token>` in all requests")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Notivio Team")
                                .email("support@notivio.com"))
                        .license(new License().name("MIT")))
                .servers(List.of(
                        new Server().url(baseUrl).description("Current Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .name("bearerAuth")
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token from OAuth2 login")));
    }
}
