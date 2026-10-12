// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.transport.lwm2m.security.sql;

import org.eclipse.californium.core.network.CoapEndpoint;
import org.eclipse.californium.core.network.Endpoint;
import org.eclipse.californium.scandium.DTLSConnector;
import org.eclipse.leshan.client.californium.endpoint.CaliforniumClientEndpointsProvider;
import org.eclipse.leshan.client.endpoint.LwM2mClientEndpointsProvider;
import org.eclipse.leshan.client.object.Security;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocation;
import org.thingsboard.server.common.data.security.DeviceCertificateRevocationRequest;
import org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.eclipse.leshan.client.object.Security.x509;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_FAILURE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_REGISTRATION_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_FAILURE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_SUCCESS;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MClientState.ON_UPDATE_TIMEOUT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

/**
 * In trust mode the core identifies a LwM2M client by its endpoint: the certificate is checked only in a full DTLS handshake.
 */
public class X509_TrustRevocationLwM2MIntegrationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Autowired
    private LwM2mClientContext lwM2mClientContext;

    @Test
    public void givenRegisteredClient_whenCertificateIsRevoked_thenResumedDtlsSessionCannotRegisterAgain() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST;
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert("");
        Security security = x509(SECURE_URI,
                shortServerId,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapSectionsServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        basicTestConnection(security, null,
                deviceCredentials,
                clientEndpoint,
                transportConfiguration,
                "await on client state (X509_Trust_Lwm2m)",
                expectedStatusesRegistrationLwm2mSuccess,
                true,
                ON_REGISTRATION_SUCCESS,
                true);

        // Trust mode credentials hold the endpoint only, so the certificate is revoked by its PEM
        DeviceCertificateRevocationRequest revocationRequest = new DeviceCertificateRevocationRequest();
        revocationRequest.setCertificate(toPem(certificate));
        doPost("/api/deviceCertificateRevocation", revocationRequest, DeviceCertificateRevocation.class);
        await("the transport unregisters the client").atMost(10, TimeUnit.SECONDS).until(() -> !isRegistered(clientEndpoint));

        // The client resumes its DTLS session, which skips the certificate verification of a full handshake
        Set<LwM2MClientState> clientStates = ConcurrentHashMap.newKeySet();
        lwM2MTestClient.setClientStates(clientStates);
        forceResumeDtlsSessions();
        lwM2MTestClient.getLeshanClient().triggerRegistrationUpdate();

        await("the client fails to update its registration").atMost(60, TimeUnit.SECONDS)
                .until(() -> clientStates.contains(ON_UPDATE_FAILURE) || clientStates.contains(ON_UPDATE_TIMEOUT)
                        || clientStates.contains(ON_REGISTRATION_FAILURE) || clientStates.contains(ON_REGISTRATION_TIMEOUT));
        await().during(5, TimeUnit.SECONDS).atMost(6, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(clientStates).doesNotContain(ON_REGISTRATION_SUCCESS, ON_UPDATE_SUCCESS);
            assertThat(isRegistered(clientEndpoint)).isFalse();
        });

        // The device is not connected any more: nothing to clean up on the server
        lwM2MTestClient.destroy();
        lwM2MTestClient = null;
    }

    private void forceResumeDtlsSessions() {
        for (LwM2mClientEndpointsProvider provider : lwM2MTestClient.getLeshanClient().getEndpointsProvider()) {
            if (provider instanceof CaliforniumClientEndpointsProvider californiumProvider) {
                for (Endpoint endpoint : californiumProvider.getCoapServer().getEndpoints()) {
                    if (endpoint instanceof CoapEndpoint coapEndpoint && coapEndpoint.getConnector() instanceof DTLSConnector dtlsConnector) {
                        dtlsConnector.forceResumeAllSessions();
                    }
                }
            }
        }
    }

    private boolean isRegistered(String endpoint) {
        return lwM2mClientContext.getLwM2mClients().stream()
                .anyMatch(client -> endpoint.equals(client.getEndpoint()) && client.getRegistration() != null);
    }

    private static String toPem(X509Certificate certificate) throws Exception {
        return "-----BEGIN CERTIFICATE-----\n" + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----\n";
    }

}
