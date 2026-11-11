// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import lombok.ToString;

import java.util.concurrent.atomic.AtomicLong;

@ToString
public class IntegrationStatistics {

    private final IntegrationContext ctx;
    private final AtomicLong messagesProcessed;
    private final AtomicLong errorsOccurred;

    public IntegrationStatistics(IntegrationContext ctx) {
        this.ctx = ctx;
        this.messagesProcessed = new AtomicLong(0);
        this.errorsOccurred = new AtomicLong(0);
    }

    public void incMessagesProcessed() {
        messagesProcessed.incrementAndGet();
        ctx.onUplinkMessageProcessed(true);
    }

    public void incErrorsOccurred() {
        errorsOccurred.incrementAndGet();
        ctx.onUplinkMessageProcessed(false);
    }

    public long getMessagesProcessed() {
        return messagesProcessed.get();
    }

    public long getErrorsOccurred() {
        return errorsOccurred.get();
    }

    public boolean isEmpty() {
        return getMessagesProcessed() == 0 && getErrorsOccurred() == 0;
    }

}
