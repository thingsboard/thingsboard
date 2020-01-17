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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.UserCredentialsEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractModelDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import java.util.UUID;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

@Component
@Slf4j
@NoSqlDao
public class CassandraUserCredentialsDao extends CassandraAbstractModelDao<UserCredentialsEntity, UserCredentials> implements UserCredentialsDao {

    public static final String EXECUTE_QUERY = "Execute query {}";

    @Override
    protected Class<UserCredentialsEntity> getColumnFamilyClass() {
        return UserCredentialsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.USER_CREDENTIALS_COLUMN_FAMILY_NAME;
    }

    @Override
    public UserCredentials findByUserId(TenantId tenantId, UUID userId) {
        log.debug("Try to find user credentials by userId [{}] ", userId);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_USER_COLUMN_FAMILY_NAME).where(eq(ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY, userId));
        log.trace(EXECUTE_QUERY, query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(tenantId, query);
        log.trace("Found user credentials [{}] by userId [{}]", userCredentialsEntity, userId);
        return DaoUtil.getData(userCredentialsEntity);
    }

    @Override
    public UserCredentials findByActivateToken(TenantId tenantId, String activateToken) {
        log.debug("Try to find user credentials by activateToken [{}] ", activateToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_ACTIVATE_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY, activateToken));
        log.trace(EXECUTE_QUERY, query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(tenantId, query);
        log.trace("Found user credentials [{}] by activateToken [{}]", userCredentialsEntity, activateToken);
        return DaoUtil.getData(userCredentialsEntity);
    }

    @Override
    public UserCredentials findByResetToken(TenantId tenantId, String resetToken) {
        log.debug("Try to find user credentials by resetToken [{}] ", resetToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_RESET_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY, resetToken));
        log.trace(EXECUTE_QUERY, query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(tenantId, query);
        log.trace("Found user credentials [{}] by resetToken [{}]", userCredentialsEntity, resetToken);
        return DaoUtil.getData(userCredentialsEntity);
    }

}
