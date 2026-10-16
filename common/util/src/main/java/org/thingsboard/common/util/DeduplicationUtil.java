// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
