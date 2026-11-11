// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.gen.MsgProtos.TbMsgProto;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TbMsgSerDesTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static EasyRandom easyRandom;

    @BeforeAll
    static void init() {
        EasyRandomParameters parameters = new EasyRandomParameters()
                .randomize(TbMsgCallback.class, () -> TbMsgCallback.EMPTY)
                .randomize(EntityId.class, () -> new DeviceId(UUID.randomUUID()));
        easyRandom = new EasyRandom(parameters);
    }

    @Test
    public void tbMsgProtoSerializationDeserialization() {
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("key1", "value1", "key2", "value2"));
        DeviceId deviceId = new DeviceId(UUID.randomUUID());

        TbMsg original = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(deviceId)
                .metaData(metaData)
                .data("{\"temperature\":42}")
                .build();

        TbMsgProto proto = TbMsg.toProto(original);
        TbMsg restored = TbMsg.fromProto(null, proto, TbMsgCallback.EMPTY);

        assertNotNull(restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getType(), restored.getType());
        assertEquals(original.getData(), restored.getData());
        assertEquals(original.getOriginator(), restored.getOriginator());
        assertEquals(original.getMetaData().getData(), restored.getMetaData().getData());
    }

    @Test
    public void tbMsgJsonSerializationDeserialization() throws Exception {
        TbMsg tbMsg = easyRandom.nextObject(TbMsg.class);
        byte[] bytes = objectMapper.writeValueAsBytes(tbMsg);
        TbMsg deserializedTbMsg = objectMapper.readValue(bytes, TbMsg.class);
        assertNotNull(deserializedTbMsg);

        assertEquals(tbMsg.getQueueName(), deserializedTbMsg.getQueueName());
        assertEquals(tbMsg.getId(), deserializedTbMsg.getId());
        assertEquals(tbMsg.getTs(), deserializedTbMsg.getTs());
        assertEquals(tbMsg.getType(), deserializedTbMsg.getType());
        assertEquals(tbMsg.getInternalType(), deserializedTbMsg.getInternalType());
        assertEquals(tbMsg.getOriginator(), deserializedTbMsg.getOriginator());
        assertEquals(tbMsg.getCustomerId(), deserializedTbMsg.getCustomerId());
        assertEquals(tbMsg.getMetaData(), deserializedTbMsg.getMetaData());
        assertEquals(tbMsg.getDataType(), deserializedTbMsg.getDataType());
        assertEquals(tbMsg.getData(), deserializedTbMsg.getData());
        assertEquals(tbMsg.getRuleChainId(), deserializedTbMsg.getRuleChainId());
        assertEquals(tbMsg.getRuleNodeId(), deserializedTbMsg.getRuleNodeId());
        assertEquals(tbMsg.getCorrelationId(), deserializedTbMsg.getCorrelationId());
        assertEquals(tbMsg.getPartition(), deserializedTbMsg.getPartition());
    }

}
