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
package org.thingsboard.server.extensions.api.plugins.msg;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serializable;
import java.util.UUID;

public abstract class AbstractRuleToPluginMsg<T extends Serializable> implements RuleToPluginMsg<T> {

    private static final long serialVersionUID = 1L;

    private final UUID uid;
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final DeviceId deviceId;
    private final T payload;

    public AbstractRuleToPluginMsg(TenantId tenantId, CustomerId customerId, DeviceId deviceId, T payload) {
        super();
        this.uid = UUID.randomUUID();
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.deviceId = deviceId;
        this.payload = payload;
    }

    @Override
    public UUID getUid() {
        return uid;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    @Override
    public DeviceId getDeviceId() {
        return deviceId;
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "AbstractRuleToPluginMsg [uid=" + uid + ", tenantId=" + tenantId + ", customerId=" + customerId
                + ", deviceId=" + deviceId + ", payload=" + payload + "]";
    }

}
