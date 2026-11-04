// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppArgument;
import org.thingsboard.server.common.data.agent.config.AgentAppArgumentSource;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.dao.entity.EntityDaoService;
import org.thingsboard.server.dao.entity.EntityServiceRegistry;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AgentAppArgumentReferenceValidator {

    private final AgentAppArgumentSourceResolver sourceEntityResolver;
    @Lazy
    private final EntityServiceRegistry entityServiceRegistry;

    public void validate(TenantId tenantId, AgentAppConfig config, AgentApplication application) {
        if (config == null || CollectionsUtil.isEmpty(config.getArguments())) {
            return;
        }
        Map<AgentAppArgumentSource, Optional<EntityId>> contextSourceCache = new EnumMap<>(AgentAppArgumentSource.class);
        Set<EntityId> verified = new HashSet<>();
        for (AgentAppArgument argument : config.getArguments()) {
            AgentAppArgumentSource sourceType = argument.getSourceType();
            if (sourceType == null) {
                continue;
            }
            EntityId entityId = resolveReferencedEntity(tenantId, application, argument, sourceType, contextSourceCache);
            if (entityId != null && verified.add(entityId)) {
                requireEntityOfTenant(tenantId, argument, entityId);
            }
        }
    }

    private EntityId resolveReferencedEntity(TenantId tenantId, AgentApplication application,
                                             AgentAppArgument argument, AgentAppArgumentSource sourceType,
                                             Map<AgentAppArgumentSource, Optional<EntityId>> contextSourceCache) {
        if (sourceType.isConcreteEntityRef()) {
            return argument.getSourceEntityId();
        }
        if (application == null) {
            return null;
        }
        EntityId entityId = contextSourceCache.computeIfAbsent(sourceType,
                type -> Optional.ofNullable(sourceEntityResolver.resolveContextSource(tenantId, application, type))).orElse(null);
        if (entityId == null && argument.getDefaultValue() == null) {
            throw new DataValidationException("Custom argument '" + argument.getName()
                    + "' could not resolve its " + sourceType + " source and no default value is set!");
        }
        return entityId;
    }

    private void requireEntityOfTenant(TenantId tenantId, AgentAppArgument argument, EntityId entityId) {
        EntityDaoService entityService;
        try {
            entityService = entityServiceRegistry.getServiceByEntityType(entityId.getEntityType());
        } catch (IllegalArgumentException e) {
            throw new DataValidationException("Custom argument '" + argument.getName()
                    + "' references an unsupported entity type " + entityId.getEntityType() + "!");
        }
        HasId<?> entity = entityService.findEntity(tenantId, entityId).orElse(null);
        if (entity == null || isForeignTenant(tenantId, entity)) {
            throw new DataValidationException("Custom argument '" + argument.getName()
                    + "' is referencing non-existent " + entityId.getEntityType() + "!");
        }
    }

    private static boolean isForeignTenant(TenantId tenantId, HasId<?> entity) {
        if (TenantId.SYS_TENANT_ID.equals(tenantId) || !(entity instanceof HasTenantId hasTenantId)) {
            return false;
        }
        TenantId entityTenantId = hasTenantId.getTenantId();
        return entityTenantId != null && !tenantId.equals(entityTenantId);
    }
}
