/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.audit;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.audit.ActionType;

import java.util.HashMap;
import java.util.Map;

public class AuditLogLevelFilter {

    private Map<EntityType, AuditLogLevelMask> entityTypeMask = new HashMap<>();

    public AuditLogLevelFilter(Map<String, String> mask) {
        entityTypeMask.clear();
        mask.forEach((entityTypeStr, logLevelMaskStr) -> {
            EntityType entityType = EntityType.valueOf(entityTypeStr.toUpperCase());
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
