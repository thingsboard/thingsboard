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
package org.thingsboard.rule.engine.math;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Optional;

public class TbMathArgumentValue {

    @Getter
    private final double value;

    private TbMathArgumentValue(double value) {
        this.value = value;
    }

    public static TbMathArgumentValue constant(TbMathArgument arg) {
        return fromString(arg.getKey());
    }

    private static TbMathArgumentValue defaultOrThrow(Double defaultValue, String error) {
        if (defaultValue != null) {
            return new TbMathArgumentValue(defaultValue);
        }
        throw new RuntimeException(error);
    }

    public static TbMathArgumentValue fromMessageBody(TbMathArgument arg, String argKey, Optional<ObjectNode> jsonNodeOpt) {
        Double defaultValue = arg.getDefaultValue();
        if (jsonNodeOpt.isEmpty()) {
            return defaultOrThrow(defaultValue, "Message body is empty!");
        }
        var json = jsonNodeOpt.get();
        if (!json.has(argKey)) {
            return defaultOrThrow(defaultValue, "Message body has no '" + argKey + "'!");
        }
        JsonNode valueNode = json.get(argKey);
        if (valueNode.isNull()) {
            return defaultOrThrow(defaultValue, "Message body has null '" + argKey + "'!");
        }
        double value;
        if (valueNode.isNumber()) {
            value = valueNode.doubleValue();
        } else if (valueNode.isTextual()) {
            var valueNodeText = valueNode.asText();
            if (StringUtils.isNotBlank(valueNodeText)) {
                try {
                    value = Double.parseDouble(valueNode.asText());
                } catch (NumberFormatException ne) {
                    throw new RuntimeException("Can't convert value '" + valueNode.asText() + "' to double!");
                }
            } else {
                return defaultOrThrow(defaultValue, "Message value is empty for '" + argKey + "'!");
            }
        } else {
            throw new RuntimeException("Can't convert value '" + valueNode.toString() + "' to double!");
        }
        return new TbMathArgumentValue(value);
    }

    public static TbMathArgumentValue fromMessageMetadata(TbMathArgument arg, String argKey, TbMsgMetaData metaData) {
        Double defaultValue = arg.getDefaultValue();
        if (metaData == null) {
            return defaultOrThrow(defaultValue, "Message metadata is empty!");
        }
        var value = metaData.getValue(argKey);
        if (StringUtils.isEmpty(value)) {
            return defaultOrThrow(defaultValue, "Message metadata has no '" + argKey + "'!");
        }
        return fromString(value);
    }

    public static TbMathArgumentValue fromLong(long value) {
        return new TbMathArgumentValue(value);
    }

    public static TbMathArgumentValue fromDouble(double value) {
        return new TbMathArgumentValue(value);
    }

    public static TbMathArgumentValue fromString(String value) {
        try {
            return new TbMathArgumentValue(Double.parseDouble(value));
        } catch (NumberFormatException ne) {
            throw new RuntimeException("Can't convert value '" + value + "' to double!");
        }
    }
}
