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
package org.thingsboard.server.dao.sql.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.PluginMetaDataEntity;
import org.thingsboard.server.dao.plugin.PluginDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 5/1/2017.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaBasePluginDao extends JpaAbstractSearchTextDao<PluginMetaDataEntity, PluginMetaData> implements PluginDao {

    @Autowired
    private PluginMetaDataRepository pluginMetaDataRepository;

    @Override
    protected Class<PluginMetaDataEntity> getEntityClass() {
        return PluginMetaDataEntity.class;
    }

    @Override
    protected CrudRepository<PluginMetaDataEntity, UUID> getCrudRepository() {
        return pluginMetaDataRepository;
    }

    @Override
    public PluginMetaData findById(PluginId pluginId) {
        log.debug("Search plugin meta-data entity by id [{}]", pluginId);
        PluginMetaData pluginMetaData = super.findById(pluginId.getId());
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", pluginMetaData != null, pluginMetaData);
        } else {
            log.debug("Search result: [{}]", pluginMetaData != null);
        }
        return pluginMetaData;
    }

    @Override
    public PluginMetaData findByApiToken(String apiToken) {
        log.debug("Search plugin meta-data entity by api token [{}]", apiToken);
        PluginMetaDataEntity entity = pluginMetaDataRepository.findByApiToken(apiToken);
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}] for plugin entity [{}]", entity != null, entity);
        } else {
            log.debug("Search result: [{}]", entity != null);
        }
        return DaoUtil.getData(entity);
    }

    @Override
    public void deleteById(UUID id) {
        log.debug("Delete plugin meta-data entity by id [{}]", id);
        pluginMetaDataRepository.delete(id);
    }

    @Override
    public void deleteById(PluginId pluginId) {
        deleteById(pluginId.getId());
    }

    @Override
    public List<PluginMetaData> findByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.debug("Try to find здгпшты by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> entities;
        if (pageLink.getIdOffset() == null) {
            entities = pluginMetaDataRepository
                    .findByTenantIdAndPageLinkFirstPage(pageLink.getLimit(), tenantId.getId(), pageLink.getTextSearch());
        } else {
            entities = pluginMetaDataRepository
                    .findByTenantIdAndPageLinkNextPage(pageLink.getLimit(), tenantId.getId(), pageLink.getTextSearch(), pageLink.getIdOffset());
        }
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<PluginMetaData> findAllTenantPluginsByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find all tenant plugins by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<PluginMetaDataEntity> entities;
        if (pageLink.getIdOffset() == null) {
            entities = pluginMetaDataRepository
                    .findAllTenantPluginsByTenantIdFirstPage(pageLink.getLimit(), tenantId, pageLink.getTextSearch());
        } else {
            entities = pluginMetaDataRepository
                    .findAllTenantPluginsByTenantIdNextPage(pageLink.getLimit(), tenantId, pageLink.getTextSearch(), pageLink.getIdOffset());
        }
        if (log.isTraceEnabled()) {
            log.trace("Search result: [{}]", Arrays.toString(entities.toArray()));
        } else {
            log.debug("Search result: [{}]", entities.size());
        }
        return DaoUtil.convertDataList(entities);
    }
}
