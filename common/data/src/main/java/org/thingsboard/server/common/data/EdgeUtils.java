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
package org.thingsboard.server.common.data;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.edge.EdgeEventType;

@Slf4j
public final class EdgeUtils {

    private EdgeUtils() {
    }

    public static EdgeEventType getEdgeEventTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case EDGE:
                return EdgeEventType.EDGE;
            case DEVICE:
                return EdgeEventType.DEVICE;
            case DEVICE_PROFILE:
                return EdgeEventType.DEVICE_PROFILE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            case DASHBOARD:
                return EdgeEventType.DASHBOARD;
            case USER:
                return EdgeEventType.USER;
            case RULE_CHAIN:
                return EdgeEventType.RULE_CHAIN;
            case ALARM:
                return EdgeEventType.ALARM;
            case TENANT:
                return EdgeEventType.TENANT;
            case CUSTOMER:
                return EdgeEventType.CUSTOMER;
            case WIDGETS_BUNDLE:
                return EdgeEventType.WIDGETS_BUNDLE;
            case WIDGET_TYPE:
                return EdgeEventType.WIDGET_TYPE;
            default:
                log.warn("Unsupported entity type [{}]", entityType);
                return null;
        }
    }
}
