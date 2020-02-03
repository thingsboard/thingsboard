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
package org.thingsboard.server.dao.model.sql;

import com.datastax.driver.core.utils.UUIDs;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;
import org.thingsboard.server.dao.device.provision.ProvisionProfileCredentials;
import org.thingsboard.server.dao.model.BaseSqlEntity;
import org.thingsboard.server.dao.model.ModelConstants;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = ModelConstants.PROVISION_PROFILE_COLUMN_FAMILY_NAME)
public final class ProvisionProfileEntity extends BaseSqlEntity<ProvisionProfile> {

    @Column(name = ModelConstants.PROVISION_PROFILE_TENANT_ID_PROPERTY)
    private String tenantId;

    @Column(name = ModelConstants.PROVISION_PROFILE_CUSTOMER_ID_PROPERTY)
    private String customerId;

    @Column(name = ModelConstants.PROVISION_PROFILE_KEY_PROPERTY)
    private String key;

    @Column(name = ModelConstants.PROVISION_PROFILE_SECRET_PROPERTY)
    private String secret;

    @Column(name = ModelConstants.PROVISION_PROFILE_PRE_PROVISION_ALLOWED_PROPERTY)
    private boolean preProvisionAllowed;

    public ProvisionProfileEntity() {
        super();
    }

    public ProvisionProfileEntity(ProvisionProfile profile) {
        if (profile.getId() != null) {
            this.setId(profile.getId().getId());
        }
        if (profile.getTenantId() != null) {
            this.tenantId = toString(profile.getTenantId().getId());
        }
        if (profile.getCustomerId() != null) {
            this.customerId = toString(profile.getCustomerId().getId());
        }
        this.key = profile.getCredentials().getProvisionProfileKey();
        this.secret = profile.getCredentials().getProvisionProfileSecret();
        this.preProvisionAllowed = profile.isPreProvisionAllowed();
    }

    @Override
    public ProvisionProfile toData() {
        ProvisionProfile profile = new ProvisionProfile(new ProvisionProfileId(getId()));
        profile.setCreatedTime(UUIDs.unixTimestamp(getId()));
        if (tenantId != null) {
            profile.setTenantId(new TenantId(toUUID(tenantId)));
        }
        if (customerId != null) {
            profile.setCustomerId(new CustomerId(toUUID(customerId)));
        }
        profile.setCredentials(new ProvisionProfileCredentials(key, secret));
        profile.setPreProvisionAllowed(preProvisionAllowed);
        return profile;
    }
}
