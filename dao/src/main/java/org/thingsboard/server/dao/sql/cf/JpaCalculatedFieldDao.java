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
package org.thingsboard.server.dao.sql.cf;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
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
    private final NativeCalculatedFieldRepository nativeCalculatedFieldRepository;

    @Override
    public List<CalculatedField> findAllByTenantId(TenantId tenantId) {
        return DaoUtil.convertDataList(calculatedFieldRepository.findAllByTenantId(tenantId.getId()));
    }

    @Override
    public List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId) {
        return calculatedFieldRepository.findCalculatedFieldIdsByTenantIdAndEntityId(tenantId.getId(), entityId.getId());
    }

    @Override
    public List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId) {
        return DaoUtil.convertDataList(calculatedFieldRepository.findAllByTenantIdAndEntityId(tenantId.getId(), entityId.getId()));
    }

    @Override
    public List<CalculatedField> findAll() {
        return DaoUtil.convertDataList(calculatedFieldRepository.findAll());
    }

    @Override
    public CalculatedField findByEntityIdAndName(EntityId entityId, String name) {
        return DaoUtil.getData(calculatedFieldRepository.findByEntityIdAndName(entityId.getId(), name));
    }

    @Override
    public PageData<CalculatedField> findAll(PageLink pageLink) {
        log.debug("Try to find calculated fields by pageLink [{}]", pageLink);
        return nativeCalculatedFieldRepository.findCalculatedFields(DaoUtil.toPageable(pageLink));
    }

    @Override
    public PageData<CalculatedField> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        log.debug("Try to find calculated fields by tenantId[{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.toPageData(calculatedFieldRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<CalculatedField> findAllByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.debug("Try to find calculated fields by entityId[{}] and pageLink [{}]", entityId, pageLink);
        return DaoUtil.toPageData(calculatedFieldRepository.findAllByTenantIdAndEntityId(tenantId.getId(), entityId.getId(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    @Transactional
    public List<CalculatedField> removeAllByEntityId(TenantId tenantId, EntityId entityId) {
        return DaoUtil.convertDataList(calculatedFieldRepository.removeAllByTenantIdAndEntityId(tenantId.getId(), entityId.getId()));
    }

    @Override
    public long countCFByEntityId(TenantId tenantId, EntityId entityId) {
        return calculatedFieldRepository.countByTenantIdAndEntityId(tenantId.getId(), entityId.getId());
    }

    @Override
    protected Class<CalculatedFieldEntity> getEntityClass() {
        return CalculatedFieldEntity.class;
    }

    @Override
    protected JpaRepository<CalculatedFieldEntity, UUID> getRepository() {
        return calculatedFieldRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
    }

}
