// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.cf;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.cf.CalculatedFieldLinkDao;
import org.thingsboard.server.dao.model.sql.CalculatedFieldLinkEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaCalculatedFieldLinkDao extends JpaAbstractDao<CalculatedFieldLinkEntity, CalculatedFieldLink> implements CalculatedFieldLinkDao {

    private final CalculatedFieldLinkRepository calculatedFieldLinkRepository;
    private final NativeCalculatedFieldRepository nativeCalculatedFieldRepository;

    @Override
    public List<CalculatedFieldLink> findCalculatedFieldLinksByCalculatedFieldId(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAllByTenantIdAndCalculatedFieldId(tenantId.getId(), calculatedFieldId.getId()));
    }

    @Override
    public List<CalculatedFieldLink> findCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId) {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAllByTenantIdAndEntityId(tenantId.getId(), entityId.getId()));
    }

    @Override
    public List<CalculatedFieldLink> findCalculatedFieldLinksByTenantId(TenantId tenantId) {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAllByTenantId(tenantId.getId()));
    }

    @Override
    public List<CalculatedFieldLink> findAll() {
        return DaoUtil.convertDataList(calculatedFieldLinkRepository.findAll());
    }

    @Override
    public PageData<CalculatedFieldLink> findAll(PageLink pageLink) {
        log.debug("Try to find calculated field links by pageLink [{}]", pageLink);
        return nativeCalculatedFieldRepository.findCalculatedFieldLinks(DaoUtil.toPageable(pageLink));
    }

    @Override
    public PageData<CalculatedFieldLink> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        log.debug("Try to find calculated field links by tenantId [{}], pageLink [{}]", tenantId, pageLink);
        return DaoUtil.toPageData(calculatedFieldLinkRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    protected Class<CalculatedFieldLinkEntity> getEntityClass() {
        return CalculatedFieldLinkEntity.class;
    }

    @Override
    protected JpaRepository<CalculatedFieldLinkEntity, UUID> getRepository() {
        return calculatedFieldLinkRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD_LINK;
    }

}
