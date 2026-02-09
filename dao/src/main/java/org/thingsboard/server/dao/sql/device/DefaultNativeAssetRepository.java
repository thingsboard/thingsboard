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
package org.thingsboard.server.dao.sql.device;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.Map;
import java.util.UUID;

@Repository
@Slf4j
public class DefaultNativeAssetRepository extends AbstractNativeRepository implements NativeAssetRepository {

    private final String COUNT_QUERY = "SELECT count(id) FROM asset;";

    public DefaultNativeAssetRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        super(jdbcTemplate, transactionTemplate);
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(Pageable pageable) {
        String PROFILE_ASSET_ID_INFO_QUERY = "SELECT tenant_id as tenantId, customer_id as customerId, asset_profile_id as profileId, id as id FROM asset ORDER BY created_time ASC LIMIT %s OFFSET %s";
        return find(COUNT_QUERY, PROFILE_ASSET_ID_INFO_QUERY, pageable, DefaultNativeAssetRepository::toInfo);
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, Pageable pageable) {
        String PROFILE_ASSET_ID_INFO_QUERY = String.format("SELECT tenant_id as tenantId, customer_id as customerId, asset_profile_id as profileId, id as id FROM asset WHERE tenant_id = '%s' ORDER BY created_time ASC LIMIT %%s OFFSET %%s", tenantId);
        String COUNT_QUERY_BY_TENANT = String.format("SELECT count(id) FROM asset WHERE tenant_id = '%s';", tenantId);
        return find(COUNT_QUERY_BY_TENANT, PROFILE_ASSET_ID_INFO_QUERY, pageable, DefaultNativeAssetRepository::toInfo);
    }

    private static ProfileEntityIdInfo toInfo(Map<String, Object> row) {
        var tenantIdObj = row.get("tenantId");
        UUID tenantId = tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId();
        AssetId id = new AssetId((UUID) row.get("id"));
        CustomerId customerId = new CustomerId((UUID) row.get("customerId"));
        EntityId ownerId = !customerId.isNullUid() ? customerId : TenantId.fromUUID(tenantId);
        AssetProfileId profileId = new AssetProfileId((UUID) row.get("profileId"));
        return ProfileEntityIdInfo.create(tenantId, ownerId, profileId, id);
    }

}
