// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ashvayka on 23.09.18.
 */
@Slf4j
@Getter
public class ConsistentHashCircle<K extends Comparable<K>, V> {
    private final ConcurrentNavigableMap<K, V> circle = new ConcurrentSkipListMap<>();
    private final AtomicLong floor = new AtomicLong();
    private final AtomicLong ceil = new AtomicLong();
    private final AtomicLong total = new AtomicLong();

    public void put(K hash, V instance) {
        circle.put(hash, instance);
    }

    public void remove(K hash) {
        circle.remove(hash);
    }

    public boolean isEmpty() {
        return circle.isEmpty();
    }

    public boolean containsKey(K hash) {
        return circle.containsKey(hash);
    }

    public ConcurrentNavigableMap<K, V> tailMap(K hash) {
        return circle.tailMap(hash);
    }

    public K firstKey() {
        return circle.firstKey();
    }

    public V get(Long hash) {
        return circle.get(hash);
    }

    public void log() {
        circle.forEach((key, value) -> log.debug("{} -> {}", key, value));
    }
}
