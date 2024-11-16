package org.thingsboard.server.transport.http;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.thingsboard.server.transport.http.limits.IpRateLimitInterceptor;

@TbHttpTransportComponent
@Configuration
public class RateLimitsInterceptorsConfig implements WebMvcConfigurer {
    private final HttpTransportContext context;

    public RateLimitsInterceptorsConfig(HttpTransportContext context) {
        this.context = context;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new IpRateLimitInterceptor(context)).addPathPatterns("/api/v1/**");
    }
}
