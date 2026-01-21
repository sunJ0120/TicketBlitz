package com.example.be.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@SecurityScheme(
    name = "BearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Access Token을 입력해주세요.")
@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info().title("TICKETBLITZ API").version("1.0").description("콘서트 티켓팅 서비스 API"))
        .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
  }

  // Auth 도메인
  @Bean
  public GroupedOpenApi authApi() {
    return GroupedOpenApi.builder().group("1. Auth").pathsToMatch("/api/v1/auth/**").build();
  }

  // Page (common) 도메인
  @Bean
  public GroupedOpenApi pageApi() {
    return GroupedOpenApi.builder().group("2. Page").pathsToMatch("/api/v1/pages/**").build();
  }

  // Concert 도메인
  @Bean
  public GroupedOpenApi concertApi() {
    return GroupedOpenApi.builder().group("3. Concert").pathsToMatch("/api/v1/concerts/**").build();
  }

  // Queue 도메인
  @Bean
  public GroupedOpenApi queueApi() {
    return GroupedOpenApi.builder().group("4. Queue").pathsToMatch("/api/v1/queue/**").build();
  }
}
