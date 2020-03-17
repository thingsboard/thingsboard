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
package org.thingsboard.server.dao.user;

import com.datastax.driver.core.querybuilder.Select.Where;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.UserEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraUserDao extends CassandraAbstractSearchTextDao<UserEntity, User> implements UserDao {

    @Override
    protected Class<UserEntity> getColumnFamilyClass() {
        return UserEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.USER_COLUMN_FAMILY_NAME;
    }

    @Override
    public User findByEmail(TenantId tenantId, String email) {
        log.debug("Try to find user by email [{}] ", email);
        Where query = select().from(ModelConstants.USER_BY_EMAIL_COLUMN_FAMILY_NAME).where(eq(ModelConstants.USER_EMAIL_PROPERTY, email));
        log.trace("Execute query {}", query);
        UserEntity userEntity = findOneByStatement(tenantId, query);
        log.trace("Found user [{}] by email [{}]", userEntity, email);
        return DaoUtil.getData(userEntity);
    }

    @Override
    public List<User> findTenantAdmins(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find tenant admin users by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<UserEntity> userEntities = findPageWithTextSearch(new TenantId(tenantId),
                ModelConstants.USER_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.USER_TENANT_ID_PROPERTY, tenantId),
                              eq(ModelConstants.USER_CUSTOMER_ID_PROPERTY, ModelConstants.NULL_UUID),
                              eq(ModelConstants.USER_AUTHORITY_PROPERTY, Authority.TENANT_ADMIN.name())),
                pageLink); 
        log.trace("Found tenant admin users [{}] by tenantId [{}] and pageLink [{}]", userEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(userEntities);
    }

    @Override
    public List<User> findCustomerUsers(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find customer users by tenantId [{}], customerId [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<UserEntity> userEntities = findPageWithTextSearch(new TenantId(tenantId),
                ModelConstants.USER_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(ModelConstants.USER_TENANT_ID_PROPERTY, tenantId),
                              eq(ModelConstants.USER_CUSTOMER_ID_PROPERTY, customerId),
                              eq(ModelConstants.USER_AUTHORITY_PROPERTY, Authority.CUSTOMER_USER.name())),
                pageLink); 
        log.trace("Found customer users [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", userEntities, tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(userEntities);
    }

}
