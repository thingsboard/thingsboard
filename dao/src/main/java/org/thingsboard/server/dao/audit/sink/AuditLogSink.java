// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.audit.sink;

import org.thingsboard.server.common.data.audit.AuditLog;

public interface AuditLogSink {

    void logAction(AuditLog auditLogEntry);
}
