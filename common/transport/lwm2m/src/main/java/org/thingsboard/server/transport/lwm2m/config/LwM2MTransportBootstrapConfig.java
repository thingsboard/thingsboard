/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.lwm2m.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

@Slf4j
@Component
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MTransportBootstrapConfig implements LwM2MSecureServerConfig {

    @Getter
    @Value("${transport.lwm2m.bootstrap.id:}")
    private Integer id;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_address:}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.bootstrap.bind_port:}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.bootstrap.security.bind_address:}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.bootstrap.security.bind_port:}")
    private Integer securePort;

    @Bean
    @ConfigurationProperties(prefix = "transport.lwm2m.bootstrap.security.credentials")
    public SslCredentialsConfig lwm2mBootstrapCredentials() {
        return new SslCredentialsConfig("LWM2M Bootstrap DTLS Credentials", false);
    }

    @Autowired
    @Qualifier("lwm2mBootstrapCredentials")
    private SslCredentialsConfig credentialsConfig;

    @Override
    public SslCredentials getSslCredentials() {
        return this.credentialsConfig.getCredentials();
    }
}
