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

import java.util.UUID;

import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.model.UserCredentialsEntity;

/**
 * The Interface UserCredentialsDao.
 *
 * @param <T> the generic type
 */
public interface UserCredentialsDao extends Dao<UserCredentialsEntity> {

    /**
     * Save or update user credentials object
     *
     * @param userCredentials the user credentials object
     * @return saved user credentials object
     */
    UserCredentialsEntity save(UserCredentials userCredentials);

    /**
     * Find user credentials by user id.
     *
     * @param userId the user id
     * @return the user credentials object
     */
    UserCredentialsEntity findByUserId(UUID userId);
    
    /**
     * Find user credentials by activate token.
     *
     * @param activateToken the activate token
     * @return the user credentials object
     */
    UserCredentialsEntity findByActivateToken(String activateToken);
    
    /**
     * Find user credentials by reset token.
     *
     * @param resetToken the reset token
     * @return the user credentials object
     */
    UserCredentialsEntity findByResetToken(String resetToken);
    
}
