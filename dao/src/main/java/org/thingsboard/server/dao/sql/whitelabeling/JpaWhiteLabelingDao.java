// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.whitelabeling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;
import org.thingsboard.server.dao.model.sql.WhiteLabelingEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDaoListeningExecutorService;
import org.thingsboard.server.dao.util.SqlDao;
import org.thingsboard.server.dao.wl.WhiteLabelingDao;

import java.util.List;
import java.util.Set;

@Component
@Slf4j
@SqlDao
public class JpaWhiteLabelingDao extends JpaAbstractDaoListeningExecutorService implements WhiteLabelingDao {

    @Autowired
    private WhiteLabelingRepository whiteLabelingRepository;

    @Override
    public WhiteLabeling save(TenantId tenantId, WhiteLabeling whiteLabeling) {
        return DaoUtil.getData(whiteLabelingRepository.save(new WhiteLabelingEntity(whiteLabeling)));
    }

    @Override
    public WhiteLabeling findById(TenantId tenantId, WhiteLabelingCompositeKey key) {
        return DaoUtil.getData(whiteLabelingRepository.findById(key));
    }

    @Override
    public WhiteLabeling findByDomainAndType(TenantId tenantId, String domain, WhiteLabelingType type) {
        return DaoUtil.getData(whiteLabelingRepository.findByDomainAndType(domain, type));
    }

    @Override
    public void removeById(TenantId tenantId, WhiteLabelingCompositeKey key) {
        whiteLabelingRepository.deleteById(key);
    }

    @Override
    public List<WhiteLabeling> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return DaoUtil.convertDataList(whiteLabelingRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, limit));
    }

    @Override
    public List<WhiteLabeling> findByImageLink(String imageLink, int limit) {
        return DaoUtil.convertDataList(whiteLabelingRepository.findByImageLink(imageLink, limit));
    }

    @Override
    public PageData<WhiteLabeling> findAllByType(PageLink pageLink, Set<WhiteLabelingType> types) {
        return DaoUtil.toPageData(whiteLabelingRepository.findAllByTypeIn(types, DaoUtil.toPageable(pageLink, List.of(
                new SortOrder("tenantId"), new SortOrder("customerId"), new SortOrder("type")
        ))));
    }

    @Override
    public PageData<WhiteLabeling> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(whiteLabelingRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink, "tenantId", "customerId", "type")));
    }

}
