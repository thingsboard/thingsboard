// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.blob;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.blob.BlobEntityInfoDao;
import org.thingsboard.server.dao.model.sql.BlobEntityInfoEntity;
import org.thingsboard.server.dao.model.sql.BlobEntityWithCustomerInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;


@Component
@SqlDao
public class JpaBlobEntityInfoDao extends JpaAbstractDao<BlobEntityInfoEntity, BlobEntityInfo> implements BlobEntityInfoDao {

    @Autowired
    BlobEntityInfoRepository blobEntityInfoRepository;

    @Override
    protected Class<BlobEntityInfoEntity> getEntityClass() {
        return BlobEntityInfoEntity.class;
    }

    @Override
    protected JpaRepository<BlobEntityInfoEntity, UUID> getRepository() {
        return blobEntityInfoRepository;
    }

    @Override
    public BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(UUID tenantId, UUID blobEntityId) {
        return DaoUtil.getData(blobEntityInfoRepository.findBlobEntityWithCustomerInfoById(blobEntityId));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(UUID tenantId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(UUID tenantId, String type, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantIdAndType(
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TimePageLink pageLink) {
        return DaoUtil.toPageData(
                blobEntityInfoRepository.findByTenantIdAndCustomerIdAndType(
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        pageLink.getStartTime(),
                        pageLink.getEndTime(),
                        DaoUtil.toPageable(pageLink, BlobEntityWithCustomerInfoEntity.blobEntityWithCustomerInfoColumnMap)));
    }

    @Override
    public ListenableFuture<List<BlobEntityInfo>> findBlobEntitiesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> blobEntityIds) {
        return service.submit(() -> DaoUtil.convertDataList(blobEntityInfoRepository.findBlobEntitiesByTenantIdAndIdIn(
                tenantId, blobEntityIds)));
    }
}
