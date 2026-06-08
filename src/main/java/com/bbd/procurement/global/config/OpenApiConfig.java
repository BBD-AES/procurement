package com.bbd.procurement.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String AUTH_USER_ID = "X-User-Id";
    private static final String AUTH_USER_ROLE = "X-User-Role";

    @Bean
    public OpenAPI procurementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Procurement Service API")
                        .version("v1")
                        .description("구매 도메인 백엔드 - Gateway에서 인증 후 X-User-Id, X-User-Role 헤더 전달"))
                .components(new Components()
                        .addSecuritySchemes(AUTH_USER_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(AUTH_USER_ID)
                                .description("사용자 사번"))
                        .addSecuritySchemes(AUTH_USER_ROLE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(AUTH_USER_ROLE)
                                .description("사용자 역할 (HQ_MANAGER, HQ_STAFF, BRANCH")))
                .security(List.of(
                        new SecurityRequirement().addList(AUTH_USER_ID),
                        new SecurityRequirement().addList(AUTH_USER_ROLE)
                ));
    }
}
