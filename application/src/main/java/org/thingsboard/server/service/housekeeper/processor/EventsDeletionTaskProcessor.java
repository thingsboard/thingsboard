// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.dao.event.EventService;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class EventsDeletionTaskProcessor extends HousekeeperTaskProcessor<HousekeeperTask> {

    private final EventService eventService;

    @Override
    public void process(HousekeeperTask task) throws Exception {
        // Only delete non-debug events for deleted entities.
        EventType[] nonDebugEventTypes = Arrays.stream(EventType.values()).filter(eventType -> !eventType.isDebug()).toArray(EventType[]::new);
        eventService.removeEvents(task.getTenantId(), task.getEntityId(), 0L, System.currentTimeMillis(), nonDebugEventTypes);
    }

    @Override
    public HousekeeperTaskType getTaskType() {
        return HousekeeperTaskType.DELETE_EVENTS;
    }

}
