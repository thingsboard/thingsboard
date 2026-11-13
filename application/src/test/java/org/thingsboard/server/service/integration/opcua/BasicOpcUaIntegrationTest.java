// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.opcua;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EventInfo;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.integration.AbstractIntegrationTest;
import org.thingsboard.server.service.integration.IntegrationDebugMessageStatus;
import org.thingsboard.server.service.integration.opcua.server.TestServer;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = {
        "js.evaluator=local",
        "service.integrations.supported=ALL",
        "transport.coap.enabled=true",
})
@Slf4j
@DaoSqlTest
public class BasicOpcUaIntegrationTest extends AbstractIntegrationTest {

    private final static String OPCUA_UPLINK_CONVERTER_FILEPATH = "opcua/default_converter_configuration.json";
    private final static String OPCUA_UPLINK_CONVERTER_NAME = "Default test uplink converter";
    private final static String OPCUA_DOWNLINK_CONVERTER_NAME = "Default test downlink converter";

    private final List<String> expectedNodes = Arrays.asList("Boolean", "Byte", "SByte", "Integer", "Int16", "Int32", "Int64", "UInteger", "UInt16", "UInt32", "UInt64", "Float", "Double", "String", "DateTime", "Guid", "ByteString", "XmlElement", "LocalizedText", "QualifiedName", "NodeId", "Variant", "Duration", "UtcTime");
    protected static TestServer server = new TestServer();

    @Before
    public void beforeTest() throws Exception {
        loginTenantAdmin();
        startServer();

        InputStream resourceAsStream = ObjectNode.class.getClassLoader().getResourceAsStream(OPCUA_UPLINK_CONVERTER_FILEPATH);
        ObjectNode jsonFile = JacksonUtil.fromBytes(resourceAsStream.readAllBytes(), ObjectNode.class);
        Assert.assertNotNull(jsonFile);

        if (jsonFile.has("configuration") && jsonFile.get("configuration").has("decoder")) {
            createConverter(OPCUA_UPLINK_CONVERTER_NAME, ConverterType.UPLINK, jsonFile.get("configuration"));
        }
        if (jsonFile.has("configuration") && jsonFile.get("configuration").has("encoder")) {
            createConverter(OPCUA_DOWNLINK_CONVERTER_NAME, ConverterType.DOWNLINK, jsonFile.get("configuration"));
        }
        Assert.assertNotNull(uplinkConverter);

        createIntegration("Test OPC-UA integration", IntegrationType.OPC_UA);
        Assert.assertNotNull(integration);
    }

    @After
    public void afterTest() throws Exception {
        try {
            disableIntegration();
            removeIntegration(integration);
            if (server.getStarted()) {
                stopServer();
            }
            Thread.sleep(5000); // Should be sufficient to process all messages from integration to properly delete tenant.
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error during removing OPC-UA integration", e);
        }
    }

