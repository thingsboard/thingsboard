package org.thingsboard.server;

import com.google.common.util.concurrent.ListenableFuture;

public interface TbQueueAdmin {

    ListenableFuture<Void> createTopicIfNotExists(String topic);

}
