// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
@AllArgsConstructor
public class ActorTestCtx {

    private volatile CountDownLatch latch;
    private final AtomicInteger invocationCount;
    private final int expectedInvocationCount;
    private final AtomicLong actual;

    public void clear() {
        latch = new CountDownLatch(1);
        invocationCount.set(0);
        actual.set(0L);
    }
}
