// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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
import org.thingsboard.server.dao.sql.KeysetPages;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Repository
@Slf4j
public class DefaultNativeAssetRepository extends AbstractNativeRepository implements NativeAssetRepository {

    private final String COUNT_QUERY = "SELECT count(id) FROM asset;";
    // keyset pages over the (tenant_id, id) index, see KeysetPages
    private static final String PROFILE_ASSET_ID_INFO_SELECT =
            "SELECT tenant_id as tenantId, customer_id as customerId, asset_profile_id as profileId, id as id FROM asset WHERE tenant_id = :tenantId ";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public DefaultNativeAssetRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        super(jdbcTemplate, transactionTemplate);
        this.jdbcTemplate = jdbcTemplate;
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

    @Override
    public void findProfileEntityIdInfosByTenantId(UUID tenantId, int pageSize, Consumer<List<ProfileEntityIdInfo>> pageConsumer) {
        KeysetPages.forEachPage(jdbcTemplate, PROFILE_ASSET_ID_INFO_SELECT + "ORDER BY id LIMIT :limit",
                PROFILE_ASSET_ID_INFO_SELECT + "AND id > :lastId ORDER BY id LIMIT :limit",
                Map.of("tenantId", tenantId), pageSize, DefaultNativeAssetRepository::toInfo, pageConsumer);
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
