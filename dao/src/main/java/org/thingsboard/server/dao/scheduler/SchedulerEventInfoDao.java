// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.scheduler;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ScheduledReportQuery;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.Dao;

import java.util.List;
import java.util.UUID;

public interface SchedulerEventInfoDao extends Dao<SchedulerEventInfo> {

    SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(UUID tenantId, UUID schedulerEventId);

    List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId);

    List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndEnabled(UUID tenantId, boolean enabled);

    PageData<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndFilter(UUID tenantId, SchedulerEventFilter filter, PageLink pageLink);

    List<SchedulerEventWithCustomerInfo> findAllSchedulerEventsByTenantIdAndEventTimeFilter(UUID tenantId, SchedulerEventTimeFilter filter, String searchText);

    List<SchedulerEventId> findSchedulerEventsIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    List<SchedulerEventId> findSchedulerEventsIdsByTenantId(UUID tenantId);

    ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> schedulerEventIds);

    PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink);

    PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(UUID tenantId, UUID edgeId, UUID customerId, PageLink pageLink);

    PageData<ScheduledReportInfo> findScheduledReportEvents(UUID tenantId, ScheduledReportQuery query);

    PageData<ScheduledReportInfo> findScheduledReportEvents(UUID tenantId, UUID customerId, ScheduledReportQuery query);

    int countScheduledReportEventsByTemplateId(UUID tenantId, UUID templateId);
}
