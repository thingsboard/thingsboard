// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.component;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.ComponentDescriptorId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.plugin.ComponentDescriptor;
import org.thingsboard.server.common.data.plugin.ComponentScope;
import org.thingsboard.server.common.data.plugin.ComponentType;

/**
 * @author Andrew Shvayka
 */
public interface ComponentDescriptorService {

    ComponentDescriptor saveComponent(TenantId tenantId, ComponentDescriptor component);

    ComponentDescriptor findById(TenantId tenantId, ComponentDescriptorId componentId);

    ComponentDescriptor findByClazz(TenantId tenantId, String clazz);

    PageData<ComponentDescriptor> findByTypeAndPageLink(TenantId tenantId, ComponentType type, PageLink pageLink);

    PageData<ComponentDescriptor> findByScopeAndTypeAndPageLink(TenantId tenantId, ComponentScope scope, ComponentType type, PageLink pageLink);

    boolean validate(TenantId tenantId, ComponentDescriptor component, JsonNode configuration);

    void deleteByClazz(TenantId tenantId, String clazz);

}
