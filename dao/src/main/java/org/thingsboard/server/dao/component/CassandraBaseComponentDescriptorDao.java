/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.component;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.core.utils.UUIDs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.ComponentDescriptorEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

/**
 * @author Andrew Shvayka
 */
@Component
@Slf4j
@NoSqlDao
public class CassandraBaseComponentDescriptorDao extends CassandraAbstractSearchTextDao<ComponentDescriptorEntity, ComponentDescriptor> implements ComponentDescriptorDao {

    @Override
    protected Class<ComponentDescriptorEntity> getColumnFamilyClass() {
        return ComponentDescriptorEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME;
    }

    @Override
    public Optional<ComponentDescriptor> saveIfNotExist(ComponentDescriptor component) {
        ComponentDescriptorEntity entity = new ComponentDescriptorEntity(component);
        log.debug("Save component entity [{}]", entity);
        Optional<ComponentDescriptor> result = saveIfNotExist(entity);
        if (log.isTraceEnabled()) {
            log.trace("Saved result: [{}] for component entity [{}]", result.isPresent(), result.orElse(null));
        } else {
            log.debug("Saved result: [{}]", result.isPresent());
        }
        return result;
    }

    @Override
    public ComponentDescriptor findById(ComponentDescriptorId componentId) {
        log.debug("Search component entity by id [{}]", componentId);
        ComponentDescriptor componentDescriptor = super.findById(componentId.getId());
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for component entity [{}]", componentDescriptor != null, componentDescriptor);
        } else {
            log.debug("Search result: [{}]", componentDescriptor != null);
        }
        return componentDescriptor;
    }

    @Override
    public ComponentDescriptor findByClazz(String clazz) {
        log.debug("Search component entity by clazz [{}]", clazz);
        Select.Where query = select().from(getColumnFamilyName()).where(eq(ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, clazz));
        log.trace("Execute query [{}]", query);
        ComponentDescriptorEntity entity = findOneByStatement(query);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for component entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return DaoUtil.getData(entity);
    }

    @Override
    public List<ComponentDescriptor> findByTypeAndPageLink(ComponentType type, TextPageLink pageLink) {
        log.debug("Try to find component by type [{}] and pageLink [{}]", type, pageLink);
        List<ComponentDescriptorEntity> entities = findPageWithTextSearch(ModelConstants.COMPONENT_DESCRIPTOR_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY, type)), pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<ComponentDescriptor> findByScopeAndTypeAndPageLink(ComponentScope scope, ComponentType type, TextPageLink pageLink) {
        log.debug("Try to find component by scope [{}] and type [{}] and pageLink [{}]", scope, type, pageLink);
        List<ComponentDescriptorEntity> entities = findPageWithTextSearch(ModelConstants.COMPONENT_DESCRIPTOR_BY_SCOPE_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY, type),
                        eq(ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY, scope.name())), pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    public boolean removeById(UUID key) {
        Statement delete = QueryBuilder.delete().all().from(ModelConstants.COMPONENT_DESCRIPTOR_BY_ID).where(eq(ModelConstants.ID_PROPERTY, key));
        log.debug("Remove request: {}", delete.toString());
        return getSession().execute(delete).wasApplied();
    }

    @Override
    public void deleteById(ComponentDescriptorId id) {
        log.debug("Delete plugin meta-data entity by id [{}]", id);
        boolean result = removeById(id.getId());
        log.debug("Delete result: [{}]", result);
    }

    @Override
    public void deleteByClazz(String clazz) {
        log.debug("Delete plugin meta-data entity by id [{}]", clazz);
        Statement delete = QueryBuilder.delete().all().from(getColumnFamilyName()).where(eq(ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, clazz));
        log.debug("Remove request: {}", delete.toString());
        ResultSet resultSet = getSession().execute(delete);
        log.debug("Delete result: [{}]", resultSet.wasApplied());
    }

    private Optional<ComponentDescriptor> saveIfNotExist(ComponentDescriptorEntity entity) {
        if (entity.getId() == null) {
            entity.setId(UUIDs.timeBased());
        }

        ResultSet rs = executeRead(QueryBuilder.insertInto(getColumnFamilyName())
                .value(ModelConstants.ID_PROPERTY, entity.getId())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_NAME_PROPERTY, entity.getName())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_CLASS_PROPERTY, entity.getClazz())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_TYPE_PROPERTY, entity.getType())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_SCOPE_PROPERTY, entity.getScope())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY, entity.getConfigurationDescriptor())
                .value(ModelConstants.COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY, entity.getActions())
                .value(ModelConstants.SEARCH_TEXT_PROPERTY, entity.getSearchText())
                .ifNotExists()
        );
        if (rs.wasApplied()) {
            return Optional.of(DaoUtil.getData(entity));
        } else {
            return Optional.empty();
        }
    }
}
