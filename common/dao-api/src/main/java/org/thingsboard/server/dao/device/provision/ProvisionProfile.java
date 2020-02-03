/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.device.provision;

import lombok.Data;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.HasCustomerId;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;

@Data
public class ProvisionProfile extends BaseData<ProvisionProfileId> implements HasTenantId, HasCustomerId {

    private static final long serialVersionUID = 1869935044931680450L;

    private ProvisionProfileCredentials credentials;
    private TenantId tenantId;
    private CustomerId customerId;
    private boolean preProvisionAllowed;

    public ProvisionProfile() {
        super();
    }

    public ProvisionProfile(ProvisionProfileId id) {
        super(id);
    }

    public ProvisionProfile(ProvisionProfile profile) {
        super(profile);
        this.credentials = profile.getCredentials();
        this.tenantId = profile.getTenantId();
        this.customerId = profile.getCustomerId();
        this.preProvisionAllowed = profile.isPreProvisionAllowed();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProvisionProfile)) return false;
        if (!super.equals(o)) return false;
        ProvisionProfile profile = (ProvisionProfile) o;
        return isPreProvisionAllowed() == profile.isPreProvisionAllowed() &&
                getCredentials().equals(profile.getCredentials()) &&
                getTenantId().equals(profile.getTenantId()) &&
                getCustomerId().equals(profile.getCustomerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getCredentials(), getTenantId(), getCustomerId(), isPreProvisionAllowed());
    }
}
