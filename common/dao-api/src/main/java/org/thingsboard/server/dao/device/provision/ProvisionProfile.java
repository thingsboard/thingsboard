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
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Objects;

@Data
public class ProvisionProfile implements HasTenantId {

    private ProvisionProfileCredentials credentials;
    private TenantId tenantId;
    private CustomerId customerId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProvisionProfile)) return false;
        ProvisionProfile profile = (ProvisionProfile) o;
        return getCredentials().equals(profile.getCredentials()) &&
                getTenantId().equals(profile.getTenantId()) &&
                Objects.equals(getCustomerId(), profile.getCustomerId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCredentials(), getTenantId(), getCustomerId());
    }
}
