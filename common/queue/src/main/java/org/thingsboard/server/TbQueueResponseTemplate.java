package org.thingsboard.server;

public interface TbQueueResponseTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    void init(TbQueueHandler<Request, Response> handler);

    void stop();
}
