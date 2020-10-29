/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.TransportPayloadType;

import java.io.IOException;

@Slf4j
public class MqttTransportConfigurationDeserializer extends StdDeserializer<MqttDeviceProfileTransportConfiguration> {

    public MqttTransportConfigurationDeserializer() {
        super(MqttDeviceProfileTransportConfiguration.class);
    }

    @Override
    public MqttDeviceProfileTransportConfiguration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        try {
            JsonNode jsonNode = jsonParser.readValueAsTree();
            if (jsonNode.hasNonNull("transportPayloadType") && jsonNode.get("transportPayloadType").asText().equals(TransportPayloadType.PROTOBUF.name())) {
                return jsonParser.getCodec().treeToValue(jsonNode, MqttProtoDeviceProfileTransportConfiguration.class);
            } else {
                return jsonParser.getCodec().treeToValue(jsonNode, MqttJsonDeviceProfileTransportConfiguration.class);
            }
        } catch (IOException e) {
             log.trace("Failed to deserialize JSON content into equivalent tree model during creating {}!", MqttDeviceProfileTransportConfiguration.class.getSimpleName(), e);
             throw new RuntimeException("Failed to deserialize JSON content into equivalent tree model during creating " + MqttDeviceProfileTransportConfiguration.class.getSimpleName() + "!", e);
        }
    }

}