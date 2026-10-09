// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.edqs.util;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
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

    /**
     * Records {@code version} for {@code key} and reports whether the event is new (not superseded by an
     * already-seen version). This is the single home for the delete-tombstone policy that every consumer of the
     * store shares.
     *
     * <p>On an accepted {@code DELETED} event two version regimes are handled differently, driven by the
     * {@code versionsResetOnDelete} flag the producer stamps on the event:
     * <ul>
     *   <li><b>Row versions may restart</b> (Citus per-row versions: after a delete, a re-created row's version
     *       counter restarts at 1). The stored tombstone version must be dropped, otherwise the re-create at v1
     *       would be rejected as stale and the resurrected entity would stay suppressed. The flag being set enables
     *       the {@link #remove(EdqsObjectKey)} below.</li>
     *   <li><b>Row versions never restart</b> (plain PostgreSQL: versions come from a global sequence). Keeping the
     *       tombstone version for the TTL window is strictly stronger: Kafka orders records per producer, not per
     *       key across producers, so a delete committed on one node can be consumed before a late update committed
     *       on another node. Retaining the tombstone lets the isNew gate reject that stale update instead of
     *       resurrecting the deleted object (including into the compacted backup topic). With the flag unset the
     *       entry is therefore kept.</li>
     * </ul>
     *
     * <p>Residual race even with the flag set: if a causally-later re-create(v1) is delivered before the delete(vN),
     * the re-create is discarded as outdated, then the delete is applied and clears the version entry — leaving a
     * live entity suppressed until the next event for it (TTL expiry only drops the version entry; it does not
     * re-apply the missed create).
     */
    public boolean isNew(EdqsObjectKey key, Long version, EdqsEventType eventType, boolean versionsResetOnDelete) {
        boolean isNew = isNew(key, version);
        if (isNew && versionsResetOnDelete && eventType == EdqsEventType.DELETED) {
            remove(key);
        }
        return isNew;
    }

    /**
     * Raw version-gate without the delete-tombstone policy. Production callers must use the 4-arg
     * {@link #isNew(EdqsObjectKey, Long, EdqsEventType, boolean)} overload (which applies the tombstone policy);
     * this bare form is package-private and exists only for the tests in this package.
     */
    @VisibleForTesting
    boolean isNew(EdqsObjectKey key, Long version) {
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

    @VisibleForTesting
    void remove(EdqsObjectKey key) {
        versions.remove(key);
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
