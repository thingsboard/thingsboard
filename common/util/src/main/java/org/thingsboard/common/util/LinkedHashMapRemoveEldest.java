// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
