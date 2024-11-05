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
package org.thingsboard.server.dao.sql.calculated_field;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.calculated_field.CalculatedField;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.calculated_field.CalculatedFieldDao;
import org.thingsboard.server.dao.model.sql.CalculatedFieldEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaCalculatedFieldDao extends JpaAbstractDao<CalculatedFieldEntity, CalculatedField> implements CalculatedFieldDao {

    private final CalculatedFieldRepository calculatedFieldRepository;

    @Override
    public boolean existsByTenantIdAndEntityId(TenantId tenantId, EntityId entityId) {
        return calculatedFieldRepository.existsByTenantIdAndEntityId(tenantId.getId(), entityId.getId());
    }

    @Override
    @Transactional
    public List<CalculatedField> removeAllByEntityId(TenantId tenantId, EntityId entityId) {
        return DaoUtil.convertDataList(calculatedFieldRepository.removeAllByTenantIdAndEntityId(tenantId.getId(), entityId.getId()));
    }

    @Override
    protected Class<CalculatedFieldEntity> getEntityClass() {
        return CalculatedFieldEntity.class;
    }

    @Override
    protected JpaRepository<CalculatedFieldEntity, UUID> getRepository() {
        return calculatedFieldRepository;
    }
}
