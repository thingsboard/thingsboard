package org.thingsboard.server.dao.eventsourcing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpringTbApplicationEventPublisher implements TbApplicationEventPublisher {

    private final ApplicationEventPublisher context;
    private static final int MAX_STACK_TRACE_ELEMENTS = 5;

    @Override
    public void publishEvent(TbSourcingEvent event) {
        log.debug("[{}][{}] event: {}", event.getTenantId(), event.getUuid(), event);
        if (log.isTraceEnabled()) {
            int count = 0;
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                log.trace("[{}][{}] event stack trace: {}", event.getTenantId(), event.getUuid(), ste);
                count++;
                if (MAX_STACK_TRACE_ELEMENTS == count) {
                    break;
                }
            }
        }
        context.publishEvent(event);
    }

    @Override
    public void publishEvictEvent(TbEvictEvent event) {
        log.debug("[{}] event: {}", event.getUuid(), event);
        context.publishEvent(event);
    }

//    @Override
//    public void publishEvent(Object event) {
//        log.debug("Event: {}", event);
//        context.publishEvent(event);
//    }

}
