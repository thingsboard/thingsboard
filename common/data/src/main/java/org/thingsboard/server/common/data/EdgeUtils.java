// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public final class EdgeUtils {

    public static final String DEFAULT_EDGE_LICENSE_KEY = "6qcGys6gz4M2ZuIqZ6hRDjWT";

    private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("(\\$\\{\\{)(.*?)(}})");
    private static final String ATTRIBUTE_PLACEHOLDER_PATTERN = "${{%s}}";
    private static final String ATTRIBUTE_REGEXP_PLACEHOLDER_PATTERN = "\\$\\{\\{%s}}";
    private static final int STACK_TRACE_LIMIT = 10;
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

    private EdgeUtils() {}

    public static boolean isAddonEdge(int licenseVersion) {
        return licenseVersion == 2;
    }

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
        return constructEdgeEvent(tenantId, edgeId, type, action, entityId, body, null);
    }

    public static EdgeEvent constructEdgeEvent(TenantId tenantId,
                                               EdgeId edgeId,
                                               EdgeEventType type,
                                               EdgeEventActionType action,
                                               EntityId entityId,
                                               JsonNode body,
                                               EntityId entityGroupId) {
        EdgeEvent edgeEvent = new EdgeEvent();
        edgeEvent.setTenantId(tenantId);
        edgeEvent.setEdgeId(edgeId);
        edgeEvent.setType(type);
        edgeEvent.setAction(action);
        if (entityId != null) {
            edgeEvent.setEntityId(entityId.getId());
        }
        if (entityGroupId != null) {
            edgeEvent.setEntityGroupId(entityGroupId.getId());
        }
        edgeEvent.setBody(body);
        return edgeEvent;
    }

    public static Set<String> getAttributeKeysFromConfiguration(String integrationConfiguration) {
        Set<String> result = new HashSet<>();
        Matcher m = ATTRIBUTE_PATTERN.matcher(integrationConfiguration);
        while (m.find()) {
            result.add(m.group(2));
        }
        return result;
    }

    public static String formatAttributeKeyToPlaceholderFormat(String attributeKey) {
        return String.format(ATTRIBUTE_PLACEHOLDER_PATTERN, attributeKey);
    }

    public static String formatAttributeKeyToRegexpPlaceholderFormat(String attributeKey) {
        return String.format(ATTRIBUTE_REGEXP_PLACEHOLDER_PATTERN, attributeKey);
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

    public static boolean isEdgeGroupAll(String groupName) {
        if (groupName == null) {
            return false;
        }
        return groupName.startsWith(EntityGroup.GROUP_EDGE_ALL_STARTS_WITH) && groupName.endsWith(EntityGroup.GROUP_EDGE_ALL_ENDS_WITH);
    }

    public static String getEdgeGroupAllName(String edgeName) {
        return getEdgeGroupAllName(null, edgeName);
    }

    public static String getEdgeGroupAllName(String customerName, String edgeName) {
        if (customerName != null) {
            return String.format(EntityGroup.GROUP_EDGE_CUSTOMER_ALL_NAME_PATTERN, customerName, edgeName);
        }
        return String.format(EntityGroup.GROUP_EDGE_ALL_NAME_PATTERN, edgeName);
    }

}
