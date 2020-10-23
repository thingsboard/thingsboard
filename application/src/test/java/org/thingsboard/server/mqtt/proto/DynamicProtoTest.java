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
package org.thingsboard.server.mqtt.proto;

import com.github.os72.protobuf.dynamic.DynamicSchema;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.server.common.data.device.profile.MqttProtoDeviceProfileTransportConfiguration;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.thingsboard.server.common.data.device.profile.MqttProtoDeviceProfileTransportConfiguration.LOCATION;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class DynamicProtoTest {

    private static final String PROTO_SCHEMA_WITH_NESTED_MSG_TYPES = "syntax = \"proto3\";\n" +
            "\n" +
            "package testnested;\n" +
            "\n" +
            "message Outer {\n" +
            "  message MiddleAA {\n" +
            "    message Inner {\n" +
            "      int64 ival = 1;\n" +
            "      bool  booly = 2;\n" +
            "    }\n" +
            "    Inner inner = 1;\n" +
            "  }\n" +
            "  message MiddleBB {\n" +
            "    message Inner {\n" +
            "      int32 ival = 1;\n" +
            "      bool  booly = 2;\n" +
            "    }\n" +
            "    Inner inner = 1;\n" +
            "  }\n" +
            "  MiddleAA middleAA = 1;\n" +
            "  MiddleBB middleBB = 2;\n" +
            "}\n";

    private static final String PROTO_SCHEMA_WITH_ONE_OFS = "syntax = \"proto3\";\n" +
            "\n" +
            "package testoneofs;\n" +
            "\n" +
            "message SubMessage {\n" +
            "   repeated string name = 1;\n" +
            "}\n" +
            "\n" +
            "message SampleMessage {\n" +
            "  oneof testOneOf {\n" +
            "     string name = 4;\n" +
            "     SubMessage subMessage = 9;\n" +
            "  }\n" +
            "}";

    private static final String IVALID_PROTO_SCHEMA_REQUIRED_FIELD_EXISTS = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message SchemaValidationTest {\n" +
            "   required int32 parameter = 1;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_NOT_VALID_SYNTAX = "syntax = \"proto2\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message SchemaValidationTest {\n" +
            "   required int32 parameter = 1;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_OPTIONS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "option java_package = \"com.test.schemavalidation\";\n" +
            "option java_multiple_files = true;\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message SchemaValidationTest {\n" +
            "   int32 parameter = 1;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_PUBLIC_IMPORTS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "import public \"oldschema.proto\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message SchemaValidationTest {\n" +
            "   int32 parameter = 1;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_IMPORTS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "import \"oldschema.proto\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message SchemaValidationTest {\n" +
            "   int32 parameter = 1;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_EXTEND_DECLARATION_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "extend google.protobuf.MethodOptions {\n" +
            "  MyMessage my_method_option = 50007;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_ENUM_OPTIONS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "enum testEnum {\n" +
            "   option allow_alias = true;\n" +
            "   DEFAULT = 0;\n" +
            "   STARTED = 1;\n" +
            "   RUNNING = 2;\n" +
            "}\n" +
            "\n" +
            "message testMessage {\n" +
            "   int32 parameter = 1;\n" +
            "}\n";

    private static final String INVALID_PROTO_SCHEMA_NO_MESSAGE_TYPES_EXISTS = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "enum testEnum {\n" +
            "   DEFAULT = 0;\n" +
            "   STARTED = 1;\n" +
            "   RUNNING = 2;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_MESSAGE_OPTIONS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message testMessage {\n" +
            "   option allow_alias = true;\n" +
            "   int32 parameter = 1;\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_MESSAGE_EXTENSIONS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message TestMessage {\n" +
            "   extensions 100 to 199;\n" +
            "}\n";

    private static final String INVALID_PROTO_SCHEMA_MESSAGE_GROUPS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message TestMessage {\n" +
            "  repeated group Result = 1 {\n" +
            "    string url = 2;\n" +
            "    string title = 3;\n" +
            "    repeated string snippets = 4;\n" +
            "  }\n" +
            "}\n";

    private static final String INVALID_PROTO_SCHEMA_MESSAGE_RESERVED_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message Foo {\n" +
            "  reserved 2, 15, 9 to 11;\n" +
            "  reserved \"foo\", \"bar\";\n" +
            "}";

    private static final String INVALID_PROTO_SCHEMA_ONE_OFS_GROUPS_NOT_SUPPORTED = "syntax = \"proto3\";\n" +
            "\n" +
            "package schemavalidation;\n" +
            "\n" +
            "message SampleMessage {\n" +
            "  oneof test_oneof {\n" +
            "     string name = 1;\n" +
            "     group Result = 2 {\n" +
            "    \tstring url = 3;\n" +
            "    \tstring title = 4;\n" +
            "    \trepeated string snippets = 5;\n" +
            "     }\n" +
            "  }\n" +
            "}";

    private static final MqttProtoDeviceProfileTransportConfiguration mqttProtoDeviceProfileTransportConfiguration = new MqttProtoDeviceProfileTransportConfiguration();

    private static void validateTransportProtoSchema(String schema, String schemaName) {
        mqttProtoDeviceProfileTransportConfiguration.validateTransportProtoSchema(schema, schemaName);
    }

    private static DynamicSchema getDynamicSchema(String schema, String schemaName) {
        ProtoFileElement protoFileElement = getTransportProtoSchema(schema);
        return mqttProtoDeviceProfileTransportConfiguration.getDynamicSchema(protoFileElement, schemaName);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testDynamicSchemaProtoFileValidation() {
        processValidation("Failed to parse: testParseToProtoFile schema due to: Syntax error in :6:4: 'required' label forbidden in proto3 field declarations", IVALID_PROTO_SCHEMA_REQUIRED_FIELD_EXISTS, "testParseToProtoFile");
    }

    @Test
    public void testDynamicSchemaSyntaxValidation() {
        processValidation("Invalid schema syntax: proto2 for: testSyntaxValidation provided! Only proto3 allowed!", INVALID_PROTO_SCHEMA_NOT_VALID_SYNTAX, "testSyntaxValidation");
    }

    @Test
    public void testDynamicSchemaOptionsValidation() {
        processValidation("Invalid testOptionsValidation schema provided! Schema options don't support!", INVALID_PROTO_SCHEMA_OPTIONS_NOT_SUPPORTED, "testOptionsValidation");
    }

    @Test
    public void testDynamicSchemaPublicImportsValidation() {
        processValidation("Invalid testPublicImportsValidation schema provided! Schema public imports don't support!", INVALID_PROTO_SCHEMA_PUBLIC_IMPORTS_NOT_SUPPORTED, "testPublicImportsValidation");
    }

    @Test
    public void testDynamicSchemaImportsValidation() {
        processValidation("Invalid testImportsValidation schema provided! Schema imports don't support!", INVALID_PROTO_SCHEMA_IMPORTS_NOT_SUPPORTED, "testImportsValidation");
    }

    @Test
    public void testDynamicSchemaExtendDeclarationsValidation() {
        processValidation("Invalid testExtendDeclarationsValidation schema provided! Schema extend declarations don't support!", INVALID_PROTO_SCHEMA_EXTEND_DECLARATION_NOT_SUPPORTED, "testExtendDeclarationsValidation");
    }

    @Test
    public void testDynamicSchemaEnumOptionsValidation() {
        processValidation("Invalid testEnumOptionsValidation schema provided! Enum definitions options are not supported!", INVALID_PROTO_SCHEMA_ENUM_OPTIONS_NOT_SUPPORTED, "testEnumOptionsValidation");
    }

    @Test
    public void testDynamicSchemaNoOneMessageTypeExistsValidation() {
        processValidation("Invalid noOneMessageTypeExists schema provided! At least one Message definition should exists!", INVALID_PROTO_SCHEMA_NO_MESSAGE_TYPES_EXISTS, "noOneMessageTypeExists");
    }

    @Test
    public void testDynamicSchemaMessageTypeOptionsValidation() {
        processValidation("Invalid messageTypeOptions schema provided! Message definition options don't support!", INVALID_PROTO_SCHEMA_MESSAGE_OPTIONS_NOT_SUPPORTED, "messageTypeOptions");
    }

    @Test
    public void testDynamicSchemaMessageTypeExtensionsValidation() {
        processValidation("Invalid messageTypeExtensions schema provided! Message definition extensions don't support!", INVALID_PROTO_SCHEMA_MESSAGE_EXTENSIONS_NOT_SUPPORTED, "messageTypeExtensions");
    }

    @Test
    public void testDynamicSchemaMessageTypeReservedElementsValidation() {
        processValidation("Invalid messageTypeReservedElements schema provided! Message definition reserved elements don't support!", INVALID_PROTO_SCHEMA_MESSAGE_RESERVED_NOT_SUPPORTED, "messageTypeReservedElements");
    }

    @Test
    public void testDynamicSchemaMessageTypeGroupsElementsValidation() {
        processValidation("Invalid messageTypeGroupsElements schema provided! Message definition groups don't support!", INVALID_PROTO_SCHEMA_MESSAGE_GROUPS_NOT_SUPPORTED, "messageTypeGroupsElements");
    }

    @Test
    public void testDynamicSchemaOneOfsTypeGroupsElementsValidation() {
        processValidation("Invalid oneOfsTypeGroupsElements schema provided! OneOf definition groups don't support!", INVALID_PROTO_SCHEMA_ONE_OFS_GROUPS_NOT_SUPPORTED, "oneOfsTypeGroupsElements");
    }

    private void processValidation(String expectedMessage, String schema, String schemaName) {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(expectedMessage);
        validateTransportProtoSchema(schema, schemaName);
    }

    @Test
    public void testDynamicSchemaCreationWithMessageNestedTypes() throws Exception {
        String testNestedTypesProtoSchema = "testNestedTypesProtoSchema";
        validateTransportProtoSchema(PROTO_SCHEMA_WITH_NESTED_MSG_TYPES, testNestedTypesProtoSchema);
        DynamicSchema dynamicSchema = getDynamicSchema(PROTO_SCHEMA_WITH_NESTED_MSG_TYPES, testNestedTypesProtoSchema);
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(5, messageTypes.size());
        assertTrue(messageTypes.contains("testnested.Outer"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleAA"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleAA.Inner"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleBB"));
        assertTrue(messageTypes.contains("testnested.Outer.MiddleBB.Inner"));

        DynamicMessage.Builder middleAAInnerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA.Inner");
        Descriptors.Descriptor middleAAInnerMsgDescriptor = middleAAInnerMsgBuilder.getDescriptorForType();
        DynamicMessage middleAAInnerMsg = middleAAInnerMsgBuilder
                .setField(middleAAInnerMsgDescriptor.findFieldByName("ival"), 1L)
                .setField(middleAAInnerMsgDescriptor.findFieldByName("booly"), true)
                .build();

        DynamicMessage.Builder middleAAMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA");
        Descriptors.Descriptor middleAAMsgDescriptor = middleAAMsgBuilder.getDescriptorForType();
        DynamicMessage middleAAMsg = middleAAMsgBuilder
                .setField(middleAAMsgDescriptor.findFieldByName("inner"), middleAAInnerMsg)
                .build();

        DynamicMessage.Builder middleBBInnerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleAA.Inner");
        Descriptors.Descriptor middleBBInnerMsgDescriptor = middleBBInnerMsgBuilder.getDescriptorForType();
        DynamicMessage middleBBInnerMsg = middleBBInnerMsgBuilder
                .setField(middleBBInnerMsgDescriptor.findFieldByName("ival"), 0L)
                .setField(middleBBInnerMsgDescriptor.findFieldByName("booly"), false)
                .build();

        DynamicMessage.Builder middleBBMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer.MiddleBB");
        Descriptors.Descriptor middleBBMsgDescriptor = middleBBMsgBuilder.getDescriptorForType();
        DynamicMessage middleBBMsg = middleBBMsgBuilder
                .setField(middleBBMsgDescriptor.findFieldByName("inner"), middleBBInnerMsg)
                .build();


        DynamicMessage.Builder outerMsgBuilder = dynamicSchema.newMessageBuilder("testnested.Outer");
        Descriptors.Descriptor outerMsgBuilderDescriptor = outerMsgBuilder.getDescriptorForType();
        DynamicMessage outerMsg = outerMsgBuilder
                .setField(outerMsgBuilderDescriptor.findFieldByName("middleAA"), middleAAMsg)
                .setField(outerMsgBuilderDescriptor.findFieldByName("middleBB"), middleBBMsg)
                .build();

        assertEquals("{\n" +
                "  \"middleAA\": {\n" +
                "    \"inner\": {\n" +
                "      \"ival\": \"1\",\n" +
                "      \"booly\": true\n" +
                "    }\n" +
                "  },\n" +
                "  \"middleBB\": {\n" +
                "    \"inner\": {\n" +
                "      \"ival\": 0,\n" +
                "      \"booly\": false\n" +
                "    }\n" +
                "  }\n" +
                "}", dynamicMsgToJson(outerMsgBuilderDescriptor, outerMsg.toByteArray()));
    }

    @Test
    public void testDynamicSchemaCreationWithMessageOneOfs() throws Exception {
        String testOneOfsProtoSchema = "testOneOfsProtoSchema";
        validateTransportProtoSchema(PROTO_SCHEMA_WITH_ONE_OFS, testOneOfsProtoSchema);
        DynamicSchema dynamicSchema = getDynamicSchema(PROTO_SCHEMA_WITH_ONE_OFS, testOneOfsProtoSchema);
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(2, messageTypes.size());
        assertTrue(messageTypes.contains("testoneofs.SubMessage"));
        assertTrue(messageTypes.contains("testoneofs.SampleMessage"));

        DynamicMessage.Builder sampleMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SampleMessage");
        Descriptors.Descriptor sampleMsgDescriptor = sampleMsgBuilder.getDescriptorForType();
        assertNotNull(sampleMsgDescriptor);

        List<Descriptors.FieldDescriptor> fields = sampleMsgDescriptor.getFields();
        assertEquals(2, fields.size());
        DynamicMessage sampleMsg = sampleMsgBuilder
                .setField(sampleMsgDescriptor.findFieldByName("name"), "Bob")
                .build();
        assertEquals("{\n" + "  \"name\": \"Bob\"\n" + "}", dynamicMsgToJson(sampleMsgDescriptor, sampleMsg.toByteArray()));

        DynamicMessage.Builder subMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SubMessage");
        Descriptors.Descriptor subMsgDescriptor = subMsgBuilder.getDescriptorForType();
        DynamicMessage subMsg = subMsgBuilder
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "Alice")
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "John")
                .build();

        DynamicMessage sampleMsgWithOneOfSubMessage = sampleMsgBuilder.setField(sampleMsgDescriptor.findFieldByName("subMessage"), subMsg).build();
        assertEquals("{\n" + "  \"subMessage\": {\n" + "    \"name\": [\"Alice\", \"John\"]\n" + "  }\n" + "}",
                dynamicMsgToJson(sampleMsgDescriptor, sampleMsgWithOneOfSubMessage.toByteArray()));

    }

    private String dynamicMsgToJson(Descriptors.Descriptor descriptor, byte[] payload) throws InvalidProtocolBufferException {
        DynamicMessage dynamicMessage = DynamicMessage.parseFrom(descriptor, payload);
        return JsonFormat.printer().includingDefaultValueFields().print(dynamicMessage);
    }

    private static ProtoFileElement getTransportProtoSchema(String protoSchema) {
        return new ProtoParser(LOCATION, protoSchema.toCharArray()).readProtoFile();
    }
}
