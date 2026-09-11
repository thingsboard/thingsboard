// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class AuditLogId extends UUIDBased {

    private static final long serialVersionUID = 1L;

    @JsonCreator
    public AuditLogId(@JsonProperty("id") UUID id) {
        super(id);
    }

    public static AuditLogId fromString(String auditLogId) {
        return new AuditLogId(UUID.fromString(auditLogId));
    }
}
