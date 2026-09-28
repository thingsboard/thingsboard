// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractTbEntityService;

@Service
@TbCoreComponent
@RequiredArgsConstructor
public class DefaultTbSchedulerService extends AbstractTbEntityService implements TbSchedulerService {

    private final SchedulerEventService schedulerEventService;

    @Override
    public SchedulerEvent save(SchedulerEvent schedulerEvent, User user) throws ThingsboardException {
        try {
            SchedulerEvent savedSchedulerEvent = checkNotNull(schedulerEventService.saveSchedulerEvent(schedulerEvent));
            logEntityActionService.logEntityAction(user.getTenantId(), savedSchedulerEvent.getId(), savedSchedulerEvent,
                    savedSchedulerEvent.getCustomerId(),
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user);
            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT), schedulerEvent,
                    schedulerEvent.getId() == null ? ActionType.ADDED : ActionType.UPDATED, user, e);
            throw e;
        }
    }

    @Override
    public void delete(SchedulerEvent schedulerEvent, User user) throws ThingsboardException {
        ActionType actionType = ActionType.DELETED;
        SchedulerEventId schedulerEventId = schedulerEvent.getId();
        try {
            schedulerEventService.deleteSchedulerEvent(user.getTenantId(), schedulerEventId);
            logEntityActionService.logEntityAction(user.getTenantId(), schedulerEventId, schedulerEvent,
                    schedulerEvent.getCustomerId(), actionType, user, schedulerEventId.getId());
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    actionType, user, e, schedulerEventId.getId());
            throw e;
        }
    }

    @Override
    public SchedulerEventInfo assignToEdge(SchedulerEventId schedulerEventId, Edge edge, User user) throws ThingsboardException {
        try {
            SchedulerEventInfo savedSchedulerEvent = checkNotNull(schedulerEventService.assignSchedulerEventToEdge(user.getTenantId(), schedulerEventId, edge.getId()));
            logEntityActionService.logEntityAction(user.getTenantId(), schedulerEventId, savedSchedulerEvent,
                    ActionType.ASSIGNED_TO_EDGE, user, schedulerEventId.getId(), savedSchedulerEvent.getName(), edge.getId().getId(), edge.getName());
            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.ASSIGNED_TO_EDGE, user, e, schedulerEventId.getId(), edge.getId().getId());

            throw e;
        }
    }

    @Override
    public SchedulerEventInfo unassignFromEdge(SchedulerEventId schedulerEventId, Edge edge, User user) throws ThingsboardException {
        try {
            SchedulerEventInfo savedSchedulerEvent = checkNotNull(schedulerEventService.unassignSchedulerEventFromEdge(user.getTenantId(), schedulerEventId, edge.getId()));
            logEntityActionService.logEntityAction(user.getTenantId(), schedulerEventId, savedSchedulerEvent, ActionType.UNASSIGNED_FROM_EDGE,
                    user, schedulerEventId.getId(), savedSchedulerEvent.getName(), edge.getId().getId(), edge.getName());
            return savedSchedulerEvent;
        } catch (Exception e) {
            logEntityActionService.logEntityAction(user.getTenantId(), emptyId(EntityType.SCHEDULER_EVENT),
                    ActionType.UNASSIGNED_FROM_EDGE, user, e, schedulerEventId.getId());
            throw e;
        }
    }

}
