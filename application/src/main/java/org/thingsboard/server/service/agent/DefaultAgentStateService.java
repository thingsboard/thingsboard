// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.List;

import static org.thingsboard.server.service.state.DefaultDeviceStateService.ACTIVITY_STATE;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_CONNECT_TIME;
import static org.thingsboard.server.service.state.DefaultDeviceStateService.LAST_DISCONNECT_TIME;

@Service
@TbCoreComponent
@Slf4j
@RequiredArgsConstructor
public class DefaultAgentStateService implements AgentStateService {

    public static final String AGENT_VERSION = "agentVersion";
    public static final String AGENT_CONTAINER_ID = "agentContainerId";

    private final TelemetrySubscriptionService tsSubService;
    private final TbClusterService clusterService;

    @Override
    public void onAgentConnect(Agent agent, long lastConnectTime, String agentVersion, String agentContainerId) {
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();
        List<AttributeKvEntry> attributes = new ArrayList<>(4);

        attributes.add(new BaseAttributeKvEntry(lastConnectTime, new BooleanDataEntry(ACTIVITY_STATE, true)));
        attributes.add(new BaseAttributeKvEntry(lastConnectTime, new LongDataEntry(LAST_CONNECT_TIME, lastConnectTime)));
        if (StringUtils.isNotEmpty(agentVersion)) {
            attributes.add(new BaseAttributeKvEntry(lastConnectTime, new StringDataEntry(AGENT_VERSION, agentVersion)));
        }
        if (StringUtils.isNotEmpty(agentContainerId)) {
            attributes.add(new BaseAttributeKvEntry(lastConnectTime, new StringDataEntry(AGENT_CONTAINER_ID, agentContainerId)));
        }
        save(tenantId, agentId, attributes);
        pushRuleEngineMessage(tenantId, agentId, lastConnectTime, TbMsgType.CONNECT_EVENT);
        // TODO: fire AGENT_CONNECTION notification rule trigger once the BE trigger stack and FE rule dialog wiring are in place.
    }

    @Override
    public void onAgentDisconnect(Agent agent, long lastDisconnectTime) {
        TenantId tenantId = agent.getTenantId();
        AgentId agentId = agent.getId();
        save(tenantId, agentId, List.of(
                new BaseAttributeKvEntry(lastDisconnectTime, new BooleanDataEntry(ACTIVITY_STATE, false)),
                new BaseAttributeKvEntry(lastDisconnectTime, new LongDataEntry(LAST_DISCONNECT_TIME, lastDisconnectTime)))
        );
        pushRuleEngineMessage(tenantId, agentId, lastDisconnectTime, TbMsgType.DISCONNECT_EVENT);
        // TODO: fire AGENT_CONNECTION notification rule trigger once the BE trigger stack and FE rule dialog wiring are in place.
    }

    private void pushRuleEngineMessage(TenantId tenantId, AgentId agentId, long ts, TbMsgType msgType) {
        try {
            ObjectNode agentState = JacksonUtil.newObjectNode();
            boolean isConnected = TbMsgType.CONNECT_EVENT.equals(msgType);
            if (isConnected) {
                agentState.put(ACTIVITY_STATE, true);
                agentState.put(LAST_CONNECT_TIME, ts);
            } else {
                agentState.put(ACTIVITY_STATE, false);
                agentState.put(LAST_DISCONNECT_TIME, ts);
            }
            String data = JacksonUtil.toString(agentState);
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(msgType)
                    .originator(agentId)
                    .copyMetaData(TbMsgMetaData.EMPTY)
                    .dataType(TbMsgDataType.JSON)
                    .data(data)
                    .build();
            clusterService.pushMsgToRuleEngine(tenantId, agentId, tbMsg, null);
        } catch (Exception e) {
            log.warn("[{}][{}] Failed to push {}", tenantId, agentId, msgType, e);
        }
    }

    private void save(TenantId tenantId, AgentId agentId, List<AttributeKvEntry> attributes) {
        tsSubService.saveAttributes(AttributesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(agentId)
                .scope(AttributeScope.SERVER_SCOPE)
                .entries(attributes)
                .callback(saveCallback(tenantId, agentId, attributes))
                .build());
    }

    private FutureCallback<Void> saveCallback(TenantId tenantId, AgentId agentId, List<AttributeKvEntry> attributes) {
        return new FutureCallback<>() {
            @Override
            public void onSuccess(Void result) {
                log.trace("[{}][{}] Saved attributes {}", tenantId, agentId, attributes);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("[{}][{}] Failed to save attributes {}", tenantId, agentId, attributes, t);
            }
        };
    }

}
