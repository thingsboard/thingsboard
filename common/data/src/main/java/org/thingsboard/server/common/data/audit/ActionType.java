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
package org.thingsboard.server.common.data.audit;

import lombok.Getter;
import org.thingsboard.server.common.data.msg.TbMsgType;

import java.util.Optional;

public enum ActionType {

    ADDED(TbMsgType.ENTITY_CREATED), // log entity
    DELETED(TbMsgType.ENTITY_DELETED), // log string id
    UPDATED(TbMsgType.ENTITY_UPDATED), // log entity
    ATTRIBUTES_UPDATED(TbMsgType.ATTRIBUTES_UPDATED), // log attributes/values
    ATTRIBUTES_DELETED(TbMsgType.ATTRIBUTES_DELETED), // log attributes
    TIMESERIES_UPDATED(TbMsgType.TIMESERIES_UPDATED), // log timeseries update
    TIMESERIES_DELETED(TbMsgType.TIMESERIES_DELETED), // log timeseries
    RPC_CALL, // log method and params
    CREDENTIALS_UPDATED, // log new credentials
    ASSIGNED_TO_CUSTOMER(TbMsgType.ENTITY_ASSIGNED), // log customer name
    UNASSIGNED_FROM_CUSTOMER(TbMsgType.ENTITY_UNASSIGNED), // log customer name
    ACTIVATED, // log string id
    SUSPENDED, // log string id
    CREDENTIALS_READ(true), // log device id
    ATTRIBUTES_READ(true), // log attributes
    RELATION_ADD_OR_UPDATE(TbMsgType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(TbMsgType.RELATION_DELETED),
    RELATIONS_DELETED(TbMsgType.RELATIONS_DELETED),
    REST_API_RULE_ENGINE_CALL, // log call to rule engine from REST API
    ALARM_ACK(TbMsgType.ALARM_ACK, true),
    ALARM_CLEAR(TbMsgType.ALARM_CLEAR, true),
    ALARM_DELETE(TbMsgType.ALARM_DELETE, true),
    ALARM_ASSIGNED(TbMsgType.ALARM_ASSIGNED, true),
    ALARM_UNASSIGNED(TbMsgType.ALARM_UNASSIGNED, true),
    LOGIN,
    LOGOUT,
    LOCKOUT,
    ASSIGNED_FROM_TENANT(TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    ASSIGNED_TO_TENANT(TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    PROVISION_SUCCESS(TbMsgType.PROVISION_SUCCESS),
    PROVISION_FAILURE(TbMsgType.PROVISION_FAILURE),
    ASSIGNED_TO_EDGE(TbMsgType.ENTITY_ASSIGNED_TO_EDGE), // log edge name
    UNASSIGNED_FROM_EDGE(TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    ADDED_COMMENT(TbMsgType.COMMENT_CREATED),
    UPDATED_COMMENT(TbMsgType.COMMENT_UPDATED),
    DELETED_COMMENT,
    SMS_SENT;

    @Getter
    private final boolean read;

    private final TbMsgType ruleEngineMsgType;

    @Getter
    private final boolean alarmAction;

    ActionType() {
        this(false, null, false);
    }

    ActionType(boolean read) {
        this(read, null, false);
    }

    ActionType(TbMsgType ruleEngineMsgType) {
        this(false, ruleEngineMsgType, false);
    }

    ActionType(TbMsgType ruleEngineMsgType, boolean isAlarmAction) {
        this(false, ruleEngineMsgType, isAlarmAction);
    }

    ActionType(boolean read, TbMsgType ruleEngineMsgType, boolean alarmAction) {
        this.read = read;
        this.ruleEngineMsgType = ruleEngineMsgType;
        this.alarmAction = alarmAction;
    }

    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}
