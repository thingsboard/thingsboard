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
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DynamicProtoUtilsTest {

    @Test
    public void testProtoSchemaWithMessageNestedTypes() throws Exception {
        String schema = "syntax = \"proto3\";\n" +
                "\n" +
                "package testnested;\n" +
                "\n" +
                "message Outer {\n" +
                "  message MiddleAA {\n" +
                "    message Inner {\n" +
                "      optional int64 ival = 1;\n" +
                "      optional bool  booly = 2;\n" +
                "    }\n" +
                "    Inner inner = 1;\n" +
                "  }\n" +
                "  message MiddleBB {\n" +
                "    message Inner {\n" +
                "      optional int32 ival = 1;\n" +
                "      optional bool  booly = 2;\n" +
                "    }\n" +
                "    Inner inner = 1;\n" +
                "  }\n" +
                "  MiddleAA middleAA = 1;\n" +
                "  MiddleBB middleBB = 2;\n" +
                "}";
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(schema);
        DynamicSchema dynamicSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, "test schema with nested types");
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
                "}", DynamicProtoUtils.dynamicMsgToJson(outerMsgBuilderDescriptor, outerMsg.toByteArray()));
    }

    @Test
    public void testProtoSchemaWithMessageOneOfs() throws Exception {
        String schema = "syntax = \"proto3\";\n" +
                "\n" +
                "package testoneofs;\n" +
                "\n" +
                "message SubMessage {\n" +
                "   repeated string name = 1;\n" +
                "}\n" +
                "\n" +
                "message SampleMessage {\n" +
                "  optional int32 id = 1;\n" +
                "  oneof testOneOf {\n" +
                "     string name = 4;\n" +
                "     SubMessage subMessage = 9;\n" +
                "  }\n" +
                "}";
        ProtoFileElement protoFileElement = DynamicProtoUtils.getProtoFileElement(schema);
        DynamicSchema dynamicSchema = DynamicProtoUtils.getDynamicSchema(protoFileElement, "test schema with message oneOfs");
        assertNotNull(dynamicSchema);
        Set<String> messageTypes = dynamicSchema.getMessageTypes();
        assertEquals(2, messageTypes.size());
        assertTrue(messageTypes.contains("testoneofs.SubMessage"));
        assertTrue(messageTypes.contains("testoneofs.SampleMessage"));

        DynamicMessage.Builder sampleMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SampleMessage");
        Descriptors.Descriptor sampleMsgDescriptor = sampleMsgBuilder.getDescriptorForType();
        assertNotNull(sampleMsgDescriptor);

        List<Descriptors.FieldDescriptor> fields = sampleMsgDescriptor.getFields();
        assertEquals(3, fields.size());
        DynamicMessage sampleMsg = sampleMsgBuilder
                .setField(sampleMsgDescriptor.findFieldByName("name"), "Bob")
                .build();
        assertEquals("{\n" + "  \"name\": \"Bob\"\n" + "}", DynamicProtoUtils.dynamicMsgToJson(sampleMsgDescriptor, sampleMsg.toByteArray()));

        DynamicMessage.Builder subMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SubMessage");
        Descriptors.Descriptor subMsgDescriptor = subMsgBuilder.getDescriptorForType();
        DynamicMessage subMsg = subMsgBuilder
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "Alice")
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "John")
                .build();

        DynamicMessage sampleMsgWithOneOfSubMessage = sampleMsgBuilder.setField(sampleMsgDescriptor.findFieldByName("subMessage"), subMsg).build();
        assertEquals("{\n" + "  \"subMessage\": {\n" + "    \"name\": [\"Alice\", \"John\"]\n" + "  }\n" + "}",
                DynamicProtoUtils.dynamicMsgToJson(sampleMsgDescriptor, sampleMsgWithOneOfSubMessage.toByteArray()));
    }

}
