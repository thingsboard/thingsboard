package org.thingsboard.server;

import com.google.common.util.concurrent.ListenableFuture;

public interface TbQueueProducer<T extends TbQueueMsg> {

    ListenableFuture<TbQueueMsgMetadata> send(T msg, TbQueueCallback callback);

}
