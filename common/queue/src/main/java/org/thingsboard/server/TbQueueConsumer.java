package org.thingsboard.server;

import java.util.List;

public interface TbQueueConsumer<T extends TbQueueMsg> {

    String getTopic();

    void subscribe();

    void unsubscribe();

    List<T> poll(long durationInMillis);

    void commit();

}
