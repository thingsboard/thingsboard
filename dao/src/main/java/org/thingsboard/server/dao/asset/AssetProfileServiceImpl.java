/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.asset;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteDaoEvent;
import org.thingsboard.server.dao.eventsourcing.SaveDaoEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("AssetProfileDaoService")
@Slf4j
public class AssetProfileServiceImpl extends AbstractCachedEntityService<AssetProfileCacheKey, AssetProfile, AssetProfileEvictEvent> implements AssetProfileService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    private static final String INCORRECT_ASSET_PROFILE_ID = "Incorrect assetProfileId ";

    private static final String INCORRECT_ASSET_PROFILE_NAME = "Incorrect assetProfileName ";

    private static final String ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Asset profile with such name already exists!";

    @Autowired
    private AssetProfileDao assetProfileDao;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DataValidator<AssetProfile> assetProfileValidator;

    @TransactionalEventListener(classes = AssetProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(AssetProfileEvictEvent event) {
        List<AssetProfileCacheKey> keys = new ArrayList<>(2);
        keys.add(AssetProfileCacheKey.fromName(event.getTenantId(), event.getNewName()));
        if (event.getAssetProfileId() != null) {
            keys.add(AssetProfileCacheKey.fromId(event.getAssetProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(AssetProfileCacheKey.defaultProfile(event.getTenantId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(AssetProfileCacheKey.fromName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    @Override
    public AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing findAssetProfileById [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        return cache.getAndPutInTransaction(AssetProfileCacheKey.fromId(assetProfileId),
                () -> assetProfileDao.findById(tenantId, assetProfileId.getId()), true);
    }

    @Override
    public AssetProfile findAssetProfileByName(TenantId tenantId, String profileName) {
        log.trace("Executing findAssetProfileByName [{}][{}]", tenantId, profileName);
        Validator.validateString(profileName, INCORRECT_ASSET_PROFILE_NAME + profileName);
        return cache.getAndPutInTransaction(AssetProfileCacheKey.fromName(tenantId, profileName),
                () -> assetProfileDao.findByName(tenantId, profileName), false);
    }

    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing findAssetProfileInfoById [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        return toAssetProfileInfo(findAssetProfileById(tenantId, assetProfileId));
    }

    @Override
    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        log.trace("Executing saveAssetProfile [{}]", assetProfile);
        AssetProfile oldAssetProfile = assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
        AssetProfile savedAssetProfile;
        try {
            savedAssetProfile = assetProfileDao.saveAndFlush(assetProfile.getTenantId(), assetProfile);
            publishEvictEvent(new AssetProfileEvictEvent(savedAssetProfile.getTenantId(), savedAssetProfile.getName(),
                    oldAssetProfile != null ? oldAssetProfile.getName() : null, savedAssetProfile.getId(), savedAssetProfile.isDefault()));
            eventPublisher.publishEvent(SaveDaoEvent.builder().tenantId(savedAssetProfile.getTenantId()).entityId(savedAssetProfile.getId())
                    .actionType(oldAssetProfile == null ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED).build());
        } catch (Exception t) {
            handleEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(),
                    oldAssetProfile != null ? oldAssetProfile.getName() : null, null, assetProfile.isDefault()));
            checkConstraintViolation(t,
                    Map.of("asset_profile_name_unq_key", ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS,
                            "asset_profile_external_id_unq_key", "Asset profile with such external id already exists!"));
            throw t;
        }
        if (oldAssetProfile != null && !oldAssetProfile.getName().equals(assetProfile.getName())) {
            PageLink pageLink = new PageLink(100);
            PageData<Asset> pageData;
            do {
                pageData = assetDao.findAssetsByTenantIdAndProfileId(assetProfile.getTenantId().getId(), assetProfile.getUuidId(), pageLink);
                for (Asset asset : pageData.getData()) {
                    asset.setType(assetProfile.getName());
                    assetService.saveAsset(asset);
                }
                pageLink = pageLink.nextPageLink();
            } while (pageData.hasNext());
        }
        return savedAssetProfile;
    }

    @Override
    @Transactional
    public void deleteAssetProfile(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing deleteAssetProfile [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        AssetProfile assetProfile = assetProfileDao.findById(tenantId, assetProfileId.getId());
        if (assetProfile != null && assetProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Asset Profile is prohibited!");
        }
        this.removeAssetProfile(tenantId, assetProfile);
    }

    private void removeAssetProfile(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfileId assetProfileId = assetProfile.getId();
        try {
            deleteEntityRelations(tenantId, assetProfileId);
            assetProfileDao.removeById(tenantId, assetProfileId.getId());
            publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(),
                    null, assetProfile.getId(), assetProfile.isDefault()));
            eventPublisher.publishEvent(DeleteDaoEvent.builder().tenantId(tenantId).entityId(assetProfileId).build());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_asset_profile")) {
                throw new DataValidationException("The asset profile referenced by the assets cannot be deleted!");
            } else {
                throw t;
            }
        }
    }

    @Override
    public PageData<AssetProfile> findAssetProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return assetProfileDao.findAssetProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return assetProfileDao.findAssetProfileInfos(tenantId, pageLink);
    }

    @Override
    public AssetProfile findOrCreateAssetProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateAssetProfile");
        AssetProfile assetProfile = findAssetProfileByName(tenantId, name);
        if (assetProfile == null) {
            try {
                assetProfile = this.doCreateDefaultAssetProfile(tenantId, name, name.equals("default"));
            } catch (DataValidationException e) {
                if (ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                    assetProfile = findAssetProfileByName(tenantId, name);
                } else {
                    throw e;
                }
            }
        }
        return assetProfile;
    }

    @Override
    public AssetProfile createDefaultAssetProfile(TenantId tenantId) {
        log.trace("Executing createDefaultAssetProfile tenantId [{}]", tenantId);
        return doCreateDefaultAssetProfile(tenantId, "default", true);
    }

    private AssetProfile doCreateDefaultAssetProfile(TenantId tenantId, String profileName, boolean defaultProfile) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setDefault(defaultProfile);
        assetProfile.setName(profileName);
        assetProfile.setDescription("Default asset profile");
        return saveAssetProfile(assetProfile);
    }

    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        log.trace("Executing findDefaultAssetProfile tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(AssetProfileCacheKey.defaultProfile(tenantId),
                () -> assetProfileDao.findDefaultAssetProfile(tenantId), true);
    }

    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultAssetProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return toAssetProfileInfo(findDefaultAssetProfile(tenantId));
    }

    @Override
    public boolean setDefaultAssetProfile(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing setDefaultAssetProfile [{}]", assetProfileId);
        Validator.validateId(assetProfileId, INCORRECT_ASSET_PROFILE_ID + assetProfileId);
        AssetProfile assetProfile = assetProfileDao.findById(tenantId, assetProfileId.getId());
        if (!assetProfile.isDefault()) {
            assetProfile.setDefault(true);
            AssetProfile previousDefaultAssetProfile = findDefaultAssetProfile(tenantId);
            boolean changed = false;
            if (previousDefaultAssetProfile == null) {
                assetProfileDao.save(tenantId, assetProfile);
                publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(), null, assetProfile.getId(), true));
                changed = true;
            } else if (!previousDefaultAssetProfile.getId().equals(assetProfile.getId())) {
                previousDefaultAssetProfile.setDefault(false);
                assetProfileDao.save(tenantId, previousDefaultAssetProfile);
                assetProfileDao.save(tenantId, assetProfile);
                publishEvictEvent(new AssetProfileEvictEvent(previousDefaultAssetProfile.getTenantId(), previousDefaultAssetProfile.getName(), null, previousDefaultAssetProfile.getId(), false));
                publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(), null, assetProfile.getId(), true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    @Override
    public void deleteAssetProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteAssetProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantAssetProfilesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAssetProfileById(tenantId, new AssetProfileId(entityId.getId())));
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteAssetProfile(tenantId, (AssetProfileId) id);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

    private PaginatedRemover<TenantId, AssetProfile> tenantAssetProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<AssetProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return assetProfileDao.findAssetProfiles(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, AssetProfile entity) {
                    removeAssetProfile(tenantId, entity);
                }
            };

    private AssetProfileInfo toAssetProfileInfo(AssetProfile profile) {
        return profile == null ? null : new AssetProfileInfo(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(),
                profile.getDefaultDashboardId());
    }

    private void publishAssetProfileDelete(TenantId tenantId, AssetProfileId assetProfileId) {
        List<EdgeId> relatedEdgeIds = edgeService.findAllRelatedEdgeIds(tenantId, assetProfileId);
        if (relatedEdgeIds != null && !relatedEdgeIds.isEmpty()) {
        }
    }

}
