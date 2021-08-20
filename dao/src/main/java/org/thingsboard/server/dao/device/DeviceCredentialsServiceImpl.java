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
package org.thingsboard.server.dao.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.leshan.core.util.Hex;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.ServerSecurityConfig;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CREDENTIALS_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;
import org.eclipse.leshan.core.SecurityMode;
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

@Service
@Slf4j
public class DeviceCredentialsServiceImpl extends AbstractEntityService implements DeviceCredentialsService {

    @Autowired
    private DeviceCredentialsDao deviceCredentialsDao;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    protected LwM2MTransportServerConfig serverConfig;

    @Autowired
    protected LwM2MTransportBootstrapConfig bootstrapConfig;

    @Override
    public DeviceCredentials findDeviceCredentialsByDeviceId(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceCredentialsByDeviceId [{}]", deviceId);
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        return deviceCredentialsDao.findByDeviceId(tenantId, deviceId.getId());
    }

    @Override
    @Cacheable(cacheNames = DEVICE_CREDENTIALS_CACHE, key = "'deviceCredentials_' + #credentialsId", unless = "#result == null")
    public DeviceCredentials findDeviceCredentialsByCredentialsId(String credentialsId) {
        log.trace("Executing findDeviceCredentialsByCredentialsId [{}]", credentialsId);
        validateString(credentialsId, "Incorrect credentialsId " + credentialsId);
        return deviceCredentialsDao.findByCredentialsId(new TenantId(EntityId.NULL_UUID), credentialsId);
    }

