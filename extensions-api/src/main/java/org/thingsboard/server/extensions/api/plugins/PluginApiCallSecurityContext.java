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
package org.thingsboard.server.extensions.api.plugins;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;

public final class PluginApiCallSecurityContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TenantId pluginTenantId;
    private final PluginId pluginId;
    private final TenantId tenantId;
    private final CustomerId customerId;

    public PluginApiCallSecurityContext(TenantId pluginTenantId, PluginId pluginId, TenantId tenantId, CustomerId customerId) {
        super();
        this.pluginTenantId = pluginTenantId;
        this.pluginId = pluginId;
        this.tenantId = tenantId;
        this.customerId = customerId;
    }

    public TenantId getPluginTenantId(){
        return pluginTenantId;
    }

    public PluginId getPluginId() {
        return pluginId;
    }

    public boolean isSystemAdmin() {
        return tenantId == null || EntityId.NULL_UUID.equals(tenantId.getId());
    }

    public boolean isTenantAdmin() {
        return !isSystemAdmin() && (customerId == null || EntityId.NULL_UUID.equals(customerId.getId()));
    }

    public boolean isCustomerUser() {
        return !isSystemAdmin() && !isTenantAdmin();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

}
