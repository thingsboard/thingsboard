// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BlockedScriptInfo {
    private final long maxScriptBlockDurationMs;
    private final AtomicInteger counter;
    private long expirationTime;

    BlockedScriptInfo(int maxScriptBlockDuration) {
        this.maxScriptBlockDurationMs = TimeUnit.SECONDS.toMillis(maxScriptBlockDuration);
        this.counter = new AtomicInteger(0);
    }

    public int get() {
        return counter.get();
    }

    public int incrementAndGet() {
        int result = counter.incrementAndGet();
        expirationTime = System.currentTimeMillis() + maxScriptBlockDurationMs;
        return result;
    }

    public long getExpirationTime() {
        return expirationTime;
    }
}
