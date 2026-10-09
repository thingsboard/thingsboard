// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.processor.scheduler;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.SchedulerEventUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class SchedulerEventEdgeProcessor extends BaseSchedulerEventProcessor implements SchedulerEventProcessor {

    @Override
    public ListenableFuture<Void> processSchedulerEventMsgFromEdge(TenantId tenantId, Edge edge, SchedulerEventUpdateMsg schedulerEventUpdateMsg) {
        log.trace("[{}] executing processSchedulerEventMsgFromEdge [{}] from edge [{}]", tenantId, schedulerEventUpdateMsg, edge.getId());
        SchedulerEventId schedulerEventId = new SchedulerEventId(new UUID(schedulerEventUpdateMsg.getIdMSB(), schedulerEventUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            return switch (schedulerEventUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE, ENTITY_UPDATED_RPC_MESSAGE -> {
                    saveOrUpdateSchedulerEvent(tenantId, schedulerEventId, schedulerEventUpdateMsg, edge);
                    yield Futures.immediateFuture(null);
                }
                case ENTITY_DELETED_RPC_MESSAGE -> {
                    SchedulerEvent schedulerEventToDelete = edgeCtx.getSchedulerEventService().findSchedulerEventById(tenantId, schedulerEventId);
                    if (schedulerEventToDelete != null) {
                        edgeCtx.getSchedulerEventService().unassignSchedulerEventFromEdge(tenantId, schedulerEventId, edge.getId());
                    }
                    yield Futures.immediateFuture(null);
                }
                default -> handleUnsupportedMsgType(schedulerEventUpdateMsg.getMsgType());
            };
        } catch (DataValidationException e) {
            log.warn("[{}] Failed to process SchedulerEventUpdateMsg from Edge [{}]", tenantId, schedulerEventUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    private void saveOrUpdateSchedulerEvent(TenantId tenantId, SchedulerEventId schedulerEventId, SchedulerEventUpdateMsg schedulerEventUpdateMsg, Edge edge) {
        Boolean created = super.saveOrUpdateSchedulerEvent(tenantId, schedulerEventId, schedulerEventUpdateMsg);
        if (created) {
            createRelationFromEdge(tenantId, edge.getId(), schedulerEventId);
            pushSchedulerEventCreatedEventToRuleEngine(tenantId, edge, schedulerEventId);
            edgeCtx.getSchedulerEventService().assignSchedulerEventToEdge(tenantId, schedulerEventId, edge.getId());
        }
    }

    private void pushSchedulerEventCreatedEventToRuleEngine(TenantId tenantId, Edge edge, SchedulerEventId schedulerEventId) {
        try {
            SchedulerEvent schedulerEvent = edgeCtx.getSchedulerEventService().findSchedulerEventById(tenantId, schedulerEventId);
            String schedulerEventAsString = JacksonUtil.toString(schedulerEvent);
            TbMsgMetaData msgMetaData = getEdgeActionTbMsgMetaData(edge, null);
            pushEntityEventToRuleEngine(tenantId, schedulerEventId, null, TbMsgType.ENTITY_CREATED, schedulerEventAsString, msgMetaData);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push scheduler event action to rule engine: {}", tenantId, schedulerEventId, TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        SchedulerEventId schedulerEventId = new SchedulerEventId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case UPDATED, ASSIGNED_TO_EDGE -> {
                SchedulerEvent schedulerEvent = edgeCtx.getSchedulerEventService().findSchedulerEventById(edgeEvent.getTenantId(), schedulerEventId);
                if (schedulerEvent != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    SchedulerEventUpdateMsg schedulerEventUpdateMsg = EdgeMsgConstructorUtils.constructSchedulerEventUpdatedMsg(msgType, schedulerEvent);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addSchedulerEventUpdateMsg(schedulerEventUpdateMsg)
                            .build();
                }
            }
            case DELETED, UNASSIGNED_FROM_EDGE -> {
                SchedulerEventUpdateMsg schedulerEventUpdateMsg = EdgeMsgConstructorUtils.constructSchedulerEventDeleteMsg(schedulerEventId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addSchedulerEventUpdateMsg(schedulerEventUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.SCHEDULER_EVENT;
    }

    @Override
    protected boolean isEnabledDuringCreation() {
        return false;
    }

    @Override
    protected void setCustomerId(TenantId tenantId, CustomerId customerId, SchedulerEvent schedulerEvent) {
        // do nothing
    }
}
