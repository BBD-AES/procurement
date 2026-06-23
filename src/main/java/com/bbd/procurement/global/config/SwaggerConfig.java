package com.bbd.procurement.global.config;

import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("procurement API")
                        .description("procurement Application API Documentation - Keycloak JWT Bearer Token 인증")
                        .version("v1.0"))

                // ===== 서버 목록 =====
                .addServersItem(new Server()
                        .url("http://localhost:8084/procurement")
                        .description("local 도커 컴포즈 / 직접 띄울 때"))
                .addServersItem(new Server()
                        .url("http://localhost:8080/procurement")
                        .description("Local Local"))
                .addServersItem(new Server()
                        .url("http://192.168.201.110/procurement")
                        .description("Nginx"))
                .addServersItem(new Server()
                        .url("http://192.168.200.220/procurement")
                        .description("강의실 노트북"))
                .addServersItem(new Server()
                        .url("http://100.73.142.41/procurement")
                        .description("TailScale 강의실 노트북"))
                .addServersItem(new Server()
                        .url("http://112.218.95.58/procurement")
                        .description("External Nginx"))
                .addServersItem(new Server()
                        .url("https://bbd.inwoohub.com/procurement")
                        .description("ECS"))

                // ===== 인증 스킴: JWT Bearer =====
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Keycloak Access Token 입력"))

                        // Swagger 문서용 공통 헤더
                        .addParameters("IdempotencyKeyHeader",
                                new Parameter()
                                        .in("header")
                                        .name("Idempotency-Key")
                                        .required(false)
                                        .description("멱등 처리를 위한 요청 고유 키. POST 또는 상태 변경 PATCH 요청에서 사용")
                                        .schema(new StringSchema()
                                                .example("018f4c2e-7b8a-7c2f-9a01-2d4e9b7c1234"))))

                .security(List.of(
                        new SecurityRequirement().addList(BEARER_AUTH)
                ));
    }
}