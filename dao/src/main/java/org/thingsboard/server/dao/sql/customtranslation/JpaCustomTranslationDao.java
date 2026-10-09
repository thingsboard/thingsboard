// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.customtranslation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.translation.CustomTranslation;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.CustomTranslationCompositeKey;
import org.thingsboard.server.dao.model.sql.CustomTranslationEntity;
import org.thingsboard.server.dao.translation.CustomTranslationDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Set;
import java.util.UUID;


@Component
@Slf4j
@SqlDao
public class JpaCustomTranslationDao implements CustomTranslationDao, TenantEntityDao<CustomTranslation> {

    @Autowired
    private CustomTranslationRepository customTranslationRepository;

    @Override
    public CustomTranslation save(TenantId tenantId, CustomTranslation customTranslation) {
        return DaoUtil.getData(customTranslationRepository.save(new CustomTranslationEntity(customTranslation)));
    }

    @Override
    public CustomTranslation findById(TenantId tenantId, CustomTranslationCompositeKey key) {
        return DaoUtil.getData(customTranslationRepository.findById(key));
    }

    @Override
    public void removeById(TenantId tenantId, CustomTranslationCompositeKey key) {
        customTranslationRepository.deleteById(key);
    }

    @Override
    public Set<String> findLocalesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        return customTranslationRepository.findLocalesByTenantIdAndCustomerId(tenantId.getId(), customerId == null ? EntityId.NULL_UUID : customerId.getId());
    }

    @Override
    public List<CustomTranslationCompositeKey> findCustomTranslationByTenantId(UUID tenantId) {
        return customTranslationRepository.findByTenantId(tenantId);
    }

    @Override
    public PageData<CustomTranslation> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(customTranslationRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink, "tenantId", "customerId", "localeCode")));
    }

}
