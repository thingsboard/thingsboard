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
package org.thingsboard.server.common.data.msg;

import lombok.Getter;
import org.thingsboard.server.common.data.StringUtils;

import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

public enum TbMsgType {

    POST_ATTRIBUTES_REQUEST("Post attributes"),
    POST_TELEMETRY_REQUEST("Post telemetry"),
    TO_SERVER_RPC_REQUEST("RPC Request from Device"),
    ACTIVITY_EVENT("Activity Event"),
    INACTIVITY_EVENT("Inactivity Event"),
    CONNECT_EVENT("Connect Event"),
    DISCONNECT_EVENT("Disconnect Event"),
    ENTITY_CREATED("Entity Created"),
    ENTITY_UPDATED("Entity Updated"),
    ENTITY_DELETED("Entity Deleted"),
    ENTITY_ASSIGNED("Entity Assigned"),
    ENTITY_UNASSIGNED("Entity Unassigned"),
    ATTRIBUTES_UPDATED("Attributes Updated"),
    ATTRIBUTES_DELETED("Attributes Deleted"),
    ALARM,
    ALARM_ACK("Alarm Acknowledged"),
    ALARM_CLEAR("Alarm Cleared"),
    ALARM_DELETE,
    ALARM_ASSIGNED("Alarm Assigned"),
    ALARM_UNASSIGNED("Alarm Unassigned"),
    COMMENT_CREATED("Comment Created"),
    COMMENT_UPDATED("Comment Updated"),
    RPC_CALL_FROM_SERVER_TO_DEVICE("RPC Request to Device"),
    ENTITY_ASSIGNED_FROM_TENANT("Entity Assigned From Tenant"),
    ENTITY_ASSIGNED_TO_TENANT("Entity Assigned To Tenant"),
    ENTITY_ASSIGNED_TO_EDGE,
    ENTITY_UNASSIGNED_FROM_EDGE,
    TIMESERIES_UPDATED("Timeseries Updated"),
    TIMESERIES_DELETED("Timeseries Deleted"),
    RPC_QUEUED("RPC Queued"),
    RPC_SENT("RPC Sent"),
    RPC_DELIVERED("RPC Delivered"),
    RPC_SUCCESSFUL("RPC Successful"),
    RPC_TIMEOUT("RPC Timeout"),
    RPC_EXPIRED("RPC Expired"),
    RPC_FAILED("RPC Failed"),
    RPC_DELETED("RPC Deleted"),
    RELATION_ADD_OR_UPDATE("Relation Added or Updated"),
    RELATION_DELETED("Relation Deleted"),
    RELATIONS_DELETED("All Relations Deleted"),
    PROVISION_SUCCESS,
    PROVISION_FAILURE,
    SEND_EMAIL,
    REST_API_REQUEST("REST API request"),

    // tellSelfOnly types
    GENERATOR_NODE_SELF_MSG(null, true),
    DEVICE_PROFILE_PERIODIC_SELF_MSG(null, true),
    DEVICE_PROFILE_UPDATE_SELF_MSG(null, true),
    DEVICE_UPDATE_SELF_MSG(null, true),
    DEDUPLICATION_TIMEOUT_SELF_MSG(null, true),
    DELAY_TIMEOUT_SELF_MSG(null, true),
    MSG_COUNT_SELF_MSG(null, true),

    // Custom or N/A type:
    NA;

    public static final List<String> NODE_CONNECTIONS = EnumSet.allOf(TbMsgType.class).stream()
            .filter(tbMsgType -> !tbMsgType.isTellSelfOnly())
            .map(TbMsgType::getRuleNodeConnection)
            .filter(connection -> !TbNodeConnectionType.OTHER.equals(connection))
            .collect(Collectors.toUnmodifiableList());

    @Getter
    private final String ruleNodeConnection;

    @Getter
    private final boolean tellSelfOnly;

    TbMsgType(String ruleNodeConnection, boolean tellSelfOnly) {
        this.ruleNodeConnection = StringUtils.isNotEmpty(ruleNodeConnection) ? ruleNodeConnection : TbNodeConnectionType.OTHER;
        this.tellSelfOnly = tellSelfOnly;
    }

    TbMsgType(String ruleNodeConnection) {
        this(ruleNodeConnection, false);
    }

    TbMsgType() {
        this(null, false);
    }

}
