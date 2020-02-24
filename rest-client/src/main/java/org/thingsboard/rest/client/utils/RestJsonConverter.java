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
package org.thingsboard.rest.client.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RestJsonConverter {
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final String LAST_UPDATE_TS = "lastUpdateTs";
    private static final String TS = "ts";

    private static final String CAN_T_PARSE_VALUE = "Can't parse value: ";

    public static List<AttributeKvEntry> toAttributes(List<JsonNode> attributes) {
        if (!CollectionUtils.isEmpty(attributes)) {
            return attributes.stream().map(attr -> {
                        KvEntry entry = parseValue(attr.get(KEY).asText(), attr.get(VALUE));
                        return new BaseAttributeKvEntry(entry, attr.get(LAST_UPDATE_TS).asLong());
                    }
            ).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static List<TsKvEntry> toTimeseries(Map<String, List<JsonNode>> timeseries) {
        if (!CollectionUtils.isEmpty(timeseries)) {
            List<TsKvEntry> result = new ArrayList<>();
            timeseries.forEach((key, values) ->
                    result.addAll(values.stream().map(ts -> {
                                KvEntry entry = parseValue(key, ts.get(VALUE));
                                return new BasicTsKvEntry(ts.get(TS).asLong(), entry);
                            }
                    ).collect(Collectors.toList()))
            );
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private static KvEntry parseValue(String key, JsonNode value) {
        if (!value.isObject()) {
            if (value.isBoolean()) {
                return new BooleanDataEntry(key, value.asBoolean());
            } else if (value.isNumber()) {
                return parseNumericValue(key, value);
            } else if (value.isTextual()) {
                return new StringDataEntry(key, value.asText());
            } else {
                throw new RuntimeException(CAN_T_PARSE_VALUE + value);
            }
        } else {
            return new JsonDataEntry(key, value.toString());
        }
    }

    private static KvEntry parseNumericValue(String key, JsonNode value) {
        if (value.isFloatingPointNumber()) {
            return new DoubleDataEntry(key, value.asDouble());
        } else {
            try {
                long longValue = Long.parseLong(value.toString());
                return new LongDataEntry(key, longValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Big integer values are not supported!");
            }
        }
    }
}
