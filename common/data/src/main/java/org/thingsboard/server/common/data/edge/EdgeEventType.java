// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
    USER(false, EntityType.USER),
    CUSTOMER(true, EntityType.CUSTOMER),
    RELATION(true, null),
    TENANT(true, EntityType.TENANT),
    TENANT_PROFILE(true, true, EntityType.TENANT_PROFILE),
    WIDGETS_BUNDLE(true, true, EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(true, true, EntityType.WIDGET_TYPE),
    ADMIN_SETTINGS(true, true, EntityType.ADMIN_SETTINGS),
    OTA_PACKAGE(true, EntityType.OTA_PACKAGE),
    QUEUE(true, true, EntityType.QUEUE),
    ENTITY_GROUP(false, EntityType.ENTITY_GROUP),
    SCHEDULER_EVENT(false, EntityType.SCHEDULER_EVENT),
    WHITE_LABELING(true, true, null),
    LOGIN_WHITE_LABELING(true, true, null),
    MAIL_TEMPLATES(true, true, null),
    CUSTOM_TRANSLATION(true, true, null),
    CUSTOM_MENU(true, true, null),
    ROLE(true, EntityType.ROLE),
    GROUP_PERMISSION(true, EntityType.GROUP_PERMISSION),
    CONVERTER(false, EntityType.CONVERTER),
    INTEGRATION(false, EntityType.INTEGRATION),
    NOTIFICATION_RULE(true, true, EntityType.NOTIFICATION_RULE),
    NOTIFICATION_TARGET(true, true, EntityType.NOTIFICATION_TARGET),
    NOTIFICATION_TEMPLATE(true, true, EntityType.NOTIFICATION_TEMPLATE),
    TB_RESOURCE(true, true, EntityType.TB_RESOURCE),
    DEVICE_GROUP_OTA(false, null),
    OAUTH2_CLIENT(true, true, EntityType.OAUTH2_CLIENT),
    DOMAIN(true, true, EntityType.DOMAIN),
    CALCULATED_FIELD(false, EntityType.CALCULATED_FIELD),
    ENCRYPTION_KEY(true, null),
    SECRET(true, EntityType.SECRET),
    REPORT_TEMPLATE(true, EntityType.REPORT_TEMPLATE),
    AI_MODEL(true, EntityType.AI_MODEL),
    API_KEY(true, EntityType.API_KEY);

    // entity is pushed to all edges of its tenant, otherwise only to edges it's assigned to
    private final boolean allTenantEdgesRelated;

    // entity can be owned by system tenant, so system level events are pushed to all edges
    private final boolean systemLevelSupported;

    private final EntityType entityType;


    EdgeEventType(boolean allTenantEdgesRelated, EntityType entityType) {
        this(allTenantEdgesRelated, false, entityType);
    }

    EdgeEventType(boolean allTenantEdgesRelated, boolean systemLevelSupported, EntityType entityType) {
        this.allTenantEdgesRelated = allTenantEdgesRelated;
        this.systemLevelSupported = systemLevelSupported;
        this.entityType = entityType;
    }
}
