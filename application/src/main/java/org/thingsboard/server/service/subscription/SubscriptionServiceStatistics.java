// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.subscription;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Data
public class SubscriptionServiceStatistics {
    private AtomicInteger alarmQueryInvocationCnt = new AtomicInteger();
    private AtomicInteger regularQueryInvocationCnt = new AtomicInteger();
    private AtomicInteger dynamicQueryInvocationCnt = new AtomicInteger();
    private AtomicLong alarmQueryTimeSpent = new AtomicLong();
    private AtomicLong regularQueryTimeSpent = new AtomicLong();
    private AtomicLong dynamicQueryTimeSpent = new AtomicLong();
}
