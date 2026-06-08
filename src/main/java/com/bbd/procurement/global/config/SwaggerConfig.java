package com.bbd.procurement.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String AUTH_USER_ID = "X-User-Id";
    private static final String AUTH_USER_ROLE = "X-User-Role";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // 1. 기존 SwaggerConfig의 정보 및 서버 설정 유지
                .info(new Info()
                        .title("procurement API")
                        .description("procurement Application API Documentation - Gateway에서 인증 후 X-User-Id, X-User-Role 헤더 전달")
                        .version("v1.0"))
                .addServersItem(new Server()
                        .url("http://localhost:8084/procurement")
                        .description("Local Direct"))
                .addServersItem(new Server()
                        .url("http://192.168.201.110/procurement")
                        .description("Nginx"))
                .addServersItem(new Server()
                        .url("http://112.218.95.58/procurement")
                        .description("External Nginx"))

                // 2. OpenApiConfig에 있던 Security 컴포넌트 추가 (병합)
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