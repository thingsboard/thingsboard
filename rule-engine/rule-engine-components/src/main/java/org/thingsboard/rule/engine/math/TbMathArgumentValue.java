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
        return fromString(arg.getValue());
    }

    public static TbMathArgumentValue fromMessageBody(String key, Optional<ObjectNode> jsonNodeOpt) {
        if (jsonNodeOpt.isEmpty()) {
            throw new RuntimeException("Message body is empty!");
        }
        var json = jsonNodeOpt.get();
        if (!json.has(key)) {
            throw new RuntimeException("Message body has no '" + key + "'!");
        }
        JsonNode valueNode = json.get(key);
        if (valueNode.isEmpty() || valueNode.isNull()) {
            throw new RuntimeException("Message body has empty or null '" + key + "'!");
        }
        double value;
        if (valueNode.isNumber()) {
            value = valueNode.doubleValue();
        } else if (valueNode.isTextual()) {
            try {
                value = Double.parseDouble(valueNode.asText());
            } catch (NumberFormatException ne) {
                throw new RuntimeException("Can't convert value '" + valueNode.asText() + "' to double!");
            }
        } else {
            throw new RuntimeException("Can't convert value '" + valueNode.toString() + "' to double!");
        }
        return new TbMathArgumentValue(value);
    }

    public static TbMathArgumentValue fromMessageMetadata(String key, TbMsgMetaData metaData) {
        if (metaData == null) {
            throw new RuntimeException("Message metadata is empty!");
        }
        var value = metaData.getValue(key);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException("Message metadata has no '" + key + "'!");
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
