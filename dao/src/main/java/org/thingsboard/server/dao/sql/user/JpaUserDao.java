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
package org.thingsboard.server.dao.sql.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.annotation.SqlDao;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.user.UserDao;

import java.util.List;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * @author Valerii Sosliuk
 */
@Component
@SqlDao
public class JpaUserDao extends JpaAbstractDao<UserEntity, User> implements UserDao {

    @Autowired
    private UserRepository userRepository;

    @Override
    protected Class<UserEntity> getEntityClass() {
        return UserEntity.class;
    }

    @Override
    protected CrudRepository<UserEntity, UUID> getCrudRepository() {
        return userRepository;
    }

    @Override
    public User findByEmail(String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    @Override
    public List<User> findTenantAdmins(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findTenantAdmins(
                                pageLink.getLimit(),
                                tenantId,
                                pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }

    @Override
    public List<User> findCustomerUsers(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                userRepository
                        .findCustomerUsers(
                                pageLink.getLimit(),
                                tenantId,
                                customerId,
                                pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset()));
    }
}
