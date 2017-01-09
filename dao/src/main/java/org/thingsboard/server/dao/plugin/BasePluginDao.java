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
package org.thingsboard.server.dao.plugin;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.AbstractSearchTextDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.PluginMetaDataEntity;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@Component
@Slf4j
public class BasePluginDao extends AbstractSearchTextDao<PluginMetaDataEntity> implements PluginDao {

    @Override
    protected Class<PluginMetaDataEntity> getColumnFamilyClass() {
        return PluginMetaDataEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.PLUGIN_COLUMN_FAMILY_NAME;
    }

    @Override
    public PluginMetaDataEntity save(PluginMetaData plugin) {
        return save(new PluginMetaDataEntity(plugin));
    }

    @Override
    public PluginMetaDataEntity findById(PluginId pluginId) {
        log.debug("Search plugin meta-data entity by id [{}]", pluginId);
        PluginMetaDataEntity entity = super.findById(pluginId.getId());
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return entity;
    }

    @Override
    public PluginMetaDataEntity findByApiToken(String apiToken) {
        log.debug("Search plugin meta-data entity by api token [{}]", apiToken);
        Select.Where query = select().from(ModelConstants.PLUGIN_BY_API_TOKEN_COLUMN_FAMILY_NAME).where(eq(ModelConstants.PLUGIN_API_TOKEN_PROPERTY, apiToken));
        log.trace("Execute query [{}]", query);
        PluginMetaDataEntity entity = findOneByStatement(query);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return entity;
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete plugin meta-data entity by id [{}]", id);
        ResultSet resultSet = removeById(id);
        log.debug("Delete result: [{}]", resultSet.wasApplied());
    }

    @Override
    public void deleteById(PluginId pluginId) {
        deleteById(pluginId.getId());
    }

    @Override
    public List<PluginMetaDataEntity> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> entities = findPageWithTextSearch(ModelConstants.PLUGIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.PLUGIN_TENANT_ID_PROPERTY, tenantId.getId())), pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return entities;
    }

    @Override
    public List<PluginMetaDataEntity> findAllTenantPluginsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> pluginEntities = findPageWithTextSearch(ModelConstants.PLUGIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(in(ModelConstants.PLUGIN_TENANT_ID_PROPERTY, Arrays.asList(NULL_UUID, tenantId))),
                pageLink);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(pluginEntities.toArray()));
        } else {
            log.debug("Search result: [{}]", pluginEntities.size());
        }
        return pluginEntities;
    }

}
