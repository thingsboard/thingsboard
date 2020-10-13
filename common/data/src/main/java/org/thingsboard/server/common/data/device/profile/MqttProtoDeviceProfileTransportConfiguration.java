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

import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.EnumDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.TypeElement;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.TransportPayloadType;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@JsonDeserialize(using = JsonDeserializer.None.class)
public class MqttProtoDeviceProfileTransportConfiguration extends MqttDeviceProfileTransportConfiguration {

    public static final Location LOCATION = new Location("", "", -1, -1);
    public static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    public static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";

    private String deviceTelemetryProtoSchema;
    private String deviceAttributesProtoSchema;

    @Override
    public DeviceTransportType getType() {
        return super.getType();
    }

    @Override
    public TransportPayloadType getTransportPayloadType() {
        return TransportPayloadType.PROTOBUF;
    }

    public void validateTransportProtoSchema(String schema, String schemaName) throws IllegalArgumentException {
        ProtoParser schemaParser = new ProtoParser(LOCATION, schema.toCharArray());
        ProtoFileElement protoFileElement;
        try {
            protoFileElement = schemaParser.readProtoFile();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse: " + schemaName + " due to: " + e.getMessage());
        }
        List<TypeElement> types = protoFileElement.getTypes();
        if (!CollectionUtils.isEmpty(types)) {
            if (types.stream().noneMatch(typeElement -> typeElement instanceof MessageElement)) {
                throw new IllegalArgumentException("Invalid " + schemaName + " provided! At least one Message definition should exists!");
            }
        } else {
            throw new IllegalArgumentException("Invalid " + schemaName + " provided!");
        }
    }

    public Descriptors.Descriptor getDynamicMessageDescriptor(String protoSchema, String schemaName) {
        ProtoFileElement protoFileElement = getTransportProtoSchema(protoSchema);
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        schemaBuilder.setName(schemaName);
        schemaBuilder.setPackage(!StringUtils.isEmpty(protoFileElement.getPackageName()) ?
                protoFileElement.getPackageName() : schemaName.toLowerCase());

        List<TypeElement> types = protoFileElement.getTypes();

        List<EnumElement> enumTypes = types.stream()
                .filter(typeElement -> typeElement instanceof EnumElement)
                .map(typeElement -> (EnumElement) typeElement)
                .collect(Collectors.toList());

        List<MessageElement> messageTypes = types.stream()
                .filter(typeElement -> typeElement instanceof MessageElement)
                .map(typeElement -> (MessageElement) typeElement)
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(enumTypes)) {
            enumTypes.forEach(enumElement -> {
                List<EnumConstantElement> enumElementTypeConstants = enumElement.getConstants();
                EnumDefinition.Builder enumDefinitionBuilder = EnumDefinition.newBuilder(enumElement.getName());
                if (!CollectionUtils.isEmpty(enumElementTypeConstants)) {
                    enumElementTypeConstants.forEach(constantElement -> enumDefinitionBuilder.addValue(constantElement.getName(), constantElement.getTag()));
                }
                EnumDefinition enumDefinition = enumDefinitionBuilder.build();
                schemaBuilder.addEnumDefinition(enumDefinition);
            });
        }

        if (!CollectionUtils.isEmpty(messageTypes)) {
            messageTypes.forEach(messageElement -> {
                List<FieldElement> messageElementFields = messageElement.getFields();
                MessageDefinition.Builder messageDefinitionBuilder = MessageDefinition.newBuilder(messageElement.getName());
                if (!CollectionUtils.isEmpty(messageElementFields)) {
                    messageElementFields.forEach(fieldElement -> messageDefinitionBuilder.addField(fieldElement.getType(), fieldElement.getName(), fieldElement.getTag(), fieldElement.getDefaultValue()));
                }
                MessageDefinition messageDefinition = messageDefinitionBuilder.build();
                schemaBuilder.addMessageDefinition(messageDefinition);
            });
            MessageElement lastMsg = messageTypes.stream().reduce((previous, last) -> last).get();
            try {
                DynamicSchema dynamicSchema = schemaBuilder.build();
                DynamicMessage.Builder builder = dynamicSchema.newMessageBuilder(lastMsg.getName());
                return builder.getDescriptorForType();
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException(e);
            }
        } else {
             throw new RuntimeException("Failed to get Message Descriptor! Message types is empty for " + schemaName + " schema!");
        }
    }


    private ProtoFileElement getTransportProtoSchema(String protoSchema) {
        return new ProtoParser(LOCATION, protoSchema.toCharArray()).readProtoFile();
    }

}
