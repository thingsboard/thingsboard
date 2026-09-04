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
package org.thingsboard.server.service.security.permission;

import org.thingsboard.server.common.data.EntityType;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum Resource {

    // The boolean flag marks resources whose entities own time series and attributes.
    ADMIN_SETTINGS(EntityType.ADMIN_SETTINGS),
    ALARM(EntityType.ALARM),
    DEVICE(true, EntityType.DEVICE),
    ASSET(true, EntityType.ASSET),
    CUSTOMER(true, EntityType.CUSTOMER),
    DASHBOARD(EntityType.DASHBOARD),
    ENTITY_VIEW(true, EntityType.ENTITY_VIEW),
    TENANT(true, EntityType.TENANT),
    RULE_CHAIN(true, EntityType.RULE_CHAIN),
    USER(true, EntityType.USER),
    WIDGETS_BUNDLE(EntityType.WIDGETS_BUNDLE),
    WIDGET_TYPE(EntityType.WIDGET_TYPE),
    OAUTH2_CLIENT(EntityType.OAUTH2_CLIENT),
    DOMAIN(EntityType.DOMAIN),
    MOBILE_APP(EntityType.MOBILE_APP),
    MOBILE_APP_BUNDLE(EntityType.MOBILE_APP_BUNDLE),
    OAUTH2_CONFIGURATION_TEMPLATE(),
    TENANT_PROFILE(true, EntityType.TENANT_PROFILE),
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    ASSET_PROFILE(EntityType.ASSET_PROFILE),
    API_USAGE_STATE(true, EntityType.API_USAGE_STATE),
    TB_RESOURCE(EntityType.TB_RESOURCE),
    OTA_PACKAGE(EntityType.OTA_PACKAGE),
    EDGE(true, EntityType.EDGE),
    RPC(EntityType.RPC),
    QUEUE(EntityType.QUEUE),
    VERSION_CONTROL,
    NOTIFICATION(EntityType.NOTIFICATION_TARGET, EntityType.NOTIFICATION_TEMPLATE,
            EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_RULE),
    MOBILE_APP_SETTINGS,
    JOB(EntityType.JOB),
    AI_MODEL(EntityType.AI_MODEL),
    API_KEY(EntityType.API_KEY);

    // Entity types in scope of the attributes and time series API. The rest own neither, or are not supported by the API at all.
    public static final Set<EntityType> ENTITY_TYPES_WITH_TS_AND_ATTRIBUTES = Collections.unmodifiableSet(
            Arrays.stream(values())
                    .filter(resource -> resource.tsAndAttributes)
                    .flatMap(resource -> resource.getEntityTypes().stream())
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(EntityType.class))));

    private final boolean tsAndAttributes;
    private final Set<EntityType> entityTypes;

    Resource() {
        this(false);
    }

    Resource(EntityType... entityTypes) {
        this(false, entityTypes);
    }

    Resource(boolean tsAndAttributes, EntityType... entityTypes) {
        this.tsAndAttributes = tsAndAttributes;
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
