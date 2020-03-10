package org.thingsboard.server;

import java.util.List;

public interface TbQueueConsumer<T extends TbQueueMsg> {

    String getTopic();

    void subscribe();

    List<T> poll(long durationInMillis);

    void commit();

}
