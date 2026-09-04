// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.thingsboard.server.queue.discovery.event.TbApplicationEvent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class TbApplicationEventListener<T extends TbApplicationEvent> implements ApplicationListener<T> {

    private int lastProcessedSequenceNumber = Integer.MIN_VALUE;
    private final Lock seqNumberLock = new ReentrantLock();

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void onApplicationEvent(T event) {
        if (!filterTbApplicationEvent(event)) {
            log.trace("Skipping event due to filter: {}", event);
            return;
        }
        boolean validUpdate = false;
        seqNumberLock.lock();
        try {
            if (event.getSequenceNumber() > lastProcessedSequenceNumber) {
                validUpdate = true;
                lastProcessedSequenceNumber = event.getSequenceNumber();
            }
        } finally {
            seqNumberLock.unlock();
        }
        if (validUpdate) {
            try {
                onTbApplicationEvent(event);
            } catch (Exception e) {
                log.error("Failed to handle partition change event: {}", event, e);
            }
        } else {
            log.info("Application event ignored due to invalid sequence number ({} > {}). Event: {}", lastProcessedSequenceNumber, event.getSequenceNumber(), event);
        }
    }

    protected abstract void onTbApplicationEvent(T event);

    protected boolean filterTbApplicationEvent(T event) {
        return true;
    }

}
