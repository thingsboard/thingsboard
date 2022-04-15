/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2022 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public final class DefaultInMemoryStorage implements InMemoryStorage {
    private final ConcurrentHashMap<String, BlockingQueue<TbQueueMsg>> storage = new ConcurrentHashMap<>();

    @Override
    public void printStats() {
        if (log.isDebugEnabled()) {
            storage.forEach((topic, queue) -> {
                if (queue.size() > 0) {
                    log.debug("[{}] Queue Size [{}]", topic, queue.size());
                }
            });
        }
    }

    @Override
    public int getLagTotal() {
        return storage.values().stream().map(BlockingQueue::size).reduce(0, Integer::sum);
    }

    @Override
    public boolean put(String topic, TbQueueMsg msg) {
        return storage.computeIfAbsent(topic, (t) -> new LinkedBlockingQueue<>()).add(msg);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends TbQueueMsg> List<T> get(String topic) throws InterruptedException {
        final BlockingQueue<TbQueueMsg> queue = storage.get(topic);
        if (queue != null) {
            final TbQueueMsg firstMsg = queue.poll();
            if (firstMsg != null) {
                final int queueSize = queue.size();
                if (queueSize > 0) {
                    final List<TbQueueMsg> entities = new ArrayList<>(Math.min(queueSize, 999) + 1);
                    entities.add(firstMsg);
                    queue.drainTo(entities, 999);
                    return (List<T>) entities;
                }
                return Collections.singletonList((T) firstMsg);
            }
        }
        return Collections.emptyList();
    }

}
