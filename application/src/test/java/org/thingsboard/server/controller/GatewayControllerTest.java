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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ContextConfiguration;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.gateway.ConnectorType;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {GatewayControllerTest.Config.class})
@DaoSqlTest
public class GatewayControllerTest extends AbstractControllerTest {

    private final static String VALID_OLD_MQTT_CONFIGURATION = "{\"broker\":{\"name\":\"DefaultLocalBroker\",\"host\":\"127.0.0.1\"," +
            "\"port\":1883,\"clientId\":\"ThingsBoard_gateway\",\"version\":5,\"maxMessageNumberPerWorker\":10," +
            "\"maxNumberOfWorkers\":100,\"sendDataOnlyOnChange\":false,\"security\":{\"type\":\"basic\",\"username\":\"user\"," +
            "\"password\":\"password\"}},\"mapping\":[{\"topicFilter\":\"sensor/data\",\"converter\":{\"type\":\"json\"," +
            "\"deviceNameJsonExpression\":\"${serialNumber}\",\"deviceTypeJsonExpression\":\"${sensorType}\"," +
            "\"sendDataOnlyOnChange\":false,\"timeout\":60000,\"attributes\":[{\"type\":\"string\",\"key\":\"model\"," +
            "\"value\":\"${sensorModel}\"}],\"timeseries\":[{\"type\":\"double\",\"key\":\"temperature\",\"value\":\"${temp}\"}]}}]," +
            "\"connectRequests\":[{\"topicFilter\":\"sensor/connect\",\"deviceNameJsonExpression\":\"${serialNumber}\"}]," +
            "\"disconnectRequests\":[{\"topicFilter\":\"sensor/disconnect\",\"deviceNameJsonExpression\":\"${serialNumber}\"}]," +
            "\"attributeRequests\":[{\"retain\":false,\"topicFilter\":\"v1/devices/me/attributes/request\"," +
            "\"deviceNameJsonExpression\":\"${serialNumber}\",\"attributeNameJsonExpression\":\"${versionAttribute},${pduAttribute}\"," +
            "\"topicExpression\":\"devices/${deviceName}/attrs\",\"valueExpression\":\"${attributeKey}:${attributeValue}\"}]," +
            "\"attributeUpdates\":[{\"retain\":true,\"deviceNameFilter\":\".*\",\"attributeFilter\":\"firmwareVersion\"," +
            "\"topicExpression\":\"sensor/${deviceName}/${attributeKey}\",\"valueExpression\":\"{\\\"${attributeKey}\\\":\\\"${attributeValue}\\\"}\"}]," +
            "\"serverSideRpc\":[{\"deviceNameFilter\":\".*\",\"methodFilter\":\"no-reply\"," +
            "\"requestTopicExpression\":\"sensor/${deviceName}/request/${methodName}/${requestId}\",\"valueExpression\":\"${params}\"}]}";

    private final static String INVALID_OLD_MQTT_CONFIGURATION = "{\"broker\":{\"name\":\"DefaultLocalBroker\"," +
            "\"host\":\"\",\"port\":\"invalidPort\",\"clientId\":\"ThingsBoard_gateway\",\"version\":5," +
            "\"maxMessageNumberPerWorker\":10,\"maxNumberOfWorkers\":100,\"sendDataOnlyOnChange\":false,\"security\":{\"type\":\"basic\"}}," +
            "\"mapping\":[{\"topicFilter\":\"sensor/data\",\"converter\":{\"type\":\"json\"," +
            "\"deviceNameJsonExpression\":\"${serialNumber}\",\"deviceTypeJsonExpression\":\"${sensorType}\"," +
            "\"sendDataOnlyOnChange\":false,\"timeout\":60000,\"attributes\":[{\"type\":\"string\",\"key\":\"model\"," +
            "\"value\":\"${sensorModel}\"}],\"timeseries\":[{\"type\":\"double\",\"key\":\"temperature\",\"value\":\"${temp}\"}]}}]," +
            "\"connectRequests\":[{\"topicFilter\":\"sensor/connect\"}]," +
            "\"disconnectRequests\":[{\"topicFilter\":\"sensor/disconnect\",\"deviceNameJsonExpression\":\"\"}]," +
            "\"attributeRequests\":[{\"retain\":false,\"topicFilter\":\"v1/devices/me/attributes/request\"," +
            "\"deviceNameJsonExpression\":\"\",\"attributeNameJsonExpression\":\"${versionAttribute},${pduAttribute}\"," +
            "\"topicExpression\":\"\",\"valueExpression\":\"${attributeKey}:${attributeValue}\"}]," +
            "\"attributeUpdates\":[{\"retain\":true,\"deviceNameFilter\":\".*\",\"attributeFilter\":\"firmwareVersion\"," +
            "\"topicExpression\":\"sensor/${deviceName}/${attributeKey}\",\"valueExpression\":\"{\\\"${attributeKey}\\\":\\\"${attributeValue}\\\"}\"}]," +
            "\"serverSideRpc\":[{\"deviceNameFilter\":\".*\",\"methodFilter\":\"no-reply\"," +
            "\"requestTopicExpression\":\"sensor/${deviceName}/request/${methodName}/${requestId}\",\"valueExpression\":\"${params}\"}]}";

