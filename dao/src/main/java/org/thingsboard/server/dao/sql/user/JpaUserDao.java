/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.user.UserDao;

import java.util.Objects;
import java.util.UUID;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * @author Valerii Sosliuk
 */
@Component
public class JpaUserDao extends JpaAbstractSearchTextDao<UserEntity, User> implements UserDao {

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
    public User findByEmail(TenantId tenantId, String email) {
        return DaoUtil.getData(userRepository.findByEmail(email));
    }

    @Override
    public PageData<User> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findByTenantId(
                                tenantId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findTenantAdmins(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                NULL_UUID,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.TENANT_ADMIN,
                                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<User> findCustomerUsers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(
                userRepository
                        .findUsersByAuthority(
                                tenantId,
                                customerId,
                                Objects.toString(pageLink.getTextSearch(), ""),
                                Authority.CUSTOMER_USER,
                                DaoUtil.toPageable(pageLink)));

    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return userRepository.countByTenantId(tenantId.getId());
    }
}
