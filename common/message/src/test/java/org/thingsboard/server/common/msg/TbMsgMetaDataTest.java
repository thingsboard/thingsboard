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
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
}
