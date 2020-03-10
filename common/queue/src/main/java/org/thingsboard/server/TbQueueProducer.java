package org.thingsboard.server;

import com.google.common.util.concurrent.ListenableFuture;

public interface TbQueueProducer<T extends TbQueueMsg> {

    void init();

    String getDefaultTopic();

    ListenableFuture<TbQueueMsgMetadata> send(T msg, TbQueueCallback callback);

    ListenableFuture<TbQueueMsgMetadata> send(String topic, T msg, TbQueueCallback callback);

}
