/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.query;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityNameOrIdFilter;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.data.search.EntitiesSearchRequest;
import org.thingsboard.server.data.search.EntitySearchResult;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.EntityType.ASSET;
import static org.thingsboard.server.common.data.EntityType.CUSTOMER;
import static org.thingsboard.server.common.data.EntityType.DASHBOARD;
import static org.thingsboard.server.common.data.EntityType.DEVICE;
import static org.thingsboard.server.common.data.EntityType.DEVICE_PROFILE;
import static org.thingsboard.server.common.data.EntityType.EDGE;
import static org.thingsboard.server.common.data.EntityType.ENTITY_VIEW;
import static org.thingsboard.server.common.data.EntityType.OTA_PACKAGE;
import static org.thingsboard.server.common.data.EntityType.RULE_CHAIN;
import static org.thingsboard.server.common.data.EntityType.TB_RESOURCE;
import static org.thingsboard.server.common.data.EntityType.TENANT;
import static org.thingsboard.server.common.data.EntityType.TENANT_PROFILE;
import static org.thingsboard.server.common.data.EntityType.USER;
import static org.thingsboard.server.common.data.EntityType.WIDGETS_BUNDLE;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CUSTOMER_ID;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.LAST_ACTIVITY_TIME;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.NAME;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.TENANT_ID;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.TYPE;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class EntitiesSearchServiceImpl implements EntitiesSearchService {
    private final EntityQueryService entityQueryService;

    private final TenantService tenantService;
    private final CustomerService customerService;
    private final DefaultDeviceStateService deviceStateService;

    private static final List<EntityKey> entityResponseFields = Stream.of(CREATED_TIME, NAME, TYPE, TENANT_ID, CUSTOMER_ID)
            .map(field -> new EntityKey(EntityKeyType.ENTITY_FIELD, field))
            .collect(Collectors.toList());

    private static final Set<EntityType> searchableEntityTypes = EnumSet.of(
            TENANT, CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, RULE_CHAIN, ENTITY_VIEW,
            WIDGETS_BUNDLE, TENANT_PROFILE, DEVICE_PROFILE, TB_RESOURCE, OTA_PACKAGE, EDGE
    );

    @Override
    public PageData<EntitySearchResult> searchEntities(SecurityUser user, EntitiesSearchRequest request, PageLink pageLink) {
        EntityType entityType = request.getEntityType();
        if (!searchableEntityTypes.contains(entityType)) {
            return new PageData<>();
        }

        EntityDataQuery query = createSearchQuery(request.getSearchQuery(), entityType, pageLink);
        PageData<EntityData> resultPage = entityQueryService.findEntityDataByQuery(user, query);

        Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache = new HashMap<>();
        return resultPage.mapData(entityData -> {
            Map<String, String> fields = new HashMap<>();
            entityData.getLatest().values().stream()
                    .flatMap(values -> values.entrySet().stream())
                    .forEach(entry -> fields.put(entry.getKey(), Strings.emptyToNull(entry.getValue().getValue())));

            EntitySearchResult entitySearchResult = new EntitySearchResult();

            entitySearchResult.setId(entityData.getEntityId());
            entitySearchResult.setName(fields.get(NAME));
            entitySearchResult.setType(fields.get(TYPE));
            entitySearchResult.setCreatedTime(Optional.ofNullable(fields.get(CREATED_TIME)).map(Long::parseLong).orElse(null));
            entitySearchResult.setLastActivityTime(Optional.ofNullable(fields.get(LAST_ACTIVITY_TIME)).map(Long::parseLong).orElse(null));
            setOwnerInfo(entitySearchResult, fields.get(TENANT_ID), fields.get(CUSTOMER_ID), localOwnersCache);

            return entitySearchResult;
        });
    }

    private EntityDataQuery createSearchQuery(String searchQuery, EntityType entityType, PageLink pageLink) {
        EntityDataPageLink entityDataPageLink = new EntityDataPageLink();
        entityDataPageLink.setPageSize(pageLink.getPageSize());
        entityDataPageLink.setPage(pageLink.getPage());
        if (pageLink.getSortOrder() != null && StringUtils.isNotEmpty(pageLink.getSortOrder().getProperty())) {
            entityDataPageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, pageLink.getSortOrder().getProperty()),
                    EntityDataSortOrder.Direction.valueOf(Optional.ofNullable(pageLink.getSortOrder().getDirection()).orElse(SortOrder.Direction.ASC).name())));
        }

        EntityNameOrIdFilter filter = new EntityNameOrIdFilter();
        filter.setEntityType(entityType);
        filter.setNameOrId(searchQuery);

        List<EntityKey> entityFields = entityResponseFields;
        List<EntityKey> latestValues = Collections.emptyList();

        if (entityType == USER) {
            entityFields = new ArrayList<>(entityFields);
            entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, LAST_ACTIVITY_TIME));
        } else if (entityType == DEVICE) {
            EntityKey lastActivityTimeKey;
            if (deviceStateService.isPersistToTelemetry()) {
                lastActivityTimeKey = new EntityKey(EntityKeyType.TIME_SERIES, LAST_ACTIVITY_TIME);
            } else {
                lastActivityTimeKey = new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_ACTIVITY_TIME);
            }
            latestValues = List.of(lastActivityTimeKey);
            if (entityDataPageLink.getSortOrder() != null && entityDataPageLink.getSortOrder().getKey().getKey().equals(LAST_ACTIVITY_TIME)) {
                entityDataPageLink.getSortOrder().setKey(lastActivityTimeKey);
            }
        }

        return new EntityDataQuery(filter, entityDataPageLink, entityFields, latestValues, Collections.emptyList());
    }

    private void setOwnerInfo(EntitySearchResult result, String tenantIdStr, String customerIdStr, Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache) {
        UUID tenantUuid = toUuid(tenantIdStr);
        UUID customerUuid = toUuid(customerIdStr);

        Tenant tenant = null;
        if (tenantUuid != null) {
            tenant = getTenant(new TenantId(tenantUuid), localOwnersCache);
        }

        ContactBased<? extends EntityId> owner;
        if (customerUuid != null) {
            owner = getCustomer(new CustomerId(customerUuid), localOwnersCache);
        } else {
            owner = tenant;
        }

        if (tenant != null) {
            result.setTenantInfo(new EntitySearchResult.EntityTenantInfo(tenant.getId(), tenant.getName()));
        }
        if (owner != null) {
            result.setOwnerInfo(new EntitySearchResult.EntityOwnerInfo(owner.getId(), owner.getName()));
        }
    }

    private Tenant getTenant(TenantId tenantId, Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache) {
        return (Tenant) localOwnersCache.computeIfAbsent(tenantId, id -> tenantService.findTenantById(tenantId));
    }

    private Customer getCustomer(CustomerId customerId, Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache) {
        return (Customer) localOwnersCache.computeIfAbsent(customerId, id -> customerService.findCustomerById(TenantId.SYS_TENANT_ID, customerId));
    }

    private UUID toUuid(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            if (!id.equals(EntityId.NULL_UUID)) {
                return id;
            }
        } catch (Exception ignored) {}

        return null;
    }

}
