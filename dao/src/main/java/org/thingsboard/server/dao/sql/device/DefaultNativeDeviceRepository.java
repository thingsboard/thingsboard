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
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.UUID;

@Repository
@Slf4j
public class DefaultNativeDeviceRepository extends AbstractNativeRepository implements NativeDeviceRepository {

    private final String COUNT_QUERY = "SELECT count(id) FROM device;";

    public DefaultNativeDeviceRepository(NamedParameterJdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        super(jdbcTemplate, transactionTemplate);
    }

    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(Pageable pageable) {
        String DEVICE_ID_INFO_QUERY = "SELECT tenant_id as tenantId, customer_id as customerId, id as id FROM device ORDER BY created_time ASC LIMIT %s OFFSET %s";
        return find(COUNT_QUERY, DEVICE_ID_INFO_QUERY, pageable, row -> {
            UUID id = (UUID) row.get("id");
            var tenantIdObj = row.get("tenantId");
            var customerIdObj = row.get("customerId");
            return new DeviceIdInfo(tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId(), customerIdObj != null ? (UUID) customerIdObj : null, id);
        });
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(Pageable pageable) {
        String PROFILE_DEVICE_ID_INFO_QUERY = "SELECT tenant_id as tenantId, device_profile_id as profileId, id as id FROM device ORDER BY created_time ASC LIMIT %s OFFSET %s";
        return find(COUNT_QUERY, PROFILE_DEVICE_ID_INFO_QUERY, pageable, row -> {
            DeviceId id = new DeviceId((UUID) row.get("id"));
            DeviceProfileId profileId = new DeviceProfileId((UUID) row.get("profileId"));
            var tenantIdObj = row.get("tenantId");
            return ProfileEntityIdInfo.create(tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId(), profileId, id);
        });
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(UUID tenantId, Pageable pageable) {
        String PROFILE_DEVICE_ID_INFO_QUERY = String.format("SELECT tenant_id as tenantId, device_profile_id as profileId, id as id FROM device WHERE tenant_id = '%s' ORDER BY created_time ASC LIMIT %%s OFFSET %%s", tenantId);
        return find(COUNT_QUERY, PROFILE_DEVICE_ID_INFO_QUERY, pageable, row -> {
            DeviceId id = new DeviceId((UUID) row.get("id"));
            DeviceProfileId profileId = new DeviceProfileId((UUID) row.get("profileId"));
            var tenantIdObj = row.get("tenantId");
            return ProfileEntityIdInfo.create(tenantIdObj != null ? (UUID) tenantIdObj : TenantId.SYS_TENANT_ID.getId(), profileId, id);
        });
    }

}