    @Override
    @CacheEvict(cacheNames = DEVICE_CREDENTIALS_CACHE, keyGenerator = "previousDeviceCredentialsId", beforeInvocation = true)
    public DeviceCredentials updateDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials) {
        return saveOrUpdate(tenantId, deviceCredentials);
    }

    @Override
    public DeviceCredentials createDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials) {
        return saveOrUpdate(tenantId, deviceCredentials);
    }

    public void verifySecurityKeyDevice(DeviceCredentials deviceCredentials) throws JsonProcessingException, InvalidConfigurationException {
        JsonNode nodeCredentialsValue = JacksonUtil.toJsonNode(deviceCredentials.getCredentialsValue());
        checkClientKey (nodeCredentialsValue.get("client"));
        checkServerKey (nodeCredentialsValue.get("bootstrap").get("bootstrapServer"), "Client`s  by bootstrapServer");
        checkServerKey (nodeCredentialsValue.get("bootstrap").get("lwm2mServer"), "Client`s by lwm2mServer");
    }

    public void verifyLwm2mSecurityKeyDeviceProfile(DeviceProfile deviceProfile) throws InvalidConfigurationException, JsonProcessingException {
        Map serverBs = ((Lwm2mDeviceProfileTransportConfiguration)deviceProfile.getProfileData().getTransportConfiguration()).getBootstrap().getBootstrapServer();
        checkDeviceProfileServer (serverBs, "Servers: BootstrapServer`s");
        Map serverLwm2m = ((Lwm2mDeviceProfileTransportConfiguration)deviceProfile.getProfileData().getTransportConfiguration()).getBootstrap().getLwm2mServer();
        checkDeviceProfileServer (serverLwm2m, "Servers: Lwm2mServer`s");
    }

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

    private DeviceCredentials saveOrUpdate(TenantId tenantId, DeviceCredentials deviceCredentials) {
        if (deviceCredentials.getCredentialsType() == null) {
            throw new DataValidationException("Device credentials type should be specified");
        }
        switch (deviceCredentials.getCredentialsType()) {
            case X509_CERTIFICATE:
                formatCertData(deviceCredentials);
                break;
            case MQTT_BASIC:
                formatSimpleMqttCredentials(deviceCredentials);
                break;
            case LWM2M_CREDENTIALS:
                formatSimpleLwm2mCredentials(deviceCredentials);
                break;
        }
        log.trace("Executing updateDeviceCredentials [{}]", deviceCredentials);
        credentialsValidator.validate(deviceCredentials, id -> tenantId);
        try {
            return deviceCredentialsDao.save(tenantId, deviceCredentials);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null
                    && (e.getConstraintName().equalsIgnoreCase("device_credentials_id_unq_key") || e.getConstraintName().equalsIgnoreCase("device_credentials_device_id_unq_key"))) {
                throw new DataValidationException("Specified credentials are already registered!");
            } else {
                throw t;
            }
        }
    }

    private void formatSimpleMqttCredentials(DeviceCredentials deviceCredentials) {
        BasicMqttCredentials mqttCredentials;
        try {
            mqttCredentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), BasicMqttCredentials.class);
            if (mqttCredentials == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new DataValidationException("Invalid credentials body for simple mqtt credentials!");
        }
        if (StringUtils.isEmpty(mqttCredentials.getClientId()) && StringUtils.isEmpty(mqttCredentials.getUserName())) {
            throw new DataValidationException("Both mqtt client id and user name are empty!");
        }
        if (StringUtils.isEmpty(mqttCredentials.getClientId())) {
            deviceCredentials.setCredentialsId(mqttCredentials.getUserName());
        } else if (StringUtils.isEmpty(mqttCredentials.getUserName())) {
            deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash(mqttCredentials.getClientId()));
        } else {
            deviceCredentials.setCredentialsId(EncryptionUtil.getSha3Hash("|", mqttCredentials.getClientId(), mqttCredentials.getUserName()));
        }
        if (!StringUtils.isEmpty(mqttCredentials.getPassword())) {
            mqttCredentials.setPassword(mqttCredentials.getPassword());
        }
        deviceCredentials.setCredentialsValue(JacksonUtil.toString(mqttCredentials));
    }

    private void formatCertData(DeviceCredentials deviceCredentials) {
        String cert = EncryptionUtil.trimNewLines(deviceCredentials.getCredentialsValue());
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);
        deviceCredentials.setCredentialsId(sha3Hash);
        deviceCredentials.setCredentialsValue(cert);
    }

    private void formatSimpleLwm2mCredentials(DeviceCredentials deviceCredentials) {
        LwM2MClientCredentials clientCredentials;
        ObjectNode json;
        try {
            json = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), ObjectNode.class);
            if (json == null) {
                throw new IllegalArgumentException();
            }
            clientCredentials = JacksonUtil.convertValue(json.get("client"), LwM2MClientCredentials.class);
            if (clientCredentials == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new DataValidationException("Invalid credentials body for LwM2M credentials!");
        }

        String credentialsId = null;

        switch (clientCredentials.getSecurityConfigClientMode()) {
            case NO_SEC:
            case RPK:
                credentialsId = clientCredentials.getEndpoint();
                break;
            case PSK:
                credentialsId = ((PSKClientCredentials) clientCredentials).getIdentity();
                break;
            case X509:
                X509ClientCredentials x509Config = (X509ClientCredentials) clientCredentials;
                if (x509Config.getCert() != null) {
                    String cert = EncryptionUtil.trimNewLines(x509Config.getCert());
                    String sha3Hash = EncryptionUtil.getSha3Hash(cert);
                    x509Config.setCert(cert);
                    ((ObjectNode) json.get("client")).put("cert", cert);
                    deviceCredentials.setCredentialsValue(JacksonUtil.toString(json));
                    credentialsId = sha3Hash;
                } else {
                    credentialsId = x509Config.getEndpoint();
                }
                break;
        }
        if (credentialsId == null) {
            throw new DataValidationException("Invalid credentials body for LwM2M credentials!");
        }
        deviceCredentials.setCredentialsId(credentialsId);
    }

    @Override
    @CacheEvict(cacheNames = DEVICE_CREDENTIALS_CACHE, key = "'deviceCredentials_' + #deviceCredentials.credentialsId")
    public void deleteDeviceCredentials(TenantId tenantId, DeviceCredentials deviceCredentials) {
        log.trace("Executing deleteDeviceCredentials [{}]", deviceCredentials);
        deviceCredentialsDao.removeById(tenantId, deviceCredentials.getUuidId());
    }

    private DataValidator<DeviceCredentials> credentialsValidator =
            new DataValidator<DeviceCredentials>() {

                @Override
                protected void validateCreate(TenantId tenantId, DeviceCredentials deviceCredentials) {
                    if (deviceCredentialsDao.findByDeviceId(tenantId, deviceCredentials.getDeviceId().getId()) != null) {
                        throw new DataValidationException("Credentials for this device are already specified!");
                    }
                    if (deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId()) != null) {
                        throw new DataValidationException("Device credentials are already assigned to another device!");
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, DeviceCredentials deviceCredentials) {
                    if (deviceCredentialsDao.findById(tenantId, deviceCredentials.getUuidId()) == null) {
                        throw new DataValidationException("Unable to update non-existent device credentials!");
                    }
                    DeviceCredentials existingCredentials = deviceCredentialsDao.findByCredentialsId(tenantId, deviceCredentials.getCredentialsId());
                    if (existingCredentials != null && !existingCredentials.getId().equals(deviceCredentials.getId())) {
                        throw new DataValidationException("Device credentials are already assigned to another device!");
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, DeviceCredentials deviceCredentials) {
                    if (deviceCredentials.getDeviceId() == null) {
                        throw new DataValidationException("Device credentials should be assigned to device!");
                    }
                    if (deviceCredentials.getCredentialsType() == null) {
                        throw new DataValidationException("Device credentials type should be specified!");
                    }
                    if (StringUtils.isEmpty(deviceCredentials.getCredentialsId())) {
                        throw new DataValidationException("Device credentials id should be specified!");
                    }
                    Device device = deviceService.findDeviceById(tenantId, deviceCredentials.getDeviceId());
                    if (device == null) {
                        throw new DataValidationException("Can't assign device credentials to non-existent device!");
                    }
                }
            };

    private void checkClientKey (JsonNode node) throws InvalidConfigurationException {
        String modeName = node.get("securityConfigClientMode").asText();
        // checks security config

        if (SecurityMode.RPK.name().equals(modeName)) {
            String value = node.get("key").textValue();
            assertIf(decodeRfc7250PublicKey(org.eclipse.leshan.core.util.Hex.decodeHex(((String) value).toCharArray())) == null,
                    "raw-public-key mode, Client`s public key or id must be RFC7250 encoded public key");
        } else if (SecurityMode.X509.name().equals(modeName)) {
            String value = node.get("cert").textValue();
            if (value != null && !value.isEmpty()) {
                assertIf(decodeCertificate(Hex.decodeHex(((String) value).toCharArray())) == null,
                        "x509 mode, Client`s public key must be DER encoded X.509 certificate");
            }
        }
    }

    private void checkServerKey (JsonNode node, String serverType) throws InvalidConfigurationException {
        String modeName = node.get("securityMode").asText();
        // checks security config
        if (SecurityMode.RPK.name().equals(modeName)) {
            checkRPKServer(node, serverType);
        } else if (SecurityMode.X509.name().equals(modeName)) {
            checkX509Server(node, serverType);
        }
    }

    protected void checkRPKServer(JsonNode node, String serverType) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        String value = node.get("clientSecretKey").textValue();
        assertIf(decodeRfc5958PrivateKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "raw-public-key mode, " + serverType + " secret key must be RFC5958 encoded private key");
        value = node.get("clientPublicKeyOrId").textValue();
        assertIf(decodeRfc7250PublicKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "raw-public-key mode, " + serverType + " public key or id must be RFC7250 encoded public key");
    }

    protected void checkX509Server(JsonNode node, String serverType) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        String value = node.get("clientSecretKey").textValue();
        assertIf(decodeRfc5958PrivateKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "x509 mode " + serverType + " secret key must be RFC5958 encoded private key");
        value = node.get("clientPublicKeyOrId").textValue();
        assertIf(decodeCertificate(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "x509 mode " + serverType + " public key must be DER encoded X.509 certificate");

    }

    protected void checkDeviceProfileServer (Map server, String serverType) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        // checks security config
        String value = (String) server.get("serverPublicKey");
        if (SecurityMode.RPK.name().equals(server.get("securityMode"))) {
            assertIf(decodeRfc7250PublicKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                    "raw-public-key mode, " + serverType + " public key or id must be RFC7250 encoded public key");
        } else if (SecurityMode.X509.name().equals(server.get("securityMode"))) {
            assertIf(decodeCertificate(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                    "x509 mode, " + serverType + " public key must be DER encoded X.509 certificate");
        }
    }

    protected PrivateKey decodeRfc5958PrivateKey(byte[] encodedKey) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        try {
            return SecurityUtil.privateKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected PublicKey decodeRfc7250PublicKey(byte[] encodedKey) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        try {
            return SecurityUtil.publicKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected Certificate decodeCertificate(byte[] encodedCert) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        try {
            return SecurityUtil.certificate.decode(encodedCert);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected static void assertIf(boolean condition, String message) throws org.eclipse.leshan.server.bootstrap.InvalidConfigurationException {
        if (condition) {
            throw new org.eclipse.leshan.server.bootstrap.InvalidConfigurationException(message);
        }
    }

}
