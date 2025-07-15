/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DynamicProtoUtils {

    public static Descriptors.Descriptor getDescriptor(String protoSchema, String schemaName) {
        try {
            DynamicMessage.Builder builder = getDynamicMessageBuilder(protoSchema, schemaName);
            return builder.getDescriptorForType();
        } catch (Exception e) {
            log.warn("Failed to get Message Descriptor due to {}", e.getMessage());
            return null;
        }
    }

    public static DynamicMessage.Builder getDynamicMessageBuilder(String protoSchema, String schemaName) {
        DynamicSchema dynamicSchema;
        try {
            dynamicSchema = DynamicSchema.parseFromProtoString(protoSchema, schemaName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create dynamic schema due to: " + e.getMessage());
        }
        if (dynamicSchema.getMessageTypes().isEmpty()) {
            throw new RuntimeException("Failed to get Dynamic Schema! Message types is empty for schema: " + schemaName);
        }
        List<String> messageNamesInDeclarationOrder = dynamicSchema.getMessageNamesInDeclarationOrder();
        Optional<String> lastMsgNameOpt = Optional.ofNullable(messageNamesInDeclarationOrder.get(messageNamesInDeclarationOrder.size() - 1));
        return lastMsgNameOpt.map(dynamicSchema::newMessageBuilder).orElseThrow();
    }

    public static String dynamicMsgToJson(Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

    public static DynamicMessage jsonToDynamicMessage(DynamicMessage.Builder builder, String payload) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(payload, builder);
        return builder.build();
    }

    // validation

    public static void validateProtoSchema(String schema, String schemaName, String exceptionPrefix) throws IllegalArgumentException {
        checkProtoFileSyntax(schema, schemaName, exceptionPrefix);
        try {DynamicSchema dynamicSchema = DynamicSchema.parseFromProtoString(schema, schemaName);
            if (dynamicSchema.getMessageTypes().isEmpty()) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " At least one Message definition should exists!");
            }
        } catch (IOException | Descriptors.DescriptorValidationException | InterruptedException e) {
            throw new RuntimeException(exceptionPrefix + " failed to parse " + schemaName + " due to: " + e.getMessage());
        }
    }

    private static void checkProtoFileSyntax(String schema, String schemaName, String exceptionPrefix) {
        if (schema == null || !schema.replaceAll("\\s+", "").contains("syntax=\"proto3\";")) {
            throw new IllegalArgumentException(exceptionPrefix + " invalid schema syntax provided " +
                                               "for " + schemaName + "! Only proto3 syntax allowed!");
        }
    }

    public static String invalidSchemaProvidedMessage(String schemaName, String exceptionPrefix) {
        return exceptionPrefix + " invalid " + schemaName + " provided!";
    }

}
