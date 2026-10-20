// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.stats;

import java.util.concurrent.atomic.AtomicInteger;

public class LocalCounter implements Counter {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public int getAndClear() {
        return counter.getAndSet(0);
    }

    @Override
    public void increment() {
        counter.incrementAndGet();
    }
}
