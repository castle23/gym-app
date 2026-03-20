package com.gym.training.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI trainingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Training Service API")
                        .description("Exercise, routine, and training program management service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Gym Platform Team")
                                .email("support@gym-platform.com")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Bearer token for authentication")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
