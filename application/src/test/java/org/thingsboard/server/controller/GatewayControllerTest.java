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
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationRecord;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationRecordType;
import org.thingsboard.server.common.data.gateway.connector.validators.GatewayConnectorValidationResult;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

    private final static String VALID_OLD_MODBUS_CONFIGURATION = "{\"master\":{\"slaves\":[{\"host\":\"127.0.0.1\",\"port\":502,\"type\":\"tcp\"," +
            "\"method\":\"socket\",\"timeout\":35,\"byteOrder\":\"LITTLE\",\"wordOrder\":\"LITTLE\",\"retries\":true,\"pollPeriod\":5000,\"unitId\":1," +
            "\"deviceName\":\"Temp Sensor\",\"attributes\":[{\"type\":\"string\",\"tag\":\"string_read\",\"functionCode\":4,\"objectsCount\":1,\"address\":1}]," +
            "\"timeseries\":[{\"type\":\"int\",\"tag\":\"temperature_read\",\"functionCode\":4,\"objectsCount\":1,\"address\":2}]},{\"port\":\"/dev/ttyTest0\"," +
            "\"type\":\"serial\",\"baudrate\":19200,\"databits\":8,\"stopbits\":1,\"parity\":\"N\",\"strict\":false,\"method\":\"ascii\",\"timeout\":35," +
            "\"byteOrder\":\"LITTLE\",\"wordOrder\":\"LITTLE\",\"retries\":true,\"pollPeriod\":5000,\"unitId\":1,\"deviceName\":\"Temp Sensor\"," +
            "\"attributes\":[{\"type\":\"string\",\"tag\":\"string_read\",\"functionCode\":4,\"objectsCount\":1,\"address\":1}],\"timeseries\":[{\"type\":\"int\"," +
            "\"tag\":\"temperature_read\",\"functionCode\":4,\"objectsCount\":1,\"address\":2}]}]},\"slave\":{\"type\":\"tcp\",\"host\":\"127.0.0.1\",\"port\":502," +
            "\"method\":\"socket\",\"deviceName\":\"Modbus Slave Example\",\"pollPeriod\":5000,\"unitId\":1,\"values\":{\"holding_registers\":[{\"attributes\":[{\"type\":\"string\"," +
            "\"tag\":\"sm\",\"objectsCount\":1,\"address\":1,\"value\":\"ON\"}],\"timeseries\":[{\"type\":\"int\",\"tag\":\"smm\",\"objectsCount\":1,\"address\":2,\"value\":\"12334\"}]}]}}}";

    private final static String INVALID_OLD_MODBUS_CONFIGURATION = "{\"master\":{\"slaves\":[{\"host\":\"\",\"port\":\"invalidPort\",\"type\":\"tcp\",\"method\":\"\",\"timeout\":-1,\"byteOrder\":1," +
            "\"wordOrder\":0,\"retries\":\"invalidBoolean\",\"pollPeriod\":-5000,\"unitId\":-1,\"deviceName\":\"\",\"deviceType\":33,\"attributes\":[{\"type\":\"\",\"tag\":\"\",\"functionCode\":0," +
            "\"objectsCount\":-1,\"address\":-1}],\"timeseries\":[{\"type\":\"\",\"tag\":\"\",\"functionCode\":0,\"objectsCount\":-1,\"address\":-1}],\"attributeUpdates\":[{\"someunknownField\":\"value\"}]," +
            "\"someUnknownArray\":[]},{\"port\":\"/invalid/port\",\"type\":\"serial\",\"baudrate\":-19200,\"databits\":-8,\"stopbits\":-1,\"parity\":1,\"strict\":\"invalidBoolean\",\"method\":\"invalidMethod\"," +
            "\"timeout\":-35,\"byteOrder\":\"\",\"wordOrder\":33.3,\"retries\":\"invalidBoolean\",\"pollPeriod\":-5000,\"unitId\":-1,\"deviceName\":\"\",\"deviceType\":\"\",\"attributes\":[{\"type\":\"\"," +
            "\"tag\":\"\",\"functionCode\":0,\"objectsCount\":-1,\"address\":-1}],\"timeseries\":[{\"type\":\"\",\"tag\":\"\",\"functionCode\":17,\"objectsCount\":-1,\"address\":-1}]}]}," +
            "\"slave\":{\"type\":\"invalidType\",\"host\":\"\",\"port\":\"invalidPort\",\"method\":\"\",\"deviceName\":\"\",\"pollPeriod\":-5000,\"unitId\":-1," +
            "\"values\":{\"holding_registers\":[{\"attributes\":[{\"type\":\"\",\"tag\":\"\",\"objectsCount\":-1,\"address\":-1,\"value\":\"\"}],\"timeseries\":[{\"type\":\"\",\"tag\":\"\",\"objectsCount\":-1," +
            "\"address\":-1,\"value\":\"invalidValue\"}]}]}}}";

    private final static String VALID_LATEST_MODBUS_CONFIGURATION = "{\"master\":{\"slaves\":[{\"name\":\"Slave 1\",\"host\":\"127.0.0.1\",\"port\":5021,\"type\":\"tcp\"," +
            "\"method\":\"socket\",\"timeout\":35,\"byteOrder\":\"LITTLE\",\"wordOrder\":\"LITTLE\",\"retries\":true,\"retryOnEmpty\":true,\"retryOnInvalid\":true," +
            "\"pollPeriod\":5000,\"unitId\":1,\"deviceName\":\"Temp Sensor\",\"deviceType\":\"default\",\"sendDataOnlyOnChange\":true,\"connectAttemptTimeMs\":5000," +
            "\"connectAttemptCount\":5,\"waitAfterFailedAttemptsMs\":300000,\"attributes\":[{\"tag\":\"string_read\",\"type\":\"string\",\"functionCode\":4,\"objectsCount\":4," +
            "\"address\":1}],\"timeseries\":[{\"tag\":\"16float_read\",\"type\":\"16float\",\"functionCode\":4,\"objectsCount\":1,\"address\":25}],\"attributeUpdates\":[{\"tag\":\"shared_attribute_write\"," +
            "\"type\":\"32int\",\"functionCode\":6,\"objectsCount\":2,\"address\":29}],\"rpc\":[{\"tag\":\"setValue\",\"type\":\"bits\",\"functionCode\":5,\"objectsCount\":1,\"address\":31}," +
            "{\"tag\":\"getValue\",\"type\":\"bits\",\"functionCode\":1,\"objectsCount\":1,\"address\":31}]},{\"name\":\"Slave 1\",\"method\":\"ascii\",\"baudrate\":4800,\"stopbits\":1,\"bytesize\":5," +
            "\"parity\":\"N\",\"strict\":true,\"unitId\":1,\"deviceName\":\"Temp Sensor\",\"deviceType\":\"default\",\"sendDataOnlyOnChange\":true,\"timeout\":35,\"byteOrder\":\"LITTLE\"," +
            "\"retries\":true,\"retryOnEmpty\":true,\"retryOnInvalid\":true,\"pollPeriod\":5000,\"connectAttemptTimeMs\":5000,\"connectAttemptCount\":5,\"waitAfterFailedAttemptsMs\":300000," +
            "\"type\":\"serial\",\"attributes\":[{\"tag\":\"string_read\",\"type\":\"string\",\"functionCode\":4,\"objectsCount\":4,\"address\":1},{\"tag\":\"bits_read\",\"type\":\"bits\"," +
            "\"functionCode\":4,\"objectsCount\":1,\"address\":5},{\"tag\":\"8int_read\",\"type\":\"8int\",\"functionCode\":4,\"objectsCount\":1,\"address\":6},{\"tag\":\"16int_read\",\"type\":\"16int\"," +
            "\"functionCode\":4,\"objectsCount\":1,\"address\":7},{\"tag\":\"32int_read_divider\",\"type\":\"32int\",\"functionCode\":4,\"objectsCount\":2,\"address\":8,\"divider\":10},{\"tag\":\"8int_read_multiplier\"," +
            "\"type\":\"8int\",\"functionCode\":4,\"objectsCount\":1,\"address\":10,\"multiplier\":10},{\"tag\":\"32int_read\",\"type\":\"32int\",\"functionCode\":4,\"objectsCount\":2,\"address\":11}," +
            "{\"tag\":\"64int_read\",\"type\":\"64int\",\"functionCode\":4,\"objectsCount\":4,\"address\":13}],\"timeseries\":[{\"tag\":\"8uint_read\",\"type\":\"8uint\",\"functionCode\":4,\"objectsCount\":1," +
            "\"address\":17},{\"tag\":\"16uint_read\",\"type\":\"16uint\",\"functionCode\":4,\"objectsCount\":2,\"address\":18},{\"tag\":\"32uint_read\",\"type\":\"32uint\",\"functionCode\":4,\"objectsCount\":4," +
            "\"address\":20},{\"tag\":\"64uint_read\",\"type\":\"64uint\",\"functionCode\":4,\"objectsCount\":1,\"address\":24},{\"tag\":\"16float_read\",\"type\":\"16float\",\"functionCode\":4,\"objectsCount\":1," +
            "\"address\":25},{\"tag\":\"32float_read\",\"type\":\"32float\",\"functionCode\":4,\"objectsCount\":2,\"address\":26},{\"tag\":\"64float_read\",\"type\":\"64float\",\"functionCode\":4,\"objectsCount\":4," +
            "\"address\":28}],\"attributeUpdates\":[{\"tag\":\"shared_attribute_write\",\"type\":\"32int\",\"functionCode\":6,\"objectsCount\":2,\"address\":29}],\"rpc\":[{\"tag\":\"setValue\",\"type\":\"bits\"," +
            "\"functionCode\":5,\"objectsCount\":1,\"address\":31},{\"tag\":\"getValue\",\"type\":\"bits\",\"functionCode\":1,\"objectsCount\":1,\"address\":31},{\"tag\":\"setCPUFanSpeed\",\"type\":\"32int\"," +
            "\"functionCode\":16,\"objectsCount\":2,\"address\":33},{\"tag\":\"getCPULoad\",\"type\":\"32int\",\"functionCode\":4,\"objectsCount\":2,\"address\":35}],\"port\":\"/dev/ttyUSB0\"}]}," +
            "\"slave\":{\"type\":\"tcp\",\"host\":\"127.0.0.1\",\"port\":5026,\"method\":\"socket\",\"deviceName\":\"Modbus Slave Example\",\"deviceType\":\"default\",\"pollPeriod\":5000,\"sendDataToThingsBoard\":false," +
            "\"byteOrder\":\"LITTLE\",\"wordOrder\":\"LITTLE\",\"unitId\":0,\"values\":{\"holding_registers\":{\"attributes\":[{\"address\":1,\"type\":\"string\",\"tag\":\"sm\",\"objectsCount\":1,\"value\":\"ON\"}]," +
            "\"timeseries\":[{\"address\":2,\"type\":\"8int\",\"tag\":\"smm\",\"objectsCount\":1,\"value\":\"12334\"}],\"attributeUpdates\":[{\"tag\":\"shared_attribute_write\",\"type\":\"32int\",\"functionCode\":6," +
            "\"objectsCount\":2,\"address\":29,\"value\":1243}],\"rpc\":[{\"tag\":\"setValue\",\"type\":\"bits\",\"functionCode\":5,\"objectsCount\":1,\"address\":31,\"value\":22}]}," +
            "\"coils_initializer\":{\"attributes\":[{\"address\":5,\"type\":\"string\",\"tag\":\"sm\",\"objectsCount\":1,\"value\":\"12\"}],\"timeseries\":[],\"attributeUpdates\":[],\"rpc\":[]}}}}";

    private final static String INVALID_LATEST_MODBUS_CONFIGURATION = "{\"master\":{\"slaves\":[{\"type\":\"invalidType\",\"name\":\"\",\"host\":\"\",\"port\":\"invalidPort\",\"method\":\"\"," +
            "\"timeout\":-35,\"byteOrder\":\"INVALID\",\"wordOrder\":\"INVALID\",\"retries\":\"invalidBoolean\",\"retryOnEmpty\":\"invalidBoolean\",\"retryOnInvalid\":\"invalidBoolean\"," +
            "\"pollPeriod\":-5000,\"unitId\":-1,\"deviceName\":\"\",\"deviceType\":\"\",\"sendDataOnlyOnChange\":\"invalidBoolean\",\"connectAttemptTimeMs\":-5000,\"connectAttemptCount\":-5," +
            "\"waitAfterFailedAttemptsMs\":-300000,\"attributes\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0}],\"timeseries\":[{\"tag\":\"\",\"type\":\"\"," +
            "\"functionCode\":0,\"objectsCount\":0,\"address\":0}],\"attributeUpdates\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0,\"value\":\"invalidValue\"}]," +
            "\"rpc\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0,\"value\":\"invalidValue\"}]},{\"type\":\"serial\",\"name\":\"\",\"method\":\"\",\"baudrate\":-4800," +
            "\"stopbits\":-1,\"bytesize\":4,\"parity\":\"INVALID\",\"strict\":\"invalidBoolean\",\"unitId\":-1,\"deviceName\":\"\",\"deviceType\":\"\",\"sendDataOnlyOnChange\":\"invalidBoolean\"," +
            "\"timeout\":-35,\"byteOrder\":\"INVALID\",\"retries\":\"invalidBoolean\",\"retryOnEmpty\":\"invalidBoolean\",\"retryOnInvalid\":\"invalidBoolean\",\"pollPeriod\":-5000," +
            "\"connectAttemptTimeMs\":-5000,\"connectAttemptCount\":-5,\"waitAfterFailedAttemptsMs\":-300000,\"attributes\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0," +
            "\"address\":0},{\"tag\":\"\",\"type\":\"bits\",\"functionCode\":0,\"objectsCount\":0,\"address\":0}],\"timeseries\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0," +
            "\"address\":0},{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0}],\"attributeUpdates\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0," +
            "\"address\":0,\"value\":\"invalidValue\"}],\"rpc\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0,\"value\":\"invalidValue\"}],\"port\":\"invalidPort\"}]}," +
            "\"slave\":{\"type\":\"invalidType\",\"host\":\"\",\"port\":\"invalidPort\",\"method\":\"\",\"deviceName\":\"\",\"deviceType\":\"\",\"pollPeriod\":-5000," +
            "\"sendDataToThingsBoard\":\"invalidBoolean\",\"byteOrder\":\"INVALID\",\"wordOrder\":\"INVALID\",\"unitId\":-1,\"values\":{\"holding_registers\":{\"attributes\":[{\"address\":0,\"type\":\"\"," +
            "\"tag\":\"\",\"objectsCount\":0,\"value\":\"\"}],\"timeseries\":[{\"address\":0,\"type\":\"\",\"tag\":\"\",\"objectsCount\":0,\"value\":\"invalidValue\"}],\"attributeUpdates\":[{\"tag\":\"\"," +
            "\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0,\"value\":\"invalidValue\"}],\"rpc\":[{\"tag\":\"\",\"type\":\"\",\"functionCode\":0,\"objectsCount\":0,\"address\":0," +
            "\"value\":\"invalidValue\"}]},\"coils_initializer\":{\"attributes\":[{\"address\":0,\"type\":\"\",\"tag\":\"\",\"objectsCount\":0,\"value\":\"invalidValue\"}],\"timeseries\":[]," +
            "\"attributeUpdates\":[],\"rpc\":[]}}}}";


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
        assertThat(validationResult.getAnnotations()).isEmpty();
    }

    @Test
    public void testValidateOldMqttConnectorConfiguration_MissingFields() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(INVALID_OLD_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.0", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getAnnotations()).isNotNull();
        List<GatewayConnectorValidationRecord> annotations = validationResult.getAnnotations();
        assertThat(annotations.size()).isEqualTo(6);
        assertThat(annotations)
                .extracting("type", "text")
                .containsExactlyInAnyOrder(
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidPort\" for field \"port\". Expected type: Integer"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"username\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"password\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"host\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"port\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"topicExpression\" must not be blank")
                );
        validationResult.getAnnotations().forEach(error -> {
            assertThat(error.getRow()).isGreaterThanOrEqualTo(0);
            assertThat(error.getColumn()).isGreaterThan(0);
        });
    }

    // Test cases for Latest MQTT Configuration

    @Test
    public void testValidateLatestMqttConnectorConfiguration_Valid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(VALID_LATEST_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.5", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getAnnotations()).isEmpty();
    }

    @Test
    public void testValidateLatestMqttConnectorConfiguration_Invalid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(INVALID_LATEST_MQTT_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.5", ConnectorType.MQTT);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getAnnotations()).isNotNull();
        List<GatewayConnectorValidationRecord> annotations = validationResult.getAnnotations();
        assertThat(annotations.size()).isEqualTo(17);
        assertThat(annotations)
                .extracting("type", "text")
                .containsExactlyInAnyOrder(
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidPort\" for field \"port\". Expected type: Integer"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Field \"type\" contains unknown value, possible values: [bytes, custom, json]"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidQoS\" for field \"responseTopicQoS\". Expected type: Integer"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidTimeout\" for field \"responseTimeout\". Expected type: Long"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceProfileExpressionSource\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"responseTimeout\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceProfileExpression\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"port\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"responseTopicQoS\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"password\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"username\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"host\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"deviceInfo\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"sendDataOnlyOnChange\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"timeout\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"attributes\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"timeseries\" is unknown")
                );
        validationResult.getAnnotations().forEach(error -> {
            assertThat(error.getRow()).isGreaterThanOrEqualTo(0);
            assertThat(error.getColumn()).isGreaterThan(0);
        });
    }

    // Test cases for Old Modbus Configuration

    @Test
    public void testValidateOldModbusConnectorConfiguration_Valid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(VALID_OLD_MODBUS_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.0", ConnectorType.MODBUS);

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getAnnotations()).isEmpty();
    }

    @Test
    public void testValidateOldModbusConnectorConfiguration_Invalid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(INVALID_OLD_MODBUS_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.0", ConnectorType.MODBUS);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getAnnotations()).isNotNull();
        List<GatewayConnectorValidationRecord> annotations = validationResult.getAnnotations();
        assertThat(annotations.size()).isEqualTo(60);
        assertThat(annotations)
                .extracting("type", "text")
                .containsExactlyInAnyOrder(
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidPort\" for field \"type\". Expected type: Integer"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retries\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"strict\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retries\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Field \"type\" contains unknown value, possible values: [serial, tcp, udp]"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"pollPeriod\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"baudrate\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"port\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"pollPeriod\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceName\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"unitId\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"unitId\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceName\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"unitId\" must be greater than or equal to 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"method\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"pollPeriod\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be less than or equal to 4"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceName\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"stopbits\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"databits\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"method\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must not be null"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"host\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"timeout\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"timeout\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"someunknownField\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"someUnknownArray\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"host\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"port\" is unknown")
                );
        validationResult.getAnnotations().forEach(error -> {
            assertThat(error.getRow()).isGreaterThanOrEqualTo(0);
            assertThat(error.getColumn()).isGreaterThan(0);
        });
    }

    // Test cases for Latest Modbus Configuration

    @Test
    public void testValidateLatestModbusConnectorConfiguration_Valid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(VALID_LATEST_MODBUS_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.5.2", ConnectorType.MODBUS);

        assertThat(validationResult.isValid()).isTrue();
        assertThat(validationResult.getAnnotations()).isEmpty();
    }

    @Test
    public void testValidateLatestModbusConnectorConfiguration_Invalid() throws Exception {
        ObjectNode configuration = JacksonUtil.fromString(INVALID_LATEST_MODBUS_CONFIGURATION, new TypeReference<>() {
        });

        GatewayConnectorValidationResult validationResult = validateConfiguration(configuration, "3.5.2", ConnectorType.MODBUS);

        assertThat(validationResult.isValid()).isFalse();
        assertThat(validationResult.getAnnotations()).isNotNull();
        List<GatewayConnectorValidationRecord> annotations = validationResult.getAnnotations();
        assertThat(annotations.size()).isEqualTo(112);
        assertThat(annotations)
                .extracting("type", "text")
                .containsExactlyInAnyOrder(
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Field \"type\" contains unknown value, possible values: [serial, tcp, udp]"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retries\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retryOnEmpty\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retryOnInvalid\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"sendDataOnlyOnChange\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"strict\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"sendDataOnlyOnChange\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retries\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retryOnEmpty\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"retryOnInvalid\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Field \"type\" contains unknown value, possible values: [serial, tcp, udp]"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "Invalid value \"invalidBoolean\" for field \"sendDataToThingsBoard\". Expected type: Boolean"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 5"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"pollPeriod\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"unitId\" must be greater than or equal to 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"pollPeriod\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"connectAttemptCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"connectAttemptTimeMs\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"connectAttemptTimeMs\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceName\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"timeout\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"name\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"method\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"connectAttemptCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceName\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"unitId\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"waitAfterFailedAttemptsMs\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"stopbits\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"method\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"timeout\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"bytesize\" must be greater than or equal to 5"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"deviceName\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"name\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"waitAfterFailedAttemptsMs\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"functionCode\" must be greater than or equal to 1"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"pollPeriod\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"baudrate\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"method\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"objectsCount\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"unitId\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"address\" must be greater than 0"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"tag\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.ERROR, "\"type\" must not be blank"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"host\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"port\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"host\" is unknown"),
                        tuple(GatewayConnectorValidationRecordType.WARNING, "\"port\" is unknown")
                );
        validationResult.getAnnotations().forEach(error -> {
            assertThat(error.getRow()).isGreaterThanOrEqualTo(0);
            assertThat(error.getColumn()).isGreaterThan(0);
        });
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
