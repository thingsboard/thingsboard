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
package org.thingsboard.server.dao.usagerecord;

import org.thingsboard.server.common.data.UsageRecord;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

public interface UsageRecordDao extends Dao<UsageRecord> {

    /**
     * Save or update usage record object
     *
     * @param usageRecord the usage record
     * @return saved usage record entity
     */
    UsageRecord save(TenantId tenantId, UsageRecord usageRecord);

    /**
     * Find usage record by tenantId.
     *
     * @param tenantId the tenantId
     * @return the corresponding usage record
     */
    UsageRecord findTenantUsageRecord(UUID tenantId);

    /**
     * Delete usage record by tenantId.
     *
     * @param tenantId the tenantId
     */
    void deleteUsageRecordsByTenantId(TenantId tenantId);
}
