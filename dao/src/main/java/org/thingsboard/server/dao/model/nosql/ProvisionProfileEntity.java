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
package org.thingsboard.server.dao.model.nosql;

import com.datastax.driver.core.utils.UUIDs;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;
import org.thingsboard.server.dao.device.provision.ProvisionProfileCredentials;
import org.thingsboard.server.dao.device.provision.ProvisionRequestValidationStrategy;
import org.thingsboard.server.dao.device.provision.ProvisionRequestValidationStrategyType;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.SearchTextEntity;
import org.thingsboard.server.dao.model.type.JsonCodec;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.PROVISION_PROFILE_ADDITIONAL_INFO_PROPERTY;
import static org.thingsboard.server.dao.model.ModelConstants.SEARCH_TEXT_PROPERTY;

@Table(name = ModelConstants.PROVISION_PROFILE_COLUMN_FAMILY_NAME)
@EqualsAndHashCode
@ToString
public final class ProvisionProfileEntity implements SearchTextEntity<ProvisionProfile> {

    @PartitionKey(value = 0)
    @Column(name = ModelConstants.ID_PROPERTY)
    private UUID id;

    @Column(name = ModelConstants.PROVISION_PROFILE_TENANT_ID_PROPERTY)
    private UUID tenantId;

    @Column(name = ModelConstants.PROVISION_PROFILE_CUSTOMER_ID_PROPERTY)
    private UUID customerId;

    @Column(name = ModelConstants.PROVISION_PROFILE_KEY_PROPERTY)
    private String key;

    @Column(name = ModelConstants.PROVISION_PROFILE_SECRET_PROPERTY)
    private String secret;

    @Column(name = ModelConstants.PROVISION_PROFILE_VALIDATION_STRATEGY_TYPE)
    private String provisionRequestValidationStrategyType;

    @Column(name = SEARCH_TEXT_PROPERTY)
    private String searchText;

    @Column(name = PROVISION_PROFILE_ADDITIONAL_INFO_PROPERTY, codec = JsonCodec.class)
    private JsonNode additionalInfo;

    public ProvisionProfileEntity() {
        super();
    }

    public ProvisionProfileEntity(ProvisionProfile profile) {
        if (profile.getId() != null) {
            this.id = profile.getId().getId();
        }
        if (profile.getTenantId() != null) {
            this.tenantId = profile.getTenantId().getId();
        }
        if (profile.getCustomerId() != null) {
            this.customerId = profile.getCustomerId().getId();
        }
        this.key = profile.getCredentials().getProvisionProfileKey();
        this.secret = profile.getCredentials().getProvisionProfileSecret();
        this.provisionRequestValidationStrategyType = profile.getStrategy().getValidationStrategyType().name();
        this.additionalInfo = profile.getAdditionalInfo();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    public ProvisionProfile toData() {
        ProvisionProfile profile = new ProvisionProfile(new ProvisionProfileId(id));
        profile.setCreatedTime(UUIDs.unixTimestamp(id));
        if (tenantId != null) {
            profile.setTenantId(new TenantId(tenantId));
        }
        if (customerId != null) {
            profile.setCustomerId(new CustomerId(customerId));
        }
        profile.setCredentials(new ProvisionProfileCredentials(key, secret));
        profile.setStrategy(new ProvisionRequestValidationStrategy(
                ProvisionRequestValidationStrategyType.valueOf(provisionRequestValidationStrategyType)));
        profile.setAdditionalInfo(additionalInfo);
        return profile;
    }

    @Override
    public String getSearchTextSource() {
        return key;
    }

    @Override
    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public String getSearchText() {
        return searchText;
    }

}
