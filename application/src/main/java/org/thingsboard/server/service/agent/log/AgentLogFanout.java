// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.log;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.agent.util.AgentProtoUtils;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.agent.AgentAppUnitService;
import org.thingsboard.server.gen.agent.v1.AgentLogChunk;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.agent.AgentGrpcService;
import org.thingsboard.server.service.agent.session.AgentSessionState;
import org.thingsboard.server.service.subscription.SubscriptionManagerService;
import org.thingsboard.server.service.subscription.TbSubscriptionUtils;

@Component
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class AgentLogFanout {

    @Lazy
    private final AgentGrpcService agentGrpcService;
    private final AgentAppUnitService unitService;
    private final LogChunkBuffer logChunkBuffer;
    private final SubscriptionManagerService subscriptionManagerService;
    private final PartitionService partitionService;
    private final TbClusterService clusterService;

    public void fanout(AgentSessionState session, AgentLogChunk chunk) {
        if (chunk.getLinesCount() == 0 && chunk.getDropped() <= 0) {
            return;
        }
        TenantId tenantId = session.getTenantId();
        AgentAppUnit unit = unitService.findByAgentAndProjectAndIdentifier(
                tenantId, session.getAgentId(), chunk.getProjectName(), chunk.getUnitId(), AgentAppUnitType.CONTAINER);
        if (unit == null) {
            log.debug("[{}] No AgentAppUnit for agent {} project {} unit {}",
                    tenantId, session.getAgentId(), chunk.getProjectName(), chunk.getUnitId());
            agentGrpcService.stopLogStream(session.getAgentId(), chunk.getProjectName(), chunk.getUnitId());
            return;
        }
        long seq = logChunkBuffer.append(tenantId, unit.getId(), AgentProtoUtils.fromProto(chunk));
        forwardWatermark(tenantId, unit.getId(), seq);
    }

    private void forwardWatermark(TenantId tenantId, EntityId unitId, long latestSeq) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, tenantId, unitId);
        if (tpi.isMyPartition()) {
            subscriptionManagerService.onLogStreamUpdate(tenantId, unitId, latestSeq, TbCallback.EMPTY);
        } else {
            clusterService.pushMsgToCore(tpi, unitId.getId(),
                    TbSubscriptionUtils.toLogStreamUpdateProto(tenantId, unitId, latestSeq), null);
        }
    }
}
