/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.edge;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.protobuf.AbstractMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldRequestMsg;
import org.thingsboard.server.gen.edge.v1.CalculatedFieldUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UplinkMsg;
import org.thingsboard.server.gen.edge.v1.UplinkResponseMsg;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
public class CalculatedFieldEdgeTest extends AbstractEdgeTest {
    private static final String DEFAULT_CF_NAME = "Edge Test CalculatedField";
    private static final String UPDATED_CF_NAME = "Updated Edge Test CalculatedField";

    @Test
    public void testCalculatedField_create_update_delete() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);

        edgeImitator.expectMessageAmount(1);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
        Assert.assertEquals(savedCalculatedField.getUuidId().getMostSignificantBits(), calculatedFieldUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCalculatedField.getUuidId().getLeastSignificantBits(), calculatedFieldUpdateMsg.getIdLSB());
        CalculatedField calculatedFieldFromMsg = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromMsg);

        Assert.assertEquals(DEFAULT_CF_NAME, calculatedFieldFromMsg.getName());
        Assert.assertEquals(savedDevice.getId(), calculatedFieldFromMsg.getEntityId());
        Assert.assertEquals(config, calculatedFieldFromMsg.getConfiguration());

        edgeImitator.expectMessageAmount(1);
        savedCalculatedField.setName(UPDATED_CF_NAME);
        savedCalculatedField = doPost("/api/calculatedField", savedCalculatedField, CalculatedField.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        calculatedFieldFromMsg = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromMsg);
        Assert.assertEquals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
        Assert.assertEquals(UPDATED_CF_NAME, calculatedFieldFromMsg.getName());

        // delete calculatedField
        edgeImitator.expectMessageAmount(1);
        doDelete("/api/calculatedField/" + savedCalculatedField.getUuidId())
                .andExpect(status().isOk());
        Assert.assertTrue(edgeImitator.waitForMessages());

        latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        Assert.assertEquals(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
        Assert.assertEquals(savedCalculatedField.getUuidId().getMostSignificantBits(), calculatedFieldUpdateMsg.getIdMSB());
        Assert.assertEquals(savedCalculatedField.getUuidId().getLeastSignificantBits(), calculatedFieldUpdateMsg.getIdLSB());
    }

    @Test
    public void testSendCalculatedFieldToCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);
        UUID uuid = Uuids.timeBased();
        UplinkMsg uplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        checkCalculatedFieldOnCloud(uplinkMsg, uuid, calculatedField.getName());
    }

    @Test
    public void testSendCalculatedFieldRequestToCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);

        edgeImitator.expectMessageAmount(1);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        CalculatedFieldRequestMsg.Builder calculatedFieldRequestMsgBuilder = CalculatedFieldRequestMsg.newBuilder();
        calculatedFieldRequestMsgBuilder.setEntityIdMSB(savedDevice.getId().getId().getMostSignificantBits());
        calculatedFieldRequestMsgBuilder.setEntityIdLSB(savedDevice.getId().getId().getLeastSignificantBits());
        calculatedFieldRequestMsgBuilder.setEntityType(savedDevice.getId().getEntityType().name());
        testAutoGeneratedCodeByProtobuf(calculatedFieldRequestMsgBuilder);

        uplinkMsgBuilder.addCalculatedFieldRequestMsg(calculatedFieldRequestMsgBuilder.build());
        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsgBuilder.build());
        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        AbstractMessage latestMessage = edgeImitator.getLatestMessage();
        Assert.assertTrue(latestMessage instanceof CalculatedFieldUpdateMsg);
        CalculatedFieldUpdateMsg calculatedFieldUpdateMsg = (CalculatedFieldUpdateMsg) latestMessage;
        CalculatedField calculatedFieldFromEdge = JacksonUtil.fromString(calculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromEdge);
        Assert.assertEquals(savedCalculatedField, calculatedFieldFromEdge);
        Assert.assertEquals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE, calculatedFieldUpdateMsg.getMsgType());
    }

    @Test
    public void testUpdateCalculatedFieldNameOnCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);
        UUID uuid = Uuids.timeBased();
        UplinkMsg uplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        checkCalculatedFieldOnCloud(uplinkMsg, uuid, calculatedField.getName());

        calculatedField.setName(UPDATED_CF_NAME);
        UplinkMsg updatedUplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE);

        checkCalculatedFieldOnCloud(updatedUplinkMsg, uuid, calculatedField.getName());
    }

    @Test
    public void testCalculatedFieldToCloudWithNameThatAlreadyExistsOnCloud() throws Exception {
        Device savedDevice = saveDeviceOnCloudAndVerifyDeliveryToEdge();

        // create calculatedField
        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();
        CalculatedField calculatedField = createSimpleCalculatedField(savedDevice.getId(), config);

        edgeImitator.expectMessageAmount(1);
        CalculatedField savedCalculatedField = doPost("/api/calculatedField", calculatedField, CalculatedField.class);
        Assert.assertTrue(edgeImitator.waitForMessages());

        UUID uuid = Uuids.timeBased();

        UplinkMsg uplinkMsg = getUplinkMsg(uuid, calculatedField, UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE);

        edgeImitator.expectResponsesAmount(1);
        edgeImitator.expectMessageAmount(1);

        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());
        Assert.assertTrue(edgeImitator.waitForMessages());

        Optional<CalculatedFieldUpdateMsg> calculatedFieldUpdateMsgOpt = edgeImitator.findMessageByType(CalculatedFieldUpdateMsg.class);
        Assert.assertTrue(calculatedFieldUpdateMsgOpt.isPresent());
        CalculatedFieldUpdateMsg latestCalculatedFieldUpdateMsg = calculatedFieldUpdateMsgOpt.get();
        CalculatedField calculatedFieldFromMsg = JacksonUtil.fromString(latestCalculatedFieldUpdateMsg.getEntity(), CalculatedField.class, true);
        Assert.assertNotNull(calculatedFieldFromMsg);
        Assert.assertNotEquals(DEFAULT_CF_NAME, calculatedFieldFromMsg.getName());

        Assert.assertNotEquals(savedCalculatedField.getUuidId(), uuid);

        CalculatedField calculatedFieldFromCloud = doGet("/api/calculatedField/" + uuid, CalculatedField.class);
        Assert.assertNotNull(calculatedFieldFromCloud);
        Assert.assertNotEquals(DEFAULT_CF_NAME, calculatedFieldFromCloud.getName());
    }

    private CalculatedField createSimpleCalculatedField(EntityId entityId, SimpleCalculatedFieldConfiguration config) {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setEntityId(entityId);
        calculatedField.setTenantId(tenantId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName(DEFAULT_CF_NAME);
        calculatedField.setDebugSettings(DebugSettings.all());

        Argument argument = new Argument();
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);
        argument.setDefaultValue("12"); // not used because real telemetry value in db is present
        config.setArguments(Map.of("T", argument));

        config.setExpression("(T * 9/5) + 32");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("fahrenheitTemp");
        output.setDecimalsByDefault(2);
        config.setOutput(output);

        calculatedField.setConfiguration(config);

        return calculatedField;
    }

    private UplinkMsg getUplinkMsg(UUID uuid, CalculatedField calculatedField, UpdateMsgType updateMsgType) throws InvalidProtocolBufferException {
        UplinkMsg.Builder uplinkMsgBuilder = UplinkMsg.newBuilder();
        CalculatedFieldUpdateMsg.Builder calculatedFieldUpdateMsgBuilder = CalculatedFieldUpdateMsg.newBuilder();
        calculatedFieldUpdateMsgBuilder.setIdMSB(uuid.getMostSignificantBits());
        calculatedFieldUpdateMsgBuilder.setIdLSB(uuid.getLeastSignificantBits());
        calculatedFieldUpdateMsgBuilder.setEntity(JacksonUtil.toString(calculatedField));
        calculatedFieldUpdateMsgBuilder.setMsgType(updateMsgType);
        testAutoGeneratedCodeByProtobuf(calculatedFieldUpdateMsgBuilder);
        uplinkMsgBuilder.addCalculatedFieldUpdateMsg(calculatedFieldUpdateMsgBuilder.build());

        testAutoGeneratedCodeByProtobuf(uplinkMsgBuilder);

        return uplinkMsgBuilder.build();
    }

    private void checkCalculatedFieldOnCloud(UplinkMsg uplinkMsg, UUID uuid, String resourceTitle) throws Exception {
        edgeImitator.expectResponsesAmount(1);
        edgeImitator.sendUplinkMsg(uplinkMsg);

        Assert.assertTrue(edgeImitator.waitForResponses());

        UplinkResponseMsg latestResponseMsg = edgeImitator.getLatestResponseMsg();
        Assert.assertTrue(latestResponseMsg.getSuccess());

        CalculatedField calculatedField = doGet("/api/calculatedField/" + uuid, CalculatedField.class);
        Assert.assertNotNull(calculatedField);
        Assert.assertEquals(resourceTitle, calculatedField.getName());
    }

}
