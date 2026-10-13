// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.scheduler;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ScheduledReportQuery;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventDescriptor;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.common.data.scheduler.TimerRepeat;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SchedulerEventInfoEntity;
import org.thingsboard.server.dao.model.sql.SchedulerEventWithCustomerInfoEntity;
import org.thingsboard.server.dao.scheduler.SchedulerEventInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@SqlDao
public class JpaSchedulerEventInfoDao extends JpaAbstractDao<SchedulerEventInfoEntity, SchedulerEventInfo> implements SchedulerEventInfoDao {

    @Autowired
    SchedulerEventInfoRepository schedulerEventInfoRepository;

    @Autowired
    ScheduledReportInfoRepository scheduledReportInfoRepository;

    @Override
    protected Class<SchedulerEventInfoEntity> getEntityClass() {
        return SchedulerEventInfoEntity.class;
    }

    @Override
    protected JpaRepository<SchedulerEventInfoEntity, UUID> getRepository() {
        return schedulerEventInfoRepository;
    }

    @Override
    public SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(UUID tenantId, UUID schedulerEventId) {
        return DaoUtil.getData(schedulerEventInfoRepository.findSchedulerEventWithCustomerInfoById(
                schedulerEventId
        ));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(UUID tenantId) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findSchedulerEventInfoEntitiesByTenantId(
                        tenantId));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndEnabled(UUID tenantId, boolean enabled) {
        return DaoUtil.convertDataList(schedulerEventInfoRepository
                .findSchedulerEventInfoEntitiesByTenantIdAndEnabled(
                        tenantId, enabled));
    }

    @Override
    public PageData<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndFilter(UUID tenantId, SchedulerEventFilter filter, PageLink pageLink) {
        UUID customerId = filter.getCustomerId() != null && !filter.getCustomerId().isNullUid() ? filter.getCustomerId().getId() : null;
        String type = StringUtils.isNotBlank(filter.getType()) ? filter.getType() : null;
        if (filter.getEdgeId() == null) {
            return DaoUtil.toPageData(schedulerEventInfoRepository.findByTenantIdAndCustomerIdAndTypeAndSearchText(tenantId, customerId, type,
                    Strings.emptyToNull(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink, SchedulerEventWithCustomerInfoEntity.schedulerEventWithCustomerInfoColumnMap)));
        } else {
            return DaoUtil.toPageData(schedulerEventInfoRepository.findByTenantIdAndCustomerIdAndTypeAndEdgeIdAndSearchText(tenantId, customerId, type,
                    filter.getEdgeId().getId(), Strings.emptyToNull(pageLink.getTextSearch()), DaoUtil.toPageable(pageLink, SchedulerEventWithCustomerInfoEntity.schedulerEventWithCustomerInfoColumnMap)));
        }
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findAllSchedulerEventsByTenantIdAndEventTimeFilter(UUID tenantId, SchedulerEventTimeFilter filter, String searchText) {
        List<SchedulerEventWithCustomerInfo> events = findSchedulerEventsByTenantIdAndFilter(tenantId, filter, new PageLink(Integer.MAX_VALUE, 0, searchText)).getData();
        long startTime = filter.getStartTime();
        long endTime = filter.getEndTime();

        List<SchedulerEventWithCustomerInfo> result = new ArrayList<>();
        for (SchedulerEventWithCustomerInfo event : events) {
            SchedulerEventDescriptor descriptor = event.toDescriptor();

            if (descriptor.repeat() instanceof TimerRepeat timerRepeat) {
                long repeatInterval = timerRepeat.getTimeUnit().toMillis(timerRepeat.getRepeatInterval());
                if (repeatInterval < TimeUnit.DAYS.toMillis(1)) { // expecting the requested time window cannot be less than a day
                    result.add(event);
                    continue;
                }
            }

            List<Long> timestamps = new ArrayList<>();
            long lastEventTime = startTime - 1;
            while (true) {
                long eventTime = descriptor.getNextEventTime(lastEventTime);
                if (eventTime == 0L || eventTime > endTime) {
                    break;
                }

                timestamps.add(eventTime);
                lastEventTime = eventTime;
            }

            if (!timestamps.isEmpty()) {
                event.setTimestamps(timestamps);
                result.add(event);
            }
        }
        return result;
    }

    @Override
    public List<SchedulerEventId> findSchedulerEventsIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId) {
        return schedulerEventInfoRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId).stream()
                .map(SchedulerEventId::new)
                .toList();
    }

    @Override
    public List<SchedulerEventId> findSchedulerEventsIdsByTenantId(UUID tenantId) {
        return schedulerEventInfoRepository.findIdsByTenantId(tenantId).stream()
                .map(SchedulerEventId::new)
                .toList();
    }

    @Override
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventsByTenantIdAndIdsAsync(UUID tenantId, List<UUID> schedulerEventIds) {
        return service.submit(() -> DaoUtil.convertDataList(schedulerEventInfoRepository.findSchedulerEventsByTenantIdAndIdIn(tenantId, schedulerEventIds)));
    }

    @Override
    public PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find scheduler event infos by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(schedulerEventInfoRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(UUID tenantId, UUID edgeId, UUID customerId, PageLink pageLink) {
        log.debug("Try to find scheduler event infos by tenantId [{}], edgeId [{}], customerId [{}] and pageLink [{}]", tenantId, edgeId, customerId, pageLink);
        return DaoUtil.toPageData(schedulerEventInfoRepository
                .findByTenantIdAndEdgeIdAndCustomerId(
                        tenantId,
                        edgeId,
                        customerId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<ScheduledReportInfo> findScheduledReportEvents(UUID tenantId, ScheduledReportQuery query) {
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(scheduledReportInfoRepository
                    .findTenantScheduledReportInfosIncludingCustomers(
                            tenantId,
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(scheduledReportInfoRepository
                    .findTenantScheduledReportInfos(
                            tenantId,
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public PageData<ScheduledReportInfo> findScheduledReportEvents(UUID tenantId, UUID customerId, ScheduledReportQuery query) {
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(scheduledReportInfoRepository
                    .findCustomerScheduledReportsIncludingSubCustomers(
                            tenantId,
                            customerId,
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(scheduledReportInfoRepository
                    .findCustomerScheduledReports(
                            tenantId,
                            customerId,
                            query.getReportTemplateId(),
                            query.getUserId(),
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public int countScheduledReportEventsByTemplateId(UUID tenantId, UUID templateId) {
        return scheduledReportInfoRepository.countScheduledReportEventsByTemplateId(tenantId, templateId);
    }

}
