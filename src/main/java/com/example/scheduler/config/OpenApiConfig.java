package com.example.scheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    @Bean
    OpenAPI schedulerServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scheduler API")
                        .description(
                                "Meeting scheduling platform: manage per-user time slots, book them into "
                                        + "meetings with participants")
                        .version("v1"));
    }
}
