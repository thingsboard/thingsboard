// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edge;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

@Getter
public enum EdgeEventType {
    DASHBOARD(EdgeEventScope.ASSIGNED_EDGES, EntityType.DASHBOARD),
    ASSET(EdgeEventScope.ASSIGNED_EDGES, EntityType.ASSET),
    DEVICE(EdgeEventScope.ASSIGNED_EDGES, EntityType.DEVICE),
    DEVICE_PROFILE(EdgeEventScope.TENANT_EDGES, EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EdgeEventScope.TENANT_EDGES, EntityType.ASSET_PROFILE),
    ENTITY_VIEW(EdgeEventScope.ASSIGNED_EDGES, EntityType.ENTITY_VIEW),
    ALARM(EdgeEventScope.ASSIGNED_EDGES, EntityType.ALARM),
    ALARM_COMMENT(EdgeEventScope.ASSIGNED_EDGES, null),
    RULE_CHAIN(EdgeEventScope.ASSIGNED_EDGES, EntityType.RULE_CHAIN),
    RULE_CHAIN_METADATA(EdgeEventScope.ASSIGNED_EDGES, null),
    EDGE(EdgeEventScope.ASSIGNED_EDGES, EntityType.EDGE),
    USER(EdgeEventScope.TENANT_EDGES, EntityType.USER),
    CUSTOMER(EdgeEventScope.TENANT_EDGES, EntityType.CUSTOMER),
    RELATION(EdgeEventScope.TENANT_EDGES, null),
    TENANT(EdgeEventScope.TENANT_EDGES, EntityType.TENANT),
    TENANT_PROFILE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.TENANT_PROFILE),
    WIDGETS_BUNDLE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.WIDGET_TYPE),
    ADMIN_SETTINGS(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.ADMIN_SETTINGS),
    OTA_PACKAGE(EdgeEventScope.TENANT_EDGES, EntityType.OTA_PACKAGE),
    QUEUE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.QUEUE),
    NOTIFICATION_RULE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.NOTIFICATION_RULE),
    NOTIFICATION_TARGET(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.NOTIFICATION_TARGET),
    NOTIFICATION_TEMPLATE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.NOTIFICATION_TEMPLATE),
    TB_RESOURCE(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.TB_RESOURCE),
    OAUTH2_CLIENT(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.OAUTH2_CLIENT),
    DOMAIN(EdgeEventScope.SYSTEM_AND_TENANT_EDGES, EntityType.DOMAIN),
    CALCULATED_FIELD(EdgeEventScope.ASSIGNED_EDGES, EntityType.CALCULATED_FIELD);

    private final EdgeEventScope scope;

    private final EntityType entityType;

    EdgeEventType(EdgeEventScope scope, EntityType entityType) {
        this.scope = scope;
        this.entityType = entityType;
    }

    // entity is pushed to all edges of its tenant, otherwise only to edges it's assigned to
    public boolean isAllTenantEdgesRelated() {
        return scope != EdgeEventScope.ASSIGNED_EDGES;
    }

    // entity can be owned by system tenant, so system level events are pushed to all edges
    public boolean isSystemLevelSupported() {
        return scope == EdgeEventScope.SYSTEM_AND_TENANT_EDGES;
    }

    public enum EdgeEventScope {
        // only edges the entity is assigned to
        ASSIGNED_EDGES,
        // all edges of the entity's tenant
        TENANT_EDGES,
        // all edges of the entity's tenant, or all edges if the entity is owned by system tenant
        SYSTEM_AND_TENANT_EDGES
    }

}