    @Test
    public void testIntegrationRegularConnect() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS, 20));
    }

    @Test
    public void testIntegrationRegularDisconnect() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS, 20));
        startTs = System.currentTimeMillis();
        stopServer();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.FAILURE, 60));
    }

    @Test
    public void testIntegrationReconnectAfterServerRestart() throws Exception {
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS, 20));
        startTs = System.currentTimeMillis();
        stopServer();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.FAILURE));
        startServer();
        startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS));
    }

    @Test
    public void testIntegrationReconnectToNotStartedServer() throws Exception {
        long startTs = System.currentTimeMillis();
        stopServer();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.FAILURE, 20));
        startTs = System.currentTimeMillis();
        startServer();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS));
    }

    @Test
    public void testUplinkProcessing() throws Exception {
        Device savedDevice = createDevice("OPCUA_device", "opcua");
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS, 20));
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 15000;

        List<String> actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsyncTyped("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId().toString() + "/keys/timeseries", new TypeReference<>() {
            });
            if (actualKeys.size() == expectedNodes.size()) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        Assert.assertNotNull(actualKeys);

        Set<String> actualKeySet = new HashSet<>(actualKeys);
        Set<String> expectedKeySet = new HashSet<>(expectedNodes);

        Assert.assertEquals(expectedKeySet, actualKeySet);

        deleteDevice(savedDevice.getId());
    }

    @Test
    public void testDownlinkProcessing() throws Exception {
        Device savedDevice = createDevice("OPCUA_device", "opcua");
        long startTs = System.currentTimeMillis();
        enableIntegration();
        Assert.assertTrue(integrationConnectionStatusEquals(startTs, IntegrationDebugMessageStatus.SUCCESS, 20));

        TransportProtos.IntegrationDownlinkMsgProto downlinkMsgProto = createIntegrationDownlinkMessage(savedDevice.getId());

        downlinkService.onRuleEngineDownlinkMsg(integration.getTenantId(), integration.getId(), downlinkMsgProto, new TbCallback() {
            @Override
            public void onSuccess() {
                log.error("SUCCESS");
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("", t);
            }
        });
        List<EventInfo> integrationDownlinkDebugMessages = getIntegrationDebugMessages(startTs, "Downlink", IntegrationDebugMessageStatus.ANY, 20);
        Assert.assertFalse(integrationDownlinkDebugMessages.isEmpty());
        long start = System.currentTimeMillis();
        long end = System.currentTimeMillis() + 30000;

        ObjectNode actualKeys = null;
        while (start <= end) {
            actualKeys = doGetAsync("/api/plugins/telemetry/DEVICE/" + savedDevice.getId().getId().toString() + "/values/timeseries?keys=String", ObjectNode.class);
            if (actualKeys.has("String") && "New value".equals(actualKeys.get("String").get(0).get("value").asText())) {
                break;
            }
            Thread.sleep(100);
            start += 100;
        }
        Assert.assertNotNull(actualKeys);
        log.info(actualKeys.toString());
        Assert.assertNotNull(actualKeys);
        Assert.assertTrue(actualKeys.has("String"));
        Assert.assertEquals("New value", actualKeys.get("String").get(0).get("value").asText());

        deleteDevice(savedDevice.getId());
    }

    @Override
    protected JsonNode createIntegrationClientConfiguration() {

        ObjectNode clientConfiguration = JacksonUtil.newObjectNode();
        clientConfiguration.put("host", "127.0.0.1");
        clientConfiguration.put("port", 12686);
        clientConfiguration.put("scanPeriodInSeconds", 5);
        clientConfiguration.put("security", "None");
        clientConfiguration.put("timeoutInMillis", 5000);

        ObjectNode identityNode = JacksonUtil.newObjectNode();
        identityNode.put("type", "anonymous");

        clientConfiguration.set("identity", identityNode);

        ArrayNode mappingNode = JacksonUtil.newArrayNode();

        ObjectNode deviceConfigurationNode = JacksonUtil.newObjectNode();
        deviceConfigurationNode.put("deviceNodePattern", "Objects\\.HelloWorld");
        deviceConfigurationNode.put("mappingType", "FQN");

        ArrayNode subscriptionTagsNodes = JacksonUtil.newArrayNode();

        for (String expectedNode : expectedNodes) {
            ObjectNode subscriptionTagNode = JacksonUtil.newObjectNode();
            subscriptionTagNode.put("key", expectedNode);
            subscriptionTagNode.put("path", String.format("ScalarTypes/%s", expectedNode));
            subscriptionTagNode.put("required", Boolean.FALSE);

            subscriptionTagsNodes.add(subscriptionTagNode);
        }

        deviceConfigurationNode.set("subscriptionTags", subscriptionTagsNodes);

        mappingNode.add(deviceConfigurationNode);
        clientConfiguration.set("mapping", mappingNode);

        ObjectNode keystoreNode = JacksonUtil.newObjectNode();
        keystoreNode.put("location", "");
        keystoreNode.put("type", "");
        keystoreNode.put("fileContent", "");
        keystoreNode.put("password", "secret");
        keystoreNode.put("alias", "opc-ua-extension");
        keystoreNode.put("keyPassword", "secret");

        clientConfiguration.set("keystore", keystoreNode);
        clientConfiguration.set("metadata", JacksonUtil.newObjectNode());

        return clientConfiguration;
    }

    private TransportProtos.IntegrationDownlinkMsgProto createIntegrationDownlinkMessage(DeviceId originatorId) {
        ObjectNode dataNode = JacksonUtil.newObjectNode();
        ObjectNode writeValuesNode = JacksonUtil.newObjectNode();
        ArrayNode writeValuesArray = JacksonUtil.newArrayNode();
        ObjectNode writeValueNode = JacksonUtil.newObjectNode();
        writeValueNode.put("nodeId", "ns=2;s=HelloWorld/ScalarTypes/String");
        writeValueNode.put("value", "New value");
        writeValuesArray.add(writeValueNode);
        writeValuesNode.set("writeValues", writeValuesArray);
        dataNode.set("data", writeValuesNode);
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(new HashMap<>());

        TbMsg tbMsg = TbMsg.newMsg()
                .type(TbMsgType.ATTRIBUTES_UPDATED)
                .originator(originatorId)
                .copyMetaData(tbMsgMetaData)
                .data(writeValueNode.toString())
                .build();
        return TransportProtos.IntegrationDownlinkMsgProto.newBuilder()
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setIntegrationIdLSB(integration.getId().getId().getLeastSignificantBits())
                .setIntegrationIdMSB(integration.getId().getId().getMostSignificantBits())
                .setDataProto(TbMsg.toProto(tbMsg))
                .build();
    }

    private Boolean integrationConnectionStatusEquals(long startTs, IntegrationDebugMessageStatus expectedStatus) throws Exception {
        return integrationConnectionStatusEquals(startTs, expectedStatus, 60);
    }

    private Boolean integrationConnectionStatusEquals(long startTs, IntegrationDebugMessageStatus expectedStatus, long timeout) throws Exception {
        List<EventInfo> eventsList = getIntegrationDebugMessages(startTs, "CONNECT", expectedStatus, timeout);
        return !eventsList.isEmpty();
    }

    protected Device createDevice(String deviceName, String deviceType) {
        Device device = new Device();
        device.setName(deviceName);
        device.setType(deviceType);
        return doPost("/api/device", device, Device.class);
    }

    private void deleteDevice(DeviceId deviceId) throws Exception {
        doDelete("/api/device/" + deviceId.getId()).andExpect(status().isOk());
    }


    private static void startServer() throws ExecutionException, InterruptedException, TimeoutException {
        server.startup().get();
        log.error("Server started");
        Assert.assertTrue(server.getStarted());
    }

    private static void stopServer() throws ExecutionException, InterruptedException, TimeoutException {
        server.shutdown().get();
        log.error("Server stopped");
        Assert.assertFalse(server.getStarted());
    }

}
