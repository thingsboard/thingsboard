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
package org.thingsboard.server.dao.sql.cf;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.cf.CalculatedFieldLinkDao;
import org.thingsboard.server.dao.model.sql.CalculatedFieldLinkEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaCalculatedFieldLinkDao extends JpaAbstractDao<CalculatedFieldLinkEntity, CalculatedFieldLink> implements CalculatedFieldLinkDao {

    private final CalculatedFieldLinkRepository calculatedFieldLinkRepository;

    @Override
    public CalculatedFieldLink findCalculatedFieldLinkByCalculatedFieldId(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        return DaoUtil.getData(calculatedFieldLinkRepository.findByTenantIdAndCalculatedFieldId(tenantId.getId(), calculatedFieldId.getId()));
    }

    @Override
    protected Class<CalculatedFieldLinkEntity> getEntityClass() {
        return CalculatedFieldLinkEntity.class;
    }

    @Override
    protected JpaRepository<CalculatedFieldLinkEntity, UUID> getRepository() {
        return calculatedFieldLinkRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD_LINK;
    }

}
