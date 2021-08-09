/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.lwm2m;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.transport.lwm2m.config.LwM2MSecureServerConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportBootstrapConfig;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MServerSecurityInfoRepository {

    private final LwM2MTransportServerConfig serverConfig;
    private final LwM2MTransportBootstrapConfig bootstrapConfig;

    public ServerSecurityConfig getServerSecurityInfo(boolean bootstrapServer) {
        ServerSecurityConfig result = getServerSecurityConfig(bootstrapServer ? bootstrapConfig : serverConfig);
        result.setBootstrapServerIs(bootstrapServer);
        return result;
    }

    private ServerSecurityConfig getServerSecurityConfig(LwM2MSecureServerConfig serverConfig) {
        ServerSecurityConfig bsServ = new ServerSecurityConfig();
        bsServ.setServerId(serverConfig.getId());
        bsServ.setHost(serverConfig.getHost());
        bsServ.setPort(serverConfig.getPort());
        bsServ.setSecurityHost(serverConfig.getSecureHost());
        bsServ.setSecurityPort(serverConfig.getSecurePort());
        bsServ.setServerPublicKey(getPublicKey(serverConfig));
        return bsServ;
    }

    private String getPublicKey(LwM2MSecureServerConfig config) {
        try {
            KeyStore keyStore = serverConfig.getKeyStoreValue();
            if (keyStore != null) {
                X509Certificate serverCertificate = (X509Certificate) serverConfig.getKeyStoreValue().getCertificate(config.getCertificateAlias());
                return Hex.encodeHexString(serverCertificate.getPublicKey().getEncoded());
            }
        } catch (Exception e) {
            log.trace("Failed to fetch public key from key store!", e);

        }
        return "";
    }

    public void verifySecurityKeyDevice(DeviceCredentials deviceCredentials) throws InvalidConfigurationException, JsonProcessingException {
        ObjectNode nodeCredentialsValue = deviceCredentials.getNodeCredentialsValue();
        checkClientKey ((ObjectNode) nodeCredentialsValue.get("client"));
        checkServerKey ((ObjectNode) nodeCredentialsValue.get("bootstrap").get("bootstrapServer"), "Client`s  by bootstrapServer");
        checkServerKey ((ObjectNode) nodeCredentialsValue.get("bootstrap").get("lwm2mServer"), "Client`s by lwm2mServer");
    }

    private void checkClientKey (ObjectNode node) throws InvalidConfigurationException {
        String modeName = node.get("securityConfigClientMode").asText();
        // checks security config

        if (SecurityMode.RPK.name().equals(modeName)) {
            String value = node.get("key").textValue();
            assertIf(decodeRfc7250PublicKey(Hex.decodeHex(((String) value).toCharArray())) == null,
                    "raw-public-key mode, Client`s public key or id must be RFC7250 encoded public key");
        } else if (SecurityMode.X509.name().equals(modeName)) {
            String value = node.get("cert").textValue();
            if (value != null && !value.isEmpty()) {
                assertIf(decodeCertificate(Hex.decodeHex(((String) value).toCharArray())) == null,
                        "x509 mode, Client`s public key must be DER encoded X.509 certificate");
            }
        }

    }

    private void checkServerKey (ObjectNode node, String serverType) throws InvalidConfigurationException {
        String modeName = node.get("securityMode").asText();
        // checks security config
        if (SecurityMode.RPK.name().equals(modeName)) {
            checkRPKServer(node, serverType);
        } else if (SecurityMode.X509.name().equals(modeName)) {
            checkX509Server(node, serverType);
        }
    }

    protected void checkRPKServer(ObjectNode node, String serverType) throws InvalidConfigurationException {
        String value = node.get("clientSecretKey").textValue();
        assertIf(decodeRfc5958PrivateKey(Hex.decodeHex(value.toCharArray())) == null,
                "raw-public-key mode, " + serverType + " secret key must be RFC5958 encoded private key");
        value = node.get("clientPublicKeyOrId").textValue();
        assertIf(decodeRfc7250PublicKey(Hex.decodeHex(value.toCharArray())) == null,
                "raw-public-key mode, " + serverType + " public key or id must be RFC7250 encoded public key");
    }

    protected void checkX509Server(ObjectNode node, String serverType) throws InvalidConfigurationException {
        String value = node.get("clientSecretKey").textValue();
        assertIf(decodeRfc5958PrivateKey(Hex.decodeHex(value.toCharArray())) == null,
                "x509 mode " + serverType + " secret key must be RFC5958 encoded private key");
        value = node.get("clientPublicKeyOrId").textValue();
        assertIf(decodeCertificate(Hex.decodeHex(value.toCharArray())) == null,
                "x509 mode " + serverType + " public key must be DER encoded X.509 certificate");

    }

    public void verifySecurityKeyDeviceProfile(DeviceProfile deviceProfile) throws InvalidConfigurationException, JsonProcessingException {
        Map serverBs = ((Lwm2mDeviceProfileTransportConfiguration)deviceProfile.getProfileData().getTransportConfiguration()).getBootstrap().getBootstrapServer();
        checkDeviceProfileServer (serverBs, "Servers: BootstrapServer`s");
        Map serverLwm2m = ((Lwm2mDeviceProfileTransportConfiguration)deviceProfile.getProfileData().getTransportConfiguration()).getBootstrap().getLwm2mServer();
        checkDeviceProfileServer (serverLwm2m, "Servers: Lwm2mServer`s");

    }

    protected void checkDeviceProfileServer (Map server, String serverType) throws InvalidConfigurationException{
        // checks security config
        String value = (String) server.get("serverPublicKey");
        if (SecurityMode.RPK.name().equals(server.get("securityMode"))) {
            assertIf(decodeRfc7250PublicKey(Hex.decodeHex(value.toCharArray())) == null,
                    "raw-public-key mode, " + serverType + " public key or id must be RFC7250 encoded public key");
        } else if (SecurityMode.X509.name().equals(server.get("securityMode"))) {
            assertIf(decodeCertificate(Hex.decodeHex(value.toCharArray())) == null,
                    "x509 mode, " + serverType + " public key must be DER encoded X.509 certificate");
        }
    }

    protected PrivateKey decodeRfc5958PrivateKey(byte[] encodedKey) throws InvalidConfigurationException {
        try {
            return SecurityUtil.privateKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected PublicKey decodeRfc7250PublicKey(byte[] encodedKey) throws InvalidConfigurationException {
        try {
            return SecurityUtil.publicKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected Certificate decodeCertificate(byte[] encodedCert) throws InvalidConfigurationException {
        try {
            return SecurityUtil.certificate.decode(encodedCert);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected static void assertIf(boolean condition, String message) throws InvalidConfigurationException {
        if (condition) {
            throw new InvalidConfigurationException(message);
        }
    }

    protected static boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }


}

