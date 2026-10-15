// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.cipher.CipherSuite;
import org.eclipse.californium.scandium.dtls.pskstore.AdvancedSinglePskStore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.transport.limits.InetAddressRateLimitStats;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The PSK store of the DTLS connector doesn't see the client's address: checks the IP rate limits are wired into the LwM2M server.
 */
@TestPropertySource(properties = {
        "transport.rate_limits.ip_limits_enabled=true",
        "transport.rate_limits.max_wrong_credentials_per_ip=3",
})
public class PskRateLimitLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Autowired
    private TransportRateLimitService rateLimitService;

    @Before
    public void clearRateLimits() {
        ipMap().clear();
    }

    @After
    public void resetRateLimits() {
        ipMap().clear();
    }

    @Test
    public void givenUnknownPskIdentities_whenClientKeepsTrying_thenItsAddressIsBlocked() throws Exception {
        for (int i = 0; i < 3; i++) {
            // The server does not answer an unknown identity: the client's handshake times out
            assertThat(registrationFailure("unknown-" + UUID.randomUUID())).contains("timeout");
        }
        assertThat(ipMap().values()).singleElement()
                .extracting(InetAddressRateLimitStats::getFailureCount).as("counted once per handshake").isEqualTo(3);

        assertThat(registrationFailure("unknown-" + UUID.randomUUID())).contains("ACCESS_DENIED");
    }

    private String registrationFailure(String pskIdentity) throws Exception {
        try {
            return tryRegister(pskIdentity) ? null : "no response";
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    private boolean tryRegister(String pskIdentity) throws Exception {
        CoapConfig.register();
        DtlsConfig.register();
        Configuration configuration = new Configuration();
        configuration.set(DtlsConfig.DTLS_ROLE, DtlsRole.CLIENT_ONLY);
        configuration.set(DtlsConfig.DTLS_RETRANSMISSION_TIMEOUT, 500, TimeUnit.MILLISECONDS);
        configuration.set(DtlsConfig.DTLS_MAX_RETRANSMISSIONS, 1);
        configuration.setAsList(DtlsConfig.DTLS_CIPHER_SUITES, CipherSuite.TLS_PSK_WITH_AES_128_CCM_8);
        DtlsConnectorConfig dtlsConfig = DtlsConnectorConfig.builder(configuration)
                .setAdvancedPskStore(new AdvancedSinglePskStore(pskIdentity, new byte[16]))
                .build();
        CoapEndpoint endpoint = new CoapEndpoint.Builder().setConfiguration(configuration).setConnector(new DTLSConnector(dtlsConfig)).build();
        CoapClient client = new CoapClient(SECURE_URI + "/rd?ep=" + pskIdentity + "&lt=300&lwm2m=1.1&b=U");
        client.setEndpoint(endpoint);
        client.setTimeout(5000L);
        try {
            return client.post("</1/0>,</3/0>", MediaTypeRegistry.APPLICATION_LINK_FORMAT) != null;
        } finally {
            client.shutdown();
            endpoint.destroy();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<InetAddress, InetAddressRateLimitStats> ipMap() {
        Object target = AopTestUtils.getUltimateTargetObject(rateLimitService);
        return (Map<InetAddress, InetAddressRateLimitStats>) ReflectionTestUtils.getField(target, "ipMap");
    }

}
