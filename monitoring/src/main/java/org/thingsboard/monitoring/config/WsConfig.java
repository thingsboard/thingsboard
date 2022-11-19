package org.thingsboard.monitoring.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class WsConfig {

    @Value("${monitoring.ws.base_url}")
    private String baseUrl;
    @Value("${monitoring.ws.request_timeout_ms}")
    private int requestTimeoutMs;
    @Value("${monitoring.ws.check_timeout_ms}")
    private int resultCheckTimeoutMs;

}
