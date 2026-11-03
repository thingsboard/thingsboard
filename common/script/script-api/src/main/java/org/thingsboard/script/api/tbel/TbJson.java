// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.fasterxml.jackson.databind.JsonNode;
import org.mvel2.ExecutionContext;
import org.mvel2.util.ArgsRepackUtil;
import org.thingsboard.common.util.JacksonUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TbJson {

    public static String stringify(Object value) {
        return value != null ? JacksonUtil.toString(value) : "null";
    }

    public static Object parse(ExecutionContext ctx, String value) throws IOException {
        if (value != null) {
            JsonNode node = JacksonUtil.toJsonNode(value);
            if (node.isObject()) {
                return ArgsRepackUtil.repack(ctx, JacksonUtil.convertValue(node, Map.class));
            } else if (node.isArray()) {
                return ArgsRepackUtil.repack(ctx, JacksonUtil.convertValue(node, List.class));
            } else if (node.isDouble()) {
                return node.doubleValue();
            } else if (node.isLong()) {
                return node.longValue();
            } else if (node.isInt()) {
                return node.intValue();
            } else if (node.isBoolean()) {
                return node.booleanValue();
            } else if (node.isTextual()) {
                return node.asText();
            } else if (node.isBinary()) {
                return node.binaryValue();
            } else if (node.isNull()) {
                return null;
            } else {
                return node.asText();
            }
        } else {
            return null;
        }
    }
}
