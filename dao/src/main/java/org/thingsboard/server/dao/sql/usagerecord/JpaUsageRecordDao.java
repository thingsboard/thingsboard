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
package org.thingsboard.server.dao.sql.usagerecord;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UsageRecord;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UsageRecordEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.usagerecord.UsageRecordDao;

import java.util.UUID;

/**
 * @author Andrii Shvaika
 */
@Component
public class JpaUsageRecordDao extends JpaAbstractDao<UsageRecordEntity, UsageRecord> implements UsageRecordDao {

    private final UsageRecordRepository usageRecordRepository;

    public JpaUsageRecordDao(UsageRecordRepository usageRecordRepository) {
        this.usageRecordRepository = usageRecordRepository;
    }

    @Override
    protected Class<UsageRecordEntity> getEntityClass() {
        return UsageRecordEntity.class;
    }

    @Override
    protected CrudRepository<UsageRecordEntity, UUID> getCrudRepository() {
        return usageRecordRepository;
    }

    @Override
    public UsageRecord findTenantUsageRecord(UUID tenantId) {
        return DaoUtil.getData(usageRecordRepository.findByTenantId(tenantId));
    }

    @Override
    public void deleteUsageRecordsByTenantId(TenantId tenantId) {
        usageRecordRepository.deleteUsageRecordsByTenantId(tenantId.getId());
    }
}
