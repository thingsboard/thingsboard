// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.audit.sink;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.AuditLog;

@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "none")
public class DummyAuditLogSink implements AuditLogSink {

    @Override
    public void logAction(AuditLog auditLogEntry) {
    }
}
