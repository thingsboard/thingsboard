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
package org.thingsboard.server.queue.memory;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.queue.TbQueueMsg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public final class InMemoryStorage {
    private static InMemoryStorage instance;
    private final ConcurrentHashMap<String, BlockingQueue<TbQueueMsg>> storage;

    private InMemoryStorage() {
        storage = new ConcurrentHashMap<>();
    }

    public void printStats() {
        storage.forEach((topic, queue) -> {
            if (queue.size() > 0) {
                log.debug("[{}] Queue Size [{}]", topic, queue.size());
            }
        });
    }

    public static InMemoryStorage getInstance() {
        if (instance == null) {
            synchronized (InMemoryStorage.class) {
                if (instance == null) {
                    instance = new InMemoryStorage();
                }
            }
        }
        return instance;
    }

    public boolean put(String topic, TbQueueMsg msg) {
        return storage.computeIfAbsent(topic, (t) -> new LinkedBlockingQueue<>()).add(msg);
    }

    public <T extends TbQueueMsg> List<T> get(String topic) {
        if (storage.containsKey(topic)) {
            List<T> entities;
            @SuppressWarnings("unchecked")
            T first = (T) storage.get(topic).poll();
            if (first != null) {
                entities = new ArrayList<>();
                entities.add(first);
                List<TbQueueMsg> otherList = new ArrayList<>();
                storage.get(topic).drainTo(otherList, 999);
                for (TbQueueMsg other : otherList) {
                    @SuppressWarnings("unchecked")
                    T entity = (T) other;
                    entities.add(entity);
                }
            } else {
                entities = Collections.emptyList();
            }
            return entities;
        }
        return Collections.emptyList();
    }

    /**
     * Used primarily for testing.
     */
    public void cleanup() {
        storage.clear();
    }

}
