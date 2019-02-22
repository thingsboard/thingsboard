/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.component;

import com.datastax.driver.core.utils.UUIDs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.component.ComponentDescriptorDao;
import org.thingsboard.server.dao.model.sql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaBaseComponentDescriptorDao extends JpaAbstractSearchTextDao<ComponentDescriptorEntity, ComponentDescriptor>
        implements ComponentDescriptorDao {

    @Autowired
    private ComponentDescriptorRepository componentDescriptorRepository;

    @Override
    protected Class<ComponentDescriptorEntity> getEntityClass() {
        return ComponentDescriptorEntity.class;
    }

    @Override
    protected CrudRepository<ComponentDescriptorEntity, String> getCrudRepository() {
        return componentDescriptorRepository;
    }

    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(TenantId tenantId, ComponentDescriptor component) {
        if (component.getId() == null) {
            component.setId(new ComponentDescriptorId(UUIDs.timeBased()));
        }
        if (!componentDescriptorRepository.existsById(UUIDConverter.fromTimeUUID(component.getId().getId()))) {
            return Optional.of(save(tenantId, component));
        }
        return Optional.empty();
    }

    @Override
    public ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId) {
        return findById(tenantId, componentId.getId());
    }

    @Override
    public ComponentDescriptor findByClazz(TenantId tenantId, String clazz) {
        return DaoUtil.getData(componentDescriptorRepository.findByClazz(clazz));
    }

    @Override
    public List<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(componentDescriptorRepository
                .findByType(
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(componentDescriptorRepository
                .findByScopeAndType(
                        type,
                        scope,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : UUIDConverter.fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    @Transactional
    public void deleteById(TenantId tenantId, ComponentDescriptorId componentId) {
        removeById(tenantId, componentId.getId());
    }

    @Override
    @Transactional
    public void deleteByClazz(TenantId tenantId, String clazz) {
        componentDescriptorRepository.deleteByClazz(clazz);
    }
}
