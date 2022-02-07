/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.common.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Valerii Sosliuk on 5/12/2017.
 */
public class JacksonUtil {

    private static final ObjectMapper OBJECT_MAPPER;
    private static final ObjectMapper OBJECT_MAPPER_WITH_UNQUOTED_FIELD_NAMES;

    static {
        OBJECT_MAPPER = getNewObjectMapperWithJavaTimeModule();
        OBJECT_MAPPER_WITH_UNQUOTED_FIELD_NAMES = getNewObjectMapperWithJavaTimeModule().
                configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), false).
                configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    }

    public static ObjectMapper getNewObjectMapperWithJavaTimeModule() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }

    public static ObjectMapper getObjectMapperWithUnquotedFieldNames() {
        return OBJECT_MAPPER_WITH_UNQUOTED_FIELD_NAMES;
    }

    public static <T> T convertValue(Object fromValue, Class<T> toValueType) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueType) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value: "
                    + fromValue + " cannot be converted to " + toValueType, e);
        }
    }

    public static <T> T convertValue(Object fromValue, TypeReference<T> toValueTypeRef) {
        try {
            return fromValue != null ? OBJECT_MAPPER.convertValue(fromValue, toValueTypeRef) : null;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("The given object value: "
                    + fromValue + " cannot be converted to " + toValueTypeRef, e);
        }
    }

    public static <T> T fromString(String string, Class<T> clazz) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String value: "
                    + string + " cannot be transformed to Json object", e);
        }
    }

    public static <T> T fromString(String string, JavaType javaType) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, javaType) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String value: "
                    + string + " cannot be transformed to Json object", e);
        }
    }

    public static <T> T fromString(String string, TypeReference<T> valueTypeRef) {
        try {
            return string != null ? OBJECT_MAPPER.readValue(string, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String value: "
                    + string + " cannot be transformed to Json object", e);
        }
    }

    public static <T> T fromBytes(byte[] bytes, Class<T> clazz) {
        try {
            return bytes != null ? OBJECT_MAPPER.readValue(bytes, clazz) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String value: "
                    + Arrays.toString(bytes) + " cannot be transformed to Json object", e);
        }
    }

    public static <T> T fromBytes(byte[] bytes, TypeReference<T> valueTypeRef) {
        try {
            return bytes != null ? OBJECT_MAPPER.readValue(bytes, valueTypeRef) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given string value: "
                    + Arrays.toString(bytes) + " cannot be transformed to Json object", e);
        }
    }

    public static JsonNode fromBytes(byte[] bytes) {
        try {
            return OBJECT_MAPPER.readTree(bytes);
        } catch (IOException e) {
            throw new IllegalArgumentException("The given byte[] value: "
                    + Arrays.toString(bytes) + " cannot be transformed to Json object", e);
        }
    }

    public static String toString(Object value) {
        try {
            return value != null ? OBJECT_MAPPER.writeValueAsString(value) : null;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + value + " cannot be transformed to a String", e);
        }
    }

    public static JsonNode toJsonNode(String value) {
        try {
            return !StringUtils.isEmpty(value) ? OBJECT_MAPPER.readTree(value) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String object value: "
                    + value + " cannot be transformed to a JsonNode", e);
        }
    }

    public static JsonNode toJsonNode(File value) {
        try {
            return value != null ? OBJECT_MAPPER.readTree(value) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given File object value: "
                    + value + " cannot be transformed to a JsonNode", e);
        }
    }

    public static JsonNode toJsonNodeUnquotedFieldNames(String value) {
        try {
            return !StringUtils.isEmpty(value) ? OBJECT_MAPPER_WITH_UNQUOTED_FIELD_NAMES.readTree(value) : null;
        } catch (IOException e) {
            throw new IllegalArgumentException("The given String object value: "
                    + value + " cannot be transformed to a JsonNode", e);
        }
    }

    public static <T> T treeToValue(JsonNode node, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.treeToValue(node, clazz);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can't convert value: " + node.toString(), e);
        }
    }

    public static ObjectNode newObjectNode() {
        return OBJECT_MAPPER.createObjectNode();
    }

    public static ObjectNode newObjectNodeUnquotedFieldNames() {
        return OBJECT_MAPPER_WITH_UNQUOTED_FIELD_NAMES.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return OBJECT_MAPPER.createArrayNode();
    }

    public static ArrayNode createArrayNodeUnquotedFieldNames() {
        return OBJECT_MAPPER_WITH_UNQUOTED_FIELD_NAMES.createArrayNode();
    }

    public static JavaType constructCollectionType(Class collectionClass, Class elementClass) {
        return OBJECT_MAPPER.getTypeFactory().constructCollectionType(collectionClass, elementClass);
    }

    public static <T> T clone(T value) {
        @SuppressWarnings("unchecked")
        Class<T> valueClass = (Class<T>) value.getClass();
        return fromString(toString(value), valueClass);
    }

    public static <T> JsonNode valueToTree(T value) {
        return OBJECT_MAPPER.valueToTree(value);
    }

    public static <T> byte[] writeValueAsBytes(T value) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("The given Json object value: "
                    + value + " cannot be transformed to a String", e);
        }
    }
}
