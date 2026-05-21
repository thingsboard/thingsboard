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
package org.thingsboard.server.dao.service.validator;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.dashboard.DashboardConfig;
import org.thingsboard.server.common.data.dashboard.EntityAlias;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DashboardDataValidator extends DataValidator<Dashboard> {

    private final TenantService tenantService;

    @Override
    protected void validateCreate(TenantId tenantId, Dashboard data) {
        validateNumberOfEntitiesPerTenant(tenantId, EntityType.DASHBOARD);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Dashboard dashboard) {
        validateString("Dashboard title", dashboard.getTitle());
        if (dashboard.getTenantId() == null) {
            throw new DataValidationException("Dashboard should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(dashboard.getTenantId())) {
                throw new DataValidationException("Dashboard is referencing to non-existent tenant!");
            }
        }
        if (dashboard.getConfiguration() == null || !dashboard.getConfiguration().isObject()) {
            return;
        }
        JsonNode aliasesNode = dashboard.getConfiguration().get("entityAliases");
        if (aliasesNode == null || aliasesNode.isNull()) {
            return;
        }
        DashboardConfig parsed;
        try {
            parsed = JacksonUtil.OBJECT_MAPPER.convertValue(dashboard.getConfiguration(), DashboardConfig.class);
        } catch (IllegalArgumentException e) {
            throw new DataValidationException("Dashboard configuration has invalid structure: " + e.getMessage());
        }
        validateEntityAliases(parsed);
    }

    private static void validateEntityAliases(DashboardConfig config) {
        if (config.getEntityAliases() == null) {
            return;
        }
        Set<ConstraintViolation<DashboardConfig>> violations = ConstraintValidator.getViolations(config);
        if (!violations.isEmpty()) {
            throw new DataValidationException(violations.stream()
                    .map(v -> formatViolation(v, config))
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", ", "Dashboard validation error: ", "")));
        }
        config.getEntityAliases().forEach(DashboardDataValidator::validateEntityAliasKey);
    }

    private static String formatViolation(ConstraintViolation<DashboardConfig> v, DashboardConfig cfg) {
        UUID aliasKey = null;
        String fieldName = null;
        Integer elementIndex = null;
        for (Path.Node node : v.getPropertyPath()) {
            if (node.getKey() instanceof UUID uuid) {
                aliasKey = uuid;
            }
            if (node.getKind() == ElementKind.CONTAINER_ELEMENT) {
                if (node.getIndex() != null) {
                    elementIndex = node.getIndex();
                }
            } else if (node.getName() != null) {
                fieldName = node.getName();
            }
        }
        String elementInfo = elementIndex != null ? " element at index " + elementIndex : "";
        if (aliasKey != null) {
            EntityAlias alias = cfg.getEntityAliases().get(aliasKey);
            String aliasName = alias != null && alias.getAlias() != null && !alias.getAlias().isBlank()
                    ? alias.getAlias() : aliasKey.toString();
            return "alias '" + aliasName + "' field '" + fieldName + "'" + elementInfo + " " + v.getMessage();
        }
        return "field '" + fieldName + "'" + elementInfo + " " + v.getMessage();
    }

    private static void validateEntityAliasKey(UUID key, EntityAlias alias) {
        if (!alias.getId().equals(key)) {
            throw new DataValidationException("Dashboard validation error: alias '" + alias.getAlias() + "' has 'id' that does not match its key!");
        }
    }

}
