/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

@Getter
public enum ActionType {
    ADDED(false), // log entity
    DELETED(false), // log string id
    UPDATED(false), // log entity
    ATTRIBUTES_UPDATED(false), // log attributes/values
    ATTRIBUTES_DELETED(false), // log attributes
    TIMESERIES_DELETED(false), // log timeseries
    RPC_CALL(false), // log method and params
    CREDENTIALS_UPDATED(false), // log new credentials
    ASSIGNED_TO_CUSTOMER(false), // log customer name
    UNASSIGNED_FROM_CUSTOMER(false), // log customer name
    ACTIVATED(false), // log string id
    SUSPENDED(false), // log string id
    CREDENTIALS_READ(true), // log device id
    ATTRIBUTES_READ(true), // log attributes
    RELATION_ADD_OR_UPDATE(false),
    RELATION_DELETED(false),
    RELATIONS_DELETED(false),
    ALARM_ACK(false),
    ALARM_CLEAR(false),
    LOGIN(false),
    LOGOUT(false),
    LOCKOUT(false),
    ASSIGNED_FROM_TENANT(false),
    ASSIGNED_TO_TENANT(false),
    PROVISION_SUCCESS(false),
    PROVISION_FAILURE(false),
    ASSIGNED_TO_EDGE(false), // log edge name
    UNASSIGNED_FROM_EDGE(false);

    private final boolean isRead;

    ActionType(boolean isRead) {
        this.isRead = isRead;
    }
}
