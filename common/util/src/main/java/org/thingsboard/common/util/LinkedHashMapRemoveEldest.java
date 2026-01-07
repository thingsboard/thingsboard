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
package org.thingsboard.common.util;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * LinkedHashMap that removed eldest entries (by insert order)
 * It guaranteed that size is not greater then maxEntries parameter. And remove time is constant O(1).
 * Example:
 *   LinkedHashMapRemoveEldest<Long, String> map =
 *                 new LinkedHashMapRemoveEldest<>(MAX_ENTRIES, this::removeConsumer);
 * */
@Getter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class LinkedHashMapRemoveEldest<K, V> extends LinkedHashMap<K, V> {
    final int maxEntries;
    final BiConsumer<K, V> removalConsumer;

    public LinkedHashMapRemoveEldest(int maxEntries, BiConsumer<K, V> removalConsumer) {
        this.maxEntries = maxEntries;
        this.removalConsumer = removalConsumer;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        if (size() <= maxEntries) {
            return false;
        }
        removalConsumer.accept(eldest.getKey(), eldest.getValue());
        return true;
    }
}
