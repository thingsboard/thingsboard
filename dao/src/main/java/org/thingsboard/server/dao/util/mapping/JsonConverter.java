// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.util.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.thingsboard.common.util.JacksonUtil;

@Converter
public class JsonConverter implements AttributeConverter<JsonNode, String> {
    @Override
    public String convertToDatabaseColumn(JsonNode jsonNode) {
        return JacksonUtil.toString(jsonNode);
    }

    @Override
    public JsonNode convertToEntityAttribute(String s) {
        return JacksonUtil.toJsonNode(s);
    }
}
