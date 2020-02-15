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
package org.thingsboard.server.dao.device;

import com.datastax.driver.core.querybuilder.Select;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.ProvisionProfileEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.PROVISION_PROFILE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME;
import static org.thingsboard.server.dao.model.ModelConstants.PROVISION_PROFILE_TENANT_ID_PROPERTY;

@Component
@Slf4j
@NoSqlDao
public class CassandraProvisionProfileDao extends CassandraAbstractSearchTextDao<ProvisionProfileEntity, ProvisionProfile> implements ProvisionProfileDao {

    @Override
    public ProvisionProfile findByKey(TenantId tenantId, String key) {
        log.debug("Try to find provision profile by key [{}]", key);
        Select.Where query = select().from(ModelConstants.PROVISION_PROFILE_BY_KEY_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.PROVISION_PROFILE_KEY_PROPERTY, key));
        log.trace("Execute query {}", query);
        ProvisionProfileEntity provisionProfileEntity = findOneByStatement(tenantId, query);
        log.trace("Found provision profile [{}] by key [{}]", provisionProfileEntity, key);
        return DaoUtil.getData(provisionProfileEntity);
    }

    @Override
    public List<ProvisionProfile> findProfilesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find profiles by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<ProvisionProfileEntity> profileEntities = findPageWithTextSearch(new TenantId(tenantId), PROVISION_PROFILE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(PROVISION_PROFILE_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found profiles [{}] by tenantId [{}] and pageLink [{}]", profileEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(profileEntities);
    }

    @Override
    protected Class<ProvisionProfileEntity> getColumnFamilyClass() {
        return ProvisionProfileEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.PROVISION_PROFILE_COLUMN_FAMILY_NAME;
    }
}
