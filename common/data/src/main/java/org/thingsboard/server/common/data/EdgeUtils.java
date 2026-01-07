/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public final class EdgeUtils {

    private static final EnumMap<EntityType, EdgeEventType> entityTypeEdgeEventTypeEnumMap;
    private static final EnumMap<ActionType, EdgeEventActionType> actionTypeEdgeEventActionTypeEnumMap;

    static {
        entityTypeEdgeEventTypeEnumMap = new EnumMap<>(EntityType.class);
        for (EdgeEventType edgeEventType : EdgeEventType.values()) {
            if (edgeEventType.getEntityType() != null) {
                entityTypeEdgeEventTypeEnumMap.put(edgeEventType.getEntityType(), edgeEventType);
            }
        }

        actionTypeEdgeEventActionTypeEnumMap = new EnumMap<>(ActionType.class);
        for (EdgeEventActionType edgeEventActionType : EdgeEventActionType.values()) {
            if (edgeEventActionType.getActionType() != null) {
                actionTypeEdgeEventActionTypeEnumMap.put(edgeEventActionType.getActionType(), edgeEventActionType);
            }
        }
    }

    private static final int STACK_TRACE_LIMIT = 10;

    private EdgeUtils() {}

    public static int nextPositiveInt() {
        return ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
    }

    public static EdgeEventType getEdgeEventTypeByEntityType(EntityType entityType) {
        return entityTypeEdgeEventTypeEnumMap.get(entityType);
    }

    public static EdgeEventActionType getEdgeEventActionTypeByActionType(ActionType actionType) {
        return actionTypeEdgeEventActionTypeEnumMap.get(actionType);
    }

    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        edgeEvent.setBody(body);
        return edgeEvent;
    }

    public static String createErrorMsgFromRootCauseAndStackTrace(Throwable t) {
        Throwable rootCause = Throwables.getRootCause(t);
        StringBuilder errorMsg = new StringBuilder(rootCause.getMessage() != null ? rootCause.getMessage() : "");
        if (rootCause.getStackTrace().length > 0) {
            int idx = 0;
            for (StackTraceElement stackTraceElement : rootCause.getStackTrace()) {
                errorMsg.append("\n").append(stackTraceElement.toString());
                idx++;
                if (idx > STACK_TRACE_LIMIT) {
                    break;
                }
            }
        }
        return errorMsg.toString();
    }
}
