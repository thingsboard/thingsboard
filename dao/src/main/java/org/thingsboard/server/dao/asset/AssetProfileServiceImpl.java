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
package org.thingsboard.server.dao.asset;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.asset.AssetProfileInfo;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service("AssetProfileDaoService")
@Slf4j
public class AssetProfileServiceImpl extends CachedVersionedEntityService<AssetProfileCacheKey, AssetProfile, AssetProfileEvictEvent> implements AssetProfileService {

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

    @Autowired
    private ImageService imageService;

    @TransactionalEventListener(classes = AssetProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(AssetProfileEvictEvent event) {
        List<AssetProfileCacheKey> toEvict = new ArrayList<>(2);
        toEvict.add(AssetProfileCacheKey.forName(event.getTenantId(), event.getNewName()));
        if (event.getSavedAssetProfile() != null) {
            cache.put(AssetProfileCacheKey.forId(event.getSavedAssetProfile().getId()), event.getSavedAssetProfile());
        } else if (event.getAssetProfileId() != null) {
            toEvict.add(AssetProfileCacheKey.forId(event.getAssetProfileId()));
        }
        if (event.isDefaultProfile()) {
            toEvict.add(AssetProfileCacheKey.forDefaultProfile(event.getTenantId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            toEvict.add(AssetProfileCacheKey.forName(event.getTenantId(), event.getOldName()));
        }
        cache.evict(toEvict);
    }

    @Override
    public AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId) {
        return findAssetProfileById(tenantId, assetProfileId, true);
    }

    @Override
    public AssetProfile findAssetProfileById(TenantId tenantId, AssetProfileId assetProfileId, boolean putInCache) {
        log.trace("Executing findAssetProfileById [{}]", assetProfileId);
        Validator.validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
        return cache.get(AssetProfileCacheKey.forId(assetProfileId),
                () -> assetProfileDao.findById(tenantId, assetProfileId.getId()), putInCache);
    }

    @Override
    public AssetProfile findAssetProfileByName(TenantId tenantId, String profileName) {
        return findAssetProfileByName(tenantId, profileName, true);
    }

    @Override
    public AssetProfile findAssetProfileByName(TenantId tenantId, String profileName, boolean putInCache) {
        log.trace("Executing findAssetProfileByName [{}][{}]", tenantId, profileName);
        Validator.validateString(profileName, s -> INCORRECT_ASSET_PROFILE_NAME + s);
        return cache.getOrFetchFromDB(AssetProfileCacheKey.forName(tenantId, profileName),
                () -> assetProfileDao.findByName(tenantId, profileName), false, putInCache);
    }

    @Override
    public AssetProfileInfo findAssetProfileInfoById(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing findAssetProfileInfoById [{}]", assetProfileId);
        Validator.validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
        return toAssetProfileInfo(findAssetProfileById(tenantId, assetProfileId));
    }

    @Override
    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        return saveAssetProfile(assetProfile, true, true);
    }

    @Override
    public AssetProfile saveAssetProfile(AssetProfile assetProfile, boolean doValidate, boolean publishSaveEvent) {
        log.trace("Executing saveAssetProfile [{}]", assetProfile);
        AssetProfile oldAssetProfile = null;
        if (doValidate) {
            oldAssetProfile = assetProfileValidator.validate(assetProfile, AssetProfile::getTenantId);
        } else if (assetProfile.getId() != null) {
            oldAssetProfile = findAssetProfileById(assetProfile.getTenantId(), assetProfile.getId(), false);
        }
        AssetProfile savedAssetProfile;
        try {
            imageService.replaceBase64WithImageUrl(assetProfile, "asset profile");
            savedAssetProfile = assetProfileDao.saveAndFlush(assetProfile.getTenantId(), assetProfile);

            publishEvictEvent(new AssetProfileEvictEvent(savedAssetProfile.getTenantId(), savedAssetProfile.getName(),
                    oldAssetProfile != null ? oldAssetProfile.getName() : null, savedAssetProfile.getId(), savedAssetProfile.isDefault(), savedAssetProfile));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedAssetProfile.getTenantId()).entity(savedAssetProfile)
                    .entityId(savedAssetProfile.getId()).created(oldAssetProfile == null).broadcastEvent(publishSaveEvent).build());
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
        Validator.validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
        deleteEntity(tenantId, assetProfileId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        AssetProfile assetProfile = assetProfileDao.findById(tenantId, id.getId());
        if (assetProfile == null) {
            return;
        }
        if (!force && assetProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Asset Profile is prohibited!");
        }
        removeAssetProfile(tenantId, assetProfile);
    }

    private void removeAssetProfile(TenantId tenantId, AssetProfile assetProfile) {
        AssetProfileId assetProfileId = assetProfile.getId();
        try {
            assetProfileDao.removeById(tenantId, assetProfileId.getId());
            publishEvictEvent(new AssetProfileEvictEvent(assetProfile.getTenantId(), assetProfile.getName(),
                    null, assetProfile.getId(), assetProfile.isDefault()));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(assetProfileId).build());
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
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return assetProfileDao.findAssetProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<AssetProfileInfo> findAssetProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAssetProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return assetProfileDao.findAssetProfileInfos(tenantId, pageLink);
    }

    @Override
    public AssetProfile findOrCreateAssetProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateAssetProfile");
        AssetProfile assetProfile = findAssetProfileByName(tenantId, name, false);
        if (assetProfile == null) {
            boolean isDefault = "default".equals(name) && findDefaultAssetProfile(tenantId) == null;
            try {
                assetProfile = this.doCreateAssetProfile(tenantId, name, isDefault, true);
            } catch (DataValidationException e) {
                if (ASSET_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                    assetProfile = findAssetProfileByName(tenantId, name, false);
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
        return doCreateAssetProfile(tenantId, "default", true, false);
    }

    private AssetProfile doCreateAssetProfile(TenantId tenantId, String profileName, boolean defaultProfile, boolean publishSaveEvent) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setTenantId(tenantId);
        assetProfile.setDefault(defaultProfile);
        assetProfile.setName(profileName);
        assetProfile.setDescription("Default asset profile");
        return saveAssetProfile(assetProfile, true, publishSaveEvent);
    }

    @Override
    public AssetProfile findDefaultAssetProfile(TenantId tenantId) {
        log.trace("Executing findDefaultAssetProfile tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(AssetProfileCacheKey.forDefaultProfile(tenantId),
                () -> assetProfileDao.findDefaultAssetProfile(tenantId), true);
    }

    @Override
    public AssetProfileInfo findDefaultAssetProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultAssetProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return toAssetProfileInfo(findDefaultAssetProfile(tenantId));
    }

    @Override
    public boolean setDefaultAssetProfile(TenantId tenantId, AssetProfileId assetProfileId) {
        log.trace("Executing setDefaultAssetProfile [{}]", assetProfileId);
        Validator.validateId(assetProfileId, id -> INCORRECT_ASSET_PROFILE_ID + id);
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
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantAssetProfilesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteAssetProfilesByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findAssetProfileById(tenantId, new AssetProfileId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(assetProfileDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ASSET_PROFILE;
    }

    @Override
    public List<EntityInfo> findAssetProfileNamesByTenantId(TenantId tenantId, boolean activeOnly) {
        log.trace("Executing findAssetProfileNamesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return assetProfileDao.findTenantAssetProfileNames(tenantId.getId(), activeOnly)
                .stream().sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());
    }

    @Override
    public ListenableFuture<List<AssetProfileInfo>> findAssetProfilesByIdsAsync(TenantId tenantId, List<AssetProfileId> assetProfileIds) {
        log.trace("Executing findAssetProfilesByIdsAsync, tenantId [{}], assetProfileIds [{}]", tenantId, assetProfileIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(assetProfileIds, ids -> "Incorrect assetProfileIds " + ids);
        return assetProfileDao.findAssetProfilesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(assetProfileIds));
    }

    private final PaginatedRemover<TenantId, AssetProfile> tenantAssetProfilesRemover =
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

}
