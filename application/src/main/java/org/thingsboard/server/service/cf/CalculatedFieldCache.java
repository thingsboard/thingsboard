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
package org.thingsboard.server.service.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.List;

public interface CalculatedFieldCache {

    CalculatedField getCalculatedField(CalculatedFieldId calculatedFieldId);

    List<CalculatedField> getCalculatedFieldsByEntityId(EntityId entityId);

    List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(EntityId entityId);

    CalculatedFieldCtx getCalculatedFieldCtx(CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldCtx> getCalculatedFieldCtxsByEntityId(EntityId entityId);

    void addCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void updateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void evict(CalculatedFieldId calculatedFieldId);

}
