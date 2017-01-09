/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.AbstractModelDao;
import org.thingsboard.server.dao.model.UserCredentialsEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.datastax.driver.core.querybuilder.Select.Where;
import org.thingsboard.server.dao.model.ModelConstants;

@Component
@Slf4j
public class UserCredentialsDaoImpl extends AbstractModelDao<UserCredentialsEntity> implements UserCredentialsDao {

    @Override
    protected Class<UserCredentialsEntity> getColumnFamilyClass() {
        return UserCredentialsEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return ModelConstants.USER_CREDENTIALS_COLUMN_FAMILY_NAME;
    }

    @Override
    public UserCredentialsEntity save(UserCredentials userCredentials) {
        log.debug("Save user credentials [{}] ", userCredentials);
        return save(new UserCredentialsEntity(userCredentials));
    }

    @Override
    public UserCredentialsEntity findByUserId(UUID userId) {
        log.debug("Try to find user credentials by userId [{}] ", userId);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_USER_COLUMN_FAMILY_NAME).where(eq(ModelConstants.USER_CREDENTIALS_USER_ID_PROPERTY, userId));
        log.trace("Execute query {}", query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by userId [{}]", userCredentialsEntity, userId);
        return userCredentialsEntity;
    }

    @Override
    public UserCredentialsEntity findByActivateToken(String activateToken) {
        log.debug("Try to find user credentials by activateToken [{}] ", activateToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_ACTIVATE_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY, activateToken));
        log.trace("Execute query {}", query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by activateToken [{}]", userCredentialsEntity, activateToken);
        return userCredentialsEntity;
    }

    @Override
    public UserCredentialsEntity findByResetToken(String resetToken) {
        log.debug("Try to find user credentials by resetToken [{}] ", resetToken);
        Where query = select().from(ModelConstants.USER_CREDENTIALS_BY_RESET_TOKEN_COLUMN_FAMILY_NAME)
                .where(eq(ModelConstants.USER_CREDENTIALS_RESET_TOKEN_PROPERTY, resetToken));
        log.trace("Execute query {}", query);
        UserCredentialsEntity userCredentialsEntity = findOneByStatement(query);
        log.trace("Found user credentials [{}] by resetToken [{}]", userCredentialsEntity, resetToken);
        return userCredentialsEntity;
    }

}
