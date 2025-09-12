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
package org.thingsboard.server.dao.audit.sink;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.AuditLogUtil;
import org.thingsboard.server.common.data.audit.AuditLog;

/**
 * The StdOutAuditLogSink class provides an implementation of the AuditLogSink interface that
 * writes audit log entries to the info-scope of standard output (stdout) using a formatted log message.
 *
 * This class is enabled when the application configuration specifies the type
 * of the audit-log sink as "stdout". It is primarily used for debugging or local development
 * purposes where audit log entries need to be observed in real-time.
 *
 * The log entries are formatted using the {@link AuditLogUtil#format(AuditLog)} method to
 * provide detailed and structured output of the audit log data.
 *
 * Annotations:
 * - {@code @Slf4j}: Enables logging functionality for logging messages.
 * - {@code @Component}: Marks this class as a Spring-managed component.
 * - {@code @ConditionalOnProperty}: Configures this class to be instantiated conditionally based
 *   on the presence and value of the "audit-log.sink.type" property in the application configuration.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "audit-log.sink", value = "type", havingValue = "stdout")
public class StdOutAuditLogSink implements AuditLogSink {

    @Override
    public void logAction(AuditLog auditLogEntry) {
        log.info("Audit log entry: {}", AuditLogUtil.format(auditLogEntry));

    }


}
