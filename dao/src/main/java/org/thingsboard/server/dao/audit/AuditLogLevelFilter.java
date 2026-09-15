// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "audit-log", value = "enabled", havingValue = "true")
public class AuditLogLevelFilter {

    private Map<EntityType, AuditLogLevelMask> entityTypeMask = new HashMap<>();

    public AuditLogLevelFilter(AuditLogLevelProperties auditLogLevelProperties) {
        Map<String, String> mask = auditLogLevelProperties.getMask();
        entityTypeMask.clear();
        mask.forEach((entityTypeStr, logLevelMaskStr) -> {
            EntityType entityType = EntityType.valueOf(entityTypeStr.toUpperCase(Locale.ENGLISH));
            AuditLogLevelMask logLevelMask = AuditLogLevelMask.valueOf(logLevelMaskStr.toUpperCase());
            entityTypeMask.put(entityType, logLevelMask);
        });
    }

    public boolean logEnabled(EntityType entityType, ActionType actionType) {
        AuditLogLevelMask logLevelMask = entityTypeMask.get(entityType);
        if (logLevelMask != null) {
            return actionType.isRead() ? logLevelMask.isRead() : logLevelMask.isWrite();
        } else {
            return false;
        }
    }

}
