package com.banula.navigationservice.config;

import io.swagger.v3.oas.models.Operation;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("Authorization",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Authorization"))
                .info(new Info()
                        .title("Navigation Service")
                        .version("1.0"));
    }

    @Bean
    public OperationCustomizer hidePlatformRequestParameter() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            if (operation.getParameters() != null) {
                operation.getParameters().removeIf(parameter -> parameter.getSchema() != null &&
                        parameter.getSchema().get$ref() != null &&
                        parameter.getSchema().get$ref().contains("PlatformRequestValues"));
            }
            return operation;
        };
    }
}
