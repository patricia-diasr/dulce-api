package com.dulce.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dulceOpenApi() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Dulce API")
                                .description(
                                        "API do sistema de gestão de encomendas da confeitaria Dulce.")
                                .version("v0"));
    }
}
