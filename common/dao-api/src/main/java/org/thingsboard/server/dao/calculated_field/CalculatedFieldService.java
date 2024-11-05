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
package org.thingsboard.server.dao.calculated_field;

import org.thingsboard.server.common.data.calculated_field.CalculatedField;
import org.thingsboard.server.common.data.calculated_field.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entity.EntityDaoService;

public interface CalculatedFieldService extends EntityDaoService {

    CalculatedField save(CalculatedField calculatedField);

    CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    CalculatedFieldLink saveCalculatedFieldLink(TenantId tenantId, CalculatedFieldLink calculatedFieldLink);

    boolean existsByEntityId(TenantId tenantId, EntityId entityId);

}
