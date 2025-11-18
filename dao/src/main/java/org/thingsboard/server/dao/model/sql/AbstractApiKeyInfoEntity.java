/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.ApiKeyId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.pat.ApiKeyInfo;
import org.thingsboard.server.dao.model.BaseEntity;
import org.thingsboard.server.dao.model.BaseSqlEntity;

import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_DESCRIPTION_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_ENABLED_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_EXPIRATION_TIME_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_TENANT_ID_COLUMN_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.API_KEY_USER_ID_COLUMN_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@MappedSuperclass
public abstract class AbstractApiKeyInfoEntity<T extends ApiKeyInfo> extends BaseSqlEntity<T> implements BaseEntity<T> {

    @Column(name = API_KEY_TENANT_ID_COLUMN_NAME)
    private UUID tenantId;

    @Column(name = API_KEY_USER_ID_COLUMN_NAME)
    private UUID userId;

    @Column(name = API_KEY_EXPIRATION_TIME_COLUMN_NAME)
    private long expirationTime;

    @Column(name = API_KEY_ENABLED_COLUMN_NAME)
    private boolean enabled;

    @Column(name = API_KEY_DESCRIPTION_COLUMN_NAME)
    private String description;

    public AbstractApiKeyInfoEntity() {
        super();
    }

    public AbstractApiKeyInfoEntity(ApiKeyInfo apiKeyInfo) {
        super(apiKeyInfo);
        this.tenantId = apiKeyInfo.getTenantId().getId();
        this.userId = apiKeyInfo.getUserId().getId();
        this.expirationTime = apiKeyInfo.getExpirationTime();
        this.description = apiKeyInfo.getDescription();
        this.enabled = apiKeyInfo.isEnabled();
    }

    protected ApiKeyInfo toApiKeyInfo() {
        ApiKeyInfo apiKeyInfo = new ApiKeyInfo(new ApiKeyId(getUuid()));
        apiKeyInfo.setCreatedTime(createdTime);
        apiKeyInfo.setTenantId(TenantId.fromUUID(tenantId));
        apiKeyInfo.setUserId(new UserId(userId));
        apiKeyInfo.setEnabled(enabled);
        apiKeyInfo.setExpirationTime(expirationTime);
        apiKeyInfo.setDescription(description);
        return apiKeyInfo;
    }

}
