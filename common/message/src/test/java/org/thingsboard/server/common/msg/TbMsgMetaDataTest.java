// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TbMsgMetaDataTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final String metadataJsonStr = "{\"deviceName\":\"Test Device\",\"deviceType\":\"default\",\"ts\":\"1645112691407\"}";
    private JsonNode metadataJson;
    private Map<String, String> metadataExpected;

    @BeforeEach
    public void startInit() throws Exception {
        metadataJson = objectMapper.readValue(metadataJsonStr, JsonNode.class);
        metadataExpected = objectMapper.convertValue(metadataJson, new TypeReference<>() {
        });
    }

    @Test
    public void testScript_whenMetadataWithoutPropertiesValueNull_returnMetadataWithAllValue() {
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(metadataExpected);
        Map<String, String> dataActual = tbMsgMetaData.values();
        assertEquals(metadataExpected.size(), dataActual.size());
    }

    @Test
    public void testScript_whenMetadataWithPropertiesValueNull_returnMetadataWithoutPropertiesValueEqualsNull() {
        metadataExpected.put("deviceName", null);
        TbMsgMetaData tbMsgMetaData = new TbMsgMetaData(metadataExpected);
        Map<String, String> dataActual = tbMsgMetaData.copy().getData();
        assertEquals(metadataExpected.size() - 1, dataActual.size());
    }

    @Test
    public void testPutAndGetTypeCastDisabledKeysRoundTripKeysWithCommas() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putTypeCastDisabledKeys(List.of("a,b", "c\"d"));
        assertThat(metaData.getValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS)).isEqualTo("[\"a,b\",\"c\\\"d\"]");
        assertThat(metaData.getTypeCastDisabledKeys()).containsExactlyInAnyOrder("a,b", "c\"d");
    }

    @Test
    public void testPutTypeCastDisabledKeysSkipsEmptyAndNull() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putTypeCastDisabledKeys(Set.of());
        metaData.putTypeCastDisabledKeys(null);
        assertThat(metaData.getValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS)).isNull();
        assertThat(metaData.getTypeCastDisabledKeys()).isEmpty();
    }

    @Test
    public void testGetTypeCastDisabledKeysIsEmptyForInvalidValue() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS, "joinEui,devAddr");
        assertThat(metaData.getTypeCastDisabledKeys()).isEmpty();
        metaData.putValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS, "{\"a\":1}");
        assertThat(metaData.getTypeCastDisabledKeys()).isEmpty();
        metaData.putValue(TbMsgMetaData.TYPE_CAST_DISABLED_KEYS, "null");
        assertThat(metaData.getTypeCastDisabledKeys()).isEmpty();
        assertThat(new TbMsgMetaData().getTypeCastDisabledKeys()).isEqualTo(Set.of());
    }

    @Test
    public void testTbMsgReadsTypeCastDisabledKeysFromItsMetaData() {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putTypeCastDisabledKeys(List.of("joinEui"));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(metaData)
                .data("{}")
                .build();
        assertThat(msg.getTypeCastDisabledKeys()).containsExactly("joinEui");
        assertThat(TbMsg.newMsg()
                .type(TbMsgType.POST_ATTRIBUTES_REQUEST)
                .originator(new DeviceId(UUID.randomUUID()))
                .metaData(new TbMsgMetaData())
                .data("{}")
                .build()
                .getTypeCastDisabledKeys()).isEmpty();
    }

}