    private final static String VALID_LATEST_MQTT_CONFIGURATION = "{\"broker\":{\"name\":\"DefaultLocalBroker\",\"host\":\"127.0.0.1\"," +
            "\"port\":1883,\"clientId\":\"ThingsBoard_gateway\",\"version\":5,\"maxMessageNumberPerWorker\":10," +
            "\"maxNumberOfWorkers\":100,\"sendDataOnlyOnChange\":false,\"security\":{\"type\":\"basic\",\"username\":\"user\"," +
            "\"password\":\"password\"}},\"dataMapping\":[{\"topicFilter\":\"sensor/data\",\"subscriptionQos\":2," +
            "\"converter\":{\"type\":\"json\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\",\"deviceNameExpression\":\"${serialNumber}\"," +
            "\"deviceProfileExpressionSource\":\"message\",\"deviceProfileExpression\":\"${sensorType}\"}," +
            "\"sendDataOnlyOnChange\":false,\"timeout\":60000,\"attributes\":[{\"type\":\"string\",\"key\":\"model\"," +
            "\"value\":\"${sensorModel}\"}],\"timeseries\":[{\"type\":\"double\",\"key\":\"temperature\",\"value\":\"${temp}\"}]}}]," +
            "\"requestsMapping\":{\"connectRequests\":[{\"topicFilter\":\"sensor/connect\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\"," +
            "\"deviceNameExpression\":\"${serialNumber}\",\"deviceProfileExpressionSource\":\"constant\",\"deviceProfileExpression\":\"Thermometer\"}}]," +
            "\"disconnectRequests\":[{\"topicFilter\":\"sensor/disconnect\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\"," +
            "\"deviceNameExpression\":\"${serialNumber}\"}}],\"attributeRequests\":[{\"retain\":false,\"topicFilter\":\"v1/devices/me/attributes/request\"," +
            "\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\",\"deviceNameExpression\":\"${serialNumber}\"}," +
            "\"attributeNameExpressionSource\":\"message\",\"attributeNameExpression\":\"${versionAttribute},${pduAttribute}\"," +
            "\"topicExpression\":\"devices/${deviceName}/attrs\",\"valueExpression\":\"${attributeKey}:${attributeValue}\"}]," +
            "\"attributeUpdates\":[{\"retain\":true,\"deviceNameFilter\":\".*\",\"attributeFilter\":\"firmwareVersion\"," +
            "\"topicExpression\":\"sensor/${deviceName}/${attributeKey}\",\"valueExpression\":\"{\\\"${attributeKey}\\\":\\\"${attributeValue}\\\"}\"}]," +
            "\"serverSideRpc\":[{\"type\":\"twoWay\",\"deviceNameFilter\":\".*\",\"methodFilter\":\"echo\"," +
            "\"requestTopicExpression\":\"sensor/${deviceName}/request/${methodName}/${requestId}\"," +
            "\"responseTopicExpression\":\"sensor/${deviceName}/response/${methodName}/${requestId}\"," +
            "\"responseTopicQoS\":1,\"responseTimeout\":10000,\"valueExpression\":\"${params}\"}]}}";

