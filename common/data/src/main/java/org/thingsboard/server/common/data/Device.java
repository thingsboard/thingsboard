/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
public class Device extends SearchTextBasedWithAdditionalInfo<DeviceId> implements HasName, HasTenantId, HasMultipleCustomers {

    private static final long serialVersionUID = 2807343040519543363L;

    private TenantId tenantId;
    private String name;
    private String type;
    @Getter @Setter
    private Set<ShortCustomerInfo> assignedCustomers = new HashSet<>();

    public Device() {
        super();
    }

    public Device(DeviceId id) {
        super(id);
    }

    public Device(Device device) {
        super(device);
        this.tenantId = device.getTenantId();
        setAssignedCustomers(device.getAssignedCustomers());
        this.name = device.getName();
        this.type = device.getType();
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public void setTenantId(TenantId tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getSearchText() {
        return getName();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Device [tenantId=");
        builder.append(tenantId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", type=");
        builder.append(type);
        builder.append(", additionalInfo=");
        builder.append(getAdditionalInfo());
        builder.append(", createdTime=");
        builder.append(createdTime);
        builder.append(", id=");
        builder.append(id);
        builder.append("]");
        return builder.toString();
    }

}
