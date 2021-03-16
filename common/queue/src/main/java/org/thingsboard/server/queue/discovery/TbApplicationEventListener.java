/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.queue.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public abstract class TbApplicationEventListener<T extends TbApplicationEvent> implements ApplicationListener<T> {

    private int lastProcessedSequenceNumber = Integer.MIN_VALUE;
    private final Lock seqNumberLock = new ReentrantLock();

    @Override
    public void onApplicationEvent(T event) {
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
            onTbApplicationEvent(event);
        } else {
            log.info("Application event ignored due to invalid sequence number ({} > {}). Event: {}", lastProcessedSequenceNumber, event.getSequenceNumber(), event);
        }
    }

    protected abstract void onTbApplicationEvent(T event);


}
