// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.EntityType;

import java.util.Collections;
import java.util.Set;

public enum Resource {

    ADMIN_SETTINGS(EntityType.ADMIN_SETTINGS),
    ALARM(EntityType.ALARM),
    DEVICE(EntityType.DEVICE),
    ASSET(EntityType.ASSET),
    CUSTOMER(EntityType.CUSTOMER),
    DASHBOARD(EntityType.DASHBOARD),
    ENTITY_VIEW(EntityType.ENTITY_VIEW),
    TENANT(EntityType.TENANT),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    USER(EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CLIENT(EntityType.OAUTH2_CLIENT),
    DOMAIN(EntityType.DOMAIN),
    MOBILE_APP(EntityType.MOBILE_APP),
    MOBILE_APP_BUNDLE(EntityType.MOBILE_APP_BUNDLE),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    API_USAGE_STATE(EntityType.API_USAGE_STATE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    OTA_PACKAGE(EntityType.OTA_PACKAGE),
    EDGE(EntityType.EDGE),
    RPC(EntityType.RPC),
    QUEUE(EntityType.QUEUE),
    VERSION_CONTROL,
    NOTIFICATION(EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_TEMPLATE,
            EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_RULE),
    MOBILE_APP_SETTINGS,
    JOB(EntityType.JOB),
    AI_MODEL(EntityType.AI_MODEL);

    private final Set<EntityType> entityTypes;

    Resource() {
        this.entityTypes = Collections.emptySet();
    }

    Resource(EntityType... entityTypes) {
        this.entityTypes = Set.of(entityTypes);
    }

    public Set<EntityType> getEntityTypes() {
        return entityTypes;
    }

    public static Resource of(EntityType entityType) {
        for (Resource resource : Resource.values()) {
            if (resource.getEntityTypes().contains(entityType)) {
                return resource;
            }
        }
        throw new IllegalArgumentException("Unknown EntityType: " + entityType.name());
    }

}
