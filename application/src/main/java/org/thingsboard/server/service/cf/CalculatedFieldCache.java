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
package org.thingsboard.server.service.cf;

import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.List;
import java.util.Set;

public interface CalculatedFieldCache {

    CalculatedField getCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldLink> getCalculatedFieldLinks(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId);

    CalculatedFieldCtx getCalculatedFieldCtx(TenantId tenantId, CalculatedFieldId calculatedFieldId, TbelInvokeService tbelInvokeService);

    Set<EntityId> getEntitiesByProfile(TenantId tenantId, EntityId entityId);

    void evict(CalculatedFieldId calculatedFieldId);

}
