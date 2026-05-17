package com.example.Config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;

import org.springframework.context.annotation.*;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()

            .info(
                new Info()

                .title(
                    "Rate Limiter API"
                )

                .version(
                    "1.0"
                )

                .description(
                    """
                    Distributed Rate Limiter
                    supporting:

                    Fixed Window
                    Sliding Window
                    Token Bucket
                    """
                )
            );
    }
}