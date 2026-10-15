// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.transport.limits;

import lombok.Data;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
public class InetAddressRateLimitStats {

    private final Lock lock = new ReentrantLock();

    // Read without the lock on the hot path; written under it
    private volatile boolean blocked;
    private volatile long lastActivityTs;
    private volatile int failureCount;
    private int connectionsCount;

}
