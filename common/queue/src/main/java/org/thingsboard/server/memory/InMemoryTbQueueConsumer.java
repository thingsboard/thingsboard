package org.thingsboard.server.memory;

import org.thingsboard.server.TbQueueConsumer;
import org.thingsboard.server.TbQueueMsg;

import java.util.Collections;
import java.util.List;

public class InMemoryTbQueueConsumer<T extends TbQueueMsg> implements TbQueueConsumer<T> {
    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    public InMemoryTbQueueConsumer(String topic) {
        this.topic = topic;
    }

    private final String topic;

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void subscribe() {

    }

    @Override
    public void unsubscribe() {

    }

    @Override
    public List<T> poll(long durationInMillis) {
        return Collections.singletonList((T)storage.get(topic));
    }

    @Override
    public void commit() {
        storage.commit(topic);
    }
}
