package com.iot.thingmodel.config;

import com.iot.thingmodel.auth.AuthInterceptor;
import com.iot.thingmodel.common.Constants;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns(Constants.Api.API_PATTERN)
                .excludePathPatterns(Constants.Api.ERROR_PATH);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(Constants.Api.API_PATTERN)
                .allowedOrigins(Constants.Api.CORS_ORIGIN)
                .allowedMethods(Constants.Api.CORS_METHODS.toArray(String[]::new))
                .allowedHeaders(Constants.Api.CORS_ALL_HEADERS);
    }
}
