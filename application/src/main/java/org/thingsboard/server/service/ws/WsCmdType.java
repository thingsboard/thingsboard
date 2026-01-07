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
package org.thingsboard.server.service.ws;

public enum WsCmdType {
    AUTH,

    ATTRIBUTES,
    TIMESERIES,
    TIMESERIES_HISTORY,
    ENTITY_DATA,
    ENTITY_COUNT,
    ALARM_DATA,
    ALARM_COUNT,
    ALARM_STATUS,

    NOTIFICATIONS,
    NOTIFICATIONS_COUNT,
    MARK_NOTIFICATIONS_AS_READ,
    MARK_ALL_NOTIFICATIONS_AS_READ,

    ALARM_DATA_UNSUBSCRIBE,
    ALARM_COUNT_UNSUBSCRIBE,
    ENTITY_DATA_UNSUBSCRIBE,
    ENTITY_COUNT_UNSUBSCRIBE,
    NOTIFICATIONS_UNSUBSCRIBE,
    ALARM_STATUS_UNSUBSCRIBE
}
