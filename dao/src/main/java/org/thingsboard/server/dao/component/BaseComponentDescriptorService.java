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
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;

import java.util.Optional;
import java.util.Set;

/**
 * @author Andrew Shvayka
 */
@Service
@Slf4j
public class BaseComponentDescriptorService implements ComponentDescriptorService {

    @Autowired
    private ComponentDescriptorDao componentDescriptorDao;

    @Autowired
    private DataValidator<ComponentDescriptor> componentValidator;

    @Override
    public ComponentDescriptor saveComponent(TenantId tenantId, ComponentDescriptor component) {
        componentValidator.validate(component, data -> TenantId.SYS_TENANT_ID);
        Optional<ComponentDescriptor> result = componentDescriptorDao.saveIfNotExist(tenantId, component);
        return result.orElseGet(() -> componentDescriptorDao.findByClazz(tenantId, component.getClazz()));
    }

    @Override
    public ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId) {
        Validator.validateId(componentId, "Incorrect component id for search request.");
        return componentDescriptorDao.findById(tenantId, componentId);
    }

    @Override
    public ComponentDescriptor findByClazz(TenantId tenantId, String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for search request.");
        return componentDescriptorDao.findByClazz(tenantId, clazz);
    }

    @Override
    public PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByTypeAndPageLink(tenantId, type, pageLink);
    }

    @Override
    public PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, PageLink pageLink) {
        Validator.validatePageLink(pageLink);
        return componentDescriptorDao.findByScopeAndTypeAndPageLink(tenantId, scope, type, pageLink);
    }

    @Override
    public void deleteByClazz(TenantId tenantId, String clazz) {
        Validator.validateString(clazz, "Incorrect clazz for delete request.");
        componentDescriptorDao.deleteByClazz(tenantId, clazz);
    }

    @Override
    public boolean validate(TenantId tenantId, ComponentDescriptor component, JsonNode configuration) {
        try {
            if (!component.getConfigurationDescriptor().has("schema")) {
                throw new DataValidationException("Configuration descriptor doesn't contain schema property!");
            }
            JsonNode configurationSchema = component.getConfigurationDescriptor().get("schema");

            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4);
            JsonSchema schema = factory.getSchema(configurationSchema);

            Set<ValidationMessage> validationMessages = schema.validate(configuration);
            return validationMessages.isEmpty();
        } catch (Exception e) {
            throw new IncorrectParameterException(e.getMessage(), e);
        }
    }
}
