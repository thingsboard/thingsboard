// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.custommenu;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.CustomMenuDeleteResult;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CMAssigneeType;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.dao.menu.CustomMenuCacheEvictEvent;
import org.thingsboard.server.dao.menu.CustomMenuCacheKey;
import org.thingsboard.server.dao.menu.CustomMenuService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.AbstractEtagCacheService;

import java.util.List;

@Service
@Slf4j
@TbCoreComponent
public class DefaultTbCustomMenuService extends AbstractEtagCacheService<CustomMenuCacheKey> implements TbCustomMenuService {

    private final CustomMenuService customMenuService;
    private final TbClusterService clusterService;


    public DefaultTbCustomMenuService(TbClusterService clusterService, CustomMenuService customMenuService,
                                      @Value("${cache.customMenu.etag.timeToLiveInMinutes:44640}") int cacheTtl,
                                      @Value("${cache.customMenu.etag.maxSize:1000000}") int cacheMaxSize) {
        super(cacheTtl, cacheMaxSize);
        this.clusterService = clusterService;
        this.customMenuService = customMenuService;
    }

    @Override
    public CustomMenu createCustomMenu(CustomMenuInfo customMenuInfo, List<EntityId> assignToList, boolean force) throws ThingsboardException {
        return customMenuService.createCustomMenu(customMenuInfo, assignToList, force);
    }

    @Override
    public CustomMenu updateCustomMenu(CustomMenu customMenu, boolean force) throws ThingsboardException {
        return customMenuService.updateCustomMenu(customMenu, force);
    }

    @Override
    public void updateAssigneeList(CustomMenu oldCustomMenu, CMAssigneeType newAssigneeType, List<EntityId> newAssignToList, String[] userGroupNames, boolean force) throws ThingsboardException {
        customMenuService.updateAssigneeList(oldCustomMenu, newAssigneeType, newAssignToList, userGroupNames, force);
    }

    @Override
    public CustomMenuDeleteResult deleteCustomMenu(CustomMenu customMenu, boolean force) {
        return customMenuService.deleteCustomMenu(customMenu, force);
    }

    @Override
    public void evictETags(CustomMenuCacheKey cacheKey) {
        if (cacheKey.getUserId() != null) {
            invalidateByFilter(key -> cacheKey.getUserId().equals(key.getUserId()));
        } else if (cacheKey.getCustomerId() != null) {
            invalidateByFilter(key -> cacheKey.getCustomerId().equals(key.getCustomerId()));
        } else if (cacheKey.getTenantId().isSysTenantId()) {
            etagCache.invalidateAll();
        } else {
            invalidateByFilter(key -> cacheKey.getTenantId().equals(key.getTenantId()));
        }
    }

    private void evictFromCache(TenantId tenantId) {
        evictETags(CustomMenuCacheKey.forTenant(tenantId));
        clusterService.broadcastToCore(TransportProtos.ToCoreNotificationMsg.newBuilder()
                .setCustomMenuCacheInvalidateMsg(TransportProtos.CustomMenuCacheInvalidateMsg.newBuilder()
                        .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                        .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                        .build())
                .build());
    }

    @TransactionalEventListener(classes = CustomMenuCacheEvictEvent.class, fallbackExecution = true)
    public void handleEvictEvent(CustomMenuCacheEvictEvent event) {
        evictFromCache(event.tenantId());
    }

}
