package org.thingsboard.server.dao.eventsourcing;

public interface TbApplicationEventPublisher {
    void publishEvent(TbSourcingEvent event);

    void publishEvictEvent(TbEvictEvent event);

//    void publishEvent(Object event);

}
