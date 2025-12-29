package com.chibao.edu.search_engine.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Search Engine API - Clean Architecture")
                        .version("2.0")
                        .description("Enterprise Search Engine with Clean Architecture + DDD"));
    }
}
