package org.thingsboard.server;

import java.util.List;

public interface TbQueueConsumer<T extends TbQueueMsg> {

    List<TbQueueMsg> poll();

    void commit();

}
