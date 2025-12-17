/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.edge.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.service.edge.EdgeContextComponent;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DownlinkMessageMapper {

    private final EdgeContextComponent ctx;

    public List<DownlinkMsg> convertToDownlinkMsgsPack(EdgeSessionState state, List<EdgeEvent> edgeEvents) {
        List<DownlinkMsg> result = new ArrayList<>();
        for (EdgeEvent edgeEvent : edgeEvents) {
            log.trace("[{}][{}] converting edge event to downlink msg [{}]", state.getTenantId(), state.getEdgeId(), edgeEvent);
            DownlinkMsg downlinkMsg = null;
            try {
                switch (edgeEvent.getAction()) {
                    case UPDATED, ADDED, DELETED, ASSIGNED_TO_EDGE, UNASSIGNED_FROM_EDGE, ALARM_ACK, ALARM_CLEAR,
                         ALARM_DELETE, CREDENTIALS_UPDATED, RELATION_ADD_OR_UPDATE, RELATION_DELETED, RPC_CALL,
                         ASSIGNED_TO_CUSTOMER, UNASSIGNED_FROM_CUSTOMER, ADDED_COMMENT, UPDATED_COMMENT, DELETED_COMMENT -> {
                        downlinkMsg = convertEntityEventToDownlink(state, edgeEvent);
                        if (downlinkMsg != null && downlinkMsg.getWidgetTypeUpdateMsgCount() > 0) {
                            log.trace("[{}][{}] widgetTypeUpdateMsg message processed, downlinkMsgId = {}",
                                    state.getTenantId(), state.getEdgeId(), downlinkMsg.getDownlinkMsgId());
                        } else {
                            log.trace("[{}][{}] entity message processed [{}]", state.getTenantId(), state.getEdgeId(), downlinkMsg);
                        }
                    }
                    case ATTRIBUTES_UPDATED, POST_ATTRIBUTES, ATTRIBUTES_DELETED, TIMESERIES_UPDATED ->
                            downlinkMsg = ctx.getTelemetryProcessor().convertTelemetryEventToDownlink(state.getEdge(), edgeEvent);
                    default -> log.warn("[{}][{}] Unsupported action type [{}]", state.getTenantId(), state.getEdgeId(), edgeEvent.getAction());
                }
            } catch (Exception e) {
                log.trace("[{}][{}] Exception during converting edge event to downlink msg", state.getTenantId(), state.getEdgeId(), e);
            }
            if (downlinkMsg != null) {
                result.add(downlinkMsg);
            }
        }
        return result;
    }

    protected DownlinkMsg convertEntityEventToDownlink(EdgeSessionState state, EdgeEvent edgeEvent) {
        log.trace("[{}] Executing convertEntityEventToDownlink, edgeEvent [{}], action [{}]", edgeEvent.getTenantId(), edgeEvent, edgeEvent.getAction());
        if ((EdgeEventType.OAUTH2_CLIENT.equals(edgeEvent.getType()) || EdgeEventType.DOMAIN.equals(edgeEvent.getType())) &&
                (EdgeVersionUtils.isEdgeVersionOlderThan(state.getEdgeVersion(), EdgeVersion.V_3_8_0))) {
            return null;
        }

        return ctx.getProcessor(edgeEvent.getType()).convertEdgeEventToDownlink(edgeEvent, state.getEdgeVersion());
    }
}
