package com.iot.thingmodel.config;

import com.iot.thingmodel.common.Constants;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI thingModelOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(Constants.Api.OPENAPI_TITLE)
                        .version(Constants.Api.OPENAPI_VERSION)
                        .description(Constants.Api.OPENAPI_DESCRIPTION))
                .components(new Components().addSecuritySchemes(
                        Constants.Api.OPENAPI_SECURITY_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme(Constants.Api.OPENAPI_BEARER_SCHEME)
                                .bearerFormat(Constants.Api.OPENAPI_BEARER_FORMAT)))
                .addSecurityItem(
                        new SecurityRequirement().addList(Constants.Api.OPENAPI_SECURITY_SCHEME));
    }
}
