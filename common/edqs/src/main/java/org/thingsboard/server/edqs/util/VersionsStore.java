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
package org.thingsboard.server.edqs.util;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class VersionsStore {

    private final ConcurrentMap<EdqsObjectKey, TimedValue> versions = new ConcurrentHashMap<>();
    private final long expirationMillis;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public VersionsStore(int ttlMinutes) {
        this.expirationMillis = TimeUnit.MINUTES.toMillis(ttlMinutes);
        startCleanupTask();
    }

    public boolean isNew(EdqsObjectKey key, Long version) {
        AtomicBoolean isNew = new AtomicBoolean(false);
        versions.compute(key, (k, prevVersion) -> {
            if (prevVersion == null || prevVersion.value <= version) {
                isNew.set(true);
                return new TimedValue(version);
            } else {
                log.debug("[{}] Version {} is outdated, the latest is {}", key, version, prevVersion);
                return prevVersion;
            }
        });
        return isNew.get();
    }

    private void startCleanupTask() {
        cleaner.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                for (Map.Entry<EdqsObjectKey, TimedValue> entry : versions.entrySet()) {
                    if (now - entry.getValue().lastUpdated > expirationMillis) {
                        versions.remove(entry.getKey(), entry.getValue());
                    }
                }
            } catch (Exception e) {
                log.error("Cleanup task failed", e);
            }
        }, expirationMillis, expirationMillis, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        cleaner.shutdown();
    }

    private static class TimedValue {
        private final long lastUpdated;
        private final long value;

        public TimedValue(long value) {
            this.value = value;
            this.lastUpdated = System.currentTimeMillis();
        }
    }

}
