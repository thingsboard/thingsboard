/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.mqtt.sparkplug.attributes;

import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.thingsboard.server.transport.mqtt.sparkplug.AbstractMqttV5ClientSparkplugTest;
import org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType;
import org.thingsboard.server.transport.mqtt.util.sparkplug.SparkplugMessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.Int32;
import static org.thingsboard.server.transport.mqtt.util.sparkplug.MetricDataType.UInt32;

/**
 * Created by nickAS21 on 12.01.23
 */
@Slf4j
public abstract class AbstractMqttV5ClientSparkplugAttributesTest extends AbstractMqttV5ClientSparkplugTest {

    protected ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * "sparkPlugAttributesMetricNames": ["SN node", "SN device", "Firmware version", "Date version", "Last date update"]
     * @throws Exception
     */

    protected void processClientWithCorrectAccessTokenPublishNCMDReBirth() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = new ArrayList<>();
        connectionWithBirth(listKeys, Int32, "Node Metric int32", nextInt32());
        // Shared attribute "Node Control/Rebirth" = true. type = NCMD.
        String key = "Node Control/Rebirth";
        boolean value = true;
        Assert.assertTrue(listKeys.contains(key));
        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + key + "\":" + value + "}";
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(key, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
    }

    /**
     * If boolean - send long 0 or 1
     * If String - try to parse long
     * If double - cast long
     * If we can't parse, cast, or JSON there - debug the message with the id of the devise/node, tenant,
     * the name and type of the attribute into an error and don't send anything.
     */
    protected void processClientWithCorrectAccessTokenPublishNCMD_BooleanType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = new ArrayList<>();
        MetricDataType metricDataType = MetricDataType.Boolean;
        String metricKey = "MyBoolean";
        Object metricValue = nextBoolean();
        connectionWithBirth(listKeys, metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);

        // Boolean <-> String
        boolean expectedValue = true;
        String valueStr = "123";
        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        expectedValue = false;
        valueStr = "0";
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Boolean <-> Integer
        expectedValue = true;
        Integer valueInt = nextInt32();
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueInt + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        expectedValue = false;
        valueInt = 0;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueInt + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getBooleanValue());
    }

    protected void processClientWithCorrectAccessTokenPublishNCMD_LongType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = new ArrayList<>();
        MetricDataType metricDataType = UInt32;
        String metricKey = "MyLong";
        Object metricValue = nextUInt32();
        connectionWithBirth(listKeys, metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);

        // Long <-> String
        String valueStr = "123";
        long expectedValue = Long.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getLongValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Long <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1L;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getLongValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0L;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getLongValue());
    }

    protected void processClientWithCorrectAccessTokenPublishNCMD_FloatType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = new ArrayList<>();
        MetricDataType metricDataType = MetricDataType.Float;
        String metricKey = "MyFloat";
        Object metricValue = nextFloat(30, 400);
        connectionWithBirth(listKeys, metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);

        // Float <-> String
        String valueStr = "123.345";
        float expectedValue = Float.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getFloatValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Float <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1f;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getFloatValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0f;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getFloatValue());
    }

    protected void processClientWithCorrectAccessTokenPublishNCMD_DoubleType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = new ArrayList<>();
        MetricDataType metricDataType = MetricDataType.Double;
        String metricKey = "MyDouble";
        Object metricValue = nextDouble();
        connectionWithBirth(listKeys, metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);

        // Double <-> String
        String valueStr = "123345456";
        double expectedValue = Double.valueOf(valueStr);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueStr + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getDoubleValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // Double <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = 1d;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getDoubleValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = 0d;
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertTrue(expectedValue == mqttCallback.getMessageArrivedMetrics().get(0).getDoubleValue());
    }

    protected void processClientWithCorrectAccessTokenPublishNCMD_StringType_IfMetricFailedTypeCheck_SendValueOk() throws Exception {
        clientWithCorrectNodeAccessTokenWithNDEATH();
        List<String> listKeys = new ArrayList<>();
        MetricDataType metricDataType = MetricDataType.String;
        String metricKey = "MyString";
        Object metricValue = nextString();
        connectionWithBirth(listKeys, metricDataType, metricKey, metricValue);
        Assert.assertTrue("Connection node is failed", client.isConnected());
        client.subscribeAndWait(NAMESPACE + "/" + groupId + "/NCMD/" + edgeNode + "/#", MqttQoS.AT_MOST_ONCE);

        // String <-> Long
        long valueLong = 123345456L;
        String expectedValue = String.valueOf(valueLong);

        String SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueLong + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getStringValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        // String <-> Boolean
        Boolean valueBoolean = true;
        expectedValue = "true";
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getStringValue());
        mqttCallback.deleteMessageArrivedMetrics(0);

        valueBoolean = false;
        expectedValue = "false";
        SHARED_ATTRIBUTES_PAYLOAD = "{\"" + metricKey + "\":" + valueBoolean + "}";
        doPostAsync("/api/plugins/telemetry/DEVICE/" + savedGateway.getId().getId() + "/attributes/SHARED_SCOPE", SHARED_ATTRIBUTES_PAYLOAD, String.class, status().isOk());
        await(alias + SparkplugMessageType.NBIRTH.name())
                .atMost(40, TimeUnit.SECONDS)
                .until(() -> {
                    return mqttCallback.getMessageArrivedMetrics().size() == 1;
                });
        Assert.assertEquals(metricKey, mqttCallback.getMessageArrivedMetrics().get(0).getName());
        Assert.assertEquals(expectedValue, mqttCallback.getMessageArrivedMetrics().get(0).getStringValue());
    }

}
