// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.custommenu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomMenuId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMScope;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuFilter;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.menu.CustomMenuDao;
import org.thingsboard.server.dao.model.sql.CustomMenuEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;


@Component
@Slf4j
@SqlDao
public class JpaCustomMenuDao extends JpaAbstractDao<CustomMenuEntity, CustomMenu> implements CustomMenuDao {

    @Autowired
    private CustomMenuRepository customMenuRepository;

    @Autowired
    private CustomMenuInfoRepository customMenuInfoRepository;

    @Override
    public CustomMenuInfo findInfoById(CustomMenuId customMenuId) {
        return DaoUtil.getData(customMenuInfoRepository.findById(customMenuId.getId()));
    }

    @Override
    public PageData<CustomMenuInfo> findInfosByFilter(TenantId tenantId, CustomMenuFilter customMenuFilter, PageLink pageLink) {
        return DaoUtil.toPageData(customMenuInfoRepository.findByTenantIdAndCustomerIdAndScopeAndAssigneeType(customMenuFilter.getTenantId().getId(),
                customMenuFilter.getCustomerId() == null ? EntityId.NULL_UUID : customMenuFilter.getCustomerId().getId(),
                customMenuFilter.getScope(), customMenuFilter.getAssigneeType(), pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public CustomMenu findDefaultMenuByScope(TenantId tenantId, CustomerId customerId, CMScope scope) {
        return DaoUtil.getData(customMenuRepository.findDefaultByTenantIdAndCustomerIdAndScope(tenantId.getId(),
                customerId == null ? EntityId.NULL_UUID : customerId.getId(), scope));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        customMenuRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public PageData<CustomMenu> findByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customMenuRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Optional<CustomMenu> findFirstByScopeAndUserGroupNames(TenantId tenantId, CustomerId customerId, CMScope scope, Set<String> userGroupNames) {
        return customMenuRepository.findFirstByScopeAndUserGroupNames(tenantId.getId(),
                customerId == null ? EntityId.NULL_UUID : customerId.getId(), scope.name(), userGroupNames.toArray(new String[0])).map(DaoUtil::getData);
    }

    @Override
    protected Class<CustomMenuEntity> getEntityClass() {
        return CustomMenuEntity.class;
    }

    @Override
    protected JpaRepository<CustomMenuEntity, UUID> getRepository() {
        return customMenuRepository;
    }

}
