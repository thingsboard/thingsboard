package org.thingsboard.server.memory;

import org.thingsboard.server.TbQueueMsg;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryStorage {
    private static InMemoryStorage instance;
    private final Map<String, Queue<TbQueueMsg>> storage;

    private InMemoryStorage() {
        storage = new ConcurrentHashMap<>();
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
        return storage.computeIfAbsent(topic, (t) -> new LinkedList<>()).add(msg);
    }

    public TbQueueMsg get(String topic) {
        if (storage.containsKey(topic)) {
            return storage.get(topic).peek();
        }
        return null;
    }

    public void commit(String topic) {
        if (storage.containsKey(topic)) {
            storage.get(topic).remove();
        }
    }
}