    private final static String INVALID_LATEST_MQTT_CONFIGURATION = "{\"broker\":{\"name\":\"DefaultLocalBroker\"," +
            "\"host\":\"\",\"port\":\"invalidPort\",\"clientId\":\"ThingsBoard_gateway\",\"version\":5,\"maxMessageNumberPerWorker\":10," +
            "\"maxNumberOfWorkers\":100,\"sendDataOnlyOnChange\":false,\"security\":{\"type\":\"basic\"}},\"dataMapping\":[{\"topicFilter\":\"sensor/data\"," +
            "\"subscriptionQos\":2,\"converter\":{\"type\":\"invalidType\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\"," +
            "\"deviceNameExpression\":\"${serialNumber}\",\"deviceProfileExpressionSource\":\"message\",\"deviceProfileExpression\":\"${sensorType}\"}," +
            "\"sendDataOnlyOnChange\":false,\"timeout\":60000,\"attributes\":[{\"type\":\"string\",\"key\":\"model\",\"value\":\"${sensorModel}\"}]," +
            "\"timeseries\":[{\"type\":\"double\",\"key\":\"temperature\",\"value\":\"${temp}\"}]}},{\"topicFilter\":\"sensor/data\",\"subscriptionQos\":2," +
            "\"converter\":{\"type\":\"json\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\",\"deviceNameExpression\":\"${serialNumber}\"," +
            "\"deviceProfileExpressionSource\":\"\"},\"sendDataOnlyOnChange\":false,\"timeout\":60000,\"attributes\":[{\"type\":\"string\",\"key\":\"model\"," +
            "\"value\":\"${sensorModel}\"}],\"timeseries\":[{\"type\":\"double\",\"key\":\"temperature\",\"value\":\"${temp}\"}]}}]," +
            "\"requestsMapping\":{\"connectRequests\":[{\"topicFilter\":\"sensor/connect\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\"," +
            "\"deviceNameExpression\":\"${serialNumber}\",\"deviceProfileExpressionSource\":\"constant\",\"deviceProfileExpression\":\"Thermometer\"}}]," +
            "\"disconnectRequests\":[{\"topicFilter\":\"sensor/disconnect\",\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\"," +
            "\"deviceNameExpression\":\"${serialNumber}\"}}],\"attributeRequests\":[{\"retain\":false,\"topicFilter\":\"v1/devices/me/attributes/request\"," +
            "\"deviceInfo\":{\"deviceNameExpressionSource\":\"message\",\"deviceNameExpression\":\"${serialNumber}\"},\"attributeNameExpressionSource\":\"message\"," +
            "\"attributeNameExpression\":\"${versionAttribute},${pduAttribute}\",\"topicExpression\":\"devices/${deviceName}/attrs\"," +
            "\"valueExpression\":\"${attributeKey}:${attributeValue}\"}],\"attributeUpdates\":[{\"retain\":true,\"deviceNameFilter\":\".*\"," +
            "\"attributeFilter\":\"firmwareVersion\",\"topicExpression\":\"sensor/${deviceName}/${attributeKey}\"," +
            "\"valueExpression\":\"{\\\"${attributeKey}\\\":\\\"${attributeValue}\\\"}\"}],\"serverSideRpc\":[{\"type\":\"twoWay\"," +
            "\"deviceNameFilter\":\".*\",\"methodFilter\":\"echo\",\"requestTopicExpression\":\"sensor/${deviceName}/request/${methodName}/${requestId}\"," +
            "\"responseTopicExpression\":\"sensor/${deviceName}/response/${methodName}/${requestId}\",\"responseTopicQoS\":\"invalidQoS\"," +
            "\"responseTimeout\":\"invalidTimeout\",\"valueExpression\":\"${params}\"}]}}";

    private Tenant savedTenant;
    private User tenantAdmin;
    private Device gatewayDevice;

    static class Config {
        @Bean
        @Primary
        public DeviceDao deviceDao(DeviceDao deviceDao) {
            return Mockito.mock(DeviceDao.class, AdditionalAnswers.delegatesTo(deviceDao));
        }
    }

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = saveTenant(tenant);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");

