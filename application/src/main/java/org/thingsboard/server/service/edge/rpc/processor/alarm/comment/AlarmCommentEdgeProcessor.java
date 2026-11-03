// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.alarm.comment;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.AlarmCommentUpdateMsg;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.alarm.BaseAlarmProcessor;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class AlarmCommentEdgeProcessor extends BaseAlarmProcessor implements AlarmCommentProcessor {

    @Override
    public ListenableFuture<Void> processAlarmCommentMsgFromEdge(TenantId tenantId, EdgeId edgeId, AlarmCommentUpdateMsg alarmCommentUpdateMsg) {
        log.trace("[{}] processAlarmCommentMsgFromEdge [{}]", tenantId, alarmCommentUpdateMsg);
        try {
            edgeSynchronizationManager.getEdgeId().set(edgeId);
            return processAlarmCommentMsg(tenantId, alarmCommentUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        switch (edgeEvent.getAction()) {
            case ADDED_COMMENT:
            case UPDATED_COMMENT:
            case DELETED_COMMENT:
                AlarmComment alarmComment = JacksonUtil.convertValue(edgeEvent.getBody(), AlarmComment.class);
                if (alarmComment != null) {
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addAlarmCommentUpdateMsg(EdgeMsgConstructorUtils.constructAlarmCommentUpdatedMsg(msgType, alarmComment))
                            .build();
                }
            default:
                return null;
        }
    }

    @Override
    public ListenableFuture<Void> processEntityNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        AlarmId alarmId = new AlarmId(new UUID(edgeNotificationMsg.getEntityIdMSB(), edgeNotificationMsg.getEntityIdLSB()));
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());
        AlarmComment alarmComment = JacksonUtil.fromString(edgeNotificationMsg.getBody(), AlarmComment.class);
        if (alarmComment == null) {
            return Futures.immediateFuture(null);
        }
        Alarm alarmById = edgeCtx.getAlarmService().findAlarmById(tenantId, new AlarmId(alarmComment.getAlarmId().getId()));
        List<ListenableFuture<Void>> delFutures = pushEventToAllRelatedEdges(tenantId, alarmById.getOriginator(),
                alarmId, actionType, JacksonUtil.valueToTree(alarmComment), originatorEdgeId, EdgeEventType.ALARM_COMMENT);
        return Futures.transform(Futures.allAsList(delFutures), voids -> null, dbCallbackExecutorService);
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.ALARM_COMMENT;
    }

}
