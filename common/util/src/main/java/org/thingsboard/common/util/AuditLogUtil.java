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
package org.thingsboard.common.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.thingsboard.server.common.data.audit.AuditLog;

import java.time.LocalDateTime;
import java.util.Date;

public class AuditLogUtil {

    public static String format(AuditLog auditLogEntry) {
        StringBuilder logMessage = new StringBuilder();
        logMessage.append("Audit log entry:\n")
                .append("EventTime  : ").append(new Date(auditLogEntry.getCreatedTime())).append("\n")
                .append("ID        : ").append(auditLogEntry.getId()).append("\n")
                .append("TenantID   : ").append(auditLogEntry.getTenantId()).append("\n");
        if (auditLogEntry.getCustomerId() != null) {
            logMessage.append("CustomerID      : ").append(auditLogEntry.getCustomerId()).append("\n");
        }
        logMessage.append("UserID      : ").append(auditLogEntry.getUserId()).append("\n")
                .append("UserName      : ").append(auditLogEntry.getUserName()).append("\n")
                .append("Action    : ").append(auditLogEntry.getActionType()).append("\n")
                .append("ActionStatus    : ").append(auditLogEntry.getActionStatus()).append("\n");
        if (auditLogEntry.getActionData() != null) {
            logMessage.append("ActionData    : ").append(auditLogEntry.getActionData()).append("\n");
        }
        if (!auditLogEntry.getActionFailureDetails().isEmpty()) {
            logMessage.append("ActionFailureDetails    : ").append(auditLogEntry.getActionFailureDetails()).append("\n");
        }
       logMessage.append("EntityType  : ").append(auditLogEntry.getEntityId().getEntityType()).append("\n")
                .append("EntityID  : ").append(auditLogEntry.getEntityId()).append("\n")
                .append("EntityName  : ").append(auditLogEntry.getEntityName()).append("\n");
        return logMessage.toString();
    }


    public static String createJsonRecord(AuditLog auditLog) {
        ObjectNode auditLogNode = JacksonUtil.newObjectNode();
        auditLogNode.put("postDate", LocalDateTime.now().toString());
        auditLogNode.put("id", auditLog.getId().getId().toString());
        auditLogNode.put("entityName", auditLog.getEntityName());
        auditLogNode.put("tenantId", auditLog.getTenantId().getId().toString());
        if (auditLog.getCustomerId() != null) {
            auditLogNode.put("customerId", auditLog.getCustomerId().getId().toString());
        }
        auditLogNode.put("entityId", auditLog.getEntityId().getId().toString());
        auditLogNode.put("entityType", auditLog.getEntityId().getEntityType().name());
        auditLogNode.put("userId", auditLog.getUserId().getId().toString());
        auditLogNode.put("userName", auditLog.getUserName());
        auditLogNode.put("actionType", auditLog.getActionType().name());
        if (auditLog.getActionData() != null) {
            auditLogNode.put("actionData", auditLog.getActionData().toString());
        }
        auditLogNode.put("actionStatus", auditLog.getActionStatus().name());
        auditLogNode.put("actionFailureDetails", auditLog.getActionFailureDetails());
        return auditLogNode.toString();
    }

}
