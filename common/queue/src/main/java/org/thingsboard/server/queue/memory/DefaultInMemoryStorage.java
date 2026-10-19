// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    public int getLag(String topic) {
        return Optional.ofNullable(storage.get(topic)).map(Collection::size).orElse(0);
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
