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
package org.thingsboard.server.common.data;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.github.os72.protobuf.dynamic.EnumDefinition;
import com.github.os72.protobuf.dynamic.MessageDefinition;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import com.squareup.wire.schema.internal.parser.TypeElement;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class DynamicProtoUtils {

    public static final Location LOCATION = new Location("", "", -1, -1);
    public static final String PROTO_3_SYNTAX = "proto3";

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
        ProtoFileElement protoFileElement = getProtoFileElement(protoSchema);
        DynamicSchema dynamicSchema = getDynamicSchema(protoFileElement, schemaName);
        String lastMsgName = getMessageTypes(protoFileElement.getTypes()).stream()
                .map(MessageElement::getName).reduce((previous, last) -> last).get();
        return dynamicSchema.newMessageBuilder(lastMsgName);
    }

    public static DynamicSchema getDynamicSchema(ProtoFileElement protoFileElement, String schemaName) {
        DynamicSchema.Builder schemaBuilder = DynamicSchema.newBuilder();
        schemaBuilder.setName(schemaName);
        schemaBuilder.setSyntax(PROTO_3_SYNTAX);
        schemaBuilder.setPackage(StringUtils.isNotEmpty(protoFileElement.getPackageName()) ?
                protoFileElement.getPackageName() : schemaName.toLowerCase());
        List<TypeElement> types = protoFileElement.getTypes();
        List<MessageElement> messageTypes = getMessageTypes(types);

        if (!messageTypes.isEmpty()) {
            List<EnumElement> enumTypes = getEnumElements(types);
            if (!enumTypes.isEmpty()) {
                enumTypes.forEach(enumElement -> {
                    EnumDefinition enumDefinition = getEnumDefinition(enumElement);
                    schemaBuilder.addEnumDefinition(enumDefinition);
                });
            }
            List<MessageDefinition> messageDefinitions = getMessageDefinitions(messageTypes);
            messageDefinitions.forEach(schemaBuilder::addMessageDefinition);
            try {
                return schemaBuilder.build();
            } catch (Descriptors.DescriptorValidationException e) {
                throw new RuntimeException("Failed to create dynamic schema due to: " + e.getMessage());
            }
        } else {
            throw new RuntimeException("Failed to get Dynamic Schema! Message types is empty for schema:" + schemaName);
        }
    }

    public static ProtoFileElement getProtoFileElement(String protoSchema) {
        return new ProtoParser(LOCATION, protoSchema.toCharArray()).readProtoFile();
    }

    public static String dynamicMsgToJson(Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

    public static DynamicMessage jsonToDynamicMessage(DynamicMessage.Builder builder, String payload) throws InvalidProtocolBufferException {
        JsonFormat.parser().ignoringUnknownFields().merge(payload, builder);
        return builder.build();
    }

    private static List<MessageElement> getMessageTypes(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof MessageElement)
                .map(typeElement -> (MessageElement) typeElement)
                .collect(Collectors.toList());
    }

    private static List<EnumElement> getEnumElements(List<TypeElement> types) {
        return types.stream()
                .filter(typeElement -> typeElement instanceof EnumElement)
                .map(typeElement -> (EnumElement) typeElement)
                .collect(Collectors.toList());
    }

    private static List<MessageDefinition> getMessageDefinitions(List<MessageElement> messageElementsList) {
        if (!messageElementsList.isEmpty()) {
            List<MessageDefinition> messageDefinitions = new ArrayList<>();
            messageElementsList.forEach(messageElement -> {
                MessageDefinition.Builder messageDefinitionBuilder = MessageDefinition.newBuilder(messageElement.getName());

                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        nestedEnumTypes.forEach(enumElement -> {
                            EnumDefinition nestedEnumDefinition = getEnumDefinition(enumElement);
                            messageDefinitionBuilder.addEnumDefinition(nestedEnumDefinition);
                        });
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    List<MessageDefinition> nestedMessageDefinitions = getMessageDefinitions(nestedMessageTypes);
                    nestedMessageDefinitions.forEach(messageDefinitionBuilder::addMessageDefinition);
                }
                List<FieldElement> messageElementFields = messageElement.getFields();
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    for (OneOfElement oneOfelement : oneOfs) {
                        MessageDefinition.OneofBuilder oneofBuilder = messageDefinitionBuilder.addOneof(oneOfelement.getName());
                        addMessageFieldsToTheOneOfDefinition(oneOfelement.getFields(), oneofBuilder);
                    }
                }
                if (!messageElementFields.isEmpty()) {
                    addMessageFieldsToTheMessageDefinition(messageElementFields, messageDefinitionBuilder);
                }
                messageDefinitions.add(messageDefinitionBuilder.build());
            });
            return messageDefinitions;
        } else {
            return Collections.emptyList();
        }
    }

    private static EnumDefinition getEnumDefinition(EnumElement enumElement) {
        List<EnumConstantElement> enumElementTypeConstants = enumElement.getConstants();
        EnumDefinition.Builder enumDefinitionBuilder = EnumDefinition.newBuilder(enumElement.getName());
        if (!enumElementTypeConstants.isEmpty()) {
            enumElementTypeConstants.forEach(constantElement -> enumDefinitionBuilder.addValue(constantElement.getName(), constantElement.getTag()));
        }
        return enumDefinitionBuilder.build();
    }


    private static void addMessageFieldsToTheMessageDefinition(List<FieldElement> messageElementFields, MessageDefinition.Builder messageDefinitionBuilder) {
        messageElementFields.forEach(fieldElement -> {
            String labelStr = null;
            if (fieldElement.getLabel() != null) {
                labelStr = fieldElement.getLabel().name().toLowerCase();
            }
            messageDefinitionBuilder.addField(
                    labelStr,
                    fieldElement.getType(),
                    fieldElement.getName(),
                    fieldElement.getTag());
        });
    }

    private static void addMessageFieldsToTheOneOfDefinition(List<FieldElement> oneOfsElementFields, MessageDefinition.OneofBuilder oneofBuilder) {
        oneOfsElementFields.forEach(fieldElement -> oneofBuilder.addField(
                fieldElement.getType(),
                fieldElement.getName(),
                fieldElement.getTag()));
        oneofBuilder.msgDefBuilder();
    }

    // validation

    public static void validateProtoSchema(String schema, String schemaName, String exceptionPrefix) throws IllegalArgumentException {
        ProtoParser schemaParser = new ProtoParser(LOCATION, schema.toCharArray());
        ProtoFileElement protoFileElement;
        try {
            protoFileElement = schemaParser.readProtoFile();
        } catch (Exception e) {
            throw new IllegalArgumentException(exceptionPrefix + " failed to parse " + schemaName + " due to: " + e.getMessage());
        }
        checkProtoFileSyntax(schemaName, protoFileElement);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getOptions().isEmpty(), " Schema options don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getPublicImports().isEmpty(), " Schema public imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getImports().isEmpty(), " Schema imports don't support!", exceptionPrefix);
        checkProtoFileCommonSettings(schemaName, protoFileElement.getExtendDeclarations().isEmpty(), " Schema extend declarations don't support!", exceptionPrefix);
        checkTypeElements(schemaName, protoFileElement, exceptionPrefix);
    }

    private static void checkProtoFileSyntax(String schemaName, ProtoFileElement protoFileElement) {
        if (protoFileElement.getSyntax() == null || !protoFileElement.getSyntax().equals(Syntax.PROTO_3)) {
            throw new IllegalArgumentException("[Transport Configuration] invalid schema syntax: " + protoFileElement.getSyntax() +
                    " for " + schemaName + " provided! Only " + Syntax.PROTO_3 + " allowed!");
        }
    }

    private static void checkProtoFileCommonSettings(String schemaName, boolean isEmptySettings, String invalidSettingsMessage, String exceptionPrefix) {
        if (!isEmptySettings) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + invalidSettingsMessage);
        }
    }

    private static void checkTypeElements(String schemaName, ProtoFileElement protoFileElement, String exceptionPrefix) {
        List<TypeElement> types = protoFileElement.getTypes();
        if (!types.isEmpty()) {
            if (types.stream().noneMatch(typeElement -> typeElement instanceof MessageElement)) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " At least one Message definition should exists!");
            } else {
                checkEnumElements(schemaName, getEnumElements(types), exceptionPrefix);
                checkMessageElements(schemaName, getMessageTypes(types), exceptionPrefix);
            }
        } else {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Type elements is empty!");
        }
    }

    private static void checkFieldElements(String schemaName, List<FieldElement> fieldElements, String exceptionPrefix) {
        if (!fieldElements.isEmpty()) {
            boolean hasRequiredLabel = fieldElements.stream().anyMatch(fieldElement -> {
                Field.Label label = fieldElement.getLabel();
                return label != null && label.equals(Field.Label.REQUIRED);
            });
            if (hasRequiredLabel) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Required labels are not supported!");
            }
            boolean hasDefaultValue = fieldElements.stream().anyMatch(fieldElement -> fieldElement.getDefaultValue() != null);
            if (hasDefaultValue) {
                throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Default values are not supported!");
            }
        }
    }

    private static void checkEnumElements(String schemaName, List<EnumElement> enumTypes, String exceptionPrefix) {
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getNestedTypes().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Nested types in Enum definitions are not supported!");
        }
        if (enumTypes.stream().anyMatch(enumElement -> !enumElement.getOptions().isEmpty())) {
            throw new IllegalArgumentException(invalidSchemaProvidedMessage(schemaName, exceptionPrefix) + " Enum definitions options are not supported!");
        }
    }

    private static void checkMessageElements(String schemaName, List<MessageElement> messageElementsList, String exceptionPrefix) {
        if (!messageElementsList.isEmpty()) {
            messageElementsList.forEach(messageElement -> {
                checkProtoFileCommonSettings(schemaName, messageElement.getGroups().isEmpty(),
                        " Message definition groups don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getOptions().isEmpty(),
                        " Message definition options don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getExtensions().isEmpty(),
                        " Message definition extensions don't support!", exceptionPrefix);
                checkProtoFileCommonSettings(schemaName, messageElement.getReserveds().isEmpty(),
                        " Message definition reserved elements don't support!", exceptionPrefix);
                checkFieldElements(schemaName, messageElement.getFields(), exceptionPrefix);
                List<OneOfElement> oneOfs = messageElement.getOneOfs();
                if (!oneOfs.isEmpty()) {
                    oneOfs.forEach(oneOfElement -> {
                        checkProtoFileCommonSettings(schemaName, oneOfElement.getGroups().isEmpty(),
                                " OneOf definition groups don't support!", exceptionPrefix);
                        checkFieldElements(schemaName, oneOfElement.getFields(), exceptionPrefix);
                    });
                }
                List<TypeElement> nestedTypes = messageElement.getNestedTypes();
                if (!nestedTypes.isEmpty()) {
                    List<EnumElement> nestedEnumTypes = getEnumElements(nestedTypes);
                    if (!nestedEnumTypes.isEmpty()) {
                        checkEnumElements(schemaName, nestedEnumTypes, exceptionPrefix);
                    }
                    List<MessageElement> nestedMessageTypes = getMessageTypes(nestedTypes);
                    checkMessageElements(schemaName, nestedMessageTypes, exceptionPrefix);
                }
            });
        }
    }

    public static String invalidSchemaProvidedMessage(String schemaName, String exceptionPrefix) {
        return exceptionPrefix + " invalid " + schemaName + " provided!";
    }

}
