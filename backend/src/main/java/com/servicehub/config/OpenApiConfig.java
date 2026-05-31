package com.servicehub.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste your JWT token here (without the 'Bearer ' prefix). Get a token from POST /api/auth/login."
)
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceHubOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ServiceHub API")
                        .description("Internal service request management system. " +
                                "Use POST /api/auth/login to obtain a JWT token, then click **Authorize** above.")
                        .version("1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
