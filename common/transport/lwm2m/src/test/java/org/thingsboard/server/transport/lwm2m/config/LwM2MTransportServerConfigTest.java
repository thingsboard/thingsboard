/**
 * Copyright © 2016-2024 The Thingsboard Authors
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = LwM2MTransportServerConfig.class)
@ContextConfiguration(classes = {LwM2MTransportServerConfig.class}, loader = SpringBootContextLoader.class)
@TestPropertySource(properties = {
        "transport.sessions.report_timeout=10",
        "transport.lwm2m.security.recommended_ciphers=true",
        "transport.lwm2m.security.recommended_supported_groups=true",
        "transport.lwm2m.downlink_pool_size=10",
        "transport.lwm2m.uplink_pool_size=10",
        "transport.lwm2m.ota_pool_size=10",
        "transport.lwm2m.clean_period_in_sec=2",
        "transport.lwm2m.dtls.connection_id_length="

})
class LwM2MTransportServerConfigTest {

    @MockBean(name = "lwm2mServerCredentials")
    private SslCredentialsConfig credentialsConfig;

    @MockBean(name = "lwm2mTrustCredentials")
    private SslCredentialsConfig trustCredentialsConfig;

    @Autowired
    private LwM2MTransportServerConfig serverConfig;

    @Test
    void getDtlsConnectionIdLength_return_null_is_property_is_empty() {
        // note: transport.lwm2m.dtls.connect_id_length is set in TestPropertySource
        assertThat(serverConfig.getDtlsConnectionIdLength()).isNull();
    }
}