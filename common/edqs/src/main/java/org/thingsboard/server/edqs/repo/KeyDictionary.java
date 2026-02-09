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
