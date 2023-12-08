/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

    ADDED(false, TbMsgType.ENTITY_CREATED), // log entity
    DELETED(false, TbMsgType.ENTITY_DELETED), // log string id
    UPDATED(false, TbMsgType.ENTITY_UPDATED), // log entity
    ATTRIBUTES_UPDATED(false, TbMsgType.ATTRIBUTES_UPDATED), // log attributes/values
    ATTRIBUTES_DELETED(false, TbMsgType.ATTRIBUTES_DELETED), // log attributes
    TIMESERIES_UPDATED(false, TbMsgType.TIMESERIES_UPDATED), // log timeseries update
    TIMESERIES_DELETED(false, TbMsgType.TIMESERIES_DELETED), // log timeseries
    RPC_CALL(false, null), // log method and params
    CREDENTIALS_UPDATED(false, null), // log new credentials
    ASSIGNED_TO_CUSTOMER(false, TbMsgType.ENTITY_ASSIGNED), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false, TbMsgType.ENTITY_UNASSIGNED), // log customer name
    ACTIVATED(false, null), // log string id
    SUSPENDED(false, null), // log string id
    CREDENTIALS_READ(true, null), // log device id
    ATTRIBUTES_READ(true, null), // log attributes
    RELATION_ADD_OR_UPDATE(false, TbMsgType.RELATION_ADD_OR_UPDATE),
    RELATION_DELETED(false, TbMsgType.RELATION_DELETED),
    RELATIONS_DELETED(false, TbMsgType.RELATIONS_DELETED),
    ALARM_ACK(false, TbMsgType.ALARM_ACK),
    ALARM_CLEAR(false, TbMsgType.ALARM_CLEAR),
    ALARM_DELETE(false, TbMsgType.ALARM_DELETE),
    ALARM_ASSIGNED(false, TbMsgType.ALARM_ASSIGNED),
    ALARM_UNASSIGNED(false, TbMsgType.ALARM_UNASSIGNED),
    LOGIN(false, null),
    LOGOUT(false, null),
    LOCKOUT(false, null),
    ASSIGNED_FROM_TENANT(false, TbMsgType.ENTITY_ASSIGNED_FROM_TENANT),
    ASSIGNED_TO_TENANT(false, TbMsgType.ENTITY_ASSIGNED_TO_TENANT),
    PROVISION_SUCCESS(false, TbMsgType.PROVISION_SUCCESS),
    PROVISION_FAILURE(false, TbMsgType.PROVISION_FAILURE),
    ASSIGNED_TO_EDGE(false, TbMsgType.ENTITY_ASSIGNED_TO_EDGE), // log edge name
    UNASSIGNED_FROM_EDGE(false, TbMsgType.ENTITY_UNASSIGNED_FROM_EDGE),
    ADDED_COMMENT(false, TbMsgType.COMMENT_CREATED),
    UPDATED_COMMENT(false, TbMsgType.COMMENT_UPDATED),
    DELETED_COMMENT(false, null),
    SMS_SENT(false, null);

    @Getter
    private final boolean isRead;

    private final TbMsgType ruleEngineMsgType;

    ActionType(boolean isRead, TbMsgType ruleEngineMsgType) {
        this.isRead = isRead;
        this.ruleEngineMsgType = ruleEngineMsgType;
    }

    public Optional<TbMsgType> getRuleEngineMsgType() {
        return Optional.ofNullable(ruleEngineMsgType);
    }

}
