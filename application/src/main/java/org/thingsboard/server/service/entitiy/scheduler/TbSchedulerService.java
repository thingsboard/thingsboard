// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.scheduler;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;

public interface TbSchedulerService {

    SchedulerEvent save(SchedulerEvent schedulerEvent, User user) throws ThingsboardException;

    void delete(SchedulerEvent schedulerEvent, User user) throws ThingsboardException;

    SchedulerEventInfo assignToEdge(SchedulerEventId schedulerEventId, Edge edge, User user) throws ThingsboardException;

    SchedulerEventInfo unassignFromEdge(SchedulerEventId schedulerEventId, Edge edge, User user) throws ThingsboardException;

}
