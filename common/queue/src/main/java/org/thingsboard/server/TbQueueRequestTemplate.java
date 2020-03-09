package org.thingsboard.server;

import com.google.common.util.concurrent.ListenableFuture;

public interface TbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    ListenableFuture<Response> post(String key, Request request);

}
