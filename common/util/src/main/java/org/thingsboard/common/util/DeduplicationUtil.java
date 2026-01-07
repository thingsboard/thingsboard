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

import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.util.ConcurrentReferenceHashMap.ReferenceType.SOFT;

public class DeduplicationUtil {

    private static final ConcurrentMap<Object, Long> cache = new ConcurrentReferenceHashMap<>(16, SOFT);

    public static boolean alreadyProcessed(Object deduplicationKey, long deduplicationDuration) {
        AtomicBoolean alreadyProcessed = new AtomicBoolean(false);
        cache.compute(deduplicationKey, (key, lastProcessedTs) -> {
            if (lastProcessedTs != null) {
                long passed = System.currentTimeMillis() - lastProcessedTs;
                if (passed <= deduplicationDuration) {
                    alreadyProcessed.set(true);
                    return lastProcessedTs;
                }
            }
            return System.currentTimeMillis();
        });
        return alreadyProcessed.get();
    }

}
