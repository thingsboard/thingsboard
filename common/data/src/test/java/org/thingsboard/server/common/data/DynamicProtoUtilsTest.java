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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class DynamicProtoUtilsTest {

    @Test
    public void testProtoSchemaWithMessageNestedTypes() throws Exception {
        String schema = """
                syntax = "proto3";
                
                package testnested;
                
                message Outer {
                  message MiddleAA {
                    message Inner {
                      optional int64 ival = 1;
                      optional bool  booly = 2;
                    }
                    Inner inner = 1;
                  }
                  message MiddleBB {
                    message Inner {
                      optional int32 ival = 1;
                      optional bool  booly = 2;
                    }
                    Inner inner = 1;
                  }
                  MiddleAA middleAA = 1;
                  MiddleBB middleBB = 2;
                }""";
        DynamicSchema dynamicSchema = DynamicSchema.parseFromProtoString(schema, "test schema with nested types");
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

        assertEquals("""
                {
                  "middleAA": {
                    "inner": {
                      "ival": "1",
                      "booly": true
                    }
                  },
                  "middleBB": {
                    "inner": {
                      "ival": 0,
                      "booly": false
                    }
                  }
                }""", DynamicProtoUtils.dynamicMsgToJson(outerMsgBuilderDescriptor, outerMsg.toByteArray()));
    }

    @Test
    public void testProtoSchemaWithMessageOneOfs() throws Exception {
        String schema = """
                syntax = "proto3";
                
                package testoneofs;
                
                message SubMessage {
                   repeated string name = 1;
                }
                
                message SampleMessage {
                  optional int32 id = 1;
                  oneof testOneOf {
                     string name = 4;
                     SubMessage subMessage = 9;
                  }
                }""";
        DynamicSchema dynamicSchema = DynamicSchema.parseFromProtoString(schema, "test schema with message oneOfs");
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
        assertEquals("""
                {
                  "name": "Bob"
                }""", DynamicProtoUtils.dynamicMsgToJson(sampleMsgDescriptor, sampleMsg.toByteArray()));

        DynamicMessage.Builder subMsgBuilder = dynamicSchema.newMessageBuilder("testoneofs.SubMessage");
        Descriptors.Descriptor subMsgDescriptor = subMsgBuilder.getDescriptorForType();
        DynamicMessage subMsg = subMsgBuilder
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "Alice")
                .addRepeatedField(subMsgDescriptor.findFieldByName("name"), "John")
                .build();

        DynamicMessage sampleMsgWithOneOfSubMessage = sampleMsgBuilder.setField(sampleMsgDescriptor.findFieldByName("subMessage"), subMsg).build();
        assertEquals("""
                        {
                          "subMessage": {
                            "name": ["Alice", "John"]
                          }
                        }""",
                DynamicProtoUtils.dynamicMsgToJson(sampleMsgDescriptor, sampleMsgWithOneOfSubMessage.toByteArray()));
    }

    private static Stream<Arguments> testCheckProtoFileSyntax() {
        return Stream.of(
                Arguments.of(
                        "valid-schema.proto",
                        """
                        syntax = "proto3";
                        message Test {
                          string name = 1;
                        }
                        """,
                        true
                ),
                Arguments.of(
                        "valid-schema.proto",
                        """
                        syntax      =      "proto3";
                        message Test {
                          string name = 1;
                        }
                        """,
                        true
                ),
                Arguments.of(
                        "compact-schema.proto",
                        """
                        syntax="proto3";message Test{string name=1;}
                        """,
                        true
                ),
                Arguments.of(
                        "missing-syntax.proto",
                        """
                        message Test {
                          string name = 1;
                        }
                        """,
                        false
                ),
                Arguments.of(
                        "proto2-schema.proto",
                        """
                        syntax = "proto2";
                        message Test {
                          required string name = 1;
                        }
                        """,
                        false
                ),
                Arguments.of(
                        "null-schema.proto",
                        null,
                        false
                )
        );
    }

    @MethodSource
    @ParameterizedTest
    void testCheckProtoFileSyntax(String schemaName, String protoSchema, boolean shouldPass) {
        if (shouldPass) {
            assertDoesNotThrow(() -> DynamicProtoUtils.validateProtoSchema(protoSchema, schemaName, null));
            return;
        }
        assertThrows(IllegalArgumentException.class,
                () -> DynamicProtoUtils.validateProtoSchema(protoSchema, schemaName, null));
    }

}
