// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.blob;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.blob.BlobEntityInfo;
import org.thingsboard.server.common.data.blob.BlobEntityWithCustomerInfo;
import org.thingsboard.server.common.data.id.BlobEntityId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.TimePaginatedRemover;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Slf4j
@Service("BlobEntityDaoService")
public class BaseBlobEntityService extends AbstractEntityService implements BlobEntityService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_BLOB_ENTITY_ID = "Incorrect blobEntityId ";

    @Autowired
    private BlobEntityDao blobEntityDao;

    @Autowired
    private BlobEntityInfoDao blobEntityInfoDao;

    @Autowired
    private DataValidator<BlobEntity> blobEntityValidator;

    @Override
    public BlobEntity findBlobEntityById(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityById [{}]", blobEntityId);
        validateId(blobEntityId, id -> INCORRECT_BLOB_ENTITY_ID + id);
        return blobEntityDao.findById(tenantId, blobEntityId.getId());
    }

    @Override
    public BlobEntityInfo findBlobEntityInfoById(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityInfoById [{}]", blobEntityId);
        validateId(blobEntityId, id -> INCORRECT_BLOB_ENTITY_ID + id);
        return blobEntityInfoDao.findById(tenantId, blobEntityId.getId());
    }

    @Override
    public BlobEntityWithCustomerInfo findBlobEntityWithCustomerInfoById(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityWithCustomerInfoById [{}]", blobEntityId);
        validateId(blobEntityId, id -> INCORRECT_BLOB_ENTITY_ID + id);
        return blobEntityInfoDao.findBlobEntityWithCustomerInfoById(tenantId.getId(), blobEntityId.getId());
    }

    @Override
    public ListenableFuture<BlobEntityInfo> findBlobEntityInfoByIdAsync(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing findBlobEntityInfoByIdAsync [{}]", blobEntityId);
        validateId(blobEntityId, id -> INCORRECT_BLOB_ENTITY_ID + id);
        return blobEntityInfoDao.findByIdAsync(tenantId, blobEntityId.getId());
    }

    @Override
    public ListenableFuture<List<BlobEntityInfo>> findBlobEntityInfoByIdsAsync(TenantId tenantId, List<BlobEntityId> blobEntityIds) {
        log.trace("Executing findBlobEntityInfoByIdsAsync, tenantId [{}], blobEntityIds [{}]", tenantId, blobEntityIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(blobEntityIds, ids -> "Incorrect blobEntityIds " + ids);
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(blobEntityIds));
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantId(TenantId tenantId, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndType(TenantId tenantId, String type, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<BlobEntityWithCustomerInfo> findBlobEntitiesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TimePageLink pageLink) {
        return blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public BlobEntity saveBlobEntity(BlobEntity blobEntity) {
        log.trace("Executing saveBlobEntity [{}]", blobEntity);
        blobEntityValidator.validate(blobEntity, BlobEntity::getTenantId);
        BlobEntity savedBlobEntity = blobEntityDao.save(blobEntity.getTenantId(), blobEntity);
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(savedBlobEntity.getTenantId())
                .entityId(savedBlobEntity.getId())
                .entity(savedBlobEntity)
                .created(blobEntity.getId() == null)
                .build());
        return savedBlobEntity;
    }

    @Override
    public void deleteBlobEntity(TenantId tenantId, BlobEntityId blobEntityId) {
        log.trace("Executing deleteBlobEntity [{}]", blobEntityId);
        validateId(blobEntityId, id -> INCORRECT_BLOB_ENTITY_ID + id);
        blobEntityDao.removeById(tenantId, blobEntityId.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(blobEntityId).build());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteBlobEntity(tenantId, (BlobEntityId) id);
    }

    @Override
    public void deleteBlobEntitiesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteBlobEntitiesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantBlobEntitiesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteBlobEntitiesByTenantId(tenantId);
    }

    @Override
    public void deleteBlobEntitiesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteBlobEntitiesByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerBlobEntitiesRemover.removeEntities(tenantId, customerId);
    }

    private final TimePaginatedRemover<TenantId, BlobEntityWithCustomerInfo> tenantBlobEntitiesRemover = new TimePaginatedRemover<>() {

        @Override
        protected PageData<BlobEntityWithCustomerInfo> findEntities(TenantId tenantId, TenantId id, TimePageLink pageLink) {
            return blobEntityInfoDao.findBlobEntitiesByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, BlobEntityWithCustomerInfo entity) {
            deleteBlobEntity(tenantId, new BlobEntityId(entity.getId().getId()));
        }

    };

    private final TimePaginatedRemover<CustomerId, BlobEntityWithCustomerInfo> customerBlobEntitiesRemover = new TimePaginatedRemover<>() {

        @Override
        protected PageData<BlobEntityWithCustomerInfo> findEntities(TenantId tenantId, CustomerId customerId, TimePageLink pageLink) {
            return blobEntityInfoDao.findBlobEntitiesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, BlobEntityWithCustomerInfo entity) {
            deleteBlobEntity(tenantId, new BlobEntityId(entity.getId().getId()));
        }

    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findBlobEntityById(tenantId, new BlobEntityId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(blobEntityDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.BLOB_ENTITY;
    }

}
