/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.service.cf.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.cf.CalculatedFieldLinkConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;

import java.util.List;
import java.util.Map;

@Data
public class CalculatedFieldAttributeUpdateRequest implements CalculatedFieldTelemetryUpdateRequest {

    private TenantId tenantId;
    private EntityId entityId;
    private AttributeScope scope;
    private List<AttributeKvEntry> kvEntries;
    private List<CalculatedFieldId> previousCalculatedFieldIds;

    public CalculatedFieldAttributeUpdateRequest(AttributesSaveRequest request) {
        this.tenantId = request.getTenantId();
        this.entityId = request.getEntityId();
        this.scope = request.getScope();
        this.kvEntries = request.getEntries();
        this.previousCalculatedFieldIds = request.getPreviousCalculatedFieldIds();
    }

    @Override
    public Map<String, String> getTelemetryKeysFromLink(CalculatedFieldLinkConfiguration linkConfiguration) {
        return switch (scope) {
            case CLIENT_SCOPE -> linkConfiguration.getClientAttributes();
            case SERVER_SCOPE -> linkConfiguration.getServerAttributes();
            case SHARED_SCOPE -> linkConfiguration.getSharedAttributes();
        };
    }

}
