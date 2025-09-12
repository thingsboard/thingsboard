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
package org.thingsboard.server.common.data.edge;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

@Getter
public enum EdgeEventType {
    DASHBOARD(false, EntityType.DASHBOARD),
    ASSET(false, EntityType.ASSET),
    DEVICE(false, EntityType.DEVICE),
    DEVICE_PROFILE(true, EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(true, EntityType.ASSET_PROFILE),
    ENTITY_VIEW(false, EntityType.ENTITY_VIEW),
    ALARM(false, EntityType.ALARM),
    ALARM_COMMENT(false, null),
    RULE_CHAIN(false, EntityType.RULE_CHAIN),
    RULE_CHAIN_METADATA(false, null),
    EDGE(false, EntityType.EDGE),
    USER(true, EntityType.USER),
    CUSTOMER(true, EntityType.CUSTOMER),
    RELATION(true, null),
    TENANT(true, EntityType.TENANT),
    TENANT_PROFILE(true, EntityType.TENANT_PROFILE),
    WIDGETS_BUNDLE(true, EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(true, EntityType.WIDGET_TYPE),
    ADMIN_SETTINGS(true, null),
    OTA_PACKAGE(true, EntityType.OTA_PACKAGE),
    QUEUE(true, EntityType.QUEUE),
    NOTIFICATION_RULE(true, EntityType.NOTIFICATION_RULE),
    NOTIFICATION_TARGET(true, EntityType.NOTIFICATION_TARGET),
    NOTIFICATION_TEMPLATE(true, EntityType.NOTIFICATION_TEMPLATE),
    TB_RESOURCE(true, EntityType.TB_RESOURCE),
    OAUTH2_CLIENT(true, EntityType.OAUTH2_CLIENT),
    DOMAIN(true, EntityType.DOMAIN),
    CALCULATED_FIELD(false, EntityType.CALCULATED_FIELD),
    AI_MODEL(true, EntityType.AI_MODEL);

    private final boolean allEdgesRelated;

    private final EntityType entityType;


    EdgeEventType(boolean allEdgesRelated, EntityType entityType) {
        this.allEdgesRelated = allEdgesRelated;
        this.entityType = entityType;
    }
}
