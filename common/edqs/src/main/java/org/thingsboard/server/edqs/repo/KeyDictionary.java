// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.repo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class KeyDictionary {

    private static final ConcurrentMap<String, Integer> keyToIdDict = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, String> idToKeyDict = new ConcurrentHashMap<>();
    private static final AtomicInteger keySeq = new AtomicInteger();

    public static Integer get(String key) {
        return keyToIdDict.computeIfAbsent(key, __ -> {
            int keyId = keySeq.incrementAndGet();
            idToKeyDict.put(keyId, key);
            return keyId;
        });
    }

    public static String get(Integer keyId) {
        return idToKeyDict.get(keyId);
    }

}
