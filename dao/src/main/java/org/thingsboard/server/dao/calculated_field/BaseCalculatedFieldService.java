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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.calculated_field.CalculatedField;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("CalculatedFieldDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseCalculatedFieldService implements CalculatedFieldService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CALCULATED_FIELD_ID = "Incorrect calculatedFieldId ";

    private final CalculatedFieldDao calculatedFieldDao;

    @Override
    public CalculatedField save(CalculatedField calculatedField) {
        log.trace("Executing save, [{}]", calculatedField);
        return calculatedFieldDao.save(calculatedField.getTenantId(), calculatedField);
    }

    @Override
    public CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        log.trace("Executing findById, tenantId [{}], rpcId [{}]", tenantId, calculatedFieldId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldId, id -> INCORRECT_CALCULATED_FIELD_ID + id);
        return calculatedFieldDao.findById(tenantId, calculatedFieldId.getId());
    }

    @Override
    public void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        log.trace("Executing deleteRpc, tenantId [{}], rpcId [{}]", tenantId, calculatedFieldId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldId, id -> INCORRECT_CALCULATED_FIELD_ID + id);
        calculatedFieldDao.removeById(tenantId, calculatedFieldId.getId());
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new CalculatedFieldId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
    }

}
