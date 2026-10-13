// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.edge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.ConverterUpdateMsg;
import org.thingsboard.server.gen.edge.v1.IntegrationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class IntegrationEdgeTest extends AbstractEdgeTest {

    @Test
    public void testIntegrations() throws Exception {
        JsonNode baseUrlAttribute = JacksonUtil.toJsonNode("{\"baseUrl\": \"http://localhost:18080\"}");
        doPost("/api/plugins/telemetry/" + EntityType.EDGE.name() + "/" + edge.getId() + "/SERVER_SCOPE", baseUrlAttribute)
                .andExpect(status().isOk());

        ObjectNode converterConfiguration = JacksonUtil.newObjectNode()
                .put("decoder", "return {deviceName: 'Device A', deviceType: 'thermostat'};");
        Converter converter = new Converter();
        converter.setName("My converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(converterConfiguration);
        converter.setEdgeTemplate(true);
        Converter savedConverter = doPost("/api/converter", converter, Converter.class);

        Integration integration = new Integration();
        integration.setName("Edge integration");
        integration.setRoutingKey(StringUtils.randomAlphanumeric(15));
        integration.setDefaultConverterId(savedConverter.getId());
        integration.setType(IntegrationType.HTTP);
        ObjectNode integrationConfiguration = JacksonUtil.newObjectNode();
        integrationConfiguration.putObject("metadata")
                .put("baseUrl", "${{baseUrl}}");
        integration.setConfiguration(integrationConfiguration);
        integration.setEdgeTemplate(true);
        Integration savedIntegration = doPost("/api/integration", integration, Integration.class);

        // wait 1 sec to make sure that save event of integration will go over EdgeEventSourcingListener
        // before integration will be assigned to edge to avoid duplicate edge events
        TimeUnit.SECONDS.sleep(1);

        // 1
        savedIntegration = validateIntegrationAssignToEdge(savedIntegration, savedConverter);

        // 2
        savedIntegration = validateIntegrationConfigurationUpdate(savedIntegration);

        // 3
        savedConverter = validateConverterConfigurationUpdate(savedConverter);

        // 4
        savedIntegration = validateIntegrationDefaultConverterUpdate(savedIntegration);

        // 5
        savedIntegration = validateIntegrationDownlinkConverterUpdate(savedIntegration);

        // 6
        validateAddingAndUpdateOfEdgeAttribute();

        // 7
        savedIntegration = validateIntegrationUnassignFromEdge(savedIntegration);

        // 8
        validateRemoveOfIntegration(savedIntegration);
    }

    private Converter validateConverterConfigurationUpdate(Converter savedConverter) throws Exception {
        edgeImitator.expectMessageAmount(1);

        savedConverter.setName("My new converter updated");
        savedConverter = doPost("/api/converter", savedConverter, Converter.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<ConverterUpdateMsg> newConverterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(newConverterUpdateMsgOpt.isPresent());
        ConverterUpdateMsg converterUpdateMsg = newConverterUpdateMsgOpt.get();
        Converter converter = JacksonUtil.fromString(converterUpdateMsg.getEntity(), Converter.class, true);
        Assert.assertNotNull(converter);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, converterUpdateMsg.getMsgType());
        Assert.assertEquals(savedConverter.getId(), converter.getId());
        Assert.assertEquals(savedConverter.getName(), converter.getName());
        return savedConverter;
    }

    private void validateAddingAndUpdateOfEdgeAttribute() throws Exception {
        edgeImitator.expectMessageAmount(3);
        JsonNode httpsBaseUrlAttribute = JacksonUtil.toJsonNode("{\"baseUrl\": \"https://localhost\"}");
        doPost("/api/plugins/telemetry/" + EntityType.EDGE.name() + "/" + edge.getId() + "/SERVER_SCOPE", httpsBaseUrlAttribute)
                .andExpect(status().isOk());

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Integration integration = JacksonUtil.fromString(integrationUpdateMsg.getEntity(), Integration.class, true);
        Assert.assertNotNull(integration);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertTrue(integration.getConfiguration().get("metadata").get("baseUrl").asText().contains("https://localhost/api/v1"));

        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(ConverterUpdateMsg.class).size());

        edgeImitator.expectMessageAmount(3);
        JsonNode deviceHWUrlAttribute = JacksonUtil.toJsonNode("{\"deviceHW\": \"PCM-2230\"}");
        doPost("/api/plugins/telemetry/" + EntityType.EDGE.name() + "/" + edge.getId() + "/SERVER_SCOPE", deviceHWUrlAttribute)
                .andExpect(status().isOk());

        Assert.assertTrue(edgeImitator.waitForMessages());

        integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        integrationUpdateMsg = integrationUpdateMsgOpt.get();
        integration = JacksonUtil.fromString(integrationUpdateMsg.getEntity(), Integration.class, true);
        Assert.assertNotNull(integration);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertTrue(integration.getConfiguration().get("metadata").get("baseUrl").asText().contains("https://localhost/api/v1"));
        Assert.assertTrue(integration.getConfiguration().get("metadata").get("deviceHW").asText().contains("PCM-2230"));

        Assert.assertEquals(2, edgeImitator.findAllMessagesByType(ConverterUpdateMsg.class).size());
    }

    private Integration validateIntegrationAssignToEdge(Integration savedIntegration, Converter savedConverter) throws Exception {
        edgeImitator.expectMessageAmount(2);

        savedIntegration = doPost("/api/edge/" + edge.getUuidId()
                + "/integration/" + savedIntegration.getUuidId(), Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Integration integration = JacksonUtil.fromString(integrationUpdateMsg.getEntity(), Integration.class, true);
        Assert.assertNotNull(integration);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        Assert.assertEquals(savedIntegration.getName(), integration.getName());
        Assert.assertTrue(integration.getConfiguration().get("metadata").get("baseUrl").asText().contains("http://localhost:18080"));

        Optional<ConverterUpdateMsg> converterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(converterUpdateMsgOpt.isPresent());
        ConverterUpdateMsg converterUpdateMsg = converterUpdateMsgOpt.get();
        Converter converter = JacksonUtil.fromString(converterUpdateMsg.getEntity(), Converter.class, true);
        Assert.assertNotNull(converter);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, converterUpdateMsg.getMsgType());
        Assert.assertEquals(savedConverter.getUuidId().getMostSignificantBits(), converterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedConverter.getUuidId().getLeastSignificantBits(), converterUpdateMsg.getIdLSB());
        Assert.assertEquals(savedConverter.getName(), converter.getName());
        return savedIntegration;
    }

    private Integration validateIntegrationConfigurationUpdate(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(2);

        ObjectNode updatedIntegrationConfig = JacksonUtil.newObjectNode();
        updatedIntegrationConfig.putObject("metadata")
                .put("baseUrl", "${{baseUrl}}/api/v1")
                .put("deviceHW", "${{deviceHW}}");
        savedIntegration.setConfiguration(updatedIntegrationConfig);
        savedIntegration = doPost("/api/integration", savedIntegration, Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Integration integration = JacksonUtil.fromString(integrationUpdateMsg.getEntity(), Integration.class, true);
        Assert.assertNotNull(integration);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertTrue(integration.getConfiguration().get("metadata").get("baseUrl").asText().contains("http://localhost:18080/api/v1"));
        return savedIntegration;
    }

    private Integration validateIntegrationDefaultConverterUpdate(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(2);

        ObjectNode newConverterConfiguration = JacksonUtil.newObjectNode()
                .put("decoder", "return {deviceName: 'Device B', deviceType: 'default'};");
        Converter converter = new Converter();
        converter.setName("My new converter");
        converter.setType(ConverterType.UPLINK);
        converter.setConfiguration(newConverterConfiguration);
        converter.setEdgeTemplate(true);
        Converter newSavedConverter = doPost("/api/converter", converter, Converter.class);

        savedIntegration.setDefaultConverterId(newSavedConverter.getId());
        savedIntegration = doPost("/api/integration", savedIntegration, Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Integration integration = JacksonUtil.fromString(integrationUpdateMsg.getEntity(), Integration.class, true);
        Assert.assertNotNull(integration);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        Assert.assertEquals(savedIntegration.getName(), integration.getName());

        Optional<ConverterUpdateMsg> newConverterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(newConverterUpdateMsgOpt.isPresent());
        ConverterUpdateMsg converterUpdateMsg = newConverterUpdateMsgOpt.get();
        Converter converterMsg = JacksonUtil.fromString(converterUpdateMsg.getEntity(), Converter.class, true);
        Assert.assertNotNull(converterMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, converterUpdateMsg.getMsgType());
        Assert.assertEquals(newSavedConverter.getId(), converterMsg.getId());
        Assert.assertEquals(newSavedConverter.getName(), converterMsg.getName());
        return savedIntegration;
    }

    private Integration validateIntegrationDownlinkConverterUpdate(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(3);

        ObjectNode downlinkConverterConfiguration = JacksonUtil.newObjectNode()
                .put("encoder", "return {contentType: 'JSON', data: '\"{\"pin\": 1}\"'};");
        Converter downlinkConverter = new Converter();
        downlinkConverter.setName("My downlink converter");
        downlinkConverter.setType(ConverterType.DOWNLINK);
        downlinkConverter.setConfiguration(downlinkConverterConfiguration);
        downlinkConverter.setEdgeTemplate(true);
        Converter savedDownlinkConverter = doPost("/api/converter", downlinkConverter, Converter.class);

        savedIntegration.setDownlinkConverterId(savedDownlinkConverter.getId());
        savedIntegration = doPost("/api/integration", savedIntegration, Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Integration integration = JacksonUtil.fromString(integrationUpdateMsg.getEntity(), Integration.class, true);
        Assert.assertNotNull(integration);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        Assert.assertEquals(savedIntegration.getName(), integration.getName());

        List<ConverterUpdateMsg> downlinkConverterUpdateMsgs = edgeImitator.findAllMessagesByType(ConverterUpdateMsg.class);

        ConverterUpdateMsg downlinkConverterUpdateMsg = null;
        for (ConverterUpdateMsg converterUpdateMsg : downlinkConverterUpdateMsgs) {
            Converter converterMsg = JacksonUtil.fromString(converterUpdateMsg.getEntity(), Converter.class, true);
            Assert.assertNotNull(converterMsg);
            if (savedDownlinkConverter.getName().equals(converterMsg.getName())) {
                downlinkConverterUpdateMsg = converterUpdateMsg;
            }
        }
        Assert.assertNotNull(downlinkConverterUpdateMsg);
        Converter converterMsg = JacksonUtil.fromString(downlinkConverterUpdateMsg.getEntity(), Converter.class, true);
        Assert.assertNotNull(converterMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, downlinkConverterUpdateMsg.getMsgType());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getMostSignificantBits(), downlinkConverterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getLeastSignificantBits(), downlinkConverterUpdateMsg.getIdLSB());
        Assert.assertEquals(savedDownlinkConverter.getName(), converterMsg.getName());

        edgeImitator.expectMessageAmount(1);

        downlinkConverterConfiguration = JacksonUtil.newObjectNode()
                .put("encoder", "return {contentType: 'JSON', data: '\"{\"pin\": 3}\"'};");
        savedDownlinkConverter.setConfiguration(downlinkConverterConfiguration);
        savedDownlinkConverter = doPost("/api/converter", savedDownlinkConverter, Converter.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<ConverterUpdateMsg> downlinkConverterUpdateMsgOpt = edgeImitator.findMessageByType(ConverterUpdateMsg.class);
        Assert.assertTrue(downlinkConverterUpdateMsgOpt.isPresent());
        downlinkConverterUpdateMsg = downlinkConverterUpdateMsgOpt.get();
        converterMsg = JacksonUtil.fromString(downlinkConverterUpdateMsg.getEntity(), Converter.class, true);
        Assert.assertNotNull(converterMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, downlinkConverterUpdateMsg.getMsgType());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getMostSignificantBits(), downlinkConverterUpdateMsg.getIdMSB());
        Assert.assertEquals(savedDownlinkConverter.getUuidId().getLeastSignificantBits(), downlinkConverterUpdateMsg.getIdLSB());
        Assert.assertEquals(downlinkConverterConfiguration, converterMsg.getConfiguration());
        return savedIntegration;
    }

    private Integration validateIntegrationUnassignFromEdge(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(1);

        savedIntegration = doDelete("/api/edge/" + edge.getUuidId()
                + "/integration/" + savedIntegration.getUuidId(), Integration.class);

        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<IntegrationUpdateMsg> integrationUpdateMsgOpt = edgeImitator.findMessageByType(IntegrationUpdateMsg.class);
        Assert.assertTrue(integrationUpdateMsgOpt.isPresent());
        IntegrationUpdateMsg integrationUpdateMsg = integrationUpdateMsgOpt.get();
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, integrationUpdateMsg.getMsgType());
        Assert.assertEquals(savedIntegration.getUuidId().getMostSignificantBits(), integrationUpdateMsg.getIdMSB());
        Assert.assertEquals(savedIntegration.getUuidId().getLeastSignificantBits(), integrationUpdateMsg.getIdLSB());
        return savedIntegration;
    }

    private void validateRemoveOfIntegration(Integration savedIntegration) throws Exception {
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/integration/" + savedIntegration.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages(10));
    }

}
