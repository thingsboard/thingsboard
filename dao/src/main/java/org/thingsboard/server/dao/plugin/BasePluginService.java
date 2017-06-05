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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.component.ComponentDescriptorService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DatabaseException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.PluginMetaDataEntity;
import org.thingsboard.server.dao.model.RuleMetaDataEntity;
import org.thingsboard.server.dao.rule.RuleDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BasePluginService extends AbstractEntityService implements PluginService {

    //TODO: move to a better place.
    public static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);

    @Autowired
    private PluginDao pluginDao;

    @Autowired
    private RuleDao ruleDao;

    @Autowired
    private ComponentDescriptorService componentDescriptorService;

    @Override
    public PluginMetaData savePlugin(PluginMetaData plugin) {
        pluginValidator.validate(plugin);
        if (plugin.getTenantId() == null) {
            log.trace("Save system plugin metadata with predefined id {}", SYSTEM_TENANT);
            plugin.setTenantId(SYSTEM_TENANT);
        }
        if (plugin.getId() != null) {
            PluginMetaData oldVersion = getData(pluginDao.findById(plugin.getId()));
            if (plugin.getState() == null) {
                plugin.setState(oldVersion.getState());
            } else if (plugin.getState() != oldVersion.getState()) {
                throw new IncorrectParameterException("Use Activate/Suspend method to control state of the plugin!");
            }
        } else {
            if (plugin.getState() == null) {
                plugin.setState(ComponentLifecycleState.SUSPENDED);
            } else if (plugin.getState() != ComponentLifecycleState.SUSPENDED) {
                throw new IncorrectParameterException("Use Activate/Suspend method to control state of the plugin!");
            }
        }
        ComponentDescriptor descriptor = componentDescriptorService.findByClazz(plugin.getClazz());
        if (descriptor == null) {
            throw new IncorrectParameterException("Plugin descriptor not found!");
        } else if (!ComponentType.PLUGIN.equals(descriptor.getType())) {
            throw new IncorrectParameterException("Plugin class is actually " + descriptor.getType() + "!");
        }
        PluginMetaDataEntity entity = pluginDao.findByApiToken(plugin.getApiToken());
        if (entity != null && (plugin.getId() == null || !entity.getId().equals(plugin.getId().getId()))) {
            throw new IncorrectParameterException("API token is already reserved!");
        }
        if (!componentDescriptorService.validate(descriptor, plugin.getConfiguration())) {
            throw new IncorrectParameterException("Filters configuration is not valid!");
        }
        return getData(pluginDao.save(plugin));
    }

    @Override
    public PluginMetaData findPluginById(PluginId pluginId) {
        Validator.validateId(pluginId, "Incorrect plugin id for search request.");
        return getData(pluginDao.findById(pluginId));
    }

    @Override
    public ListenableFuture<PluginMetaData> findPluginByIdAsync(PluginId pluginId) {
        validateId(pluginId, "Incorrect plugin id for search plugin request.");
        ListenableFuture<PluginMetaDataEntity> pluginEntity = pluginDao.findByIdAsync(pluginId.getId());
        return Futures.transform(pluginEntity, (com.google.common.base.Function<? super PluginMetaDataEntity, ? extends PluginMetaData>) input -> getData(input));
    }

    @Override
    public PluginMetaData findPluginByApiToken(String apiToken) {
        Validator.validateString(apiToken, "Incorrect plugin apiToken for search request.");
        return getData(pluginDao.findByApiToken(apiToken));
    }

    @Override
    public TextPageData<PluginMetaData> findSystemPlugins(TextPageLink pageLink) {
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search system plugin request.");
        List<PluginMetaDataEntity> pluginEntities = pluginDao.findByTenantIdAndPageLink(SYSTEM_TENANT, pageLink);
        List<PluginMetaData> plugins = convertDataList(pluginEntities);
        return new TextPageData<>(plugins, pageLink);
    }

    @Override
    public TextPageData<PluginMetaData> findTenantPlugins(TenantId tenantId, TextPageLink pageLink) {
        Validator.validateId(tenantId, "Incorrect tenant id for search plugins request.");
        Validator.validatePageLink(pageLink, "Incorrect PageLink object for search plugin request.");
        List<PluginMetaDataEntity> pluginEntities = pluginDao.findByTenantIdAndPageLink(tenantId, pageLink);
        List<PluginMetaData> plugins = convertDataList(pluginEntities);
        return new TextPageData<>(plugins, pageLink);
    }

    @Override
    public List<PluginMetaData> findSystemPlugins() {
        log.trace("Executing findSystemPlugins");
        List<PluginMetaData> plugins = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(300);
        TextPageData<PluginMetaData> pageData = null;
        do {
            pageData = findSystemPlugins(pageLink);
            plugins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return plugins;
    }

    @Override
    public TextPageData<PluginMetaData> findAllTenantPluginsByTenantIdAndPageLink(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findAllTenantPluginsByTenantIdAndPageLink, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<PluginMetaDataEntity> pluginsEntities = pluginDao.findAllTenantPluginsByTenantId(tenantId.getId(), pageLink);
        List<PluginMetaData> plugins = convertDataList(pluginsEntities);
        return new TextPageData<>(plugins, pageLink);
    }

    @Override
    public List<PluginMetaData> findAllTenantPluginsByTenantId(TenantId tenantId) {
        log.trace("Executing findAllTenantPluginsByTenantId, tenantId [{}]", tenantId);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        List<PluginMetaData> plugins = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(300);
        TextPageData<PluginMetaData> pageData = null;
        do {
            pageData = findAllTenantPluginsByTenantIdAndPageLink(tenantId, pageLink);
            plugins.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        return plugins;
    }

    @Override
    public void activatePluginById(PluginId pluginId) {
        updateLifeCycleState(pluginId, ComponentLifecycleState.ACTIVE);
    }

    @Override
    public void suspendPluginById(PluginId pluginId) {
        PluginMetaDataEntity plugin = pluginDao.findById(pluginId);
        List<RuleMetaDataEntity> affectedRules = ruleDao.findRulesByPlugin(plugin.getApiToken())
                .stream().filter(rule -> rule.getState() == ComponentLifecycleState.ACTIVE).collect(Collectors.toList());
        if (affectedRules.isEmpty()) {
            updateLifeCycleState(pluginId, ComponentLifecycleState.SUSPENDED);
        } else {
            throw new DataValidationException("Can't suspend plugin that has active rules!");
        }
    }

    private void updateLifeCycleState(PluginId pluginId, ComponentLifecycleState state) {
        Validator.validateId(pluginId, "Incorrect plugin id for state change request.");
        PluginMetaDataEntity plugin = pluginDao.findById(pluginId);
        if (plugin != null) {
            plugin.setState(state);
            pluginDao.save(plugin);
        } else {
            throw new DatabaseException("Plugin not found!");
        }
    }

    @Override
    public void deletePluginById(PluginId pluginId) {
        Validator.validateId(pluginId, "Incorrect plugin id for delete request.");
        deleteEntityRelations(pluginId);
        checkRulesAndDelete(pluginId.getId());
    }

    private void checkRulesAndDelete(UUID pluginId) {
        PluginMetaDataEntity plugin = pluginDao.findById(pluginId);
        List<RuleMetaDataEntity> affectedRules = ruleDao.findRulesByPlugin(plugin.getApiToken());
        if (affectedRules.isEmpty()) {
            pluginDao.deleteById(pluginId);
        } else {
            throw new DataValidationException("Plugin deletion will affect existing rules!");
        }
    }

    @Override
    public void deletePluginsByTenantId(TenantId tenantId) {
        Validator.validateId(tenantId, "Incorrect tenant id for delete plugins request.");
        tenantPluginRemover.removeEntitites(tenantId);
    }


    private DataValidator<PluginMetaData> pluginValidator =
            new DataValidator<PluginMetaData>() {
                @Override
                protected void validateDataImpl(PluginMetaData plugin) {
                    if (StringUtils.isEmpty(plugin.getName())) {
                        throw new DataValidationException("Plugin name should be specified!.");
                    }
                    if (StringUtils.isEmpty(plugin.getClazz())) {
                        throw new DataValidationException("Plugin clazz should be specified!.");
                    }
                    if (StringUtils.isEmpty(plugin.getApiToken())) {
                        throw new DataValidationException("Plugin api token is not set!");
                    }
                    if (plugin.getConfiguration() == null) {
                        throw new DataValidationException("Plugin configuration is not set!");
                    }
                }
            };

    private PaginatedRemover<TenantId, PluginMetaDataEntity> tenantPluginRemover =
            new PaginatedRemover<TenantId, PluginMetaDataEntity>() {

                @Override
                protected List<PluginMetaDataEntity> findEntities(TenantId id, TextPageLink pageLink) {
                    return pluginDao.findByTenantIdAndPageLink(id, pageLink);
                }

                @Override
                protected void removeEntity(PluginMetaDataEntity entity) {
                    checkRulesAndDelete(entity.getId());
                }
            };
}
