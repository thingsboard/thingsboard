// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.scheduler;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.report.ScheduledReportQuery;
import org.thingsboard.server.common.data.scheduler.ScheduledReportInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.data.scheduler.SchedulerEventFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventInfo;
import org.thingsboard.server.common.data.scheduler.SchedulerEventTimeFilter;
import org.thingsboard.server.common.data.scheduler.SchedulerEventWithCustomerInfo;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Slf4j
@Service("SchedulerEventDaoService")
public class BaseSchedulerEventService extends AbstractEntityService implements SchedulerEventService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_SCHEDULER_EVENT_ID = "Incorrect schedulerEventId ";

    @Autowired
    private SchedulerEventDao schedulerEventDao;

    @Autowired
    private SchedulerEventInfoDao schedulerEventInfoDao;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private DataValidator<SchedulerEvent> schedulerEventValidator;

    @Autowired
    private EntityCountService entityCountService;

    @Override
    public SchedulerEvent findSchedulerEventById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventById [{}]", schedulerEventId);
        validateId(schedulerEventId, id -> INCORRECT_SCHEDULER_EVENT_ID + id);
        return schedulerEventDao.findById(tenantId, schedulerEventId.getId());
    }

    @Override
    public SchedulerEventInfo findSchedulerEventInfoById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventInfoById [{}]", schedulerEventId);
        validateId(schedulerEventId, id -> INCORRECT_SCHEDULER_EVENT_ID + id);
        return schedulerEventInfoDao.findById(tenantId, schedulerEventId.getId());
    }

    @Override
    public SchedulerEventWithCustomerInfo findSchedulerEventWithCustomerInfoById(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventWithCustomerInfoById [{}]", schedulerEventId);
        validateId(schedulerEventId, id -> INCORRECT_SCHEDULER_EVENT_ID + id);
        return schedulerEventInfoDao.findSchedulerEventWithCustomerInfoById(tenantId.getId(), schedulerEventId.getId());
    }

    @Override
    public ListenableFuture<SchedulerEventInfo> findSchedulerEventInfoByIdAsync(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing findSchedulerEventInfoByIdAsync [{}]", schedulerEventId);
        validateId(schedulerEventId, id -> INCORRECT_SCHEDULER_EVENT_ID + id);
        return schedulerEventInfoDao.findByIdAsync(tenantId, schedulerEventId.getId());
    }

    @Override
    public ListenableFuture<List<SchedulerEventInfo>> findSchedulerEventInfoByIdsAsync(TenantId tenantId, List<SchedulerEventId> schedulerEventIds) {
        log.trace("Executing findSchedulerEventInfoByIdsAsync, tenantId [{}], schedulerEventIds [{}]", tenantId, schedulerEventIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(schedulerEventIds, ids -> "Incorrect schedulerEventIds " + ids);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(schedulerEventIds));
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantId(TenantId tenantId) {
        log.trace("Executing findSchedulerEventsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return schedulerEventInfoDao.findSchedulerEventsByTenantId(tenantId.getId());
    }

    @Override
    public List<SchedulerEventInfo> findSchedulerEventsByTenantIdAndEnabled(TenantId tenantId, boolean enabled) {
        log.trace("Executing findSchedulerEventsByTenantIdAndEnabled, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndEnabled(tenantId.getId(), enabled);
    }

    @Override
    public PageData<SchedulerEventWithCustomerInfo> findSchedulerEventsByTenantIdAndFilter(TenantId tenantId, SchedulerEventFilter filter, PageLink pageLink) {
        log.trace("Executing findSchedulerEventsByTenantIdAndFilter, tenantId [{}], filter [{}], pageLink [{}]", tenantId, filter, pageLink);
        return schedulerEventInfoDao.findSchedulerEventsByTenantIdAndFilter(tenantId.getId(), filter, pageLink);
    }

    @Override
    public List<SchedulerEventWithCustomerInfo> findAllSchedulerEventsByTenantIdAndEventTimeFilter(TenantId tenantId, SchedulerEventTimeFilter filter, String searchText) {
        log.trace("Executing findAllSchedulerEventsByTenantIdAndEventTimeFilter, tenantId [{}], filter [{}], searchText [{}]", tenantId, filter, searchText);
        return schedulerEventInfoDao.findAllSchedulerEventsByTenantIdAndEventTimeFilter(tenantId.getId(), filter, searchText);
    }

    @Override
    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent) {
        return saveSchedulerEvent(schedulerEvent, true);
    }

    @Override
    public SchedulerEvent saveSchedulerEvent(SchedulerEvent schedulerEvent, boolean doValidate) {
        return saveEntity(schedulerEvent, () -> doSaveSchedulerEvent(schedulerEvent, doValidate));
    }

    private SchedulerEvent doSaveSchedulerEvent(SchedulerEvent schedulerEvent, boolean doValidate) {
        log.trace("Executing saveSchedulerEvent [{}]", schedulerEvent);
        SchedulerEvent oldSchedulerEvent = null;
        if (doValidate) {
            oldSchedulerEvent = schedulerEventValidator.validate(schedulerEvent, SchedulerEventInfo::getTenantId);
        } else if (schedulerEvent.getId() != null) {
            oldSchedulerEvent = findSchedulerEventById(schedulerEvent.getTenantId(), schedulerEvent.getId());
        }
        try {
            SchedulerEvent savedSchedulerEvent = schedulerEventDao.save(schedulerEvent.getTenantId(), schedulerEvent);
            if (schedulerEvent.getId() == null) {
                entityCountService.publishCountEntityEvictEvent(schedulerEvent.getTenantId(), EntityType.SCHEDULER_EVENT);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(schedulerEvent.getTenantId())
                    .entityId(savedSchedulerEvent.getId()).entity(savedSchedulerEvent).created(oldSchedulerEvent == null).build());
            return savedSchedulerEvent;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "scheduler_event_external_id_unq_key", "SchedulerEvent with such external id already exists!");
            throw e;
        }
    }

    @Override
    public void deleteSchedulerEvent(TenantId tenantId, SchedulerEventId schedulerEventId) {
        log.trace("Executing deleteSchedulerEvent [{}]", schedulerEventId);
        validateId(schedulerEventId, id -> INCORRECT_SCHEDULER_EVENT_ID + id);
        SchedulerEvent schedulerEvent = findSchedulerEventById(tenantId, schedulerEventId);
        if (schedulerEvent == null) {
            return;
        }
        schedulerEventDao.removeById(tenantId, schedulerEventId.getId());
        entityCountService.publishCountEntityEvictEvent(tenantId, EntityType.SCHEDULER_EVENT);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(schedulerEventId).entity(schedulerEvent).build());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteSchedulerEvent(tenantId, (SchedulerEventId) id);
    }

    @Override
    public void deleteSchedulerEventsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteSchedulerEventsByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        List<SchedulerEventId> schedulerEvents = schedulerEventInfoDao.findSchedulerEventsIdsByTenantId(tenantId.getId());
        for (SchedulerEventId id : schedulerEvents) {
            deleteSchedulerEvent(tenantId, id);
        }
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteSchedulerEventsByTenantId(tenantId);
    }

    @Override
    public void deleteSchedulerEventsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteSchedulerEventsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        List<SchedulerEventId> schedulerEvents = schedulerEventInfoDao.findSchedulerEventsIdsByTenantIdAndCustomerId(tenantId.getId(), customerId.getId());
        for (SchedulerEventId id : schedulerEvents) {
            deleteSchedulerEvent(tenantId, id);
        }
    }

    @Override
    public SchedulerEventInfo assignSchedulerEventToEdge(TenantId tenantId, SchedulerEventId schedulerEventId, EdgeId edgeId) {
        SchedulerEventInfo schedulerEventInfo = findSchedulerEventInfoById(tenantId, schedulerEventId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign scheduler event to non-existent edge!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, schedulerEventId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create scheduler event relation. Edge Id: [{}]", schedulerEventId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(schedulerEventId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
        return schedulerEventInfo;
    }

    @Override
    public SchedulerEventInfo unassignSchedulerEventFromEdge(TenantId tenantId, SchedulerEventId schedulerEventId, EdgeId edgeId) {
        SchedulerEventInfo schedulerEventInfo = findSchedulerEventInfoById(tenantId, schedulerEventId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign scheduler event from non-existent edge group!");
        }
        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, schedulerEventId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete scheduler event relation. Edge group id: [{}]", schedulerEventId, edgeId);
            throw new RuntimeException(e);
        }
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(schedulerEventId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return schedulerEventInfo;
    }

    @Override
    public PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findSchedulerEventInfosByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        return schedulerEventInfoDao.findSchedulerEventInfosByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<SchedulerEventInfo> findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(TenantId tenantId, EdgeId edgeId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findSchedulerEventInfosByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        Validator.validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        return schedulerEventInfoDao.findSchedulerEventInfosByTenantIdAndEdgeIdAndCustomerId(tenantId.getId(), edgeId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<ScheduledReportInfo> findScheduledReportEvents(TenantId tenantId, ScheduledReportQuery query) {
        log.trace("Executing findScheduledReportEvents, tenantId [{}], query [{}]", tenantId, query);
        return schedulerEventInfoDao.findScheduledReportEvents(tenantId.getId(), query);
    }

    @Override
    public PageData<ScheduledReportInfo> findScheduledReportEvents(TenantId tenantId, CustomerId customerId, ScheduledReportQuery query) {
        log.trace("Executing findScheduledReportEvents, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        return schedulerEventInfoDao.findScheduledReportEvents(tenantId.getId(), customerId.getId(), query);
    }

    @Override
    public int countScheduledReportEventsByTemplateId(TenantId tenantId, ReportTemplateId reportTemplateId) {
        log.trace("Executing countScheduledReportEventsByTemplateId, tenantId [{}], reportTemplateId [{}]", tenantId, reportTemplateId);
        return schedulerEventInfoDao.countScheduledReportEventsByTemplateId(tenantId.getId(), reportTemplateId.getId());
    }

    @Override
    public PageData<SchedulerEvent> findSchedulerEventsByTenantIdAndEdgeId(TenantId tenantId,
                                                                           EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findSchedulerEventsByTenantIdAndEdgeId, tenantId [{}], edgeId [{}]", tenantId, edgeId);
        Validator.validateId(tenantId, id -> "Incorrect tenantId " + id);
        Validator.validateId(edgeId, id -> "Incorrect edgeId " + id);
        return schedulerEventDao.findSchedulerEventsByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    public static EntityId getOriginatorId(SchedulerEvent event) {
        return event.getOriginatorId() != null ? event.getOriginatorId() : event.getId();
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findSchedulerEventById(tenantId, new SchedulerEventId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(schedulerEventDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return schedulerEventDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