        Device gateway = new Device();
        gateway.setName("My gateway");
        gateway.setType("default");
        ObjectNode additionalInfo = JacksonUtil.newObjectNode();
        additionalInfo.put("gateway", true);
        gateway.setAdditionalInfo(additionalInfo);

        gatewayDevice = doPost("/api/device", gateway, Device.class);
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();
        deleteTenant(savedTenant.getId());
    }

    // Test cases for Old MQTT Configuration

    @Test
    public void testValidateOldMqttConnectorConfiguration_Valid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(VALID_OLD_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.0", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void testValidateOldMqttConnectorConfiguration_MissingFields() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(INVALID_OLD_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.0", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getErrors())
                .extracting("path", "error")
                .containsExactlyInAnyOrder(
                        tuple(".broker.port", "Invalid value 'invalidPort' for field. Expected type: Integer"),
                        tuple(".broker.host", "must not be blank"),
                        tuple(".broker.port", "must not be null"),
                        tuple(".broker.security.username", "must not be blank"),
                        tuple(".broker.security.password", "must not be blank"),
                        tuple(".attributeRequests.0.topicExpression", "must not be blank")
                );

    }

    // Test cases for Latest MQTT Configuration

    @Test
    public void testValidateLatestMqttConnectorConfiguration_Valid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(VALID_LATEST_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.5", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getErrors()).isEmpty();
    }

    @Test
    public void testValidateLatestMqttConnectorConfiguration_MissingFields() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(INVALID_LATEST_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.5", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getErrors()).extracting("path", "error").containsExactlyInAnyOrder(
                tuple(".broker.port", "Invalid value 'invalidPort' for field. Expected type: Integer"),
                tuple(".dataMapping.0.converter.type", "Field contains unknown value, possible values: [bytes, custom, json]"),
                tuple(".requestsMapping.serverSideRpc.0.responseTopicQoS", "Invalid value 'invalidQoS' for field. Expected type: Integer"),
                tuple(".requestsMapping.serverSideRpc.0.responseTimeout", "Invalid value 'invalidTimeout' for field. Expected type: Long"),
                tuple(".dataMapping.1.converter.deviceInfo.deviceProfileExpression", "must not be blank"),
                tuple(".dataMapping.1.converter.deviceInfo.deviceProfileExpressionSource", "must not be blank"),
                tuple(".broker.security.username", "must not be blank"),
                tuple(".broker.host", "must not be blank"),
                tuple(".requestsMapping.serverSideRpc.0.responseTimeout", "must not be null"),
                tuple(".broker.security.password", "must not be blank"),
                tuple(".broker.port", "must not be null"),
                tuple(".requestsMapping.serverSideRpc.0.responseTopicQoS", "must not be null")
        );
        assertThat(validationResult.getWarnings()).extracting("path", "warning").containsExactlyInAnyOrder(
                tuple(".dataMapping.0.converter.deviceInfo", "deviceInfo is unknown"),
                tuple(".dataMapping.0.converter.sendDataOnlyOnChange", "sendDataOnlyOnChange is unknown"),
                tuple(".dataMapping.0.converter.timeout", "timeout is unknown"),
                tuple(".dataMapping.0.converter.attributes", "attributes is unknown"),
                tuple(".dataMapping.0.converter.timeseries", "timeseries is unknown")
        );

    }

    private GatewayConnectorValidationResult validateConfiguration(ObjectNode configuration, String version, ConnectorType connectorType) throws Exception {
        saveGatewayVersion(version);
        return readResponse(doPost("/api/gateway/" + gatewayDevice.getId().getId() + "/configuration/" + connectorType.name() + "/validate", configuration),
                GatewayConnectorValidationResult.class);
    }

    private void saveGatewayVersion(String version) throws Exception {
        AttributeKvEntry versionAttribute = new BaseAttributeKvEntry(new StringDataEntry("Version", version), System.currentTimeMillis());
        attributesService.save(tenantAdmin.getTenantId(), gatewayDevice.getId(), AttributeScope.CLIENT_SCOPE, versionAttribute).get(10, TimeUnit.SECONDS);
    }

}
