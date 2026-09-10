// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;

@Data
@AllArgsConstructor
@Builder
public class EdqsEvent {
    
    private final TenantId tenantId;
    private final ObjectType objectType;
    private final EdqsEventType eventType;
    private final EdqsObject object;

}
