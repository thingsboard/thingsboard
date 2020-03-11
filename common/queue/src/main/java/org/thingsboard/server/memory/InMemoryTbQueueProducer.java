package org.thingsboard.server.memory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.TbQueueCallback;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueMsgMetadata;
import org.thingsboard.server.TbQueueProducer;

public class InMemoryTbQueueProducer<T extends TbQueueMsg> implements TbQueueProducer<T> {

    private final InMemoryStorage storage = InMemoryStorage.getInstance();

    private String defaultTopic;

    @Override
    public void init() {

    }

    @Override
    public String getDefaultTopic() {
        return defaultTopic;
    }

    @Override
    public ListenableFuture<TbQueueMsgMetadata> send(T msg, TbQueueCallback callback) {
        return send(defaultTopic, msg, callback);
    }

    @Override
    public ListenableFuture<TbQueueMsgMetadata> send(String topic, T msg, TbQueueCallback callback) {
        boolean result = storage.put(topic, msg);
        if (result) {
            callback.onSuccess(null);
            return Futures.immediateCheckedFuture(null);
        } else {
            Exception e = new RuntimeException("Failure add msg to InMemoryQueue");
            callback.onFailure(e);
            return Futures.immediateFailedFuture(e);
        }
    }
}
