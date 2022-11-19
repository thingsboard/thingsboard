package org.thingsboard.monitoring.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.config.TransportType;
import org.thingsboard.monitoring.data.TransportInfo;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.Device;

import java.time.Duration;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TbRestClient extends RestClient {

    @Value("${monitoring.auth.username}")
    private String username;
    @Value("${monitoring.auth.password}")
    private String password;

    public TbRestClient(@Value("${monitoring.auth.base_url}") String baseUrl) {
        super(new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(2))
                .build(), baseUrl);
    }

    public String logIn() {
        login(username, password);
        return getToken();
    }

    public Device createDeviceForMonitoringIfNotExists(TransportInfo transportInfo, String deviceName) {
//        getTenantDevice(name)
//                .orElseGet(() -> {
//                    Device device =
//                })
    }

}
