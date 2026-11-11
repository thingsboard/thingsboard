// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.UserInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.UserEntity;
import org.thingsboard.server.dao.model.sql.UserInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.user.UserInfoDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaUserInfoDao extends JpaAbstractDao<UserInfoEntity, UserInfo> implements UserInfoDao {

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override
    protected Class<UserInfoEntity> getEntityClass() {
        return UserInfoEntity.class;
    }

    @Override
    protected JpaRepository<UserInfoEntity, UUID> getRepository() {
        return userInfoRepository;
    }

    @Override
    public PageData<UserInfo> findUsersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<UserInfo> findTenantUsersByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findTenantUsersByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<UserInfo> findUsersByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }

    @Override
    public PageData<UserInfo> findUsersByTenantIdAndCustomerIdIncludingSubCustomers(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(userInfoRepository
                .findByTenantIdAndCustomerIdIncludingSubCustomers(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink, UserEntity.userColumnMap)));
    }
}
