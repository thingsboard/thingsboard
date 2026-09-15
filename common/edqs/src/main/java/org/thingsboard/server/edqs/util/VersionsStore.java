// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
