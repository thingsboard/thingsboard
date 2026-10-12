// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.coap.security.sql;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocationRequest;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import org.thingsboard.server.transport.coap.security.AbstractCoapSecurityIntegrationTest;
import org.thingsboard.server.transport.coap.x509.CertPrivateKey;
import org.thingsboard.server.transport.coap.x509.CoapClientX509Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * CoAP over DTLS with X.509 device certificates: revocation of the certificate of an active DTLS session and IP rate limits.
 */
@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "transport.rate_limits.ip_limits_enabled=true",
        "transport.rate_limits.max_wrong_credentials_per_ip=3",
})
public class CoapClientX509RevocationIntegrationTest extends AbstractCoapSecurityIntegrationTest {

    @Autowired
    private TransportRateLimitService rateLimitService;

    private DeviceProfile deviceProfile;

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest();
        ipMap().clear();
        deviceProfile = createCoapDeviceProfile(CoapTestConfigProperties.builder()
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build());
    }

    @After
    public void afterTest() {
        ipMap().clear();
    }

    @Test
    public void givenActiveDtlsSession_whenCertificateIsRevoked_thenNextRequestOfTheSessionIsRejected() throws Exception {
        CertPrivateKey certPrivateKey = new CertPrivateKey(CREDENTIALS_PATH_CLIENT_CERT_PEM, CREDENTIALS_PATH_CLIENT_KEY_PEM);
        Device device = createDeviceWithX509("CoapX509Revoked", deviceProfile.getId(), certPrivateKey.getCert());
        CoapClientX509Test client = new CoapClientX509Test(certPrivateKey, FeatureType.TELEMETRY, COAPS_BASE_URL, null);
        try {
            assertThat(client.postMethod(PAYLOAD_VALUES_STR).getCode()).isEqualTo(CoAP.ResponseCode.CREATED);

            revoke(device);

            // The transport keeps the device of a DTLS session: the revocation reaches it as a broadcast
            await().atMost(10, TimeUnit.SECONDS).untilAsserted(() ->
                    assertThat(client.postMethod(PAYLOAD_VALUES_STR).getCode()).isEqualTo(CoAP.ResponseCode.UNAUTHORIZED));
        } finally {
            client.disconnect();
        }
    }

    @Test
    public void givenRevokedCertificate_whenItKeepsReconnecting_thenTheAddressIsBlocked() throws Exception {
        CertPrivateKey certPrivateKey = new CertPrivateKey(CREDENTIALS_PATH_CLIENT + "cert_01.pem", CREDENTIALS_PATH_CLIENT + "key_01.pem");
        Device device = createDeviceWithX509("CoapX509Blocked", deviceProfile.getId(), certPrivateKey.getCert());
        revoke(device);

        for (int i = 0; i < 3; i++) {
            // Every attempt is a new DTLS handshake from a new port
            CoapClientX509Test client = new CoapClientX509Test(certPrivateKey, FeatureType.TELEMETRY, COAPS_BASE_URL, null);
            try {
                assertThat(client.postMethod(PAYLOAD_VALUES_STR).getCode()).isEqualTo(CoAP.ResponseCode.UNAUTHORIZED);
            } finally {
                client.disconnect();
            }
        }

        CoapClientX509Test blockedClient = new CoapClientX509Test(certPrivateKey, FeatureType.TELEMETRY, COAPS_BASE_URL, null);
        try {
            assertThatThrownBy(() -> blockedClient.postMethod(PAYLOAD_VALUES_STR))
                    .hasRootCauseInstanceOf(HandshakeException.class)
                    .hasMessageContaining("ACCESS_DENIED");
        } finally {
            blockedClient.disconnect();
        }
        CoapTestClient tokenClient = new CoapTestClient("token-of-a-device-behind-the-same-address", FeatureType.TELEMETRY);
        try {
            CoapResponse response = tokenClient.postMethod(PAYLOAD_VALUES_STR);
            assertThat(response.getCode()).isEqualTo(CoAP.ResponseCode.TOO_MANY_REQUESTS);
        } finally {
            tokenClient.disconnect();
        }
    }

    private void revoke(Device device) {
        doPost("/api/device/" + device.getId().getId() + "/credentials/revoke", new DeviceCertificateRevocationRequest(), DeviceCertificateRevocation.class);
    }

    private Map<?, ?> ipMap() {
        Object target = AopTestUtils.getUltimateTargetObject(rateLimitService);
        return (Map<?, ?>) ReflectionTestUtils.getField(target, "ipMap");
    }

}
