/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.asset;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.edqs.fields.AssetProfileFields;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.model.sql.AssetProfileEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaAssetProfileDao extends JpaAbstractDao<AssetProfileEntity, AssetProfile> implements AssetProfileDao, TenantEntityDao<AssetProfile> {

    @Autowired
    private AssetProfileRepository assetProfileRepository;

    @Override
    protected Class<AssetProfileEntity> getEntityClass() {
        return AssetProfileEntity.class;
    }

    @Override
    protected JpaRepository<AssetProfileEntity, UUID> getRepository() {
        return assetProfileRepository;
    }

    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, UUID assetProfileId) {
        return assetProfileRepository.findAssetProfileInfoById(assetProfileId);
    }

    @Override
    public PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(
                assetProfileRepository.findAssetProfiles(
                        tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                assetProfileRepository.findAssetProfileInfos(
                        tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        return DaoUtil.getData(assetProfileRepository.findByDefaultTrueAndTenantId(tenantId.getId()));
    }

    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        return assetProfileRepository.findDefaultAssetProfileInfo(tenantId.getId());
    }

    @Override
    public AssetProfile findByName(TenantId tenantId, String profileName) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId.getId(), profileName));
    }

    @Override
    public PageData<AssetProfile> findAllWithImages(PageLink pageLink) {
        return DaoUtil.toPageData(assetProfileRepository.findAllByImageNotNull(DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<EntityInfo> findTenantAssetProfileNames(UUID tenantId, boolean activeOnly) {
        return activeOnly ?
                assetProfileRepository.findActiveTenantAssetProfileNames(tenantId) :
                assetProfileRepository.findAllTenantAssetProfileNames(tenantId);
    }

    @Override
    public ListenableFuture<List<AssetProfileInfo>> findAssetProfilesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> assetProfileIds) {
        return service.submit(() -> assetProfileRepository.findAssetProfileInfosByTenantIdAndIdIn(tenantId, assetProfileIds));
    }

    @Override
    public AssetProfile findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public AssetProfile findByTenantIdAndName(UUID tenantId, String name) {
        return DaoUtil.getData(assetProfileRepository.findByTenantIdAndName(tenantId, name));
    }

    @Override
    public PageData<AssetProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return findAssetProfiles(TenantId.fromUUID(tenantId), pageLink);
    }

    @Override
    public AssetProfileId getExternalIdByInternal(AssetProfileId internalId) {
        return Optional.ofNullable(assetProfileRepository.getExternalIdById(internalId.getId()))
                .map(AssetProfileId::new).orElse(null);
    }

    @Override
    public AssetProfile findDefaultEntityByTenantId(UUID tenantId) {
        return findDefaultAssetProfile(TenantId.fromUUID(tenantId));
    }

    @Override
    public List<AssetProfileInfo> findByTenantAndImageLink(TenantId tenantId, String imageLink, int limit) {
        return assetProfileRepository.findByTenantAndImageLink(tenantId.getId(), imageLink, PageRequest.of(0, limit));
    }

    @Override
    public List<AssetProfileInfo> findByImageLink(String imageLink, int limit) {
        return assetProfileRepository.findByImageLink(imageLink, PageRequest.of(0, limit));
    }

    @Override
    public PageData<AssetProfile> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<AssetProfileFields> findNextBatch(UUID id, int batchSize) {
        return assetProfileRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

}
