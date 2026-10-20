// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.edge;

import lombok.Getter;
import org.thingsboard.server.common.data.audit.ActionType;

@Getter
public enum EdgeEventActionType {
    ADDED(ActionType.ADDED),
    UPDATED(ActionType.UPDATED),
    DELETED(ActionType.DELETED),
    POST_ATTRIBUTES(null),
    ATTRIBUTES_UPDATED(ActionType.ATTRIBUTES_UPDATED),
    ATTRIBUTES_DELETED(ActionType.ATTRIBUTES_DELETED),
    TIMESERIES_UPDATED(ActionType.TIMESERIES_UPDATED),
    CREDENTIALS_UPDATED(ActionType.CREDENTIALS_UPDATED),
    RELATION_ADD_OR_UPDATE(ActionType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(ActionType.RELATION_DELETED),
    RPC_CALL(ActionType.RPC_CALL),
    ALARM_ACK(ActionType.ALARM_ACK),
    ALARM_CLEAR(ActionType.ALARM_CLEAR),
    ALARM_DELETE(ActionType.ALARM_DELETE),
    ALARM_ASSIGNED(ActionType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(ActionType.ALARM_UNASSIGNED),
    ADDED_COMMENT(ActionType.ADDED_COMMENT),
    UPDATED_COMMENT(ActionType.UPDATED_COMMENT),
    DELETED_COMMENT(ActionType.DELETED_COMMENT),
    ASSIGNED_TO_EDGE(ActionType.ASSIGNED_TO_EDGE),
    UNASSIGNED_FROM_EDGE(ActionType.UNASSIGNED_FROM_EDGE),
    CREDENTIALS_REQUEST(null), // deprecated
    ADDED_TO_ENTITY_GROUP(ActionType.ADDED_TO_ENTITY_GROUP),
    REMOVED_FROM_ENTITY_GROUP(ActionType.REMOVED_FROM_ENTITY_GROUP),
    CHANGE_OWNER(ActionType.CHANGE_OWNER),
    ENTITY_MERGE_REQUEST(null); // deprecated

    private final ActionType actionType;

    EdgeEventActionType(ActionType actionType) {
        this.actionType = actionType;
    }

}
