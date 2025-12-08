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
package org.thingsboard.server.dao.tenant;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.mobile.QrCodeSettingService;
import org.thingsboard.server.dao.notification.NotificationSettingsService;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.TenantDataValidator;
import org.thingsboard.server.dao.trendz.TrendzSettingsService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.dao.user.UserService;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;

@Service("TenantDaoService")
@Slf4j
public class TenantServiceImpl extends AbstractCachedEntityService<TenantId, Tenant, TenantEvictEvent> implements TenantService {

    private static final String DEFAULT_TENANT_REGION = "Global";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private TenantProfileService tenantProfileService;
    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    private AssetProfileService assetProfileService;
    @Autowired
    private DeviceProfileService deviceProfileService;
    @Lazy
    @Autowired
    private ApiUsageStateService apiUsageStateService;
    @Autowired
    private NotificationSettingsService notificationSettingsService;
    @Autowired
    private QrCodeSettingService qrCodeSettingService;
    @Autowired
    private TrendzSettingsService trendzSettingsService;
    @Autowired
    private TenantDataValidator tenantValidator;
    @Autowired
    protected TbTransactionalCache<TenantId, Boolean> existsTenantCache;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(TenantEvictEvent event) {
        TenantId tenantId = event.getTenantId();
        cache.evict(tenantId);
        if (event.isInvalidateExists()) {
            existsTenantCache.evict(tenantId);
        }
    }

    @Override
    public Tenant findTenantById(TenantId tenantId) {
        log.trace("Executing findTenantById [{}]", tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);

        return cache.getAndPutInTransaction(tenantId, () -> tenantDao.findById(tenantId, tenantId.getId()), true);
    }

    @Override
    public TenantInfo findTenantInfoById(TenantId tenantId) {
        log.trace("Executing findTenantInfoById [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return tenantDao.findTenantInfoById(tenantId, tenantId.getId());
    }

    @Override
    public ListenableFuture<Tenant> findTenantByIdAsync(TenantId callerId, TenantId tenantId) {
        log.trace("Executing findTenantByIdAsync [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return tenantDao.findByIdAsync(callerId, tenantId.getId());
    }

    @Override
    @Transactional
    public Tenant saveTenant(Tenant tenant) {
        return saveTenant(tenant, null);
    }

    @Override
    @Transactional
    public Tenant saveTenant(Tenant tenant, Consumer<TenantId> defaultEntitiesCreator) {
        log.trace("Executing saveTenant [{}]", tenant);
        tenant.setRegion(DEFAULT_TENANT_REGION);
        if (tenant.getTenantProfileId() == null) {
            TenantProfile tenantProfile = this.tenantProfileService.findOrCreateDefaultTenantProfile(TenantId.SYS_TENANT_ID);
            tenant.setTenantProfileId(tenantProfile.getId());
        }
        tenantValidator.validate(tenant, Tenant::getId);
        boolean create = tenant.getId() == null;

        Tenant savedTenant = tenantDao.save(tenant.getId(), tenant);
        TenantId tenantId = savedTenant.getId();
        publishEvictEvent(new TenantEvictEvent(tenantId, create));

        if (create && defaultEntitiesCreator != null) {
            defaultEntitiesCreator.accept(tenantId);
        }

        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(tenantId).entity(savedTenant).created(create).build());

        if (create) {
            deviceProfileService.createDefaultDeviceProfile(tenantId);
            assetProfileService.createDefaultAssetProfile(tenantId);
            apiUsageStateService.createDefaultApiUsageState(tenantId, null);
            notificationSettingsService.createDefaultNotificationConfigs(tenantId);
        }

        return savedTenant;
    }

    @Override
    public void deleteTenant(TenantId tenantId) {
        log.trace("Executing deleteTenant [{}]", tenantId);
        Tenant tenant = findTenantById(tenantId);
        Validator.validateId(tenantId, id -> INCORRECT_TENANT_ID + id);

        userService.deleteAllByTenantId(tenantId);
        notificationSettingsService.deleteNotificationSettings(tenantId);
        trendzSettingsService.deleteTrendzSettings(tenantId);
        qrCodeSettingService.deleteByTenantId(tenantId);

        tenantDao.removeById(tenantId, tenantId.getId());
        publishEvictEvent(new TenantEvictEvent(tenantId, true));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(tenantId).entity(tenant).build());

        cleanUpService.removeTenantEntities(tenantId, // don't forget to implement deleteEntity from EntityDaoService when adding entity type to this list
                EntityType.ADMIN_SETTINGS, EntityType.JOB, EntityType.ENTITY_VIEW, EntityType.WIDGETS_BUNDLE, EntityType.WIDGET_TYPE,
                EntityType.ASSET, EntityType.ASSET_PROFILE, EntityType.DEVICE, EntityType.DEVICE_PROFILE,
                EntityType.DASHBOARD, EntityType.EDGE, EntityType.RULE_CHAIN, EntityType.API_USAGE_STATE,
                EntityType.TB_RESOURCE, EntityType.OTA_PACKAGE, EntityType.RPC, EntityType.QUEUE,
                EntityType.NOTIFICATION_REQUEST, EntityType.NOTIFICATION_RULE, EntityType.NOTIFICATION_TEMPLATE,
                EntityType.NOTIFICATION_TARGET, EntityType.QUEUE_STATS, EntityType.CUSTOMER,
                EntityType.DOMAIN, EntityType.MOBILE_APP_BUNDLE, EntityType.MOBILE_APP, EntityType.OAUTH2_CLIENT,
                EntityType.AI_MODEL
        );
    }

    @Override
    public PageData<Tenant> findTenants(PageLink pageLink) {
        log.trace("Executing findTenants pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenants(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    public PageData<TenantInfo> findTenantInfos(PageLink pageLink) {
        log.trace("Executing findTenantInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantInfos(TenantId.SYS_TENANT_ID, pageLink);
    }

    @Override
    public List<TenantId> findTenantIdsByTenantProfileId(TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantsByTenantProfileId [{}]", tenantProfileId);
        return tenantDao.findTenantIdsByTenantProfileId(tenantProfileId);
    }

    @Override
    public Tenant findTenantByName(String name) {
        log.trace("Executing findTenantByName [{}]", name);
        return tenantDao.findTenantByName(TenantId.SYS_TENANT_ID, name);
    }

    @Override
    public void deleteTenants() {
        log.trace("Executing deleteTenants");
        tenantsRemover.removeEntities(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID);
    }

    @Override
    public PageData<TenantId> findTenantsIds(PageLink pageLink) {
        log.trace("Executing findTenantsIds");
        Validator.validatePageLink(pageLink);
        return tenantDao.findTenantsIds(pageLink);
    }

    @Override
    public List<Tenant> findTenantsByIds(TenantId callerId, List<TenantId> tenantIds) {
        log.trace("Executing findTenantsByIds, callerId [{}], tenantIds [{}]", callerId, tenantIds);
        return tenantDao.findTenantsByIds(callerId.getId(), toUUIDs(tenantIds));
    }

    @Override
    public boolean tenantExists(TenantId tenantId) {
        return existsTenantCache.getAndPutInTransaction(tenantId, () -> tenantDao.existsById(tenantId, tenantId.getId()), false);
    }

    private final PaginatedRemover<TenantId, Tenant> tenantsRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Tenant> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return tenantDao.findTenants(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Tenant entity) {
            deleteTenant(TenantId.fromUUID(entity.getUuidId()));
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findTenantById(TenantId.fromUUID(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(findTenantByIdAsync(tenantId, TenantId.fromUUID(entityId.getId())))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT;
    }

}
