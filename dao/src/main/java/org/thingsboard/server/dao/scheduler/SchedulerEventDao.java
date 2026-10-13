// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.scheduler;

import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ExportableEntityDao;
import org.thingsboard.server.dao.TenantEntityDao;

import java.util.UUID;

public interface SchedulerEventDao extends Dao<SchedulerEvent>, TenantEntityDao<SchedulerEvent>, ExportableEntityDao<SchedulerEventId, SchedulerEvent> {

    PageData<SchedulerEvent> findSchedulerEventsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

}
