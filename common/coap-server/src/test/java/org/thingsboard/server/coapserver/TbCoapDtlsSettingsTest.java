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
package org.thingsboard.server.coapserver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TbCoapDtlsSettings.class)
@TestPropertySource(properties = {
        "coap.dtls.enabled=true",
        "coap.dtls.bind_address=192.168.1.1",
        "coap.dtls.bind_port=1234",
        "coap.dtls.retransmission_timeout=100",
        "coap.dtls.connection_id_length=500",
        "coap.dtls.x509.skip_validity_check_for_client_cert=true",
        "coap.dtls.x509.dtls_session_inactivity_timeout=1000",
        "coap.dtls.x509.dtls_session_report_timeout=3000",
})
class TbCoapDtlsSettingsTest {

    @Autowired
    TbCoapDtlsSettings coapDtlsSettings;
    @MockBean
    SslCredentialsConfig sslCredentialsConfig;
    @MockBean
    private TransportService transportService;
    @MockBean
    private TbServiceInfoProvider serviceInfoProvider;

    @Test
    public void testCoapDtlsProperties() {
        assertThat(coapDtlsSettings).as("bean created").isNotNull();
        assertThat(coapDtlsSettings.getHost()).as("host").isEqualTo("192.168.1.1");
        assertThat(coapDtlsSettings.getPort()).as("port").isEqualTo(1234);
        assertThat(coapDtlsSettings.getDtlsRetransmissionTimeout()).as("retransmission_timeout").isEqualTo(100);
        assertThat(coapDtlsSettings.isSkipValidityCheckForClientCert()).as("skip_validity_check_for_client_cert").isTrue();
        assertThat(coapDtlsSettings.getDtlsSessionInactivityTimeout()).as("dtls_session_inactivity_timeout").isEqualTo(1000);
        assertThat(coapDtlsSettings.getDtlsSessionReportTimeout()).as("dtls_session_report_timeout").isEqualTo(3000);
    }

}
