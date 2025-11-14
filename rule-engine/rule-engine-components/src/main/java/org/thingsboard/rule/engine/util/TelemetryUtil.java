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
package org.thingsboard.rule.engine.util;

import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BasicTsKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TelemetryUtil {

    public static List<TsKvEntry> toTsKvEntryList(Map<Long, List<KvEntry>> tsKvMap) {
        List<TsKvEntry> tsKvEntryList = new ArrayList<>();
        for (Map.Entry<Long, List<KvEntry>> tsKvEntry : tsKvMap.entrySet()) {
            for (KvEntry kvEntry : tsKvEntry.getValue()) {
                tsKvEntryList.add(new BasicTsKvEntry(tsKvEntry.getKey(), kvEntry));
            }
        }
        return tsKvEntryList;
    }

    public static List<AttributeKvEntry> filterChangedAttr(List<AttributeKvEntry> currentAttributes, List<AttributeKvEntry> newAttributes) {
        if (currentAttributes == null || currentAttributes.isEmpty()) {
            return newAttributes;
        }

        Map<String, AttributeKvEntry> currentAttrMap = currentAttributes.stream()
                .collect(Collectors.toMap(AttributeKvEntry::getKey, Function.identity(), (existing, replacement) -> existing));

        return newAttributes.stream()
                .filter(item -> {
                    AttributeKvEntry cacheAttr = currentAttrMap.get(item.getKey());
                    return cacheAttr == null
                            || !Objects.equals(item.getValue(), cacheAttr.getValue()) //JSON and String can be equals by value, but different by type
                            || !Objects.equals(item.getDataType(), cacheAttr.getDataType());
                })
                .collect(Collectors.toList());
    }

}
