package org.thingsboard.server.transport.lwm2m.security.sql;

import org.apache.commons.codec.binary.Base64;
import org.eclipse.leshan.client.object.Security;
import org.eclipse.leshan.core.util.Hex;
import org.junit.Test;
import org.springframework.util.Base64Utils;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.RPKClientCredential;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredential;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import static org.eclipse.leshan.client.object.Security.psk;
import static org.eclipse.leshan.client.object.Security.rpk;
import static org.eclipse.leshan.client.object.Security.x509;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.PSK;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.RPK;
import static org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode.X509;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.LWM2M_ONLY;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class AllSecurityLwM2MIntegrationTestConnectionDifferentPort extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecConnectBsSuccess_UpdateLwm2mSectionAndLm2m_ConnectLwm2mSuccess() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "DiffPort";
        String awaitAlias = "await on client state (NoSec different port)";
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsNoSec(LWM2M_ONLY));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Security security = SECURITY_NO_SEC;
        basicTestConnectionDifferentPort(
                security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccessUpdate,
                false);
    }


    @Test
    public void testWithPskConnectLwm2mSuccessDifferentPort() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_PSK + "DiffPort";
        String awaitAlias = "await on client state (Psk different port)";
        String identity = CLIENT_PSK_IDENTITY + "DiffPort";
        String keyPsk = CLIENT_PSK_KEY + CLIENT_PSK_KEY;
        PSKClientCredential clientCredentials = new PSKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setIdentity(identity);
        clientCredentials.setKey(keyPsk);
        Security security = psk(SECURE_URI,
                shortServerId,
                identity.getBytes(StandardCharsets.UTF_8),
                Hex.decodeHex(keyPsk.toCharArray()));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(PSK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, null, null, PSK, false);
        basicTestConnectionDifferentPort(
                security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccessUpdate,
                false);
    }


    @Test
    public void testWithRpkConnectLwm2mSuccessDifferentPort() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_RPK + "DiffPort";
        String awaitAlias = "await on client state (Rpk different port)";
        X509Certificate certificate = clientX509CertTrust;
        PrivateKey privateKey = clientPrivateKeyFromCertTrust;
        RPKClientCredential clientCredentials = new RPKClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setKey(Base64.encodeBase64String(certificate.getPublicKey().getEncoded()));
        Security security = rpk(SECURE_URI,
                shortServerId,
                certificate.getPublicKey().getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getPublicKey().getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(RPK, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, RPK, false);
        basicTestConnectionDifferentPort(
                security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccessUpdate,
                false);
    }


    @Test
    public void testWithX509NoTrustConnectLwm2mSuccessDifferentPort() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST_NO;
        String awaitAlias = "await on client state (X509_Trust_NO different port)";
        X509Certificate certificate = clientX509CertTrustNo;
        PrivateKey privateKey = clientPrivateKeyFromCertTrustNo;
        X509ClientCredential clientCredentials = new X509ClientCredential();
        clientCredentials.setEndpoint(clientEndpoint);
        clientCredentials.setCert(Base64Utils.encodeToString(certificate.getEncoded()));
        Security security = x509(SECURE_URI,
                shortServerId,
                certificate.getEncoded(),
                privateKey.getEncoded(),
                serverX509Cert.getEncoded());
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        basicTestConnectionDifferentPort(
                security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccessUpdate,
                false);
    }


    @Test
    public void testWithX509TrustConnectLwm2mSuccessDifferentPort() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_X509_TRUST;
        String awaitAlias = "await on client state (X509_Trust different port)";
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
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(OBSERVE_ATTRIBUTES_WITHOUT_PARAMS, getBootstrapServerCredentialsSecure(X509, NONE));
        LwM2MDeviceCredentials deviceCredentials = getDeviceCredentialsSecure(clientCredentials, privateKey, certificate, X509, false);
        basicTestConnectionDifferentPort(
                security,
                deviceCredentials,
                COAP_CONFIG,
                clientEndpoint,
                transportConfiguration,
                awaitAlias,
                expectedStatusesRegistrationLwm2mSuccessUpdate,
                false);
    }
}
