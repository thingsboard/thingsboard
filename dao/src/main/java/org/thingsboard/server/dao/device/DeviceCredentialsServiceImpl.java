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

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.eclipse.leshan.core.SecurityMode;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.PSKClientCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.X509ClientCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CREDENTIALS_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;


@Service
@Slf4j
public class DeviceCredentialsServiceImpl extends AbstractEntityService implements DeviceCredentialsService {

    @Autowired
    private DeviceCredentialsDao deviceCredentialsDao;

    @Autowired
    private DeviceService deviceService;

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
                verifySecurityKeyDevice(deviceCredentials);
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


    private void verifySecurityKeyDevice(DeviceCredentials deviceCredentials) {
        try {
//            LwM2MCredentialsValid credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(), LwM2MCredentialsValid.class);
            JsonNode nodeCredentialsValue = JacksonUtil.toJsonNode(deviceCredentials.getCredentialsValue());
            String [] fields = {"client", "bootstrap", "bootstrap:bootstrapServer", "bootstrap:lwm2mServer"};
            String validateMsg = validateNodeCredentials (nodeCredentialsValue,  fields);
            if (validateMsg.isEmpty()) {
                checkClientKey(nodeCredentialsValue.get("client"));
                checkServerKey(nodeCredentialsValue.get("bootstrap").get("bootstrapServer"), "Client`s by bootstrapServer");
                checkServerKey(nodeCredentialsValue.get("bootstrap").get("lwm2mServer"), "Client`s by lwm2mServer");
            }
            else {
                throw new DataValidationException(validateMsg);
            }
        } catch (DataValidationException | DecoderException e) {
            throw new DataValidationException(e.getMessage());
        }
    }

    public void verifyLwm2mSecurityKeyDeviceProfile(Lwm2mDeviceProfileTransportConfiguration transportConfiguration) {
        try {
            Map serverBs = transportConfiguration.getBootstrap().getBootstrapServer();
            checkDeviceProfileServer (serverBs, "Servers: BootstrapServer`s");
            Map serverLwm2m = transportConfiguration.getBootstrap().getLwm2mServer();
            checkDeviceProfileServer (serverLwm2m, "Servers: Lwm2mServer`s");
        } catch (DataValidationException e) {
            throw new DataValidationException(e.getMessage());

        }
    }

    private String validateNodeCredentials (JsonNode nodeCredentialsValue,  String [] fields) {
        Set  msgSet = ConcurrentHashMap.newKeySet();
        String msg = "";
        for (String field : fields) {
            if (field.contains(":")) {
                String [] keys = field.split(":");
                if (!nodeCredentialsValue.hasNonNull(keys[0])) {
                    msgSet.add(keys[1]);
                }
                else {
                    if (!nodeCredentialsValue.get(keys[0]).hasNonNull(keys[1])) msgSet.add(keys[1]);
                }
            }
            else {
                if (!nodeCredentialsValue.hasNonNull(field)) msgSet.add(field);
            }
        }
        if (msgSet.size() > 0) msg = "Device credentials are missing fields or mandatory value in this fields: " + String.join(", ", msgSet);
        return  msg;
    }

    private void checkClientKey (JsonNode node) throws DataValidationException, DecoderException {
        String modeName = node.get("securityConfigClientMode").asText();
        // checks security config
        // HexDec Len = 32,64,128
        if (SecurityMode.PSK.name().equals(modeName)) {
            String key = node.get("key").textValue();
            assertIf(key == null || key.isEmpty(),
                    "pre-shared-key mode, Client`s private key or id must be not null or empty.");
            assertIf(!key.matches("-?[0-9a-fA-F]+"),
                    "pre-shared-key mode, Client`s private key or id must be HexDecimal format.");
            assertIf(key.length()%32 != 0 ,
                    "pre-shared-key mode, Client`s private key or id must be a multiple of 32.");
            assertIf(key.length() > 128,
                    "pre-shared-key mode, Client`s private key or id must be not more than 128.");
            String identity = node.get("identity").textValue();
            assertIf(identity == null || identity.isEmpty(),
                    "pre-shared-key mode, Client`s identity key must be not null or empty.");
        } else if (SecurityMode.RPK.name().equals(modeName)) {
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

    private void checkServerKey (JsonNode node, String serverType) throws DataValidationException {
        String modeName = node.get("securityMode").asText();
        // checks security config
        if (SecurityMode.RPK.name().equals(modeName)) {
            checkRPKServer(node, serverType);
        } else if (SecurityMode.X509.name().equals(modeName)) {
            checkX509Server(node, serverType);
        }
    }

    protected void checkRPKServer(JsonNode node, String serverType) throws DataValidationException  {
        String value = node.get("clientSecretKey").textValue();
        assertIf(decodeRfc5958PrivateKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "raw-public-key mode, " + serverType + " secret key must be RFC5958 encoded private key");
        value = node.get("clientPublicKeyOrId").textValue();
        assertIf(decodeRfc7250PublicKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "raw-public-key mode, " + serverType + " public key or id must be RFC7250 encoded public key");
    }

    protected void checkX509Server(JsonNode node, String serverType) throws DataValidationException {
        String value = node.get("clientSecretKey").textValue();
        assertIf(decodeRfc5958PrivateKey(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "x509 mode " + serverType + " secret key must be RFC5958 encoded private key");
        value = node.get("clientPublicKeyOrId").textValue();
        assertIf(decodeCertificate(org.eclipse.leshan.core.util.Hex.decodeHex(value.toCharArray())) == null,
                "x509 mode " + serverType + " public key must be DER encoded X.509 certificate");

    }

    protected void checkDeviceProfileServer (Map server, String serverType) throws DataValidationException {
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

    protected PrivateKey decodeRfc5958PrivateKey(byte[] encodedKey) {
        try {
            return SecurityUtil.privateKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected PublicKey decodeRfc7250PublicKey(byte[] encodedKey) {
        try {
            return SecurityUtil.publicKey.decode(encodedKey);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected Certificate decodeCertificate(byte[] encodedCert) {
        try {
            return SecurityUtil.certificate.decode(encodedCert);
        } catch (IOException | GeneralSecurityException e) {
            return null;
        }
    }

    protected static void assertIf(boolean condition, String message) throws DataValidationException {
        if (condition) {
            throw new DataValidationException(message);
        }
    }
}
